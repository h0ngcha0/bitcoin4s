package me.hongchao.bitcoin4s

import io.github.yzernik.bitcoinscodec.messages.Tx

object TransactionOps {
  implicit class Transaction(tx: Tx) {
    def hash(): Seq[Byte] = {
      ???
    }
  }
}

