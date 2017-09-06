package me.hongchao.bitcoin4s.script

import eu.timepit.refined.api.Refined
import shapeless.nat._
import eu.timepit.refined.collection.MaxSize

// Reference: https://github.com/bitcoin/bitcoin/blob/master/src/script/script.h#L205
case class ScriptNum(value: Long) extends ScriptConstant {
  override val bytes = ScriptNum.serialize(value)

  def == (that: ScriptNum) = value == that.value
  def != (that: ScriptNum) = value != that.value
  def <= (that: ScriptNum) = value <= that.value
  def <  (that: ScriptNum) = value < that.value
  def >= (that: ScriptNum) = value >= that.value
  def >  (that: ScriptNum) = value > that.value

  def +  (that: ScriptNum) = ScriptNum(value + that.value)
  def -  (that: ScriptNum) = ScriptNum(value - that.value)
}

object ScriptNum {
  val DefaultMaxNumSize = 4

  // Does not accept byte array with more than 4 bytes
  def apply(bytes: Seq[Byte] Refined MaxSize[_4], fRequireMinimal: Boolean, maxNumSize: Int = DefaultMaxNumSize): ScriptNum = {
    val violateMinimalEncoding = fRequireMinimal && minimallyEncoded(bytes.value)
    require(!violateMinimalEncoding, "non-minimally encoded script number")
    ScriptNum(toLong(bytes.value))
  }

  def serialize(value: Long): Seq[Byte] = {
    ???
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
