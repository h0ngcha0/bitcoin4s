package it.softfork.bitcoin4s.crypto

import it.softfork.bitcoin4s.crypto.Hash._

object Base58Check {
  def checksum(input: Array[Byte]): Array[Byte] = Hash256(input).value.take(4)

  def encode(versionPrefix: Byte, payload: Array[Byte]): String = {
    encode(Array(versionPrefix), payload)
  }

  def encode(versionPrefix: Array[Byte], payload: Array[Byte]): String = {
    val versionPrefixAndPayload = versionPrefix ++ payload
    Base58.encode(versionPrefixAndPayload ++ checksum(versionPrefixAndPayload))
  }

  // Type the prefix
  def decode(input: String): (Byte, Array[Byte]) = {
    val raw = Base58.decode(input)
    val (versionAndHash, checksum) = raw.splitAt(raw.length - 4)
    require(checksum == Base58Check.checksum(versionAndHash.toArray), s"invalid Base58Check data $input")
    (versionAndHash.head, versionAndHash.tail.toArray)
  }

  sealed trait VersionPrefix {
    val value: Byte
  }

  case object P2PKHVersionPrefix extends VersionPrefix {
    val value = 0x00.toByte
  }

  case object P2SHVersionPrefix extends VersionPrefix {
    val value = 0x05.toByte
  }

  case object P2PKHTestnetVersionPrefix extends VersionPrefix {
    val value = 0x6F.toByte
  }

  case object PrivateKeyWIFVersionPrefix extends VersionPrefix {
    val value = 0x80.toByte
  }

  case object BIP38EncryptedPrivateKeyVersionPrefix extends VersionPrefix {
    val value = 0x0142.toByte
  }

  case object BIP32ExtendedPublicKeyVersionPrefix extends VersionPrefix {
    val value = 0x0488B21E.toByte
  }
}
