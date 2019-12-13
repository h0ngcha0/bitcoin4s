package it.softfork.bitcoin4s.transaction

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, variableSizeBytes}
import it.softfork.bitcoin4s.transaction.structure.VarInt

case class Script(value: ByteVector) {
  val hex = value.toHex
  override def toString = s"Script(0x$hex)"
}

object Script {
  implicit val codec: Codec[Script] = {
    val countCodec = VarInt.varIntCodec.xmap(_.toInt, (i: Int) => i.toLong)
    variableSizeBytes(countCodec, bytes).as[Script]
  }

  val empty = Script(ByteVector.empty)
}
