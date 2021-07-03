package it.softfork.bitcoin4s.crypto

import it.softfork.bitcoin4s.Utils._
import java.math.BigInteger

object Signature {
  case class ECDSASignature(r: BigInteger, s: BigInteger) extends Signature
  case object EmptySignature extends Signature

  val MinDerSignatureSize = 9
  val MaxDerSignatureSize = 73

  // Signature is DER encoded
  // structure: `sequence (r, s)`
  // `sequence`, `r`, and `s` are structured as `tlv` format respectively
  // Reference: https://msdn.microsoft.com/en-us/library/windows/desktop/bb648640(v=vs.85).aspx
  def decode(bytes: Seq[Byte]): Option[(Signature, Seq[Byte])] = {
    bytes match {
      case Nil =>
        Some((EmptySignature, Seq.empty))
      case _ :: _ if bytes.length > MaxDerSignatureSize || bytes.length < MinDerSignatureSize =>
        None
      case head +: tail if head == 0x30 =>
        for {
          (_, restOfBytesAfterRlength) <- getLength(tail)
          _ <- checkHeadTypeIsInt(restOfBytesAfterRlength)
          (rLength, restOfBytesAfterRlength) <- getLength(restOfBytesAfterRlength.tail)
          _ <- checkPositive(rLength)
          (r, restOfBytesAfterR) <- restOfBytesAfterRlength.splitAtOpt(rLength)
          _ <- checkHeadTypeIsInt(restOfBytesAfterR)
          (sLength, restOfBytesAfterSLength) <- getLength(restOfBytesAfterR.tail)
          _ <- checkPositive(sLength)
          (s, restOfBytesAfterS) <- restOfBytesAfterSLength.splitAtOpt(sLength)
        } yield {
          (ECDSASignature(new BigInteger(1, r.toArray), new BigInteger(1, s.toArray)), restOfBytesAfterS)
        }
      case _ =>
        None
    }
  }

  // Try to have the same logic as
  // https://github.com/bitcoin/bitcoin/blob/17f2acedbe078f179556f4550eca547726f087e1/src/script/interpreter.cpp#L108
  def isValidSignatureEncoding(sig: Seq[Byte]): Boolean = {
    // Format: 0x30 [total-length] 0x02 [R-length] [R] 0x02 [S-length] [S] [sighash]

    def validSize = sig.length >= MinDerSignatureSize && sig.length <= MaxDerSignatureSize
    def isCompoundType = sig(0) == 0x30
    def signatureSize = sig(1)
    // -3 because of type (1), length (1) and sighash type (1)
    def isSignatureLengthCorrect = signatureSize == (sig.length - 3)
    def lenR = sig(3)
    // def isLenSWithinBoundary = 5 + lenR < sig.length
    def lenS = sig(5 + lenR)
    def lenRAndLenSAddup = (lenR + lenS + 7) == sig.length
    def isRInt = sig(2) == 0x02
    def isLenRZero = lenR == 0
    def isRNegative = (sig(4) & 0x80) != 0
    def isNullBytesAtStartOfR = lenR > 1 && (sig(4) == 0x00) && ((sig(5) & 0x80) == 0)
    def isSInt = sig(lenR + 4) == 0x02
    def isLenSZero = lenS == 0
    def isSNegative = (sig(lenR + 6) & 0x80) != 0
    def isNullBytesAtStartOfS = lenS > 1 && (sig(lenR + 6) == 0x00) && ((sig(lenR + 7) & 0x80) == 0)

    validSize &&
    isCompoundType &&
    isSignatureLengthCorrect &&
    lenRAndLenSAddup &&
    isRInt &&
    (!isLenRZero) &&
    (!isRNegative) &&
    (!isNullBytesAtStartOfR) &&
    isSInt &&
    (!isLenSZero) &&
    (!isSNegative) &&
    (!isNullBytesAtStartOfS)
  }

  private def checkHeadTypeIsInt(bytesIn: Seq[Byte]): Option[Unit] = {
    bytesIn.headOption.flatMap { rType =>
      (rType == 0x02).option(())
    }
  }

  private def checkPositive(value: Int): Option[Unit] = {
    (value > 0).option(())
  }

  private def getLength(bytesIn: Seq[Byte]): Option[(Int, Seq[Byte])] = {
    bytesIn match {
      case head +: tail =>
        if ((head & 0x80) == 0) {
          Some((head.toInt, tail))
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
