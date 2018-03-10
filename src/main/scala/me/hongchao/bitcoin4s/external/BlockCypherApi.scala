package me.hongchao.bitcoin4s.external

import java.time.ZonedDateTime

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}
import BlockCypherApi._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import me.hongchao.bitcoin4s.transaction.Tx
import me.hongchao.bitcoin4s.transaction.{TxIn, TxOut}
import me.hongchao.bitcoin4s.transaction.structure.{OutPoint, Hash => ScodecHash}
import me.hongchao.bitcoin4s.script.Parser
import play.api.libs.json.{JsError, JsSuccess, Json}
import scodec.bits.ByteVector
import tech.minna.playjson.macros.json
import me.hongchao.bitcoin4s.crypto.Hash

// https://www.blockcypher.com/dev/bitcoin/#transaction-api
class BlockCypherApi(httpSender: HttpSender)(
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

object BlockCypherApi {

  @json case class TransactionInput(
    prev_hash: String,
    output_index: Int,
    script: String,
    output_value: Long,
    sequence: Long,
    addresses: List[String],
    script_type: String,
    age: Long,
    witness: Option[List[String]] = None
  ) {
    val prevTxHash = ScodecHash(ByteVector(Hash.fromHex(prev_hash)))
    def toTxIn = TxIn(
      previous_output = OutPoint(prevTxHash, output_index),
      sig_script = ByteVector(Hash.fromHex(script)),
      sequence = sequence
    )
  }

  @json case class TransactionOutput(
    value: Long,
    script: String,
    spent_by: Option[String],
    addresses: List[String],
    script_type: String
  ) {
    def toTxOut = TxOut(
      value = value,
      pk_script = ByteVector(Parser.parse(Hash.fromHex(script)).flatMap(_.bytes))
    )
  }

  @json case class Transaction(
    block_hash: String,
    block_height: Long,
    block_index: Int,
    hash: String,
    addresses: Seq[String],
    total: Long,
    fees: Long,
    size: Long,
    confirmed: ZonedDateTime,
    received: ZonedDateTime,
    ver: Int,
    lock_time: Long = 0,
    double_spend: Boolean,
    vin_sz: Int,
    vout_sz: Int,
    confirmations: Long,
    confidence: Int,
    inputs: Seq[TransactionInput],
    outputs: Seq[TransactionOutput]
  ) {
    def toTx = Tx(
      version = ver,
      tx_in = inputs.map(_.toTxIn).toList,
      tx_out = outputs.map(_.toTxOut).toList,
      lock_time = lock_time
    )
  }

  protected def rawTxUrl(txId: String) = Uri(s"https://api.blockcypher.com/v1/btc/main/txs/$txId")

  def parseTransaction(raw: String): Transaction = {
    Json.fromJson[Transaction](Json.parse(raw)) match {
      case JsSuccess(value, _) =>
        value
      case JsError(e) =>
        throw new RuntimeException(s"Failed to parse transaction $e")
    }
  }

  protected implicit val transactionUnmarshaller = Unmarshaller.stringUnmarshaller.map(parseTransaction)
}