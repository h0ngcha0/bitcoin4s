package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.transaction.structure.VarList
import scodec.Codec
import scodec.{Attempt, DecodeResult}
import scodec.codecs._
import scodec.bits.BitVector
import it.softfork.bitcoin4s.transaction.structure.ListCodec2

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

case class TxWithoutLockTime(
  version: Long,
  flag: Boolean,
  tx_in: List[TxIn],
  tx_out: List[TxOut],
  tx_witness: List[List[TxWitness]]
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
        tx <- decodeWithFlag(bits, isWitness, version)
      } yield tx
    }

    Codec[Tx](encode _, decode _)
  }.as[Tx]

//  def codec(version: Int): Codec[Tx] = {
//    codecWithoutWitness(version)
//  }

  def codecWithoutWitness(version: Int) = {
    ("version" | uint32L) ::
    ("flag" | provide(false)) ::
    ("tx_in" | VarList.varList(Codec[TxIn])) ::
    ("tx_out" | VarList.varList(Codec[TxOut])) ::
    ("tx_witness" | provide(List[List[TxWitness]]())) ::
    ("lock_time" | uint32L)
  }.as[Tx]


  def codecWithWitness2(version: Int, txInsCount: Int): Codec[TxWithoutLockTime] = {
    // val listWithLimit = new ListCodec2(VarList.varList(Codec[TxWitness]), Some(txInsCount))

    ("version" | uint32L) ::
    ("flag" | booleanFlagCodec) ::
    ("tx_in" | VarList.varList(Codec[TxIn])) ::
    ("tx_out" | VarList.varList(Codec[TxOut])) ::
    ("tx_witness" | list(VarList.varList(Codec[TxWitness])))
  }.as[TxWithoutLockTime]

  def codecWithWitness(version: Int, txInsCount: Int): Codec[Tx] = {
    val listWithLimit = new ListCodec2(VarList.varList(Codec[TxWitness]), Some(txInsCount))

    ("version" | uint32L) ::
    ("flag" | booleanFlagCodec) ::
    ("tx_in" | VarList.varList(Codec[TxIn])) ::
    ("tx_out" | VarList.varList(Codec[TxOut])) ::
    ("tx_witness" | listWithLimit) ::
    ("lock_time" | uint32L)
  }.as[Tx]

  case class WitnessFlag(flag1: Int, flag2: Int) {
    def isWitness: Boolean =
      flag1 == 0 && flag2 == 1
  }

  implicit def flagCodec: Codec[WitnessFlag] = {
    ("flag1" | uint8L) ::
    ("flag2" | uint8L)
  }.as[WitnessFlag]

  def booleanFlagCodec: Codec[Boolean] = {
    ("flag" | mappedEnum(flagCodec,
      false -> WitnessFlag(0, 0), true -> WitnessFlag(0, 1)))
  }.as[Boolean]

  def decodeWitnessFlag(bits: BitVector) = {
    for {
      v <- uint32L.decode(bits)
      witnessFlag <- Codec[WitnessFlag].decode(v.remainder)
    } yield witnessFlag
  }

  def decodeTxInWithoutWitness(bits: BitVector): Attempt[DecodeResult[List[TxIn]]] = {
    for {
      v <- uint32L.decode(bits)
      txIns <- VarList.varList(Codec[TxIn]).decode(v.remainder)
    } yield txIns
  }

  def decodeTxInWithWitness(bits: BitVector): Attempt[DecodeResult[List[TxIn]]] = {
    for {
      v <- uint32L.decode(bits)
      witnessFlag <- Codec[WitnessFlag].decode(v.remainder)
      txIns <- VarList.varList(Codec[TxIn]).decode(witnessFlag.remainder)
    } yield txIns
  }

  def decodeWithFlag(bits: BitVector, isWitness: Boolean, version: Int): Attempt[DecodeResult[Tx]] = {
    if (isWitness) {
      for {
        txIn <- decodeTxInWithWitness(bits)
        value <- codecWithWitness(version, txIn.value.length).decode(bits)
      } yield value
    } else {
      codecWithoutWitness(version).decode(bits)
    }
  }
}
