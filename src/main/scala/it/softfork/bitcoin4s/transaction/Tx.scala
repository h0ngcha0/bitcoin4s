package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.transaction.structure.VarList
import scodec.Codec
import scodec.{Attempt, DecodeResult}
import scodec.codecs._
import scodec.bits.BitVector
import it.softfork.bitcoin4s.transaction.structure.ListCodec

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
      v <- uint32L.decode(bits)
      witnessFlag <- Codec[WitnessFlag].decode(v.remainder)
    } yield witnessFlag
  }

  private def decodeTx(bits: BitVector, isWitness: Boolean, version: Int): Attempt[DecodeResult[Tx]] = {
    if (isWitness) {
      for {
        v <- uint32L.decode(bits)
        witnessFlag <- Codec[WitnessFlag].decode(v.remainder)
        txIns <- VarList.varList(Codec[TxIn]).decode(witnessFlag.remainder)
        value <- codecWithWitness(version, txIns.value.length).decode(bits)
      } yield value
    } else {
      codecWithoutWitness(version).decode(bits)
    }
  }
}
