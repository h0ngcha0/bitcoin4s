package it.softfork.bitcoin4s.crypto

import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.crypto.params.ECDomainParameters

object Secp256k1 {
  val params = SECNamedCurves.getByName("secp256k1")
  val curve = new ECDomainParameters(params.getCurve, params.getG, params.getN, params.getH)
  val halfCurveOrder = params.getN().shiftRight(1)
}
