package me.hongchao.bitcoin4s


object Base58Check {
  def checksum(input: Array[Byte]) = Sha256(Sha256(input)).take(4)

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
}
