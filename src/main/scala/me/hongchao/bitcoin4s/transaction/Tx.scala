package me.hongchao.bitcoin4s.transaction

import scodec.Codec
import scodec.codecs._


sealed trait Tx {
  def version: Long
  def tx_in: List[TxIn]
  def tx_out: List[TxOut]
  def lock_time: Long
}

case class RegularTx(
  version: Long,
  tx_in: List[TxIn],
  tx_out: List[RegularTxOut],
  lock_time: Long) extends Tx {
  type E = RegularTx
  def companion = RegularTx
}

case class TxWitness(
  version: Long,
  tx_in: List[TxIn],
  tx_out: List[TxOutWitness],
  lock_time: Long) extends Tx {
  type E = TxWitness
  def companion = TxWitness
}

object RegularTx {
  def codec(version: Int) = {
    ("version" | uint32L) ::
      ("tx_in" | VarList.varList(Codec[TxIn])) ::
      ("tx_out" | VarList.varList(Codec[RegularTxOut])) ::
      ("lock_time" | uint32L)
  }.as[RegularTx]
  def command = "tx"
}

object TxWitness {
  def codec(version: Int) = {
    ("version" | uint32L) ::
      ("tx_in" | VarList.varList(Codec[TxIn])) ::
      ("tx_out" | VarList.varList(Codec[TxOutWitness])) ::
      ("lock_time" | uint32L)
  }.as[TxWitness]
  def command = "tx"
}
