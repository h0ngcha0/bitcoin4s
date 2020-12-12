package it.softfork.bitcoin4s.transaction

import play.api.libs.json.Json
import scodec.{Attempt, Codec}

case class TxWitness(witness: Script)

object TxWitness {

  implicit val codec: Codec[TxWitness] =
    Codec[Script].as[TxWitness]
}

case class TxWitnessRaw(witness: String) {
  val hex = witness
}

object TxWitnessRaw {
  implicit val format = Json.format[TxWitnessRaw]

  def apply(txWitness: TxWitness): Attempt[TxWitnessRaw] = {
    TxWitness.codec.encode(txWitness).map { sequenceBitVector =>
      TxWitnessRaw(sequenceBitVector.toHex)
    }
  }
}

case class TxWitnessesRaw(
  count: String,
  txWitnesses: List[TxWitnessRaw]
) {
  val hex = s"$count${txWitnesses.map(_.hex).mkString}"
}

object TxWitnessesRaw {
  implicit val format = Json.format[TxWitnessesRaw]
}
