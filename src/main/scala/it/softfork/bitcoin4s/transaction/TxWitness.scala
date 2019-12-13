package it.softfork.bitcoin4s.transaction

import scodec.Codec

case class TxWitness(witness: Script)

object TxWitness {
  implicit val codec: Codec[TxWitness] =
    Codec[Script].as[TxWitness]
}