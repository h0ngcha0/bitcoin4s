package it.softfork.bitcoin4s.transaction

import play.api.libs.json.Json
import scodec.{Attempt, Codec}
import scodec.bits.ByteOrdering
import scodec.codecs._

import it.softfork.bitcoin4s.transaction.structure.LongCodecWithNegValue

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

case class TxInRaw(
  previousOutput: OutPointRaw,
  sigScript: String,
  sequence: String
) {
  val hex = s"${previousOutput.hex}$sigScript$sequence"
}

object TxInRaw {
  implicit val format = Json.format[TxInRaw]

  def apply(txIn: TxIn): Attempt[TxInRaw] = {
    for {
      previousOutputRaw <- OutPointRaw(txIn.previous_output)
      sigScriptBitVector <- Codec[Script].encode(txIn.sig_script)
      sequenceBitVector <- TxIn.uInt32WithNegValue.encode(txIn.sequence)
    } yield {
      TxInRaw(
        previousOutput = previousOutputRaw,
        sigScript = sigScriptBitVector.toHex,
        sequence = sequenceBitVector.toHex
      )
    }
  }
}

case class TxInsRaw(
  count: String,
  txIns: List[TxInRaw]
) {
  val hex = s"$count${txIns.map(_.hex).mkString}"
}

object TxInsRaw {
  implicit val format = Json.format[TxInsRaw]
}
