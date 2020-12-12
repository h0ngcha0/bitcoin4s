package it.softfork.bitcoin4s

import akka.http.scaladsl.Http
import scala.util.{Failure, Success}

object Main extends App with Boot {

  import system.dispatcher

  val serverFuture = Http().newServerAt("0.0.0.0", 8888).bind(routes())

  serverFuture.onComplete {
    case Success(Http.ServerBinding(address)) =>
      logger.info(s"Bitcoin4s http server bound to $address")

    case Failure(ex) =>
      logger.error(s"Failed to bind for webhooks server", ex)
  }
}
