package me.hongchao.bitcoin4s

import me.hongchao.bitcoin4s.Base58Check.VersionPrefix

sealed trait Address {
  val value: String
}

case class P2PKHAddress(value: String) extends Address {
  def fromPublicKey(version: VersionPrefix, publicKey: PublicKey): P2PKHAddress = {
    val hash = RipeMD160(Sha256(publicKey.getByteArray()))

    P2PKHAddress(Base58Check.encode(version.value, hash))
  }
}