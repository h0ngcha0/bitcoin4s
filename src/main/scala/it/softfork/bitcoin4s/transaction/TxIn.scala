package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.transaction.structure.{LongCodecWithNegValue}
import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs._

case class TxIn(
  previous_output: OutPoint,
  sig_script: Script,
  sequence: Long
)

object TxIn {
  val uInt32WithNegValue: Codec[Long] = new LongCodecWithNegValue(32, false, ByteOrdering.LittleEndian)

  implicit val codec: Codec[TxIn] = {
    ("previous_output" | Codec[OutPoint]) ::
      ("sig_script" | Codec[Script]) ::
      ("sequence" | uInt32WithNegValue)
  }.as[TxIn]

}
