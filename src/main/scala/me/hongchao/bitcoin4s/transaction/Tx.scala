package me.hongchao.bitcoin4s.transaction

import scodec.Codec
import scodec.codecs._

case class Tx(
  version: Long,
  tx_in: List[TxIn],
  tx_out: List[TxOut],
  lock_time: Long
)

object Tx {
  def codec(version: Int) = {
    ("version" | uint32L) ::
      ("tx_in" | VarList.varList(Codec[TxIn])) ::
      ("tx_out" | VarList.varList(Codec[TxOut])) ::
      ("lock_time" | uint32L)
  }.as[Tx]
  def command = "tx"
}
