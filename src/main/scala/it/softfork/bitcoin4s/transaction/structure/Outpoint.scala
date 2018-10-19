package it.softfork.bitcoin4s.transaction.structure

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs._

case class OutPoint(
  hash: Hash,
  index: Long
)

object OutPoint {
  val uInt32WithNegValue: Codec[Long] = new LongCodecWithNegValue(32, false, ByteOrdering.LittleEndian)

  implicit val codec: Codec[OutPoint] = {
    ("hash" | Codec[Hash]) ::
      ("index" | uInt32WithNegValue)
  }.as[OutPoint]

}