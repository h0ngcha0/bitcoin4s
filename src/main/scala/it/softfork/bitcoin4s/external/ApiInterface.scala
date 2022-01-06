package it.softfork.bitcoin4s.external

import scala.concurrent.Future

import it.softfork.bitcoin4s.ApiModels.Transaction
import it.softfork.bitcoin4s.transaction.TxId

// https://www.blockcypher.com/dev/bitcoin/#transaction-api
trait ApiInterface {
  def getTransaction(txId: TxId): Future[Option[Transaction]]
}
