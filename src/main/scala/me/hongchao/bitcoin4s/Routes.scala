package me.hongchao.bitcoin4s

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import me.hongchao.bitcoin4s.external.blockcypher.{Service => BlockCypherService}
import me.hongchao.bitcoin4s.transaction.TxId

class Routes(blockcypherService: BlockCypherService) extends PlayJsonSupport {

  val transactionRoute = pathPrefix("transaction") {
    pathPrefix(Segment.map(TxId.apply)) { txId =>
      pathEndOrSingleSlash {
        complete(blockcypherService.getTransaction(txId))
      } ~
        pathPrefix("input") {
          pathPrefix(IntNumber) { inputIndex =>
            pathEndOrSingleSlash {
              rejectEmptyResponse {
                complete(blockcypherService.getTransactionInput(txId, inputIndex))
              }
            } ~
            pathPrefix("interpret") {
              rejectEmptyResponse {
                complete(blockcypherService.interpret(txId, inputIndex))
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

}
