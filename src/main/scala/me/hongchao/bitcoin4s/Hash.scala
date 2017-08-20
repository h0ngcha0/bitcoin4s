package me.hongchao.bitcoin4s

import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.digests.{RIPEMD160Digest, SHA256Digest, SHA512Digest}
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.spongycastle.crypto.params.KeyParameter


trait Hash {
  val digest: Digest

  def apply(input: Array[Byte]): Array[Byte] = {
    digest.update(input, 0, input.length)
    val result = new Array[Byte](digest.getDigestSize)
    digest.doFinal(result, 0)
    result
  }
}

case object Sha256 extends Hash {
  val digest = new SHA256Digest
}

case object RipeMD160 extends Hash {
  val digest = new RIPEMD160Digest
}

case object PBKDF2WithHmacSha512 extends Hash {
  val digest = new SHA512Digest()

  override def apply(input: Array[Byte]): Array[Byte] = {
    apply(input, "".getBytes)
  }

  def apply(input: Array[Byte], salt: Array[Byte]): Array[Byte] = {
    val gen = new PKCS5S2ParametersGenerator(digest)
    gen.init(input, salt, 2048)
    val keyParams = gen.generateDerivedParameters(512).asInstanceOf[KeyParameter]
    keyParams.getKey
  }
}