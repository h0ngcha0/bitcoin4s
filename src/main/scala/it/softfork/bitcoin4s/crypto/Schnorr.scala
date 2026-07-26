package it.softfork.bitcoin4s.crypto

import java.math.BigInteger

import scala.util.Try

object Schnorr {
  private val fieldOrder = Secp256k1.params.getCurve.getField.getCharacteristic
  private val curveOrder = Secp256k1.params.getN

  def verify(publicKey: Seq[Byte], message: Seq[Byte], signature: Seq[Byte]): Boolean = {
    if (publicKey.length != 32 || signature.length != 64) {
      false
    } else {
      Try {
        val point = liftX(publicKey).get
        val r = unsigned(signature.take(32))
        val s = unsigned(signature.drop(32))

        if (r.compareTo(fieldOrder) >= 0 || s.compareTo(curveOrder) >= 0) {
          false
        } else {
          val challenge = unsigned(
            Hash.TaggedSha256(
              "BIP0340/challenge",
              (signature.take(32) ++ publicKey ++ message).toArray
            ).toIndexedSeq
          ).mod(curveOrder)
          val reconstructed =
            Secp256k1.params.getG.multiply(s).subtract(point.multiply(challenge)).normalize()

          !reconstructed.isInfinity &&
          !reconstructed.getAffineYCoord.toBigInteger.testBit(0) &&
          reconstructed.getAffineXCoord.toBigInteger == r
        }
      }.getOrElse(false)
    }
  }

  private[bitcoin4s] def liftX(bytes: Seq[Byte]) = {
    if (bytes.length != 32 || unsigned(bytes).compareTo(fieldOrder) >= 0) {
      None
    } else {
      Try {
        val compressed = 0x02.toByte +: bytes
        Secp256k1.params.getCurve.decodePoint(compressed.toArray).normalize()
      }.toOption.filter(point => point.isValid && !point.isInfinity)
    }
  }

  private[bitcoin4s] def unsigned(bytes: Seq[Byte]): BigInteger = {
    new BigInteger(1, bytes.toArray)
  }
}
