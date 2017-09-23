package me.hongchao.bitcoin4s.script

sealed trait ScriptFlag extends Product {
  val value: Int
  val name = productPrefix

  override def toString: String = name
}

object ScriptFlag {
  // Name and comment copy from https://github.com/bitcoin/bitcoin/blob/master/src/script/interpreter.h

  case object SCRIPT_VERIFY_NONE extends ScriptFlag { val value =  0 }

  // Evaluate P2SH subscripts (softfork safe, BIP16).
  case object SCRIPT_VERIFY_P2SH extends ScriptFlag { val value = 1 << 0 }

  // Passing a non-strict-DER signature or one with undefined hashtype to a checksig operation causes script failure.
  // Evaluating a pubkey that is not (0x04 + 64 bytes) or (0x02 or 0x03 + 32 bytes) by checksig causes script failure.
  // (softfork safe, but not used or intended as a consensus rule).
  case object SCRIPT_VERIFY_STRICTENC extends ScriptFlag { val value = 1 << 1 }

  // Passing a non-strict-DER signature to a checksig operation causes script failure (softfork safe, BIP62 rule 1)
  case object SCRIPT_VERIFY_DERSIG extends ScriptFlag { val value = 1 << 2 }

  // Passing a non-strict-DER signature or one with S > order/2 to a checksig operation causes script failure
  // (softfork safe, BIP62 rule 5).
  case object SCRIPT_VERIFY_LOW_S extends ScriptFlag { val value = 1 << 3 }

  // verify dummy stack item consumed by CHECKMULTISIG is of zero-length (softfork safe, BIP62 rule 7).
  case object SCRIPT_VERIFY_NULLDUMMY extends ScriptFlag { val value = 1 << 4 }

  // Using a non-push operator in the scriptSig causes script failure (softfork safe, BIP62 rule 2).
  case object SCRIPT_VERIFY_SIGPUSHONLY extends ScriptFlag { val value = 1 << 5 }

  // Require minimal encodings for all push operations (OP_0... OP_16, OP_1NEGATE where possible, direct
  // pushes up to 75 bytes, OP_PUSHDATA up to 255 bytes, OP_PUSHDATA2 for anything larger). Evaluating
  // any other push causes the script to fail (BIP62 rule 3).
  // In addition, whenever a stack element is interpreted as a number, it must be of minimal length (BIP62 rule 4).
  // (softfork safe)
  case object SCRIPT_VERIFY_MINIMALDATA extends ScriptFlag { val value = 1 << 6 }

  // Discourage use of NOPs reserved for upgrades (NOP1-10)
  //
  // Provided so that nodes can avoid accepting or mining transactions
  // containing executed NOP's whose meaning may change after a soft-fork,
  // thus rendering the script invalid; with this flag set executing
  // discouraged NOPs fails the script. This verification flag will never be
  // a mandatory flag applied to scripts in a block. NOPs that are not
  // executed, e.g.  within an unexecuted IF ENDIF block, are *not* rejected.
  case object SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_NOPS  extends ScriptFlag { val value = 1 << 7 }

  // Require that only a single stack element remains after evaluation. This changes the success criterion from
  // "At least one stack element must remain, and when interpreted as a boolean, it must be true" to
  // "Exactly one stack element must remain, and when interpreted as a boolean, it must be true".
  // (softfork safe, BIP62 rule 6)
  // Note: CLEANSTACK should never be used without P2SH or WITNESS.
  case object SCRIPT_VERIFY_CLEANSTACK extends ScriptFlag { val value = 1 << 8 }

  // Verify CHECKLOCKTIMEVERIFY
  //
  // See BIP65 for details.
  case object SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY extends ScriptFlag { val value = 1 << 9 }

  // support CHECKSEQUENCEVERIFY opcode
  //
  // See BIP112 for details
  case object SCRIPT_VERIFY_CHECKSEQUENCEVERIFY extends ScriptFlag { val value = 1 << 10 }

  // Support segregated witness
  //
  case object SCRIPT_VERIFY_WITNESS extends ScriptFlag { val value = 1 << 11 }

  // Making v1-v16 witness program non-standard
  //
  case object SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM extends ScriptFlag { val value = 1 << 12 }

  // Segwit script only: Require the argument of OP_IF/NOTIF to be exactly 0x01 or empty vector
  //
  case object SCRIPT_VERIFY_MINIMALIF extends ScriptFlag { val value = 1 << 13 }

  // Signature(s) must be empty vector if an CHECK(MULTI)SIG operation failed
  //
  case object SCRIPT_VERIFY_NULLFAIL extends ScriptFlag { val value = 1 << 14 }

  // Public keys in segregated witness scripts must be compressed
  //
  case object SCRIPT_VERIFY_WITNESS_PUBKEYTYPE extends ScriptFlag { val value = 1 << 15 }

  val all = Seq(
    SCRIPT_VERIFY_NONE,
    SCRIPT_VERIFY_P2SH,
    SCRIPT_VERIFY_STRICTENC,
    SCRIPT_VERIFY_DERSIG,
    SCRIPT_VERIFY_LOW_S,
    SCRIPT_VERIFY_NULLDUMMY,
    SCRIPT_VERIFY_SIGPUSHONLY,
    SCRIPT_VERIFY_MINIMALDATA,
    SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_NOPS,
    SCRIPT_VERIFY_CLEANSTACK,
    SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY,
    SCRIPT_VERIFY_CHECKSEQUENCEVERIFY,
    SCRIPT_VERIFY_WITNESS,
    SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM,
    SCRIPT_VERIFY_MINIMALIF,
    SCRIPT_VERIFY_NULLFAIL,
    SCRIPT_VERIFY_WITNESS_PUBKEYTYPE
  )

  def fromString(str: String): Option[ScriptFlag] = {
    all.find(_.name == str) orElse all.find(_.name.stripPrefix("SCRIPT_VERIFY_") == str)
  }
}
