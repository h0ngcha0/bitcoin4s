package me.hongchao.bitcoin4s.transaction


import scodec.Codec
import scodec.bits.{ByteOrdering, ByteVector}
import scodec.codecs._

case class TxIn(
  previous_output: OutPoint,
  sig_script: ByteVector,
  sequence: Long)

object TxIn {
  val uInt32WithNegValue: Codec[Long] = new LongCodecWithNegValue(32, false, ByteOrdering.BigEndian)

  val scriptCodec = {
    val countCodec = VarInt.varIntCodec.xmap(_.toInt, (i: Int) => i.toLong)
    variableSizeBytes(countCodec, bytes)
  }

  implicit val codec: Codec[TxIn] = {
    ("previous_output" | Codec[OutPoint]) ::
      ("sig_script" | scriptCodec) ::
      ("sequence" | uInt32WithNegValue)
  }.as[TxIn]

}

