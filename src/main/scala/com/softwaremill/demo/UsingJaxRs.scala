package com.softwaremill.demo

import javax.ws.rs.{GET, Path, PathParam, Produces, QueryParam, WebApplicationException}
import javax.ws.rs.core.{Context, MediaType, Response}
import javax.ws.rs.ext.{ParamConverterProvider, Provider}
import javax.ws.rs.ext.Providers
import javax.ws.rs.ext.ParamConverter
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util.UUID

import javax.xml.bind.annotation.XmlRootElement

object UsingJaxRs {

  case class Year(year: Int)

  @XmlRootElement
  case class Book(id: UUID, title: String, year: Year)

  @Provider
  class YearParamConverterProvider extends ParamConverterProvider {
    @Context
    val providers: Providers = ???

    override def getConverter[T](rawType: Class[T], genericType: Type, annotations: Array[Annotation]): ParamConverter[T] = ???
  }

  // GET /books?year=...&limit=... (parameters optional) -> json list of books
  // GET /book/c5e41285-a229-419a-93f3-1a834842b352 -> json book

  class BooksService {
    @GET
    @Path("/books")
    @Produces(Array(MediaType.APPLICATION_JSON))
    def getBooks(@QueryParam("year") year: Year, @QueryParam("limit") limit: Integer): Response = {
      Response.status(Response.Status.OK).entity(???).`type`(MediaType.APPLICATION_JSON).build
    }

    @GET
    @Path("/books/{id}")
    @Produces(Array(MediaType.APPLICATION_JSON))
    def getBookById(@PathParam("id") id: UUID): Response = {
      if ( /* check if there is a book with the given id */ true) {
        Response.status(Response.Status.OK).entity(???).`type`(MediaType.APPLICATION_JSON).build
      } else {
        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(s"Book with id $id not found").build())
      }
    }
  }

}
