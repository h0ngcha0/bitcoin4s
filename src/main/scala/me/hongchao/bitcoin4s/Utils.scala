package me.hongchao.bitcoin4s

package object Utils {
  implicit class Rich[T](value: T) {
    def toHex: String = "%02x".format(value)
  }

  implicit class RichSeqByte(bytes: Seq[Byte]) {
    def toHex: String = bytes.map(_.toHex).mkString
  }
}
