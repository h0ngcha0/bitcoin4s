package me.hongchao.bitcoin4s.external.blockcypher

import java.time.ZonedDateTime

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import me.hongchao.bitcoin4s.external.blockcypher.Api.Transaction
import me.hongchao.bitcoin4s.external.blockcypher.TransactionCacheActor._
import me.hongchao.bitcoin4s.transaction.TxId

import scala.collection.immutable.HashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object TransactionCacheActor {
  def props(): Props = {
    Props(classOf[TransactionCacheActor])
  }

  sealed trait Message

  case object InvalidateExpired extends Message
  case class Get(txId: TxId) extends Message
  case class Set(txId: TxId, payload: Transaction) extends Message

  implicit val timeout: Timeout = Timeout(5.seconds)

  def getTransaction(txId: TxId)(
    implicit
    ec: ExecutionContext,
    transactionCacheActor: ActorRef
  ): Future[Option[Transaction]] = {
    (transactionCacheActor ? Get(txId)).mapTo[Option[Transaction]]
  }

  def setTransaction(txId: TxId, tx: Transaction)(
    implicit
    ec: ExecutionContext,
    transactionCacheActor: ActorRef
  ): Unit = {
    transactionCacheActor ! Set(txId, tx)
  }
}

class TransactionCacheActor() extends Actor with StrictLogging {
  import context.dispatcher

  case class TransactionWithCreationTime(tx: Transaction, createdAt: ZonedDateTime)

  var transactions: HashMap[TxId, TransactionWithCreationTime] = HashMap.empty

  def scheduleInvalidateExpired(): Unit = {
    context.system.scheduler.schedule(initialDelay = 10.seconds, interval = 2.minutes, receiver = self, message = InvalidateExpired)
    ()
  }

  override def preStart(): Unit = {
    logger.info("Blockcypher transaction cache actor is about to start...")
    scheduleInvalidateExpired()
  }

  override def receive: Receive = {
    case InvalidateExpired =>
      transactions = transactions.filterNot {
        case (txId@_, TransactionWithCreationTime(tx@_, createdAt)) =>
          ZonedDateTime.now.isAfter(createdAt.plusMinutes(2))
      }

    case Get(txId: TxId) => {
      sender ! transactions.get(txId).map(_.tx)
    }

    case Set(txId: TxId, transaction: Transaction) => {
      transactions = transactions.updated(txId, TransactionWithCreationTime(transaction, ZonedDateTime.now))
    }

    case message =>
      logger.error(s"Received unexpected message $message")
  }

}
