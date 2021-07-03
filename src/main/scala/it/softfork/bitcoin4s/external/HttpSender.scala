package it.softfork.bitcoin4s.external

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

class HttpSender(implicit system: ActorSystem) {

  def apply(request: HttpRequest): Future[HttpResponse] = {
    Http().singleRequest(request)
  }
}
