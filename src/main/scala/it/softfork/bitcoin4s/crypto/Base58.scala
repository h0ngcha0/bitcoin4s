package it.softfork.bitcoin4s.crypto

import scala.annotation.tailrec

object Base58 {
  val Alphabets = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
  val AlphabetsWithIndex = Alphabets.zipWithIndex.toMap
  val One = Alphabets.charAt(0)
  val Base = BigInt(58)

  def encode(input: Array[Byte]): String = {
    val inputAsBigInt = BigInt(1, input)
    val encodedBigInt = encodeBigInt(inputAsBigInt)
    val encodedLeadingZeros = input.takeWhile(_ == 0).map(_.toInt).map(Alphabets.charAt)

    (encodedBigInt ++ encodedLeadingZeros).reverse.toString
  }

  def decode(input: String): Seq[Byte] = {
    val (leadingZeros, restOfInput) = input.partition(_ == One) // Index of One is 0
    val restOfInputAsBigInt = restOfInput.foldLeft(BigInt(0)) { (acc, inputChar) =>
      (acc * Base) + BigInt(AlphabetsWithIndex(inputChar))
    }
    val restOfInputAsByteArray = if (restOfInput.isEmpty) {
      Array.emptyByteArray
    } else {
      restOfInputAsBigInt.toByteArray
    }

    leadingZeros.map(_ => 0.toByte) ++ restOfInputAsByteArray
  }

  @tailrec
  private def encodeBigInt(input: BigInt, acc: Seq[Char] = Seq.empty): Seq[Char] = {
    if (input > 0) {
      val reminder = input.mod(Base)
      val quotient = input / Base

      encodeBigInt(quotient, Alphabets.charAt(reminder.intValue) +: acc)
    } else {
      acc
    }
  }
}
