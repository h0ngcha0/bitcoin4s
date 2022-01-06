package it.softfork.bitcoin4s.transaction.structure

import scala.language.implicitConversions

import scodec.Codec
import scodec.codecs.listOfN

object VarList {
  val countCodec = VarInt.varIntCodec.xmap(_.toInt, (i: Int) => i.toLong)

  implicit def varList[A](codec: Codec[A]): Codec[List[A]] = {
    listOfN(countCodec, codec)
  }
}
