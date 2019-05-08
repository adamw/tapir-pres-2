package com.softwaremill.demo

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import tapir.model.{StatusCode, StatusCodes}
import tapir.openapi.OpenAPI
import tapir.json.circe._
import io.circe.generic.auto._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {

  case class Year(year: Int)

  case class Book(id: UUID, title: String, year: Year)

  case class ErrorInfo(statusCode: StatusCode, msg: String)

  val books = List(
    Book(UUID.randomUUID(), "Lords and Ladies", Year(1992)),
    Book(UUID.randomUUID(), "The Sorrows of Young Werther", Year(1774)),
    Book(UUID.randomUUID(), "Iliad", Year(-8000)),
    Book(UUID.randomUUID(), "Nad Niemnem", Year(1888)),
    Book(UUID.randomUUID(), "The Pelican Brief", Year(1992)),
    Book(UUID.randomUUID(), "The Art of Computer Programming", Year(1968)),
    Book(UUID.randomUUID(), "The English Patient", Year(1992)),
    Book(UUID.randomUUID(), "Pharaoh", Year(1897))
  )

  import tapir._

  object V1 {
    // GET /books?year=...&limit=... (parameters optional) -> json list of books
    val getBooksEndpoint: Endpoint[(Option[Int], Option[Int]), String, List[Book], Nothing] = endpoint
      .get
      .in("books").in(query[Option[Int]]("year")).in(query[Option[Int]]("limit"))
      .errorOut(stringBody)
      .out(jsonBody[List[Book]])

    // GET /book/c5e41285-a229-419a-93f3-1a834842b352 -> json book
    val getBookEndpoint: Endpoint[UUID, String, Book, Nothing] = endpoint
      .get
      .in("books" / path[UUID]("bookId"))
      .errorOut(stringBody)
      .out(jsonBody[Book])
  }

  object V2 {
    implicit val yearCodec: Codec[Year, MediaType.TextPlain, String] = Codec.intPlainCodec.map(Year)(_.year)

    case class BookFilter(year: Option[Year], limit: Option[Int])

    val bookFilterInput: EndpointInput[BookFilter] =
      query[Option[Year]]("fromYear")
        .and(query[Option[Int]]("limit")).mapTo(BookFilter)

    val errorInfoOutput: EndpointOutput[ErrorInfo] = statusCode.and(stringBody).mapTo(ErrorInfo)

    val baseEndpoint: Endpoint[Unit, ErrorInfo, Unit, Nothing] = endpoint
      .in("books")
      .errorOut(errorInfoOutput)

    val getBooksEndpoint: Endpoint[BookFilter, ErrorInfo, List[Book], Nothing] = baseEndpoint
      .get
      .in(bookFilterInput)
      .out(jsonBody[List[Book]])

    val getBookEndpoint: Endpoint[UUID, ErrorInfo, Book, Nothing] = baseEndpoint
      .get
      .in(path[UUID]("bookId"))
      .out(jsonBody[Book].example(books.head))
  }

  object Docs {

    import tapir.docs.openapi._
    import tapir.openapi.circe.yaml._

    import V2._

    val docs: OpenAPI = List(getBooksEndpoint, getBookEndpoint).toOpenAPI("The tapir Library", "1.0")
    val yml: String = docs.toYaml
  }

  object Server {

    import tapir.server.akkahttp._
    import V2._

    val getBooksRoute: Route = getBooksEndpoint.toRoute { filter =>
      val books1 = filter.year.map(year => books.filter(_.year == year)).getOrElse(books)
      val books2 = filter.limit.map(limit => books1.take(limit)).getOrElse(books1)
      Future(Right(books2))
    }

    val getBookRoute: Route = getBookEndpoint.toRoute { bookId =>
      books.find(_.id == bookId) match {
        case Some(book) => Future(Right(book))
        case None => Future(Left(ErrorInfo(StatusCodes.NotFound, s"Book with id $bookId not found")))
      }
    }

    def start() {
      implicit val actorSystem: ActorSystem = ActorSystem()
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      val routes: Route = getBooksRoute ~ getBookRoute ~ new SwaggerUI(Docs.yml).routes
      Await.result(Http().bindAndHandle(routes, "localhost", 8080), 1.minute)
    }
  }

  Server.start()
}
