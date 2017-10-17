bitcoin4s
=========
[![Build Status](https://travis-ci.org/liuhongchao/bitcoin4s.svg?branch=master)](https://travis-ci.org/liuhongchao/bitcoin4s)
[![Coverage Status](https://coveralls.io/repos/github/liuhongchao/bitcoin4s/badge.svg?branch=master)](https://coveralls.io/github/liuhongchao/bitcoin4s?branch=master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Scala library for experimenting with Bitcoin

# How to use

Add the following to your build.sbt

```scala
libraryDependencies += "me.hongchao" %% "bitcoin4s" % "0.0.1"
```

with the following resolver

```scala
resolvers += Resolver.bintrayRepo("liuhongchao", "maven")
```

# Parse Bitcoin script

```scala
scala> import me.hongchao.bitcoin4s.script.Parser
import me.hongchao.bitcoin4s.script.Parser

scala> Parser.parse("0 IF 0 ELSE 1 ELSE 0 ENDIF")
res0: Seq[me.hongchao.bitcoin4s.script.ScriptElement] = List(OP_0, OP_IF, OP_0, OP_ELSE, OP_1, OP_ELSE, OP_0, OP_ENDIF)

scala> Parser.parse("0x41 0x0479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8 CHECKSIG")
res1: Seq[me.hongchao.bitcoin4s.script.ScriptElement] = List(OP_PUSHDATA(65), ScriptConstant: List(4, 121, -66, 102, 126, -7, -36, -69, -84, 85, -96, 98, -107, -50, -121, 11, 7, 2, -101, -4, -37, 45, -50, 40, -39, 89, -14, -127, 91, 22, -8, 23, -104, 72, 58, -38, 119, 38, -93, -60, 101, 93, -92, -5, -4, 14, 17, 8, -88, -3, 23, -76, 72, -90, -123, 84, 25, -100, 71, -48, -113, -5, 16, -44, -72), OP_CHECKSIG)

```

# Run interpreter

```scala
val initialState = InterpreterState(
  scriptSig = Seq(OP_0),
  scriptPubKey = Seq(OP_IF, OP_0, OP_ELSE, OP_1, OP_ELSE, OP_0, OP_ENDIF),
  flags = Seq(ScriptFlag.SCRIPT_VERIFY_STRICTENC),
  transaction = spendingTx,
  inputIndex = 0
)

Interpreter.interpret().run(initialState) match {
  case Right((finalState, result)) =>
    ???
  case Left(error) =>
    ???
}

```

# License

MIT: http://rem.mit-license.org
