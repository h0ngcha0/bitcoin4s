package it.softfork.bitcoin4s.transaction

import scodec.{Attempt, Codec}

case class TxWitness(witness: Script)

object TxWitness {
  implicit val codec: Codec[TxWitness] =
    Codec[Script].as[TxWitness]

  case class TxWitnessesRaw(
    count: String,
    txWitnesses: List[TxWitnessRaw]
  ) {
    val hex = s"$count${txWitnesses.map(_.hex).mkString}"
  }

  case class TxWitnessRaw(witness: String) {
    val hex = witness
  }

  object TxWitnessRaw {

    def apply(txWitness: TxWitness): Attempt[TxWitnessRaw] = {
      codec.encode(txWitness).map { sequenceBitVector =>
        TxWitnessRaw(sequenceBitVector.toHex)
      }
    }
  }
}
