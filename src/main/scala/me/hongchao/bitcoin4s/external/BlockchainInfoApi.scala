package me.hongchao.bitcoin4s.external

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}
import BlockchainInfoApi._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import io.github.yzernik.bitcoinscodec.messages.TxWitness
import io.github.yzernik.bitcoinscodec.structures.{OutPoint, TxIn, TxOutWitness, Hash => ScodecHash}
import me.hongchao.bitcoin4s.script.Parser
import play.api.libs.json.{JsError, JsSuccess, Json}
import scodec.bits.ByteVector
import tech.minna.playjson.macros.json
import me.hongchao.bitcoin4s.crypto.Hash

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

// TODO: Get rid of the deps for bitcoinscodec
// 1) do we actually need both tx and tx witness
// 2) if not, remove one of them and rewrite the transaction part in terms of scodec
object BlockchainInfoApi {
  @json case class PreviousOutput(
    spent: Boolean,
    tx_index: Long,
    `type`: Int,
    addr: String,
    value: Long,
    n: Int,
    script: String
  ) {
    def toOutPoint = OutPoint(
      hash = ScodecHash(ByteVector(Hash.fromHex(addr))),
      index = tx_index
    )
  }

  @json case class TransactionInput(
    sequence: Long,
    witness: String,
    prev_out: Option[PreviousOutput],
    script: String
  ) {
    def toTxIn = TxIn(
      previous_output = prev_out.get.toOutPoint, // FIXME: remove .get
      sig_script = ByteVector(Hash.fromHex(script)),
      sequence = sequence
    )

    def toWitnessScript = ??? // FIXME: how is witness structured
  }

  @json case class TransactionOutput(
    spent: Boolean,
    tx_index: Long,
    `type`: Int,
    addr: Option[String],
    value: Long,
    n: Int,
    script: String
  ) {
    def toTxOut = TxOutWitness(
      value = value,
      pk_script = ByteVector(Parser.parse(script).flatMap(_.bytes))
    )
  }

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
    witn: String,
    vout_sz: Int
  ) {
    def toTxWitness = TxWitness(
      version = ver,
      tx_in = inputs.map(_.toTxIn).toList,
      tx_out = out.map(_.toTxOut).toList,
      lock_time = lock_time
    )
  }

  protected def rawTxUrl(txId: String) = Uri(s"https://blockchain.info/rawtx/$txId")

  protected implicit val transactionUnmarshaller = Unmarshaller.stringUnmarshaller.map { raw =>
    Json.fromJson[Transaction](Json.parse(raw)) match {
      case JsSuccess(value, _) =>
        value
      case JsError(e) =>
        throw new RuntimeException(s"Failed to parse transaction $e")
    }
  }
}