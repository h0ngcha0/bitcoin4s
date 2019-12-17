package it.softfork.bitcoin4s.external

import it.softfork.bitcoin4s.ApiModels.Transaction
import it.softfork.bitcoin4s.transaction.TxId

import scala.concurrent.Future

// https://www.blockcypher.com/dev/bitcoin/#transaction-api
trait ApiInterface {
  def getTransaction(txId: TxId): Future[Option[Transaction]]
}
