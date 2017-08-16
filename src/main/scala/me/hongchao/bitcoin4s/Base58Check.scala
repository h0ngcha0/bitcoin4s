package me.hongchao.bitcoin4s


object Base58Check {
  def checksum(input: Seq[Byte]) = Sha256(Sha256(input)).take(4)

  def encode(versionPrefix: Byte, payload: Seq[Byte]): String = {
    encode(Seq(versionPrefix), payload)
  }

  def encode(versionPrefix: Seq[Byte], payload: Seq[Byte]): String = {
    val versionPrefixAndPayload = versionPrefix ++ payload
    Base58.encode(versionPrefixAndPayload ++ checksum(versionPrefixAndPayload))
  }

  // Type the prefix
  def decode(input: String): (Byte, Seq[Byte]) = {
    val raw = Base58.decode(input)
    val (versionAndHash, checksum) = raw.splitAt(raw.length - 4)
    assert(checksum == Base58Check.checksum(versionAndHash), s"invalid Base58Check data $input")
    (versionAndHash.head, versionAndHash.tail)
  }
}
