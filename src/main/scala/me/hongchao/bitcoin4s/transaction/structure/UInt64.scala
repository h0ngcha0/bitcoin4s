package me.hongchao.bitcoin4s.transaction.structure

import scodec.Codec
import scodec.codecs.int64L

import scala.math.BigInt.{int2bigInt, long2bigInt}

case class UInt64(value: Long) {

  import UInt64._

  override def toString = longToBigInt(value).toString
}

object UInt64 {

  def longToBigInt(unsignedLong: Long): BigInt =
    (BigInt(unsignedLong >>> 1) << 1) + (unsignedLong & 1)

  def bigIntToLong(n: BigInt): Long = {
    val smallestBit = (n & 1).toLong
    ((n >> 1).toLong << 1) | smallestBit
  }

  implicit val codec: Codec[UInt64] = int64L.xmap(UInt64.apply, _.value)

  implicit val bigIntCodec: Codec[BigInt] = Codec[UInt64].xmap(
    n => (UInt64.longToBigInt(n.value)),
    b => UInt64(UInt64.bigIntToLong(b)))
}
