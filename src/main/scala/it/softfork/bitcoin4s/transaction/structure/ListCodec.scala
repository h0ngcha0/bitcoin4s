package it.softfork.bitcoin4s.transaction.structure

import scodec._
import scodec.bits.BitVector

final class ListCodec[A](codec: Codec[A], limit: Option[Int] = None) extends Codec[List[A]] {

  def sizeBound: SizeBound = limit match {
    case None => SizeBound.unknown
    case Some(lim) => codec.sizeBound * lim.toLong
  }

  def encode(list: List[A]): Attempt[BitVector] = Encoder.encodeSeq(codec)(list)

  def decode(buffer: BitVector): Attempt[DecodeResult[List[A]]] =
    Decoder.decodeCollect[List, A](codec, limit)(buffer)

  override def toString: String = s"list($codec)"
}
