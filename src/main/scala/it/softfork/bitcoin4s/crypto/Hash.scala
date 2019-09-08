package it.softfork.bitcoin4s.crypto

import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.digests.{RIPEMD160Digest, SHA1Digest, SHA256Digest, SHA512Digest}
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.util.encoders.Hex

trait Hash {
  val digest: Digest

  def hashBytes(input: Array[Byte]): Array[Byte] = {
    digest.update(input, 0, input.length)
    val result = new Array[Byte](digest.getDigestSize)
    digest.doFinal(result, 0)
    result
  }
}

object Hash {

  case object Sha1 extends Hash {
    val digest = new SHA1Digest
  }

  case class Sha256(value: Array[Byte])
  case object Sha256 extends Hash {
    val digest = new SHA256Digest

    def hash(input: Array[Byte]): Sha256 = {
      Sha256(hashBytes(input))
    }
  }

  case class Hash160(value: Array[Byte])
  object Hash160 {
    def hash(input: Array[Byte]): Hash160 = {
      Hash160(RipeMD160.hashBytes(Sha256.hashBytes(input)))
    }

    def hashBytes(input: Array[Byte]): Array[Byte] = {
      hash(input).value
    }
  }

  case class Hash256(value: Array[Byte])
  object Hash256 {
    def hash(input: Array[Byte]): Hash256 = {
      Hash256(Sha256.hashBytes(Sha256.hashBytes(input)))
    }

    def hashBytes(input: Array[Byte]): Array[Byte] = {
      hash(input).value
    }
  }

  case class RipeMD160(value: Array[Byte])
  case object RipeMD160 extends Hash {
    val digest = new RIPEMD160Digest

    def hash(input: Array[Byte]): RipeMD160 = {
      RipeMD160(Sha256.hashBytes(Sha256.hashBytes(input)))
    }
  }

  case class PBKDF2WithHmacSha512(value: Array[Byte])
  case object PBKDF2WithHmacSha512 extends Hash {
    val digest = new SHA512Digest()

    def hash(input: Array[Byte]): PBKDF2WithHmacSha512 = {
      hash(input, Array.emptyByteArray)
    }

    def hash(input: Array[Byte], salt: Array[Byte]): PBKDF2WithHmacSha512 = {
      val gen = new PKCS5S2ParametersGenerator(digest)
      gen.init(input, salt, 2048)
      val keyParams = gen.generateDerivedParameters(512).asInstanceOf[KeyParameter]
      PBKDF2WithHmacSha512(keyParams.getKey)
    }
  }

  val zeros: Array[Byte] = Hash256.hash(fromHex("0000000000000000000000000000000000000000000000000000000000000000")).value

  def fromHex(hex: String): Array[Byte] = {
    Hex.decode(hex.stripPrefix("0x"))
  }
}
