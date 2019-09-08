package it.softfork.bitcoin4s.miniscript

// Terminal in rust-bitcoin
sealed trait AbstractSyntaxTree[Pk <: MiniscriptKey[_]] {
  val description: String
}

// Marked as [***] means can not find it in `http://bitcoin.sipa.be/miniscript/`
object AbstractSyntaxTree {
  // boolean values
  // Seems that this is the best we can do for a inherited value that doesn't
  // actually need a type parameter?
  case class True[Pk <: MiniscriptKey[_]]() extends AbstractSyntaxTree[Pk] {
    override val description: String = "1"
  }

  case class False[Pk <: MiniscriptKey[_]]() extends AbstractSyntaxTree[Pk] {
    override val description: String = "0"
  }

  // pubkey checks
  case class Pk[Pk <: MiniscriptKey[_]](value: Pk) extends AbstractSyntaxTree[Pk] {
    override val description: String = "<key>"
  }

  case class PkH[Pk <: MiniscriptKey[_]](value: Pk) extends AbstractSyntaxTree[Pk] {
    override val description: String = "DUP HASH160 <keyhash> EQUALVERIFY"
  }

  // timelocks
  case class After[Pk <: MiniscriptKey[_]](value: Long) extends AbstractSyntaxTree[Pk] {
    override val description: String = "<n> CHECKSEQUENCEVERIFY"
  }

  case class Older[Pk <: MiniscriptKey[_]](value: Long) extends AbstractSyntaxTree[Pk] {
    override val description: String = "<n> CHECKLOCKTIMEVERIFY"
  }

  // hashlocks
  //case class Sha256()

  // wrappers

  // thresholds
  case class Multi[P <: MiniscriptKey[_]](k: Int, keys: Seq[P]) extends AbstractSyntaxTree[P] {
    override val description: String = "<k> <keys...> <n> CHECKMULTISIG"
  }

  case class MultiV[P <: MiniscriptKey[_]](k: Int, keys: Seq[P]) extends AbstractSyntaxTree[P] {
    override val description: String = "<k> <keys...> <n> CHECKMULTISIGVERIFY"
  }

}