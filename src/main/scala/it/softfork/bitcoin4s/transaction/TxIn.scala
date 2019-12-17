package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.transaction.structure.LongCodecWithNegValue
import OutPoint.{Raw => OutpointRaw}
import scodec.{Attempt, Codec}
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

  case class Raw(
    previous_output: OutpointRaw,
    sig_script: String,
    sequence: String
  )

  object Raw {
    def apply(txIn: TxIn): Attempt[Raw] = {
      for {
        previousOutputRaw <- OutPoint.Raw(txIn.previous_output)
        sigScriptBitVector <- Codec[Script].encode(txIn.sig_script)
        sequenceBitVector <- uInt32WithNegValue.encode(txIn.sequence)
      } yield {
        Raw(
          previous_output = previousOutputRaw,
          sig_script = sigScriptBitVector.toHex,
          sequence = sequenceBitVector.toHex
        )
      }
    }
  }

}
