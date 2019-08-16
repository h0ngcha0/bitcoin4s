package it.softfork.bitcoin4s.crypto

import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.math.ec.ECPoint
import it.softfork.bitcoin4s.Utils._
import it.softfork.bitcoin4s.crypto.Signature.ECDSASignature

case class PublicKey(point: ECPoint, compressed: Boolean) {
  val encoded: Array[Byte] = point.getEncoded(compressed)

  def verify(data: Seq[Byte], signature: ECDSASignature): Boolean = {
    val signer = new ECDSASigner
    val params = new ECPublicKeyParameters(point, Secp256k1.curve)
    signer.init(false, params)
    signer.verifySignature(data.toArray, signature.r, signature.s)
  }

  def isCompressed(): Boolean = {
    encoded.length == 33 && (encoded(0) == 2 || encoded(0) == 3)
  }

  def isUnCompressed(): Boolean = {
    encoded.length == 65 && encoded(0) == 4
  }
}

object PublicKey {
  sealed trait DecodeResult

  object DecodeResult {
    case class Ok(key: PublicKey) extends DecodeResult
    case object Failure extends DecodeResult
    case class OkButNotStrictEncoded(key: PublicKey) extends DecodeResult
  }

  def decode(data: Seq[Byte], strictEnc: Boolean): DecodeResult = {
    def toECPoint = Secp256k1.curve.getCurve.decodePoint(data.toArray)

    data.length match {
      case 65 if data.head == 4 =>
        DecodeResult.Ok(PublicKey(toECPoint, false))
      case 65 if data.head == 6 || data.head == 7 =>
        val pubKey = PublicKey(toECPoint, false)
        strictEnc
          .option(DecodeResult.OkButNotStrictEncoded(pubKey))
          .getOrElse(DecodeResult.Ok(pubKey))

      case 33 if data.head == 2 || data.head == 3 =>
        DecodeResult.Ok(PublicKey(toECPoint, true))
      case _ =>
        DecodeResult.Failure
    }
  }

}
