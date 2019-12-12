package it.softfork.bitcoin4s.external.bitcoind

import it.softfork.bitcoin4s.crypto.Hash
import it.softfork.bitcoin4s.transaction.structure.{OutPoint, Hash => ScodecHash}
import it.softfork.bitcoin4s.transaction.{TxIn, TxOut, Tx, TxId}
import scodec.bits.ByteVector
import scala.collection.immutable.ArraySeq
import it.softfork.bitcoin4s.script.Parser
import play.api.libs.json.{Format, Json, JsError, JsSuccess}
import play.api.libs.functional.syntax._
import akka.stream.Materializer
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.model.{HttpRequest, Uri}
import it.softfork.bitcoin4s.external.HttpSender
import scala.concurrent.{Future, ExecutionContext}
import Api._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import com.typesafe.scalalogging.StrictLogging
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials

class Api(httpSender: HttpSender)(
  implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends StrictLogging {
  def getTransaction(txId: TxId): Future[Option[Transaction]] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = rawTxUrl(txId),
      headers = Seq(Authorization(BasicHttpCredentials( "h0ngcha0", "7Rq@s4%7WmDS"))),
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        GetRawTransactionJsonRPC(txId)
      )
    )
    httpSender(request).flatMap { response =>
      if (response.status.isSuccess()) {
        transactionUnmarshaller(response.entity).map(Option.apply _)
      } else {
        logger.info(s"status: ${response.status.toString}, entity: ${Unmarshaller.stringUnmarshaller(response.entity)}")
        Future.successful(None)
      }
    }
  }
}

object Api {
  object GetRawTransactionJsonRPC {
    def apply(txid: TxId) = s"""
      | {
      |   "jsonrpc": "1.0",
      |   "id": "nioctib",
      |   "method": "getrawtransaction",
      |   "params": ["${txid.value}", true]
      | }
    """.stripMargin
  }

  case class ScriptSig(
    asm: String,
    hex: String
  )

  object ScriptSig {
    implicit val format: Format[ScriptSig] = Json.format[ScriptSig]
  }

  case class TransactionInput(
    txid: String, // tx id of the output being spent
    vout: Int,
    scriptSig: Option[ScriptSig],
    txinwitness: Option[List[String]] = None,
    sequence: Long
  ) {
    val prevTxHash = ScodecHash(ByteVector(Hash.fromHex(txid)))

    def toTxIn = TxIn(
      previous_output = OutPoint(prevTxHash, vout),
      sig_script = scriptSig
        .map { s =>
          ByteVector(Hash.fromHex(s.hex))
        }
        .getOrElse(ByteVector.empty),
      sequence = sequence
    )
  }

  object TransactionInput {
    implicit val format: Format[TransactionInput] = Json.format[TransactionInput]
  }

  case class ScriptPubKey(
    asm: String,
    hex: String,
    reqSigs: Int,
    `type`: String,
    addresses: Seq[String]
  )

  object ScriptPubKey {
    implicit val format: Format[ScriptPubKey] = Json.format[ScriptPubKey]
  }

  case class TransactionOutput(
    value: Double,
    n: Int,
    scriptPubKey: ScriptPubKey
  ) {
    def toTxOut = {
      val pkScriptInBytes = ByteVector(
        Parser.parse(ArraySeq.unsafeWrapArray(Hash.fromHex(scriptPubKey.hex))).flatMap(_.bytes)
      )

      TxOut(
        value = (value * 100000000).toLong,
        pk_script = pkScriptInBytes
      )
    }
  }

  object TransactionOutput {
    implicit val formatter: Format[TransactionOutput] = Json.format[TransactionOutput]
  }

  case class Transaction(
    txid: String,
    hash: String,
    version: Int,
    size: Long,
    locktime: Long,
    vin: Seq[TransactionInput],
    vout: Seq[TransactionOutput]
  ) {
    def toTx = Tx(
      version = version,
      tx_in = vin.map(_.toTxIn).toList,
      tx_out = vout.map(_.toTxOut).toList,
      lock_time = locktime
    )
  }

  object Transaction {
    implicit val formatter: Format[Transaction] = Json.format[Transaction]
  }

  case class TransactionResult(
    result: Transaction
  )

  object TransactionResult {
    implicit val formatter: Format[TransactionResult] = Json.format[TransactionResult]
  }

  protected def rawTxUrl(txId: TxId) = Uri(s"http://localhost:8332")

  def parseTransaction(raw: String): Transaction = {
    Json.fromJson[TransactionResult](Json.parse(raw)) match {
      case JsSuccess(value, _) =>
        value.result
      case JsError(e) =>
        throw new RuntimeException(s"Failed to parse transaction $e")
    }
  }

  implicit protected val transactionUnmarshaller = Unmarshaller.stringUnmarshaller.map(parseTransaction)
}