package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.ConstantOp._
import eu.timepit.refined._
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import shapeless.nat._

import scala.annotation.tailrec

class Parser {


  def parse(bytes: Seq[Byte]): Seq[ScriptElement] = {
    parse(bytes, Seq.empty[ScriptElement])
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
