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

  case class Raw(
    value: String,
    pk_script: String
  )

  object Raw {
    def apply(txIn: TxOut): Attempt[Raw] = {
      for {
        valueBitVector <- int64L.encode(txIn.value)
        pkScriptBitVector <- Codec[Script].encode(txIn.pk_script)
      } yield {
        Raw(
          value = valueBitVector.toHex,
          pk_script = pkScriptBitVector.toHex
        )
      }
    }
  }
}
