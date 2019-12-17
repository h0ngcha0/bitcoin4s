package it.softfork.bitcoin4s.transaction

import scodec.{Attempt, Codec}
import scodec.codecs._

case class TxOut(
  value: Long,
  pk_script: Script
)

object TxOut {
  implicit val codec: Codec[TxOut] = {
    ("value" | int64L) ::
      ("pk_script" | Codec[Script])
  }.as[TxOut]
}

case class TxOutsRaw(
  count: String,
  txOuts: List[TxOutRaw]
) {
  val hex = s"$count${txOuts.map(_.hex).mkString}"
}

case class TxOutRaw(
  value: String,
  pk_script: String
) {
  val hex = s"$value$pk_script"
}

object TxOutRaw {

  def apply(txIn: TxOut): Attempt[TxOutRaw] = {
    for {
      valueBitVector <- int64L.encode(txIn.value)
      pkScriptBitVector <- Codec[Script].encode(txIn.pk_script)
    } yield {
      TxOutRaw(
        value = valueBitVector.toHex,
        pk_script = pkScriptBitVector.toHex
      )
    }
  }
}
