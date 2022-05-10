import _ from 'lodash';

const Values: string[] = [
  'ScriptConstant', 'ScriptNum'
];

const StackOps: string[] = [
  'OP_TOALTSTACK', 'OP_FROMALTSTACK', 'OP_2DROP', 'OP_2DUP', 'OP_3DUP', 'OP_2OVER',
  'OP_2ROT', 'OP_2SWAP', 'OP_IFDUP', 'OP_DEPTH', 'OP_DROP', 'OP_DUP', 'OP_NIP', 'OP_OVER',
  'OP_PICK', 'OP_ROLL', 'OP_ROT', 'OP_SWAP', 'OP_TUCK'
];

const CryptoOps: string[] = [
  'OP_RIPEMD160', 'OP_SHA1', 'OP_SHA256', 'OP_HASH160', 'OP_HASH256', 'OP_CODESEPARATOR',
  'OP_CHECKSIG', 'OP_CHECKSIGVERIFY', 'OP_CHECKMULTISIG', 'OP_CHECKMULTISIGVERIFY'
];

const ConstantOps: string[] = [
  'OP_0', 'OP_FALSE', 'OP_PUSHDATA1', 'OP_PUSHDATA2', 'OP_PUSHDATA4', 'OP_1NEGATE',
  'OP_1', 'OP_TRUE', 'OP_2', 'OP_3', 'OP_4', 'OP_5', 'OP_6', 'OP_7', 'OP_8', 'OP_9', 'OP_10',
  'OP_11', 'OP_12', 'OP_13', 'OP_14', 'OP_15', 'OP_16', 'OP_PUSHDATA'
];

const BitwiseLogicOps: string[] = [
  'OP_INVERT', 'OP_AND', 'OP_OR', 'OP_XOR', 'OP_EQUAL', 'OP_EQUALVERIFY'
];

const ArithmeticOps: string[] = [
  'OP_1ADD', 'OP_1SUB', 'OP_2MUL', 'OP_2DIV', 'OP_NEGATE', 'OP_ABS', 'OP_NOT', 'OP_0NOTEQUAL',
  'OP_ADD', 'OP_SUB', 'OP_MUL', 'OP_DIV', 'OP_MOD', 'OP_LSHIFT', 'OP_RSHIFT', 'OP_BOOLAND',
  'OP_BOOLOR', 'OP_NUMEQUAL', 'OP_NUMEQUALVERIFY', 'OP_NUMNOTEQUAL', 'OP_LESSTHAN',
  'OP_GREATERTHAN', 'OP_LESSTHANOREQUAL', 'OP_GREATERTHANOREQUAL', 'OP_MIN', 'OP_MAX', 'OP_WITHIN'
];

const FlowControlOps: string[] = [
  'OP_NOP', 'OP_IF', 'OP_NOTIF', 'OP_ELSE', 'OP_ENDIF', 'OP_VERIFY', 'OP_RETURN'
];

const LocktimeOps: string[] = [
  'OP_CHECKLOCKTIMEVERIFY', 'OP_CHECKSEQUENCEVERIFY', 'OP_NOP2', 'OP_NOP3'
];

const PseudoOps: string[] = [
  'OP_PUBKEYHASH', 'OP_PUBKEY', 'OP_INVALIDOPCODE'
];

const ReservedOps: string[] = [
  'OP_RESERVED', 'OP_VER', 'OP_VERIF', 'OP_VERNOTIF', 'OP_RESERVED1', 'OP_RESERVED2',
  'OP_NOP1', 'OP_NOP4', 'OP_NOP5', 'OP_NOP6', 'OP_NOP7', 'OP_NOP8', 'OP_NOP9', 'OP_NOP10'
];

const SpliceOps: string[] = [
  'OP_CAT', 'OP_SUBSTR', 'OP_LEFT', 'OP_RIGHT', 'OP_SIZE'
];

const allElements = {
  'Values': Values,
  'StackOps': StackOps,
  'CryptoOps': CryptoOps,
  'ConstantOps': ConstantOps,
  'BitwiseLogicOps': BitwiseLogicOps,
  'ArithmeticOps': ArithmeticOps,
  'FlowControlOps': FlowControlOps,
  'LocktimeOps': LocktimeOps,
  'PseudoOps': PseudoOps,
  'ReservedOps': ReservedOps,
  'SpliceOps': SpliceOps
};

export function findElementType(scriptElement: string): string | undefined {
  const value = _.toPairs(allElements).find(([key, value]) => {
    return _.includes(value, scriptElement);
  });

  return value ? value[0] : undefined;
};
