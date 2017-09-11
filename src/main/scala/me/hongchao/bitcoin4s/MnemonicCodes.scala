package me.hongchao.bitcoin4s

import me.hongchao.bitcoin4s.crypto.Hash._
import me.hongchao.bitcoin4s.Config.MnemonicCodesConfig

import scala.util.Try

class MnemonicCodes(config: MnemonicCodesConfig) {
  // FIXME: type input to have length [128, 160, 192, 224, 256]
  def fromEntropy(input: Array[Byte]): Seq[String] = {
    val inputAsBinaryString = input.map(byteToPaddedBinaryString).mkString
    val checkSumAsBinaryString = Sha256(input).map(byteToPaddedBinaryString).mkString
    val checkSumBitsLength = input.length / 4
    val allBits = inputAsBinaryString ++ checkSumAsBinaryString.take(checkSumBitsLength)

    allBits.grouped(11).toSeq.map { indexBinaryString =>
      val index = Integer.parseInt(indexBinaryString, 2)
      config.words(index)
    }
  }

  def toEntropy(mnemonicCodes: Seq[String]): Array[Byte] = {
    val mnemonicCodesAsPaddedBinaryString = mnemonicCodes
      .map(config.words.indexOf)
      .map(_.toByte)
      .map(byteToPaddedBinaryString)
    val checkSumLengthInByte = mnemonicCodesAsPaddedBinaryString.length / 8 / 32
    val checkSumIndex = mnemonicCodesAsPaddedBinaryString.length - checkSumLengthInByte / 8
    val (entropyBinaryString, checksumBinaryString) = mnemonicCodesAsPaddedBinaryString.splitAt(checkSumIndex)
    val entropy = entropyBinaryString.mkString.grouped(8).toArray.map { byteAsBinaryString =>
      Integer.parseInt(byteAsBinaryString, 2).toByte
    }

    if (Sha256(entropy).map(byteToPaddedBinaryString).mkString != checksumBinaryString) {
      throw new IllegalArgumentException(s"Incorrect mnemonic codes, checksum mismatched: $mnemonicCodes")
    }

    entropy
  }

  def validMnemonicCodes(mnemonicCodes: Seq[String]): Boolean = {
    Try(toEntropy(mnemonicCodes)).isSuccess
  }

  def toSeed(mnemonicCodes: Seq[String], passphrase: Option[String]): Array[Byte] = {
    val salt = ("mnemonic" + passphrase.getOrElse("")).getBytes
    PBKDF2WithHmacSha512(mnemonicCodes.mkString(" ").getBytes, salt)
  }

  private def byteToPaddedBinaryString(byte: Byte): String = {
    f"${byte.toBinaryString}8s".replace(' ', '0')
  }
}
