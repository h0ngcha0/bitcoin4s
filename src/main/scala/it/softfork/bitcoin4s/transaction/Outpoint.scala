package it.softfork.bitcoin4s.transaction

import play.api.libs.json.Json
import scodec.{Attempt, Codec}
import scodec.bits.ByteOrdering
import scodec.codecs._

import it.softfork.bitcoin4s.transaction.structure._

case class OutPoint(
  hash: Hash,
  index: Long
)

object OutPoint {
  val uInt32WithNegValue: Codec[Long] =
    new LongCodecWithNegValue(32, false, ByteOrdering.LittleEndian)

  implicit val codec: Codec[OutPoint] = {
    ("hash" | Codec[Hash]) ::
      ("index" | uInt32WithNegValue)
  }.as[OutPoint]
}

case class OutPointRaw(
  hash: String,
  index: String
) {
  val hex = s"$hash$index"
}

object OutPointRaw {
  implicit val format = Json.format[OutPointRaw]

  def apply(outpoint: OutPoint): Attempt[OutPointRaw] = {
    for {
      hashBitVector <- Codec[Hash].encode(outpoint.hash)
      indexBitVector <- OutPoint.uInt32WithNegValue.encode(outpoint.index)
    } yield {
      OutPointRaw(hash = hashBitVector.toHex, index = indexBitVector.toHex)
    }
  }
}
