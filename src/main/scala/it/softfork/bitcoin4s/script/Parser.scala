package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.Utils._
import it.softfork.bitcoin4s.script.ConstantOp._
import it.softfork.bitcoin4s.script.PseudoOp.OP_INVALIDOPCODE
import it.softfork.bitcoin4s.script.ReservedOp.OP_NOP10
import org.spongycastle.util.encoders.Hex

import scala.util.control.Exception.allCatch
import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq

object Parser {

  def parse(bytes: Seq[Byte]): Seq[ScriptElement] = {
    parse(bytes, Seq.empty).reverse.flatten
  }

  // * example: "OP_DUP OP_HASH160 e2e7c1ab3f807151e832dd1accb3d4f5d7d19b4b OP_EQUALVERIFY OP_CHECKSIG"
  // * example: ["0", "IF 0x50 ENDIF 1", "P2SH,STRICTENC", "0x50 is reserved (ok if not executed)"] (from script_valid.json) */
  def parse(str: String): Seq[ScriptElement] = {
    val stringTokens = str.split(" ").toList
    val bytes: Seq[Byte] = parseTokensToBytes(stringTokens).reverse.flatten

    parse(bytes)
  }

  @tailrec
  private def parseTokensToBytes(tokens: List[String], acc: Seq[Seq[Byte]] = Seq.empty): Seq[Seq[Byte]] = {
    tokens match {
      case head :: tail =>
        head match {
          case "0" =>
            parseTokensToBytes(tail, OP_0.bytes +: acc)
          case "" =>
            parseTokensToBytes(tail, acc)
          case "-1" =>
            parseTokensToBytes(tail, OP_1NEGATE.bytes +: acc)
          case t if isNumber(t) =>
            val number = t.toLong
            val bytes: Seq[Byte] =
              if (number >= 1 && number < 16) {
                val opValue = OP_1.value + number - 1
                ConstantOp.all.find(_.value == opValue).map(_.bytes).get // Must exist
              } else {
                bytesAndLength(ScriptNum.encode(number))
              }
            parseTokensToBytes(tail, bytes +: acc)
          case t if isHex(t) =>
            parseTokensToBytes(tail, Hex.decode(t.drop(2)).toSeq +: acc)
          case t if isOpCode(t) =>
            val opCode = OpCodes.fromString(t).get // FIXME: remove .get
            parseTokensToBytes(tail, opCode.bytes +: acc)
          case t if t.length >= 2 && t.head == '\'' && t.last == '\'' =>
            val unquotedString = t.tail.dropRight(1)

            if (unquotedString == "") {
              parseTokensToBytes(tail, OP_0.bytes +: acc)
            } else {
              parseTokensToBytes(tail, bytesAndLength(ArraySeq.unsafeWrapArray(unquotedString.getBytes())) +: acc)
            }

          case t =>
            val dataBytes = t.getBytes()
            parseTokensToBytes(tail, bytesAndLength(ArraySeq.unsafeWrapArray(dataBytes)) +: acc)
        }
      case Nil =>
        acc
    }
  }

  @tailrec
  private def parse(bytes: Seq[Byte], acc: Seq[Seq[ScriptElement]]): Seq[Seq[ScriptElement]] = {
    bytes match {
      case Nil => acc
      case head +: tail =>
        val opCode = OpCodes.all
          .find(_.hex == head.toHex)
          .orElse {
            val opCodeValue = Integer.parseInt(head.toHex, 16)
            val isInvalidOpCode = (opCodeValue > OP_NOP10.value) && (opCodeValue < OP_INVALIDOPCODE.value)
            isInvalidOpCode.option(OP_INVALIDOPCODE)
          }
          .getOrElse {
            // FIXME: better exception
            throw new RuntimeException(s"No opcode found: $bytes")
          }

        def pushData(opCode: ScriptOpCode, numberOfBytesToPush: Int, restOfData: Seq[Byte]): (Seq[Byte], Seq[Seq[ScriptElement]]) = {
          val maybeBytesToPush = restOfData.takeOpt(numberOfBytesToPush)
          val restOfBytes = restOfData.drop(numberOfBytesToPush)
          val maybeConstantToBePushed = maybeBytesToPush.map { bytesToPush =>
            (bytesToPush.isEmpty).option(OP_0).getOrElse(ScriptConstant(bytesToPush.toArray))
          }

          (restOfBytes, (Seq(opCode) ++ maybeConstantToBePushed.toList) +: acc)
        }

        val (restOfBytes, newAcc) = opCode match {
          case OP_PUSHDATA(value) =>
            pushData(opCode, value.toInt, tail)

          case OP_PUSHDATA1 =>
            val numberOfBytesToPush = bytesToUInt8(tail.forceTake(1))
            pushData(opCode, numberOfBytesToPush, tail.drop(1))

          case OP_PUSHDATA2 =>
            val numberOfBytesToPush = bytesToUInt16(tail.forceTake(2))
            pushData(opCode, numberOfBytesToPush, tail.drop(2))

          case OP_PUSHDATA4 =>
            val numberOfBytesToPush = bytesToUInt32(tail.forceTake(4))
            pushData(opCode, numberOfBytesToPush, tail.drop(4))

          case otherOpCode =>
            (tail, Seq(otherOpCode) +: acc)
        }

        parse(restOfBytes, newAcc)
    }
  }

  private def bytesAndLength(dataBytes: Seq[Byte]): Seq[Byte] = {
    val dataBytesLength = dataBytes.length.toLong

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

      pushOpCode.bytes ++ numberOfBytesToPush
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
