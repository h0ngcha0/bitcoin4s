package me.hongchao.bitcoin4s.crypto


import me.hongchao.bitcoin4s.Utils._
import java.math.BigInteger

object Signature {
  case class ECDSASignature(r: BigInteger, s: BigInteger) extends Signature
  case object EmptySignature extends Signature

  // Signature is DER encoded
  // structure: `sequence (r, s)`
  // `sequence`, `r`, and `s` are structured as `tlv` format respectively
  // Reference: https://msdn.microsoft.com/en-us/library/windows/desktop/bb648640(v=vs.85).aspx
  def decode(bytes: Seq[Byte]): Option[(Signature, Seq[Byte])] = {
    bytes match {
      case Nil =>
        Some((EmptySignature, Seq.empty))
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
          (ECDSASignature(new BigInteger(1, r.toArray), new BigInteger(1, s.toArray)), restOfBytesAfterS)
        }
      case _ =>
        None
    }
  }

  def isDERSignature(bytes: Seq[Byte]): Boolean = {
    decode(bytes) match {
      case Some((signature, _)) if signature != EmptySignature=>
        true
      case _ =>
        false
    }
  }

  private def checkHeadTypeIsInt(bytesIn: Seq[Byte]): Option[Unit] = {
    bytesIn.headOption.flatMap { rType =>
      (rType == 0x02).option(())
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

sealed trait Signature
