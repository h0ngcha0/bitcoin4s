package it.softfork.bitcoin4s.transaction

import Tx.WitnessFlag
import play.api.libs.json.Json
import scodec.{Attempt, DecodeResult}
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

import it.softfork.bitcoin4s.transaction.structure.{ListCodec, VarList}
import it.softfork.bitcoin4s.utils.{hexToBytes, AttemptSeq}

// Credit: https://github.com/yzernik/bitcoin-scodec

case class TxId(value: String)

case class Tx(
  version: Long,
  flag: Boolean,
  tx_in: List[TxIn],
  tx_out: List[TxOut],
  tx_witness: List[List[TxWitness]],
  lock_time: Long
)

object Tx {

  def codec(): Codec[Tx] = {
    def encode(tx: Tx): Attempt[BitVector] = {
      val txCodec: Codec[Tx] = {
        if (tx.flag) {
          codecWithWitness(tx.tx_in.length)
        } else {
          codecWithoutWitness()
        }
      }
      txCodec.encode(tx)
    }

    def decode(bits: BitVector): Attempt[DecodeResult[Tx]] = {
      for {
        witnessFlag <- decodeWitnessFlag(bits)
        isWitness = witnessFlag.value.isWitness
        tx <- decodeTx(bits, isWitness)
      } yield tx
    }

    Codec[Tx](encode _, decode _)
  }.as[Tx]

  def fromHex(hex: String): Tx = {
    Tx.codec().decodeValue(BitVector(hexToBytes(hex))) match {
      case Attempt.Successful(tx) =>
        tx
      case Attempt.Failure(err) =>
        throw new RuntimeException(err.messageWithContext)
    }
  }

  def toHex(tx: Tx): String = {
    Tx.codec().encode(tx) match {
      case Attempt.Successful(tx) =>
        tx.toHex
      case Attempt.Failure(err) =>
        throw new RuntimeException(err.messageWithContext)
    }
  }

  case class WitnessFlag(flag1: Int, flag2: Int) {
    def isWitness: Boolean = flag1 == 0 && flag2 == 1
  }

  object WitnessFlag {

    implicit def codec: Codec[WitnessFlag] = {
      ("flag1" | uint8L) ::
        ("flag2" | uint8L)
    }.as[WitnessFlag]
  }

  // ==== private ====

  private def codecWithoutWitness() = {
    ("version" | uint32L) ::
      ("flag" | provide(false)) ::
      ("tx_in" | VarList.varList(Codec[TxIn])) ::
      ("tx_out" | VarList.varList(Codec[TxOut])) ::
      ("tx_witness" | provide(List[List[TxWitness]]())) ::
      ("lock_time" | uint32L)
  }.as[Tx]

  private def codecWithWitness(txInsCount: Int): Codec[Tx] = {
    ("version" | uint32L) ::
      ("flag" | mappedEnum(
        WitnessFlag.codec,
        false -> WitnessFlag(0, 0),
        true -> WitnessFlag(0, 1)
      )) ::
      ("tx_in" | VarList.varList(Codec[TxIn])) ::
      ("tx_out" | VarList.varList(Codec[TxOut])) ::
      ("tx_witness" | new ListCodec(VarList.varList(Codec[TxWitness]), Some(txInsCount))) ::
      ("lock_time" | uint32L)
  }.as[Tx]

  private def decodeWitnessFlag(bits: BitVector) = {
    for {
      version <- uint32L.decode(bits)
      witnessFlag <- Codec[WitnessFlag].decode(version.remainder)
    } yield witnessFlag
  }

  private def decodeTx(bits: BitVector, isWitness: Boolean): Attempt[DecodeResult[Tx]] = {
    if (isWitness) {
      for {
        vsn <- uint32L.decode(bits)
        witnessFlag <- Codec[WitnessFlag].decode(vsn.remainder)
        txIns <- VarList.varList(Codec[TxIn]).decode(witnessFlag.remainder)
        tx <- codecWithWitness(txIns.value.length).decode(bits)
      } yield tx
    } else {
      codecWithoutWitness().decode(bits)
    }
  }
}

case class TxRaw(
  version: String,
  flag: Option[String],
  txIns: TxInsRaw,
  txOuts: TxOutsRaw,
  txWitnesses: List[TxWitnessesRaw],
  lockTime: String
) {

  val hex: String = {
    val flagStr: String = flag.getOrElse("")
    val txInsStr: String = txIns.hex
    val txOutsStr: String = txOuts.hex
    val txWitnessesStr: String = txWitnesses.map(_.hex).mkString

    s"$version$flagStr$txInsStr$txOutsStr$txWitnessesStr$lockTime"
  }
}

object TxRaw {
  implicit val format = Json.format[TxRaw]

  // scalastyle:off method.length
  def apply(tx: Tx): Attempt[TxRaw] = {
    val txInsRawAttempt = for {
      txInCount <- VarList.countCodec.encode(tx.tx_in.size).map(_.toHex)
      txInRaw <- AttemptSeq.apply(tx.tx_in.map(TxInRaw.apply))
    } yield TxInsRaw(txInCount, txInRaw)

    val txOutsRawAttempt = for {
      txOutCount <- VarList.countCodec.encode(tx.tx_out.size).map(_.toHex)
      txOutRaw <- AttemptSeq.apply(tx.tx_out.map(TxOutRaw.apply))
    } yield TxOutsRaw(txOutCount, txOutRaw)

    val flagAttempt = mappedEnum(
      WitnessFlag.codec,
      false -> WitnessFlag(0, 0),
      true -> WitnessFlag(0, 1)
    ).encode(tx.flag).map(_.toHex)

    if (tx.flag) {
      val rawWitnessesAttempt: Attempt[List[TxWitnessesRaw]] = AttemptSeq.apply(
        tx.tx_witness.map { witnesses =>
          for {
            count <- VarList.countCodec.encode(witnesses.size).map(_.toHex)
            rawWitnesses <- AttemptSeq.apply(witnesses.map(TxWitnessRaw.apply))
          } yield {
            TxWitnessesRaw(count, rawWitnesses)
          }
        }
      )

      for {
        versionBitVector <- uint32L.encode(tx.version)
        flag <- flagAttempt
        txInsRaw <- txInsRawAttempt
        txOutsRaw <- txOutsRawAttempt
        txWitnessesRaw <- rawWitnessesAttempt
        locktimeBitVector <- uint32L.encode(tx.lock_time)
      } yield TxRaw(
        version = versionBitVector.toHex,
        flag = Some(flag),
        txIns = txInsRaw,
        txOuts = txOutsRaw,
        txWitnesses = txWitnessesRaw,
        lockTime = locktimeBitVector.toHex
      )
    } else {
      for {
        versionBitVector <- uint32L.encode(tx.version)
        txInsRaw <- txInsRawAttempt
        txOutsRaw <- txOutsRawAttempt
        locktimeBitVector <- uint32L.encode(tx.lock_time)
      } yield TxRaw(
        version = versionBitVector.toHex,
        flag = None,
        txIns = txInsRaw,
        txOuts = txOutsRaw,
        txWitnesses = List.empty,
        lockTime = locktimeBitVector.toHex
      )
    }
  }
  // scalastyle:on method.length
}
