package it.softfork.bitcoin4s

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{Json, Writes}

import it.softfork.bitcoin4s.ApiModels.{InterpreterOutcome, TransactionInputNotFound}
import it.softfork.bitcoin4s.external.blockcypher.{Service => BlockCypherService}
import it.softfork.bitcoin4s.transaction.TxId

class Routes(blockcypherService: BlockCypherService) extends PlayJsonSupport {

  val clientRoute = {
    pathSingleSlash {
      getFromResource("client/index.html")
    } ~
      pathPrefix("static") {
        getFromResourceDirectory("client/static")
      }
  }

  val healthRoute = {
    (path("ping") & get) {
      complete(StatusCodes.OK)
    }
  }

  val transactionRoute = pathPrefix("transaction") {
    pathPrefix(Segment.map(TxId.apply)) { txId =>
      pathEndOrSingleSlash {
        rejectEmptyResponse {
          complete(blockcypherService.getTransaction(txId))
        }
      } ~
        pathPrefix("input") {
          pathPrefix(IntNumber) { inputIndex =>
            pathEndOrSingleSlash {
              rejectEmptyResponse {
                complete(blockcypherService.getTransactionInput(txId, inputIndex))
              }
            } ~
              rejectEmptyResponse {
                pathPrefix("interpret-with-steps" / IntNumber) { number =>
                  pathEndOrSingleSlash {
                    complete(blockcypherService.interpret(txId, inputIndex, Some(number)))
                  }
                } ~
                  pathPrefix("interpret") {
                    rejectEmptyResponse {
                      complete(blockcypherService.interpret(txId, inputIndex))
                    }
                  }
              } ~
              path("stream-interpret") {
                extractWebSocketUpgrade { upgrade =>
                  complete {
                    val interpretSource = blockcypherService
                      .interpretStream(txId, inputIndex)
                      .map(toJsonTextMessage[InterpreterOutcome])
                      .recover { case TransactionInputNotFound =>
                        TextMessage(Json.obj("error" -> "TransactionInputNotFound").toString)
                      }

                    upgrade.handleMessagesWith(Sink.ignore, interpretSource)
                  }
                }
              }
          }
        } ~
        pathPrefix("output") {
          pathPrefix(IntNumber) { inputIndex =>
            rejectEmptyResponse {
              complete(blockcypherService.getTransactionOutput(txId, inputIndex))
            }
          }
        }
    }
  }

  def apply(): Route = {
    clientRoute ~
      pathPrefix("api") {
        transactionRoute ~
          healthRoute
      }
  }

  private def toJsonTextMessage[T: Writes](message: T): TextMessage.Strict = {
    val jsonValue = Json.toJson(message)
    TextMessage(Json.stringify(jsonValue))
  }
}
