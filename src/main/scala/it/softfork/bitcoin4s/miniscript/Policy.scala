package it.softfork.bitcoin4s.miniscript

import it.softfork.bitcoin4s.crypto.Hash

sealed trait Policy[Pk <: MiniscriptKey[_]] {
  val description: String
}

object Policy {
  case class Key[Pk <: MiniscriptKey[_]](value: Pk) extends Policy[Pk] {
    override val description: String = "A public key which must sign to satisfy the descriptor"
  }

  case class After[Pk <: MiniscriptKey[_]](value: Long) extends Policy[Pk] {
    override val description: String = "A relative locktime restriction"
  }

  case class Older[Pk <: MiniscriptKey[_]](value: Long) extends Policy[Pk] {
    override val description: String = "An absolute locktime restriction"
  }

  case class Sha256[Pk <: MiniscriptKey[_]](value: Hash.Sha256) extends Policy[Pk] {
    override val description: String = "A SHA256 whose preimage must be provided to satisfy the descriptor"
  }

  case class Hash256[Pk <: MiniscriptKey[_]](value: Hash.Hash256) extends Policy[Pk] {
    override val description: String = "A Hash256 whose preimage must be provided to satisfy the descriptor"
  }

  case class Ripemd160[Pk <: MiniscriptKey[_]](value: Hash.RipeMD160) extends Policy[Pk] {
    override val description: String = "A RIPEMD160 whose preimage must be provided to satisfy the descriptor"
  }

  case class Hash160[Pk <: MiniscriptKey[_]](value: Hash.RipeMD160) extends Policy[Pk] {
    override val description: String = "A HASH160 whose preimage must be provided to satisfy the descriptor"
  }

  case class And[Pk <: MiniscriptKey[_]](values: Seq[Policy[Pk]]) extends Policy[Pk] {
    override val description: String = "A list of sub-policies, all of which must be satisfied"
  }

  case class Or[Pk <: MiniscriptKey[_]](values: Seq[(Double, Policy[Pk])]) extends Policy[Pk] {
    override val description: String = {
      "A list of sub-policies, one of which must be satisfied, along with relative probabilities for reach one"
    }
  }

  case class Threshold[Pk <: MiniscriptKey[_]](k: Int, values: Seq[Policy[Pk]]) extends Policy[Pk] {
    override val description: String = "A set of descriptors, satisfactions must be provided for `k` of them"
  }
}
