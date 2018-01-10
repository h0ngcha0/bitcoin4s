package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.Utils._
import org.spongycastle.util.encoders.Hex

import scala.collection.mutable.ArrayBuffer

// Reference: https://en.bitcoin.it/wiki/Script

trait ScriptElement {
  def bytes: Seq[Byte]
}

trait ScriptConstant extends ScriptElement {
  override def toString: String = s"ScriptConstant: $bytes"
}

object ScriptConstant {
  def apply(bytesIn: Seq[Byte]) = new ScriptConstant { override val bytes = bytesIn }
}

// Reference: https://github.com/bitcoin/bitcoin/blob/master/src/script/script.h#L205
trait ScriptNum extends ScriptConstant {
  val value: Long

  def == (that: ScriptNum) = value == that.value
  def == (that: Long) = value == that
  def != (that: Long) = value != that
  def != (that: ScriptNum) = value != that.value
  def <= (that: ScriptNum) = value <= that.value
  def <  (that: ScriptNum) = value < that.value
  def >= (that: ScriptNum) = value >= that.value
  def >  (that: ScriptNum) = value > that.value

  def +  (that: ScriptNum) = ScriptNum(value + that.value)
  def +  (that: Long) = ScriptNum(value + that)
  def -  (that: ScriptNum) = ScriptNum(value - that.value)
  def -  (that: Long) = ScriptNum(value - that)

  override def toString: String = s"ScriptNum($value)"
}

object ScriptNum {
  val DefaultMaxNumSize = 4

  def apply(valueIn: Long) = new ScriptNum {
    override val value: Long = valueIn
    override val bytes = encode(valueIn)
  }

  // Does not accept byte array with more than 4 bytes
  def apply(bytesIn: Seq[Byte], fRequireMinimal: Boolean, maxNumSize: Int = DefaultMaxNumSize): ScriptNum = {
    require(bytesIn.length <= maxNumSize, s"bytes length exceeds maxNumSize $maxNumSize")
    val violateMinimalEncoding = fRequireMinimal && minimallyEncoded(bytesIn)
    require(!violateMinimalEncoding, "non-minimally encoded script number")
    new ScriptNum {
      override val value = toLong(bytesIn)
      override val bytes = bytesIn
    }
  }

  // copied from bitcoin-core code.
  def encode(value: Long): Seq[Byte] = {
    if (value == 0) {
      Seq.empty[Byte]
    } else {
      val result = ArrayBuffer.empty[Byte]
      val neg = value < 0
      var absoluteValue = Math.abs(value)

      while (absoluteValue > 0) {
        result += (absoluteValue & 0xff).toByte
        absoluteValue >>= 8
      }

      //    - If the most significant byte is >= 0x80 and the value is positive, push a
      //    new zero-byte to make the significant byte < 0x80 again.

      //    - If the most significant byte is >= 0x80 and the value is negative, push a
      //    new 0x80 byte that will be popped off when converting to an integral.

      //    - If the most significant byte is < 0x80 and the value is negative, add
      //    0x80 to it, since it will be subtracted and interpreted as a negative when
      //    converting to an integral.

      if ((result.last & 0x80) != 0) {
        result += {
          if (neg) 0x80.toByte else 0
        }
      } else if (neg) {
        result(result.length - 1) = (result(result.length - 1) | 0x80).toByte
      }

      result
    }
  }

  def toLong(bytes : Seq[Byte]) : Long = {
    val bytesReversed = bytes.reverse

    (bytes.size == 0 || bytes.size == 1 && bytes.head == -128).option(0L).getOrElse {
      val isNegative = !bytesReversed.headOption.exists(significantBitNotSet)
      val positiveBytes = isNegative.option(setPositive(bytesReversed)).getOrElse(bytesReversed)
      val isFirstByteZero = positiveBytes.headOption.exists(equalToZero)
      val noLeadingZeroBytes = (isFirstByteZero).option(positiveBytes.tail).getOrElse(positiveBytes)
      val positiveValue = parseLong(noLeadingZeroBytes)

      isNegative.option(-positiveValue).getOrElse(positiveValue)
    }
  }

  private def setPositive(bytes : Seq[Byte]): Seq[Byte] = {
    bytes match {
      case head +: tail =>
        (head & 0x7F).toByte +: tail
      case Nil =>
        Nil
    }
  }

  private def parseLong(bytes : Seq[Byte]): Long = {
    (bytes.isEmpty).option(0L).getOrElse {
      java.lang.Long.parseLong(bytes.toHex,16)
    }
  }

  // Check the byte - excluding the sign bit - is zero
  private def equalToZero(byte: Byte): Boolean = {
    (byte & 0x7f) == 0
  }

  private def significantBitNotSet(byte: Byte): Boolean = {
    (byte & 0x80) == 0
  }

  private def minimallyEncoded(bytes: Seq[Byte]): Boolean = {
    bytes.reverse match {
      case Nil =>
        false
      case first :: Nil =>
        equalToZero(first)
      case first :: second :: _ =>
        equalToZero(first) && significantBitNotSet(second)
    }
  }
}

trait ScriptOpCode extends ScriptElement with Product {
  val value: Long
  def hex: String = value.toHex
  val name: String = productPrefix
  override def bytes = Hex.decode(hex.stripPrefix("0x")).toList
}

object OpCodes {
  val all = ArithmeticOp.all ++
    BitwiseLogicOp.all ++
    ConstantOp.all ++
    CryptoOp.all ++
    FlowControlOp.all ++
    LocktimeOp.all ++
    PseudoOp.all ++
    ReservedOp.all ++
    SpliceOp.all ++
    StackOp.all

  val disabled = SpliceOp.disabled ++
    BitwiseLogicOp.disabled ++
    ArithmeticOp.disabled

  val invalid = Seq(ReservedOp.OP_VERIF, ReservedOp.OP_VERNOTIF)

  // An non-existent op code, only used internally.
  case object OP_UNKNOWN extends ScriptOpCode {
    val value = -1
  }

  def isNonReservedOpCode(scriptElement: ScriptElement): Boolean = {
    val validOpCodes = all.filterNot(ReservedOp.all.contains)
    validOpCodes.find(_.hex == scriptElement.bytes.toHex).isDefined
  }

  // e.g. `OP_DUP` or `DUP`
  def fromString(str: String): Option[ScriptOpCode] = {
    all.find(_.name == str)
      .orElse(all.find(_.name.replace("OP_", "") == str))
  }
}
