package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.transaction.structure.VarList
import scodec.Codec
import scodec.{Attempt, DecodeResult}
import scodec.codecs._
import scodec.bits.BitVector
import it.softfork.bitcoin4s.transaction.structure.ListCodec
import it.softfork.bitcoin4s.Utils.{AttemptSeq, hexToBytes}

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

  def codec(version: Int): Codec[Tx] = {
    def encode(tx: Tx) = {
      val txCodec: Codec[Tx] =
        if (tx.flag) codecWithWitness(version, tx.tx_in.length)
        else codecWithoutWitness(version)
      txCodec.encode(tx)
    }

    def decode(bits: BitVector) = {
      for {
        witnessFlag <- decodeWitnessFlag(bits)
        isWitness = witnessFlag.value.isWitness
        tx <- decodeTx(bits, isWitness, version)
      } yield tx
    }

    Codec[Tx](encode _, decode _)
  }.as[Tx]

  def fromHex(hex: String): Tx = {
    Tx.codec(1).decodeValue(BitVector(hexToBytes(hex))) match {
      case Attempt.Successful(tx) =>
        tx
      case Attempt.Failure(err) =>
        throw new RuntimeException(err.messageWithContext)
    }
  }

  def toHex(tx: Tx): String = {
    Tx.codec(1).encode(tx) match {
      case Attempt.Successful(tx) =>
        tx.toHex
      case Attempt.Failure(err) =>
        throw new RuntimeException(err.messageWithContext)
    }
  }

  case class Raw(
    version: String,
    flag: Option[String],
    tx_in: List[TxIn.Raw],
    tx_out: List[TxOut.Raw],
    tx_witness: List[List[TxWitness.Raw]],
    lock_time: String
  )

  object Raw {
    def apply(tx: Tx): Attempt[Raw] = {
      if (tx.flag) {
        for {
          versionBitVector <- uint32L.encode(tx.version)
          flagBitVector <- mappedEnum(WitnessFlag.codec, false -> WitnessFlag(0, 0), true -> WitnessFlag(0, 1)).encode(tx.flag)
          txInRaw <- AttemptSeq.apply(tx.tx_in.map(TxIn.Raw.apply))
          txOutRaw <- AttemptSeq.apply(tx.tx_out.map(TxOut.Raw.apply))
          txWitnessRaw <- AttemptSeq.apply(tx.tx_witness.map(_.map(TxWitness.Raw.apply)).map(AttemptSeq.apply))
          locktimeBitVector <- uint32L.encode(tx.lock_time)
        } yield Raw(
          version = versionBitVector.toHex,
          flag = Some(flagBitVector.toHex),
          tx_in = txInRaw,
          tx_out = txOutRaw,
          tx_witness = txWitnessRaw,
          lock_time = locktimeBitVector.toHex
        )
      } else {
        for {
          versionBitVector <- uint32L.encode(tx.version)
          txInRaw <- AttemptSeq.apply(tx.tx_in.map(TxIn.Raw.apply))
          txOutRaw <- AttemptSeq.apply(tx.tx_out.map(TxOut.Raw.apply))
          locktimeBitVector <- uint32L.encode(tx.lock_time)
        } yield Raw(
          version = versionBitVector.toHex,
          flag = None,
          tx_in = txInRaw,
          tx_out = txOutRaw,
          tx_witness = List.empty,
          lock_time = locktimeBitVector.toHex
        )
      }
    }
  }


  // ==== private ====

  private[Tx] case class WitnessFlag(flag1: Int, flag2: Int) {
    def isWitness: Boolean = flag1 == 0 && flag2 == 1
  }

  private[Tx] object WitnessFlag {
    implicit def codec: Codec[WitnessFlag] = {
      ("flag1" | uint8L) ::
        ("flag2" | uint8L)
    }.as[WitnessFlag]
  }

  private def codecWithoutWitness(version: Int) = {
    ("version" | uint32L) ::
      ("flag" | provide(false)) ::
      ("tx_in" | VarList.varList(Codec[TxIn])) ::
      ("tx_out" | VarList.varList(Codec[TxOut])) ::
      ("tx_witness" | provide(List[List[TxWitness]]())) ::
      ("lock_time" | uint32L)
  }.as[Tx]

  private def codecWithWitness(version: Int, txInsCount: Int): Codec[Tx] = {
    ("version" | uint32L) ::
      ("flag" | mappedEnum(WitnessFlag.codec, false -> WitnessFlag(0, 0), true -> WitnessFlag(0, 1))) ::
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

  private def decodeTx(bits: BitVector, isWitness: Boolean, version: Int): Attempt[DecodeResult[Tx]] = {
    if (isWitness) {
      for {
        vsn <- uint32L.decode(bits)
        witnessFlag <- Codec[WitnessFlag].decode(vsn.remainder)
        txIns <- VarList.varList(Codec[TxIn]).decode(witnessFlag.remainder)
        tx <- codecWithWitness(version, txIns.value.length).decode(bits)
      } yield tx
    } else {
      codecWithoutWitness(version).decode(bits)
    }
  }
}
