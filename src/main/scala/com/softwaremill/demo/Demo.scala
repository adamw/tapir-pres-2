package com.softwaremill.demo

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import tapir.model.{StatusCode, StatusCodes}
import tapir.openapi.OpenAPI

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Demo extends App {

}
