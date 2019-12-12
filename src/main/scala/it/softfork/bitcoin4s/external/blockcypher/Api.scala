package it.softfork.bitcoin4s.external.blockcypher

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import it.softfork.bitcoin4s.crypto.Hash
import it.softfork.bitcoin4s.external.HttpSender
import it.softfork.bitcoin4s.external.blockcypher.Api._
import it.softfork.bitcoin4s.script.{Parser, ScriptElement}
import it.softfork.bitcoin4s.transaction.structure.{OutPoint, Hash => ScodecHash}
import it.softfork.bitcoin4s.transaction.{Tx, TxId, TxIn, TxOut}
import play.api.libs.json.{Format, JsError, JsSuccess, Json}
import scodec.bits.ByteVector
import it.softfork.bitcoin4s.ApiModels.scriptElementFormat
import it.softfork.bitcoin4s.external._

import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}

// https://www.blockcypher.com/dev/bitcoin/#transaction-api
trait ApiInterface {
  def getTransaction(txId: TxId): Future[Option[Transaction]]
}

class Api(httpSender: HttpSender)(
  implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends ApiInterface {
  override def getTransaction(txId: TxId): Future[Option[Transaction]] = {
    httpSender(HttpRequest(uri = rawTxUrl(txId))).flatMap { response =>
      if (response.status.isSuccess()) {
        transactionUnmarshaller(response.entity).map(Option.apply _)
      } else {
        Future.successful(None)
      }
    }
  }
}

class CachedApi(api: Api)(
  implicit
  system: ActorSystem,
  ec: ExecutionContext
) extends ApiInterface {
  implicit val transactionCacheActor = system.actorOf(TransactionCacheActor.props())

  override def getTransaction(txId: TxId): Future[Option[Transaction]] = {
    TransactionCacheActor.getTransaction(txId).flatMap { cachedTx =>
      cachedTx match {
        case None =>
          api.getTransaction(txId).map { maybeFreshTx =>
            maybeFreshTx match {
              case Some(freshTx) =>
                TransactionCacheActor.setTransaction(txId, freshTx)
                Some(freshTx)
              case None =>
                None
            }
          }
        case Some(tx) =>
          Future.successful(Some(tx))
      }
    }
  }
}

object Api {
  protected def rawTxUrl(txId: TxId) = Uri(s"https://api.blockcypher.com/v1/btc/main/txs/${txId.value}?limit=1000")

  def parseTransaction(raw: String): Transaction = {
    Json.fromJson[Transaction](Json.parse(raw)) match {
      case JsSuccess(value, _) =>
        value
      case JsError(e) =>
        throw new RuntimeException(s"Failed to parse transaction $e")
    }
  }

  implicit protected val transactionUnmarshaller = Unmarshaller.stringUnmarshaller.map(parseTransaction)
}
