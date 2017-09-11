package me.hongchao.bitcoin4s.crypto

import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.math.ec.ECPoint

case class PublicKey(point: ECPoint, compressed: Boolean) {
  def getByteArray(): Array[Byte] = {
    point.getEncoded(compressed)
  }

  def verify(data: Seq[Byte], signature: Signature): Boolean = {
    signature.decode().exists {
      case (r, s) =>
        val signer = new ECDSASigner
        val params = new ECPublicKeyParameters(point, Secp256k1.curve)
        signer.init(false, params)
        signer.verifySignature(data.toArray, r, s)
    }
  }
}
