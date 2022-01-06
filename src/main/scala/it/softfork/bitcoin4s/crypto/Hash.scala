package it.softfork.bitcoin4s.crypto

import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.digests.{RIPEMD160Digest, SHA1Digest, SHA256Digest, SHA512Digest}
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.spongycastle.crypto.params.KeyParameter

import it.softfork.bitcoin4s.utils.hexToBytes

trait Hash {
  val digest: Digest

  def apply(input: Array[Byte]): Array[Byte] = {
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

  case object Sha256 extends Hash {
    val digest = new SHA256Digest
  }

  object Hash160 {

    def apply(input: Array[Byte]): Array[Byte] = {
      RipeMD160(Sha256(input))
    }
  }

  object Hash256 {

    def apply(input: Array[Byte]): Array[Byte] = {
      Sha256(Sha256(input))
    }
  }

  case object RipeMD160 extends Hash {
    val digest = new RIPEMD160Digest
  }

  case object PBKDF2WithHmacSha512 extends Hash {
    val digest = new SHA512Digest()

    override def apply(input: Array[Byte]): Array[Byte] = {
      apply(input, Array.emptyByteArray)
    }

    //scalastyle:off magic.number
    def apply(input: Array[Byte], salt: Array[Byte]): Array[Byte] = {
      val gen = new PKCS5S2ParametersGenerator(digest)
      gen.init(input, salt, 2048)
      val keyParams = gen.generateDerivedParameters(512).asInstanceOf[KeyParameter]
      keyParams.getKey
    }
    //scalastyle:on magic.number
  }

  val zeros: Array[Byte] = Hash256(
    hexToBytes("0000000000000000000000000000000000000000000000000000000000000000")
  )
}
