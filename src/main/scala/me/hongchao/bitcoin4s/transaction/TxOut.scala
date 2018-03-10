package me.hongchao.bitcoin4s.transaction

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

sealed trait TxOut {
  def value: Long
  def pk_script: ByteVector
}

case class RegularTxOut(
  value: Long,
  pk_script: ByteVector
) extends TxOut

object RegularTxOut {

  val scriptCodec = {
    val countCodec = VarInt.varIntCodec.xmap(_.toInt, (i: Int) => i.toLong)
    variableSizeBytes(countCodec, bytes)
  }

  implicit val codec: Codec[RegularTxOut] = {
    ("value" | int64) ::
      ("pk_script" | scriptCodec)
  }.as[RegularTxOut]

}

case class TxOutWitness(
  value: Long,
  pk_script: ByteVector
) extends TxOut

object TxOutWitness {

  val scriptCodec = {
    val countCodec = VarInt.varIntCodec.xmap(_.toInt, (i: Int) => i.toLong)
    variableSizeBytes(countCodec, bytes)
  }

  implicit val codec: Codec[TxOutWitness] = {
    ("value" | int64L) ::
      ("pk_script" | scriptCodec)
  }.as[TxOutWitness]

}
