package me.hongchao.bitcoin4s.external

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}
import BlockchainInfoApi._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import play.api.libs.json.{JsError, JsSuccess, Json}
import tech.minna.playjson.macros.json

// NOTE: https://blockchain.info/api/blockchain_api
class BlockchainInfoApi(httpSender: HttpSender)(
  implicit
  ec: ExecutionContext,
  materializer: Materializer
) {
  def getTransaction(txId: String): Future[Transaction] = {
    httpSender(HttpRequest(uri = rawTxUrl(txId))).flatMap { response =>
      transactionUnmarshaller(response.entity)
    }
  }
}

object BlockchainInfoApi {
  @json case class PreviousOutput(
    spent: Boolean,
    tx_index: Long,
    `type`: Int,
    addr: String,
    value: Long,
    n: Int,
    script: String
  )

  @json case class TransactionInput(
    sequence: Long,
    witness: String,
    prev_out: PreviousOutput,
    script: String
  )

  @json case class TransactionOutput(
    spent: Boolean,
    tx_index: Long,
    `type`: Int,
    addr: String,
    value: Long,
    n: Int,
    script: String
  )

  @json case class Transaction(
    ver: Int,
    inputs: Seq[TransactionInput],
    weight: Long,
    block_height: Long,
    relayed_by: String,
    out: Seq[TransactionOutput],
    lock_time: Long,
    size: Long,
    double_spend: Boolean,
    time: Long,
    tx_index: Long,
    vin_sz: Int,
    hash: String,
    vout_sz: Int
  )

  def rawTxUrl(txId: String) = Uri(s"https://blockchain.info/rawtx/$txId")

  implicit val transactionUnmarshaller = Unmarshaller.stringUnmarshaller.map { raw =>
    Json.fromJson[Transaction](Json.parse(raw)) match {
      case JsSuccess(value, _) =>
        value
      case JsError(e) =>
        throw new RuntimeException(s"Failed to parse transaction $e")
    }
  }
}