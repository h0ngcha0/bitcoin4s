package it.softfork.bitcoin4s.miniscript

import it.softfork.bitcoin4s.crypto.Hash

// Terminal in rust-bitcoin
sealed trait Terminal[Pk <: MiniscriptKey[_]] {
  val description: String
}

// Marked as [***] means can not find it in `http://bitcoin.sipa.be/miniscript/`
object Terminal {
  // boolean values
  // Seems that this is the best we can do for a inherited value that doesn't
  // actually need a type parameter?
  case class True[Pk <: MiniscriptKey[_]]() extends Terminal[Pk] {
    override val description: String = "1"
  }

  case class False[Pk <: MiniscriptKey[_]]() extends Terminal[Pk] {
    override val description: String = "0"
  }

  // pubkey checks
  case class Pk[Pk <: MiniscriptKey[_]](value: Pk) extends Terminal[Pk] {
    override val description: String = "<key>"
  }

  case class PkH[Pk <: MiniscriptKey[_]](value: Pk) extends Terminal[Pk] {
    override val description: String = "DUP HASH160 <keyhash> EQUALVERIFY"
  }

  // timelocks
  case class After[Pk <: MiniscriptKey[_]](value: Long) extends Terminal[Pk] {
    override val description: String = "<n> CHECKSEQUENCEVERIFY"
  }

  case class Older[Pk <: MiniscriptKey[_]](value: Long) extends Terminal[Pk] {
    override val description: String = "<n> CHECKLOCKTIMEVERIFY"
  }

  // hashlocks
  case class Sha256[Pk <: MiniscriptKey[_]](value: Hash.Sha256) extends Terminal[Pk] {
    override val description: String = "SIZE 32 EQUALVERIFY SHA256 <hash> EQUAL"
  }

  case class Hash256[Pk <: MiniscriptKey[_]](value: Hash.Hash256) extends Terminal[Pk] {
    override val description: String = "SIZE 32 EQUALVERIFY HASH256 <hash> EQUAL"
  }

  case class Ripemd160[Pk <: MiniscriptKey[_]](value: Hash.RipeMD160) extends Terminal[Pk] {
    override val description: String = "SIZE 32 EQUALVERIFY RIPEMD160 <hash> EQUAL"
  }

  case class Hash160[Pk <: MiniscriptKey[_]](value: Hash.Hash160) extends Terminal[Pk] {
    override val description: String = "SIZE 32 EQUALVERIFY HASH160 <hash> EQUAL"
  }

  // wrappers
  case class Alt[Pk <: MiniscriptKey[_]](value: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "TOALTSTACK [E] FROMALTSTACK"
  }

  case class Swap[Pk <: MiniscriptKey[_]](value: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "SWAP [E1]"
  }

  case class Check[Pk <: MiniscriptKey[_]](value: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[Kt]/[Ke] CHECKSIG"
  }

  case class DupIf[Pk <: MiniscriptKey[_]](value: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "DUP IF [V] ENDIF"
  }

  case class Verify[Pk <: MiniscriptKey[_]](value: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[T] Verify"
  }

  case class NoneZero[Pk <: MiniscriptKey[_]](value: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "SIZE 0NOTEQUAL IF [Fn] ENDIF"
  }

  case class ZeroNotEqual[Pk <: MiniscriptKey[_]](value: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[X] 0NOTEQUAL"
  }

  // conjunctions
  case class AndV[Pk <: MiniscriptKey[_]](left: Miniscript[Pk], right: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[V] [T]/[V]/[F]/[Kt]"
  }

  case class AndB[Pk <: MiniscriptKey[_]](left: Miniscript[Pk], right: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[E] [W] BOOLAND"
  }

  case class AndOr[Pk <: MiniscriptKey[_]](first: Miniscript[Pk], second: Miniscript[Pk], third: MiniscriptKey[Pk]) extends Terminal[Pk] {
    override val description: String = "[various] NOTIF [various] ELSE [various] ENDIF"
  }

  // disjunction
  case class OrB[Pk <: MiniscriptKey[_]](left: Miniscript[Pk], right: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[E] [W] BOOLOR"
  }

  case class OrD[Pk <: MiniscriptKey[_]](left: Miniscript[Pk], right: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[E] IFDUP NOTIF [T]/[E] ENDIF"
  }

  case class OrC[Pk <: MiniscriptKey[_]](left: Miniscript[Pk], right: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "[E] NOTIF [V] ENDIF"
  }

  case class OrI[Pk <: MiniscriptKey[_]](left: Miniscript[Pk], right: Miniscript[Pk]) extends Terminal[Pk] {
    override val description: String = "IF [various] ELSE [various] ENDIF"
  }

  // thresholds
  case class Thresh[Pk <: MiniscriptKey[_]](k: Int, values: Seq[Miniscript[Pk]]) extends Terminal[Pk] {
    override val description: String = "[E] ([W] ADD)* k EQUAL"
  }

  case class ThreshM[Pk <: MiniscriptKey[_]](k: Int, keys: Seq[Pk]) extends Terminal[Pk] {
    override val description: String = "k (<key>)* n CHECKMULTISIG"
  }
}
