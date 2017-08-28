package me.hongchao.bitcoin4s

sealed trait Script {
  val opcode: Int
  val hex: String = opcode.toHexString
}

case class OP_FALSE(opcode: Int = 0) extends Script
case class OP_0(opcode: Int = 0) extends Script
case class OP_PUSHDATA1(opcode: Int = 76) extends Script
case class OP_PUSHDATA2(opcode: Int = 77) extends Script
case class OP_PUSHDATA4(opcode: Int = 78) extends Script
case class OP_1NEGATE(opcode: Int = 79) extends Script
case class OP_RESERVED(opcode: Int = 80) extends Script
case class OP_1(opcode: Int = 81) extends Script
case class OP_TRUE(opcode: Int = 81) extends Script
case class OP_2(opcode: Int = 82) extends Script
case class OP_3(opcode: Int = 83) extends Script
case class OP_4(opcode: Int = 84) extends Script
case class OP_5(opcode: Int = 85) extends Script
case class OP_6(opcode: Int = 86) extends Script
case class OP_7(opcode: Int = 87) extends Script
case class OP_8(opcode: Int = 88) extends Script
case class OP_9(opcode: Int = 89) extends Script
case class OP_10(opcode: Int = 90) extends Script
case class OP_11(opcode: Int = 91) extends Script
case class OP_12(opcode: Int = 92) extends Script
case class OP_13(opcode: Int = 93) extends Script
case class OP_14(opcode: Int = 94) extends Script
case class OP_15(opcode: Int = 95) extends Script
case class OP_16(opcode: Int = 96) extends Script
case class OP_NOP(opcode: Int = 97) extends Script
case class OP_VER(opcode: Int = 98) extends Script
case class OP_IF(opcode: Int = 99) extends Script
case class OP_NOTIF(opcode: Int = 100) extends Script
case class OP_VERIF(opcode: Int = 101) extends Script
case class OP_VERNOTIF(opcode: Int = 102) extends Script
case class OP_ELSE(opcode: Int = 103) extends Script
case class OP_ENDIF(opcode: Int = 104) extends Script
case class OP_VERIFY(opcode: Int = 105) extends Script
case class OP_RETURN(opcode: Int = 106) extends Script
case class OP_TOALTSTACK(opcode: Int = 107) extends Script
case class OP_FROMALTSTACK(opcode: Int = 108) extends Script
case class OP_2DROP(opcode: Int = 109) extends Script
case class OP_2DUP(opcode: Int = 110) extends Script
case class OP_3DUP(opcode: Int = 111) extends Script
case class OP_2OVER(opcode: Int = 112) extends Script
case class OP_2ROT(opcode: Int = 113) extends Script
case class OP_2SWAP(opcode: Int = 114) extends Script
case class OP_IFDUP(opcode: Int = 115) extends Script
case class OP_DEPTH(opcode: Int = 116) extends Script
case class OP_DROP(opcode: Int = 117) extends Script
case class OP_DUP(opcode: Int = 118) extends Script
case class OP_NIP(opcode: Int = 119) extends Script
case class OP_OVER(opcode: Int = 120) extends Script
case class OP_PICK(opcode: Int = 121) extends Script
case class OP_ROLL(opcode: Int = 122) extends Script
case class OP_ROT(opcode: Int = 123) extends Script
case class OP_SWAP(opcode: Int = 124) extends Script
case class OP_TUCK(opcode: Int = 125) extends Script

case class OP_CAT(opcode: Int = 126) extends Script
case class OP_SUBSTR(opcode: Int = 127) extends Script
case class OP_LEFT(opcode: Int = 128) extends Script
case class OP_RIGHT(opcode: Int = 129) extends Script
case class OP_SIZE(opcode: Int = 130) extends Script

