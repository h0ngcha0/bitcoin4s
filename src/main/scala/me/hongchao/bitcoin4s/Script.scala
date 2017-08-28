package me.hongchao.bitcoin4s

sealed trait Script extends Product {
  val opcode: Int
  val hex: String = opcode.toHexString
  val name: String = productPrefix
}

case object OP_FALSE extends Script { val opcode = 0 }
case object OP_0 extends Script { val opcode = 0 }
case object OP_PUSHDATA1 extends Script { val opcode = 76 }
case object OP_PUSHDATA2 extends Script { val opcode = 77 }
case object OP_PUSHDATA4 extends Script { val opcode = 78 }
case object OP_1NEGATE extends Script { val opcode = 79 }
case object OP_RESERVED extends Script { val opcode = 80 }
case object OP_1 extends Script { val opcode = 81 }
case object OP_TRUE extends Script { val opcode = 81 }
case object OP_2 extends Script { val opcode = 82 }
case object OP_3 extends Script { val opcode = 83 }
case object OP_4 extends Script { val opcode = 84 }
case object OP_5 extends Script { val opcode = 85}
case object OP_6 extends Script { val opcode = 86 }
case object OP_7 extends Script { val opcode = 87 }
case object OP_8 extends Script { val opcode = 88 }
case object OP_9 extends Script { val opcode = 89 }
case object OP_10 extends Script { val opcode = 90 }
case object OP_11 extends Script { val opcode = 91 }
case object OP_12 extends Script { val opcode = 92 }
case object OP_13 extends Script { val opcode = 93 }
case object OP_14 extends Script { val opcode = 94 }
case object OP_15 extends Script { val opcode = 95 }
case object OP_16 extends Script { val opcode = 96 }
case object OP_NOP extends Script { val opcode = 97 }
case object OP_VER extends Script { val opcode = 98 }
case object OP_IF extends Script { val opcode = 99 }
case object OP_NOTIF extends Script { val opcode = 100 }
case object OP_VERIF extends Script { val opcode = 101 }
case object OP_VERNOTIF extends Script { val opcode = 102 }
case object OP_ELSE extends Script { val opcode = 103 }
case object OP_ENDIF extends Script { val opcode = 104 }
case object OP_VERIFY extends Script { val opcode = 105 }
case object OP_RETURN extends Script { val opcode = 106 }
case object OP_TOALTSTACK extends Script { val opcode = 107 }
case object OP_FROMALTSTACK extends Script { val opcode = 108 }
case object OP_2DROP extends Script { val opcode = 109 }
case object OP_2DUP extends Script { val opcode = 110 }
case object OP_3DUP extends Script { val opcode = 111 }
case object OP_2OVER extends Script { val opcode = 112 }
case object OP_2ROT extends Script { val opcode = 113 }
case object OP_2SWAP extends Script { val opcode = 114 }
case object OP_IFDUP extends Script { val opcode = 115 }
case object OP_DEPTH extends Script { val opcode = 116 }
case object OP_DROP extends Script { val opcode = 117 }
case object OP_DUP extends Script { val opcode = 118 }
case object OP_NIP extends Script { val opcode = 119 }
case object OP_OVER extends Script { val opcode = 120 }
case object OP_PICK extends Script { val opcode = 121 }
case object OP_ROLL extends Script { val opcode = 122 }
case object OP_ROT extends Script { val opcode = 123 }
case object OP_SWAP extends Script { val opcode = 124 }
case object OP_TUCK extends Script { val opcode = 125 }

case object OP_CAT extends Script { val opcode = 126 }
case object OP_SUBSTR extends Script { val opcode = 127 }
case object OP_LEFT extends Script { val opcode = 128 }
case object OP_RIGHT extends Script { val opcode = 129 }
case object OP_SIZE extends Script { val opcode = 130 }

case object OP_INVERT extends Script { val opcode = 131 }
case object OP_AND extends Script { val opcode = 132 }
case object OP_OR extends Script { val opcode = 133 }
case object OP_XOR extends Script { val opcode = 134 }
case object OP_EQUAL extends Script { val opcode = 135 }
case object OP_EQUALVERIFY extends Script { val opcode = 136 }
case object OP_RESERVED1 extends Script { val opcode = 137 }
case object OP_RESERVED2 extends Script { val opcode = 138 }

case object OP_1ADD extends Script { val opcode = 139 }
case object OP_1SUB extends Script { val opcode = 140 }
case object OP_2MUL extends Script { val opcode = 141 }
case object OP_2DIV extends Script { val opcode = 142 }
case object OP_NEGATE extends Script { val opcode = 143 }
case object OP_ABS extends Script { val opcode = 144 }
case object OP_NOT extends Script { val opcode = 145 }
case object OP_0NOTEQUAL extends Script { val opcode = 146 }
case object OP_ADD extends Script { val opcode = 147 }
case object OP_SUB extends Script { val opcode = 148 }
case object OP_MUL extends Script { val opcode = 149 }
case object OP_DIV extends Script { val opcode = 150 }
case object OP_MOD extends Script { val opcode = 151 }
case object OP_LSHIFT extends Script { val opcode = 152 }
case object OP_RSHIFT extends Script { val opcode = 153 }

case object OP_BOOLAND extends Script { val opcode = 154 }
case object OP_BOOLOR extends Script { val opcode = 155 }
case object OP_NUMEQUAL extends Script { val opcode = 156 }
case object OP_NUMEQUALVERIFY extends Script { val opcode = 157 }
case object OP_NUMNOTEQUAL extends Script { val opcode = 158 }
case object OP_LESSTHAN extends Script { val opcode = 159 }
case object OP_GREATERTHAN extends Script { val opcode = 160 }
case object OP_LESSTHANOREQUAL extends Script { val opcode = 161 }
case object OP_GREATERTHANOREQUAL extends Script { val opcode = 162 }
case object OP_MIN extends Script { val opcode = 163 }
case object OP_MAX extends Script { val opcode = 164 }

case object OP_WITHIN extends Script { val opcode = 165 }

case object OP_RIPEMD160 extends Script { val opcode = 166 }
case object OP_SHA1 extends Script { val opcode = 167 }
case object OP_SHA256 extends Script { val opcode = 168 }
case object OP_HASH160 extends Script { val opcode = 169 }
case object OP_HASH256 extends Script { val opcode = 170 }
case object OP_CODESEPARATOR extends Script { val opcode = 171 }
case object OP_CHECKSIG extends Script { val opcode = 172 }
case object OP_CHECKSIGVERIFY extends Script { val opcode = 173 }
case object OP_CHECKMULTISIG extends Script { val opcode = 174 }
case object OP_CHECKMULTISIGVERIFY extends Script { val opcode = 175 }

case object OP_NOP1 extends Script { val opcode = 176 }
case object OP_NOP2 extends Script { val opcode = 177 }
case object OP_CHECKLOCKTIMEVERIFY extends Script { val opcode = 177 }

case object OP_NOP3 extends Script { val opcode = 178 }
case object OP_NOP4 extends Script { val opcode = 179 }
case object OP_NOP5 extends Script { val opcode = 180 }
case object OP_NOP6 extends Script { val opcode = 181 }
case object OP_NOP7 extends Script { val opcode = 182 }
case object OP_NOP8 extends Script { val opcode = 183 }
case object OP_NOP9 extends Script { val opcode = 184 }
case object OP_NOP10 extends Script { val opcode = 185 }

case object OP_PUBKEYHASH extends Script { val opcode = 253 }
case object OP_PUBKEY extends Script { val opcode = 254 }
case object OP_INVALIDOPCODE extends Script { val opcode = 255 }

object Script {

}
