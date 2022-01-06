package it.softfork.bitcoin4s

import scala.util.{Failure, Success}

import akka.http.scaladsl.Http

object Main extends App with Boot {

  import system.dispatcher

  val boundedIP = "0.0.0.0"
  val boundedPort = 8888
  val serverFuture = Http().newServerAt(boundedIP, boundedPort).bind(routes())

  serverFuture.onComplete {
    case Success(Http.ServerBinding(address)) =>
      logger.info(s"Bitcoin4s http server bound to $address")

    case Failure(ex) =>
      logger.error(s"Failed to bind for webhooks server", ex)
  }
}
