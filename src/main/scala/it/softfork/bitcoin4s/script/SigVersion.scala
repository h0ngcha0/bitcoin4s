package it.softfork.bitcoin4s.script

sealed trait SigVersion {
  val value: Int
}

object SigVersion {
  case object SIGVERSION_BASE extends SigVersion { val value = 0 }
  case object SIGVERSION_WITNESS_V0 extends SigVersion { val value = 1 }
  case object SIGVERSION_TAPROOT extends SigVersion { val value = 2 }
  case object SIGVERSION_TAPSCRIPT extends SigVersion { val value = 3 }
}
