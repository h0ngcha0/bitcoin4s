package it.softfork.bitcoin4s.miniscript

import it.softfork.bitcoin4s.crypto.Hash.Hash160
import it.softfork.bitcoin4s.crypto.PublicKey
import simulacrum._

import scala.language.implicitConversions

@typeclass trait MiniscriptKey[KeyType] {
  type Hash
  def toPubKeyHash(key: KeyType): Hash
}

object MiniscriptKey {

  implicit val publicKey = new MiniscriptKey[PublicKey] {
    type Hash = Array[Byte]

    def toPubKeyHash(publicKey: PublicKey): Hash = {
      Hash160.hash(publicKey.encoded).value
    }
  }

  implicit val string = new MiniscriptKey[String] {
    type Hash = String

    def toPubKeyHash(str: String): Hash = str
  }
}