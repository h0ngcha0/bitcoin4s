package it.softfork.bitcoin4s.transaction

import scodec.Codec
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
