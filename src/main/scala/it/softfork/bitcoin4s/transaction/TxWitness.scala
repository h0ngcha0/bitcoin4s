package it.softfork.bitcoin4s.transaction

import scodec.{Attempt, Codec}

case class TxWitness(witness: Script)

object TxWitness {
  implicit val codec: Codec[TxWitness] =
    Codec[Script].as[TxWitness]

  case class Raw(witness: String)

  object Raw {
    def apply(txWitness: TxWitness): Attempt[Raw] = {
      codec.encode(txWitness).map { sequenceBitVector =>
        Raw(sequenceBitVector.toHex)
      }
    }
  }
}
