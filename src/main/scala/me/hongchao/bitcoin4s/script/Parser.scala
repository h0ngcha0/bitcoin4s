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

object Parser {

  def parse(bytes: Seq[Byte]): Seq[ScriptElement] = {
    parse(bytes, Seq.empty[ScriptElement])
  }

  // * example: "OP_DUP OP_HASH160 e2e7c1ab3f807151e832dd1accb3d4f5d7d19b4b OP_EQUALVERIFY OP_CHECKSIG"
  // * example: ["0", "IF 0x50 ENDIF 1", "P2SH,STRICTENC", "0x50 is reserved (ok if not executed)"] (from script_valid.json) */
  def parse(str: String): Seq[ScriptElement] = {
    val stringTokens = str.split(" ").toList
    val bytes: Seq[Byte] = parseTokensToBytes(stringTokens)
    parse(bytes)
  }

  @tailrec
  private def parseTokensToBytes(tokens: List[String], acc: Seq[Byte] = Seq.empty): Seq[Byte] = {
    tokens match {
      case head :: tail =>
        head match {
          case "0" =>
            parseTokensToBytes(tail, OP_0.bytes ++ acc)
          case "" =>
            parseTokensToBytes(tail, acc)
          case "-1" =>
            parseTokensToBytes(tail, OP_1NEGATE.bytes ++ acc)
          case t if isNumber(t) =>
            val dataBytes: Seq[Byte] = ScriptNum.encode(t.toLong)
            parseTokensToBytes(tail, bytesAndLength(dataBytes) ++ acc)
          case t if isHex(t) =>
            parseTokensToBytes(tail, Hex.decode(t.drop(2)) ++ acc)
          case t if isOpCode(t) =>
            val opCode = OpCodes.fromString(t).get // FIXME: remove .get
            parseTokensToBytes(tail, opCode.bytes ++ acc)
          case t if t.length >= 2 && t.head == '\'' && t.last == '\'' =>
            val unquotedString = t.tail.dropRight(1)
            parseTokensToBytes(unquotedString :: tail, acc)
          case t =>
            val dataBytes = t.getBytes()
            parseTokensToBytes(tail, bytesAndLength(dataBytes) ++ acc)
        }
      case Nil =>
        acc
    }
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

  private def bytesAndLength(dataBytes: Seq[Byte]): Seq[Byte] = {
    val dataBytesLength = dataBytes.length

    val lengthBytes: Seq[Byte] = if (dataBytesLength <= 75) {
      OP_PUSHDATA(dataBytesLength).bytes
    } else {
      val numberOfBytesToPush = ScriptNum.encode(dataBytesLength)

      val pushOpCode: ConstantOp =
        if (dataBytesLength < Byte.MaxValue) {
          OP_PUSHDATA1
        } else if (dataBytesLength < Short.MaxValue) {
          OP_PUSHDATA2
        } else if (dataBytesLength < Int.MaxValue) {
          OP_PUSHDATA4
        } else {
          throw new RuntimeException(s"Can not push $dataBytesLength bytes")
        }

      numberOfBytesToPush ++ pushOpCode.bytes
    }

    lengthBytes ++ dataBytes
  }

  private def isNumber(str: String) = {
    allCatch.opt(str.toLong).isDefined
  }

  private def isHex(str: String) = {
    allCatch.opt {
      assume(str.substring(0, 2) == "0x")
      Hex.decode(str.drop(2))
    }.isDefined
  }

  private def isOpCode(str: String) = {
    OpCodes.fromString(str).isDefined
  }
}
