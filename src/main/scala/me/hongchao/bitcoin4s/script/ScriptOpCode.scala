package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.Utils._

// Reference: https://en.bitcoin.it/wiki/Script

trait ScriptElement {
  val bytes: Seq[Byte]
}

trait ScriptConstant extends ScriptElement

object ScriptConstant {
  def apply(bytesIn: Seq[Byte]) = new ScriptConstant { override val bytes = bytesIn }
}

// Reference: https://github.com/bitcoin/bitcoin/blob/master/src/script/script.h#L205
case class ScriptNum(value: Long) extends ScriptConstant {
  override val bytes = ???

  def == (that: ScriptNum) = value == that.value
  def != (that: ScriptNum) = value != that.value
  def <= (that: ScriptNum) = value <= that.value
  def <  (that: ScriptNum) = value < that.value
  def >= (that: ScriptNum) = value >= that.value
  def >  (that: ScriptNum) = value > that.value

  def +  (that: ScriptNum) = ScriptNum(value + that.value)
  def +  (that: Long) = ScriptNum(value + that)
  def -  (that: ScriptNum) = ScriptNum(value - that.value)
  def -  (that: Long) = ScriptNum(value - that)
  def *  (that: ScriptNum) = ScriptNum(value * that.value)
  def *  (that: Long) = ScriptNum(value * that)
}

object ScriptNum {
  val DefaultMaxNumSize = 4

  // Does not accept byte array with more than 4 bytes
  def apply(bytes: Seq[Byte], fRequireMinimal: Boolean, maxNumSize: Int = DefaultMaxNumSize): ScriptNum = {
    require(bytes.length <= DefaultMaxNumSize, s"bytes length exceeds maxNumSize $maxNumSize")
    val violateMinimalEncoding = fRequireMinimal && minimallyEncoded(bytes)
    require(!violateMinimalEncoding, "non-minimally encoded script number")
    ScriptNum(toLong(bytes))
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

  private def toLong(bytes: Seq[Byte]): Long = {
    var result = 0L
    for (i <- 0 until bytes.size) {
      result |= bytes(i) << (8 * i)
    }

    // If the input vector's most significant byte is 0x80, remove it from
    // the result's msb and return a negative.
    if ((bytes.last & 0x80) != 0)
      -(result & ~(0x80L << (8 * (bytes.size - 1))))
    else
      result
  }

  // Check the byte - excluding the sign bit - is zero
  private def equalToZero(byte: Byte) = (byte & 0x7f) == 0

  private def significantBitNotSet(byte: Byte) = (byte & 0x80) == 0

}

trait ScriptOpCode extends ScriptElement with Product {
  val value: Long
  val hex: String = value.toHex
  val name: String = productPrefix
  override val bytes = Seq() // FIXME: decode hex
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
}
