package it.softfork.bitcoin4s.transaction.structure

import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}
import scodec.bits.{BitVector, ByteOrdering}

// NOTE: Allow -1 so that we can run Bitcoin core tests.
final class LongCodecWithNegValue(bits: Int, signed: Boolean, ordering: ByteOrdering)
    extends Codec[Long] {

  //scalastyle:off magic.number
  require(
    bits > 0 && bits <= (if (signed) 64 else 63),
    "bits must be in range [1, 64] for signed and [1, 63] for unsigned"
  )
  //scalastyle:on magic.number

  val MaxValue = (1L << (if (signed) (bits - 1) else bits)) - 1
  val MinValue = if (signed) -(1L << (bits - 1)) else -1 // allow -1

  private val bitsL = bits.toLong

  private def description: String = s"$bits-bit ${if (signed) "signed" else "unsigned"} integer"

  override def sizeBound: SizeBound = SizeBound.exact(bitsL)

  override def encode(i: Long): Attempt[BitVector] = {
    if (i > MaxValue) {
      Attempt.failure(Err(s"$i is greater than maximum value $MaxValue for $description"))
    } else if (i < MinValue) {
      Attempt.failure(Err(s"$i is less than minimum value $MinValue for $description"))
    } else {
      Attempt.successful(BitVector.fromLong(i, bits, ordering))
    }
  }

  override def decode(buffer: BitVector): Attempt[DecodeResult[Long]] = {
    if (buffer.sizeGreaterThanOrEqual(bitsL)) {
      Attempt.successful(
        DecodeResult(buffer.take(bitsL).toLong(signed, ordering), buffer.drop(bitsL))
      )
    } else {
      Attempt.failure(Err.insufficientBits(bitsL, buffer.size))
    }
  }

  override def toString: String = description
}
