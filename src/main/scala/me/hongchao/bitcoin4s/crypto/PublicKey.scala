package me.hongchao.bitcoin4s.crypto

import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.math.ec.ECPoint

case class PublicKey(point: ECPoint, compressed: Boolean) {
  def encode(): Seq[Byte] = {
    point.getEncoded(compressed)
  }

  def verify(data: Seq[Byte], signature: Signature): Boolean = {
    val signer = new ECDSASigner
    val params = new ECPublicKeyParameters(point, Secp256k1.curve)
    signer.init(false, params)
    signer.verifySignature(data.toArray, signature.r, signature.s)
  }
}

object PublicKey {
  def decode(data: Seq[Byte]): Option[PublicKey] = {
    def toECPoint = Secp256k1.curve.getCurve.decodePoint(data.toArray)

    data.length match {
      case 65 if data.head == 4 =>
        Some(PublicKey(toECPoint, false))
      case 65 if data.head == 6 || data.head == 7 =>
        Some(PublicKey(toECPoint, false))
      case 33 if data.head == 2 || data.head == 3 =>
        Some(PublicKey(toECPoint, true))
      case _ =>
        None
    }
  }
}
