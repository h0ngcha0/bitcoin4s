package me.hongchao.bitcoin4s

import org.spongycastle.math.ec.ECPoint

case class PublicKey(point: ECPoint, compressed: Boolean) {
  def getByteArray(): Array[Byte] = {
    point.getEncoded(compressed)
  }
}
