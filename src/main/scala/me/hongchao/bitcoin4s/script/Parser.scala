package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.ConstantOp._
import eu.timepit.refined._
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import org.spongycastle.util.encoders.Hex
import shapeless.nat._

import scala.util.control.Exception.allCatch
import scala.annotation.tailrec

class Parser {

  def parse(bytes: Seq[Byte]): Seq[ScriptElement] = {
    parse(bytes, Seq.empty[ScriptElement])
  }

  // * example: "OP_DUP OP_HASH160 e2e7c1ab3f807151e832dd1accb3d4f5d7d19b4b OP_EQUALVERIFY OP_CHECKSIG"
  // * example: ["0", "IF 0x50 ENDIF 1", "P2SH,STRICTENC", "0x50 is reserved (ok if not executed)"] (from script_valid.json) */
  def parse(str: String): Seq[Byte] = {
    val stringTokens = str.split(" ").toList

    def isNumber(str: String) = allCatch.opt(str.toLong).isDefined
    def isHex(str: String) = allCatch.opt {
      assume(str.substring(0, 2) == "0x")
      Hex.decode(str.drop(2))
    }.isDefined
    def isOpCode(str: String) = OpCodes.fromString(str).isDefined

    stringTokens.foldLeft(Seq.empty[Byte])((acc, token) => {
      token match {
        case t if isNumber(t) =>
        case t if isHex(t) =>
        case t if isOpCode(t) =>
        case t =>
          // normal data, push
      }
      acc
    })
  }

  @tailrec
  private def parse(bytes: Seq[Byte], acc: Seq[ScriptElement]): Seq[ScriptElement] = bytes match {
    case Nil => acc
    case head :: tail =>
      val opCode = OpCodes.all.find(_.hex == head.toHex).getOrElse {
        // FIXME: better exception
        throw new RuntimeException(s"No opcode found: $bytes")
      }

      def pushData(opCode: ScriptOpCode, maybeNumberOfBytesToPush: Either[String, Int]): (Seq[Byte], Seq[ScriptElement]) = {
        val numberOfBytesToPush = maybeNumberOfBytesToPush match {
          case Right(number) => number
          case Left(error) => throw new RuntimeException(error)
        }

        // FIXME: Perhaps not enough bytes
        val bytesToPush = tail.drop(numberOfBytesToPush).take(numberOfBytesToPush)
        val restOfBytes = tail.drop(numberOfBytesToPush).drop(numberOfBytesToPush)

        (restOfBytes, opCode +: ScriptConstant(bytesToPush) +: acc)
      }

      val (restOfBytes, newAcc) = opCode match {
        case OP_PUSHDATA(value) =>
          val numberOfBytesToPush = Right(value.toInt)
          pushData(opCode, numberOfBytesToPush)

        case OP_PUSHDATA1 =>
          val numberBytes: Seq[Byte] = tail.take(1)
          val numberOfBytesToPush = refineV[Size[Equal[_1]]](numberBytes).map(toUInt8)
          pushData(opCode, numberOfBytesToPush)

        case OP_PUSHDATA2 =>
          val numberBytes: Seq[Byte] = tail.take(2)
          val numberOfBytesToPush = refineV[Size[Equal[_2]]](numberBytes).map(toUInt16)
          pushData(opCode, numberOfBytesToPush)

        case OP_PUSHDATA4 =>
          val numberBytes: Seq[Byte] = tail.take(4)
          val numberOfBytesToPush = refineV[Size[Equal[_4]]](numberBytes).map(toUInt32).map(_.toInt)
          pushData(opCode,numberOfBytesToPush)

        case otherOpCode =>
          (tail,  otherOpCode +: acc)
      }

      parse(restOfBytes, newAcc)
  }
}
