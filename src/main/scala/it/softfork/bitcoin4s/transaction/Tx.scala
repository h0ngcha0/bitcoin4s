package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.transaction.structure.VarList
import scodec.Codec
import scodec.{Attempt, DecodeResult}
import scodec.codecs._
import scodec.bits.BitVector
// import it.softfork.bitcoin4s.transaction.structure.ListCodec2

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

  def codec(version: Int) = {
    def encode(tx: Tx) = {
      val txCodec: Codec[Tx] =
        if (tx.flag) codecWithWitness(version, tx.tx_in.length)
        else codecWithoutWitness(version)
      txCodec.encode(tx)
    }

    def decode(bits: BitVector) = {
      //val (first, last@_) = bits.splitAt(bits.length - 32)
      for {
        f <- decodeFlag(bits)
        flag = f._1.value.isWitness
        tx <- decodeWithFlag(bits, flag, f._2.value.length, version)
      } yield tx
    }

    Codec[Tx](encode _, decode _)
  }.as[Tx]

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
    // val listWithLimit = new ListCodec2(VarList.varList(Codec[TxWitness]), Some(txInsCount))

    ("version" | uint32L) ::
    ("flag" | booleanFlagCodec) ::
    ("tx_in" | VarList.varList(Codec[TxIn])) ::
    ("tx_out" | VarList.varList(Codec[TxOut])) ::
    ("tx_witness" | list(VarList.varList(Codec[TxWitness]))) ::
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

  private def decodeFlag(bits: BitVector) = {
    for {
      v <- uint32L.decode(bits)
      witnessFlag <- Codec[WitnessFlag].decode(v.remainder)
      txIns <- VarList.varList(Codec[TxIn]).decode(witnessFlag.remainder)
    } yield (witnessFlag, txIns)
  }

  private def decodeWithFlag(bits: BitVector, flag: Boolean, txInsCount: Int, version: Int): Attempt[DecodeResult[Tx]] = {
    if (flag) {
//      val (first, last) = bits.splitAt(bits.length - 32)
//      for {
//        txWithoutLockTime <- codecWithWitness2(version, txInsCount).decode(first)
//        lockTime <- uint32L.decode(last)
//      } yield {
//        val tx2 = txWithoutLockTime.value
//        val lockTime2 = lockTime.value
//        Tx(
//          tx2.version,
//          tx2.flag,
//          tx2.tx_in,
//          tx2.tx_out,
//          tx2.tx_witness,
//          lockTime2
//        )
//      }
//codecWithWitness(version, txInsCount).decode(bits)
      foo(bits, txInsCount, version)
    } else {
      codecWithoutWitness(version).decode(bits)
    }
  }

  def foo(bits: BitVector, txInsCount: Int, version: Int): Attempt[DecodeResult[Tx]] = {
    val (first, last) = bits.splitAt(bits.length - 32)
    for {
      txWithoutLockTime <- codecWithWitness2(version, txInsCount).decode(first)
      lockTime <- uint32L.decode(last)
    } yield {
      val tx2 = txWithoutLockTime.value
      val lockTime2 = lockTime.value
      DecodeResult(
        Tx(
          tx2.version,
          tx2.flag,
          tx2.tx_in,
          tx2.tx_out,
          tx2.tx_witness,
          lockTime2
        ),
        lockTime.remainder
      )
    }
  }
}
