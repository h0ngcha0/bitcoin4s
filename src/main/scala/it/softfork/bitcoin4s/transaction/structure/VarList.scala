package it.softfork.bitcoin4s.transaction.structure

import scodec.Codec
import scodec.codecs.listOfN

import scala.language.implicitConversions

object VarList {

  implicit def varList[A](codec: Codec[A]): Codec[List[A]] = {
    val countCodec = VarInt.varIntCodec.xmap(_.toInt, (i: Int) => i.toLong)
    listOfN(countCodec, codec)
  }
}
