package it.softfork.bitcoin4s.crypto

import java.security.SecureRandom
import org.spongycastle.crypto.generators.ECKeyPairGenerator
import org.spongycastle.crypto.params.{ECKeyGenerationParameters, ECPrivateKeyParameters}

case class PrivateKey(value: BigInt, compressed: Boolean = false) {

  def apply(byteArray: Array[Byte]): PrivateKey = {
    val uncompressedKeyLength = 32
    val (first32Bytes, restOfTheBytes) = byteArray.splitAt(uncompressedKeyLength)
    val paddingLength = Math.max(uncompressedKeyLength - first32Bytes.length, 0)
    val paddings = Array.fill(paddingLength)(0.toByte)
    val keyBytes = paddings ++ first32Bytes

    restOfTheBytes match {
      case Array.emptyByteArray => PrivateKey(BigInt(keyBytes), compressed = false)
      case Array(1) => PrivateKey(BigInt(keyBytes), compressed = true)
      case _ => throw new IllegalArgumentException(s"Can not convert $byteArray to private key")
    }
  }

  def publicKey() = {
    PublicKey(Secp256k1.curve.getG.multiply(value.bigInteger), compressed)
  }

  def getByteArray() = {
    if (compressed) {
      value.toByteArray :+ 1.toByte
    } else {
      value.toByteArray
    }
  }
}

object PrivateKey {

  def generate(compressed: Boolean) = {
    val secureRandom = new SecureRandom
    val generator = new ECKeyPairGenerator
    val keyGenParams = new ECKeyGenerationParameters(Secp256k1.curve, secureRandom)

    generator.init(keyGenParams)

    val keypair = generator.generateKeyPair
    val privParams = keypair.getPrivate.asInstanceOf[ECPrivateKeyParameters]
    PrivateKey(BigInt(privParams.getD), compressed)
  }
}
