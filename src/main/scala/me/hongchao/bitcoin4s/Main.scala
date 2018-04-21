package me.hongchao.bitcoin4s

import akka.http.scaladsl.Http
import scala.util.{Failure, Success}

object Main extends App with Boot {

  import system.dispatcher

  val serverFuture = Http().bindAndHandle(
    handler = routes(),
    interface = "0.0.0.0",
    port = 8888
  )

  serverFuture.onComplete {
    case Success(Http.ServerBinding(address)) =>
      logger.info(s"Bitcoin4s http server bound to $address")

    case Failure(ex) =>
      logger.error(s"Failed to bind for webhooks server", ex)
  }
}
