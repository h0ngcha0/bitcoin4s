package it.softfork.bitcoin4s

import it.softfork.bitcoin4s.crypto.Hash.Hash160
import it.softfork.bitcoin4s.crypto.PublicKey
import it.softfork.bitcoin4s.crypto.Base58Check
import it.softfork.bitcoin4s.crypto.Base58Check.VersionPrefix

sealed trait Address {
  val value: String
}

case class P2PKHAddress(value: String) extends Address {

  def fromPublicKey(version: VersionPrefix, publicKey: PublicKey): P2PKHAddress = {
    val hash = Hash160(publicKey.encoded.toArray)
    P2PKHAddress(Base58Check.encode(version.value, hash))
  }
}
