package me.hongchao.bitcoin4s

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import me.hongchao.bitcoin4s.external.blockcypher.{Api => BlockCypherApi}
import me.hongchao.bitcoin4s.transaction.TxId

class Routes(blockcypherApi: BlockCypherApi) extends PlayJsonSupport {

  val transactionRoute = pathPrefix("transaction") {
    pathPrefix(Segment.map(TxId.apply)) { txId =>
      pathEndOrSingleSlash {
        complete(blockcypherApi.getTransaction(txId))
      } ~
        pathPrefix("input") {
          pathPrefix(IntNumber) { inputIndex =>
            rejectEmptyResponse {
              complete(blockcypherApi.getTransactionInput(txId, inputIndex))
            }
          }
        } ~
        pathPrefix("output") {
          pathPrefix(IntNumber) { inputIndex =>
            rejectEmptyResponse {
              complete(blockcypherApi.getTransactionOutput(txId, inputIndex))
            }
          }
        }
    }
  }

}
