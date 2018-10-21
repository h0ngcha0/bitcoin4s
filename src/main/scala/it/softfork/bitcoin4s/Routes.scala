package it.softfork.bitcoin4s

import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Sink
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import it.softfork.bitcoin4s.ApiModels.{InterpreterOutcome, TransactionInputNotFound}
import it.softfork.bitcoin4s.external.blockcypher.{Service => BlockCypherService}
import it.softfork.bitcoin4s.transaction.TxId
import play.api.libs.json.{Json, Writes}

class Routes(blockcypherService: BlockCypherService) extends PlayJsonSupport {

  val clientRoute = {
    pathSingleSlash {
      getFromResource("client/index.html")
    } ~
      pathPrefix("static") {
        getFromResourceDirectory("client/static")
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
                pathPrefix("interpret") {
                  rejectEmptyResponse {
                    complete(blockcypherService.interpret(txId, inputIndex))
                  }
                } ~
                  pathPrefix("interpret-with-steps" / IntNumber) { number =>
                    pathEndOrSingleSlash {
                      complete(blockcypherService.interpret(txId, inputIndex, Some(number)))
                    }
                  }
              } ~
              path("stream-interpret") {
                extractUpgradeToWebSocket { upgrade =>
                  complete {
                    val interpretSource = blockcypherService
                      .interpretStream(txId, inputIndex)
                      .map(toJsonTextMessage[InterpreterOutcome])
                      .recover {
                        case TransactionInputNotFound =>
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

  def apply() = {
    clientRoute ~
    pathPrefix("api") {
      transactionRoute
    }
  }

  private def toJsonTextMessage[T: Writes](message: T): TextMessage.Strict = {
    val jsonValue = Json.toJson(message)
    TextMessage(Json.stringify(jsonValue))
  }
}