case class OP_INVERT(opcode: Int = 131) extends Script
case class OP_AND(opcode: Int = 132) extends Script
case class OP_OR(opcode: Int = 133) extends Script
case class OP_XOR(opcode: Int = 134) extends Script
case class OP_EQUAL(opcode: Int = 135) extends Script
case class OP_EQUALVERIFY(opcode: Int = 136) extends Script
case class OP_RESERVED1(opcode: Int = 137) extends Script
case class OP_RESERVED2(opcode: Int = 138) extends Script

case class OP_1ADD(opcode: Int = 139) extends Script
case class OP_1SUB(opcode: Int = 140) extends Script
case class OP_2MUL(opcode: Int = 141) extends Script
case class OP_2DIV(opcode: Int = 142) extends Script
case class OP_NEGATE(opcode: Int = 143) extends Script
case class OP_ABS(opcode: Int = 144) extends Script
case class OP_NOT(opcode: Int = 145) extends Script
case class OP_0NOTEQUAL(opcode: Int = 146) extends Script
case class OP_ADD(opcode: Int = 147) extends Script
case class OP_SUB(opcode: Int = 148) extends Script
case class OP_MUL(opcode: Int = 149) extends Script
case class OP_DIV(opcode: Int = 150) extends Script
case class OP_MOD(opcode: Int = 151) extends Script
case class OP_LSHIFT(opcode: Int = 152) extends Script
case class OP_RSHIFT(opcode: Int = 153) extends Script

case class OP_BOOLAND(opcode: Int = 154) extends Script
case class OP_BOOLOR(opcode: Int = 155) extends Script
case class OP_NUMEQUAL(opcode: Int = 156) extends Script
case class OP_NUMEQUALVERIFY(opcode: Int = 157) extends Script
case class OP_NUMNOTEQUAL(opcode: Int = 158) extends Script
case class OP_LESSTHAN(opcode: Int = 159) extends Script
case class OP_GREATERTHAN(opcode: Int = 160) extends Script
case class OP_LESSTHANOREQUAL(opcode: Int = 161) extends Script
case class OP_GREATERTHANOREQUAL(opcode: Int = 162) extends Script
case class OP_MIN(opcode: Int = 163) extends Script
case class OP_MAX(opcode: Int = 164) extends Script

case class OP_WITHIN(opcode: Int = 165) extends Script

case class OP_RIPEMD160(opcode: Int = 166) extends Script
case class OP_SHA1(opcode: Int = 167) extends Script
case class OP_SHA256(opcode: Int = 168) extends Script
case class OP_HASH160(opcode: Int = 169) extends Script
case class OP_HASH256(opcode: Int = 170) extends Script
case class OP_CODESEPARATOR(opcode: Int = 171) extends Script
case class OP_CHECKSIG(opcode: Int = 172) extends Script
case class OP_CHECKSIGVERIFY(opcode: Int = 173) extends Script
case class OP_CHECKMULTISIG(opcode: Int = 174) extends Script
case class OP_CHECKMULTISIGVERIFY(opcode: Int = 175) extends Script

case class OP_NOP1(opcode: Int = 176) extends Script
case class OP_NOP2(opcode: Int = 177) extends Script
case class OP_CHECKLOCKTIMEVERIFY(opcode: Int = 177) extends Script

case class OP_NOP3(opcode: Int = 178) extends Script
case class OP_NOP4(opcode: Int = 179) extends Script
case class OP_NOP5(opcode: Int = 180) extends Script
case class OP_NOP6(opcode: Int = 181) extends Script
case class OP_NOP7(opcode: Int = 182) extends Script
case class OP_NOP8(opcode: Int = 183) extends Script
case class OP_NOP9(opcode: Int = 184) extends Script
case class OP_NOP10(opcode: Int = 185) extends Script

case class OP_PUBKEYHASH(opcode: Int = 253) extends Script
case class OP_PUBKEY(opcode: Int = 254) extends Script
case class OP_INVALIDOPCODE(opcode: Int = 255) extends Script

object Script {

}
