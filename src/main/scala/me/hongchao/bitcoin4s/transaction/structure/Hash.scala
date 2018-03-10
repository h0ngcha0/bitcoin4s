package me.hongchao.bitcoin4s.transaction.structure

import scodec.Codec
import scodec.bits.{ByteVector, HexStringSyntax}
import scodec.codecs.bytes

case class Hash(value: ByteVector) {
  require(value.size == 32)

  override def toString = s"${value.toHex}"
}

object Hash {
  implicit val codec: Codec[Hash] = bytes(32)
    .xmap(b => Hash.apply(b.reverse), _.value.reverse)

  val NULL = Hash(hex"0000000000000000000000000000000000000000000000000000000000000000")
}
