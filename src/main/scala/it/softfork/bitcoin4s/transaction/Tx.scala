package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.transaction.structure.VarList
import scodec.Codec
import scodec.codecs._

// Credit: https://github.com/yzernik/bitcoin-scodec

case class TxId(value: String)

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
}
