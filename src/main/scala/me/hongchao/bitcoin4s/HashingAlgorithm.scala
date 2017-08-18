package me.hongchao.bitcoin4s

import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.digests.{RIPEMD160Digest, SHA256Digest}


trait HashingAlgorithm {
  val digest: Digest

  def apply(input: Array[Byte]): Array[Byte] = {
    digest.update(input, 0, input.length)
    val result = new Array[Byte](digest.getDigestSize)
    digest.doFinal(result, 0)
    result
  }
}

case object Sha256 extends HashingAlgorithm {
  val digest = new SHA256Digest
}

case object RipeMD160 extends HashingAlgorithm {
  val digest = new RIPEMD160Digest
}