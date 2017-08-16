package me.hongchao.bitcoin4s

import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.digests.{RIPEMD160Digest, SHA256Digest}


trait Hash {
  val digest: Digest

  def apply(input: Seq[Byte]): Array[Byte] = {
    digest.update(input.toArray, 0, input.length)
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