package me.hongchao.bitcoin4s.crypto

import me.hongchao.bitcoin4s.Utils._

import java.math.BigInteger

case class Signature(bytes: Seq[Byte]) {
  // Signature is DER encoded
  // structure: `sequence (r, s)`
  // `sequence`, `r`, and `s` are structured as `tlv` format respectively
  // Reference: https://msdn.microsoft.com/en-us/library/windows/desktop/bb648640(v=vs.85).aspx
  def decode(): Option[(BigInteger, BigInteger)] = {
    bytes match {
      case 0x30 :: tail =>
        for {
          (_, restOfBytesAfterRlength) <- getLength(tail)
          _ <- checkHeadTypeIsInt(restOfBytesAfterRlength)
          (rLength, restOfBytesAfterRlength) <- getLength(restOfBytesAfterRlength.tail)
          (r, restOfBytesAfterR) <- restOfBytesAfterRlength.splitAtOpt(rLength)
          _ <- checkHeadTypeIsInt(restOfBytesAfterR)
          (sLength, restOfBytesAfterSLength) <- getLength(restOfBytesAfterR.tail)
          (s, restOfBytesAfterS) <- restOfBytesAfterSLength.splitAtOpt(sLength)
        } yield {
          require(restOfBytesAfterS.isEmpty, "Not all bytes are consumed while decoding the signature")
          (new BigInteger(1, r.toArray), new BigInteger(1, s.toArray))
        }
      case _ =>
        None
    }
  }

  private def checkHeadTypeIsInt(bytesIn: Seq[Byte]): Option[Unit] = {
    bytesIn.headOption.map { rType =>
      require(rType == 0x02, "type must be int")
    }
  }

  private def getLength(bytesIn: Seq[Byte]): Option[(Int, Seq[Byte])] = {
    bytesIn match {
      case head :: tail =>
        if ((head & 0x80) == 0) {
          Some((head, tail))
        } else {
          val lengthBytesNumber: Int = head - 0x80

          tail.splitAtOpt(lengthBytesNumber).map {
            case (lengthBytes, restOfBytes) =>
              val length = lengthBytes.foldLeft(0) {
                case (acc, byte) => (acc << 8) + byte
              }

              (length, restOfBytes)
          }
        }
      case Nil =>
        None
    }
  }
}
