bitcoin4s
=========
[![Build Status](https://travis-ci.org/liuhongchao/bitcoin4s.svg?branch=master)](https://travis-ci.org/liuhongchao/bitcoin4s)
[![Coverage Status](https://coveralls.io/repos/github/liuhongchao/bitcoin4s/badge.svg?branch=master)](https://coveralls.io/github/liuhongchao/bitcoin4s?branch=master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Scala library for experimenting with Bitcoin

# How to use

Add the following to your build.sbt

```scala
libraryDependencies += "me.hongchao" %% "bitcoin4s" % "0.0.2"
```

with the following resolver

```scala
resolvers += Resolver.bintrayRepo("liuhongchao", "maven")
```

# Parse bitcoin script

```scala
scala> import me.hongchao.bitcoin4s.script.Parser
import me.hongchao.bitcoin4s.script.Parser

scala> Parser.parse("0 IF 0 ELSE 1 ELSE 0 ENDIF")
res0: Seq[me.hongchao.bitcoin4s.script.ScriptElement] = List(OP_0, OP_IF, OP_0, OP_ELSE, OP_1, OP_ELSE, OP_0, OP_ENDIF)

scala> Parser.parse("0x41 0x0479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8 CHECKSIG")
res1: Seq[me.hongchao.bitcoin4s.script.ScriptElement] = List(OP_PUSHDATA(65), ScriptConstant: List(4, 121, -66, 102, 126, -7, -36, -69, -84, 85, -96, 98, -107, -50, -121, 11, 7, 2, -101, -4, -37, 45, -50, 40, -39, 89, -14, -127, 91, 22, -8, 23, -104, 72, 58, -38, 119, 38, -93, -60, 101, 93, -92, -5, -4, 14, 17, 8, -88, -3, 23, -76, 72, -90, -123, 84, 25, -100, 71, -48, -113, -5, 16, -44, -72), OP_CHECKSIG)

```

# Run bitcoin script interpreter

Following is a [test case](https://github.com/liuhongchao/bitcoin4s/blob/81996cf471ac4a25a28c4bfcb2060d3d0f2cc8bc/src/test/resources/script_test.json#L2145) for "Basic P2WSH with compressed key" from [bitcoin core](https://github.com/bitcoin/bitcoin).

```
[
    [
        "304402204256146fcf8e73b0fd817ffa2a4e408ff0418ff987dd08a4f485b62546f6c43c02203f3c8c3e2febc051e1222867f5f9d0eaf039d6792911c10940aa3cc74123378e01",
        "210279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ac",
        0.00000001
    ],
    "",
    "0 0x20 0x1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262",
    "P2SH,WITNESS,WITNESS_PUBKEYTYPE",
    "OK",
    "Basic P2WSH with compressed key"
],
```

which is parsed into the following scala code

```scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

import me.hongchao.bitcoin4s.transaction._
import me.hongchao.bitcoin4s.script.ConstantOp._
import me.hongchao.bitcoin4s.script.ScriptFlag._
import me.hongchao.bitcoin4s.script.SigVersion.SIGVERSION_BASE
import me.hongchao.bitcoin4s.script.ScriptExecutionStage.ExecutingScriptSig
import me.hongchao.bitcoin4s.script._
import scodec.bits._
import cats.implicits._

val transaction = Tx(
  version = 1,
  tx_in = List(
    TxIn(
      previous_output = OutPoint(Hash(hex"7ca98806a4b4ab8d2952d3d65ccb450b411def420b3f8f0140bf11d8991ac5ab"), 0),
      sig_script = ByteVector.empty,
      sequence = -1
    )
  )
  ,tx_out = List(
    TxOut(
      value = 1,
      pk_script = ByteVector.empty
    )
  ),
  lock_time = 0
)

val initialState = InterpreterState(
  scriptPubKey = Seq(
    OP_0, OP_PUSHDATA(32),
    ScriptConstant(List(24, 99, 20, 60, 20, -59, 22, 104, 4, -67, 25, 32, 51, 86, -38, 19, 108, -104, 86, 120, -51, 77, 39, -95, -72, -58, 50, -106, 4, -112, 50, 98))
  ),
  scriptSig = Seq.empty[ScriptElement],
  currentScript = Seq.empty[ScriptElement],
  scriptP2sh = None,
  scriptWitness = None,
  scriptWitnessStack = Some(
    List(
      ScriptConstant(Seq(33, 2, 121, -66, 102, 126, -7, -36, -69, -84, 85, -96, 98, -107, -50, -121, 11, 7, 2, -101, -4, -37, 45, -50, 40, -39, 89, -14, -127, 91, 22, -8, 23, -104, -84)),
      ScriptConstant(Seq(48, 68, 2, 32, 66, 86, 20, 111, -49, -114, 115, -80, -3, -127, 127, -6, 42, 78, 64, -113, -16, 65, -113, -7, -121, -35, 8, -92, -12, -123, -74, 37, 70, -10, -60, 60, 2, 32, 63, 60, -116, 62, 47, -21, -64, 81, -31, 34, 40, 103, -11, -7, -48, -22, -16, 57, -42, 121, 41, 17, -63, 9, 64, -86,
, -57, 65, 35, 55, -114, 1))
    )
  ),
  stack = List(),
  altStack = List(),
  flags = Seq(SCRIPT_VERIFY_P2SH, SCRIPT_VERIFY_WITNESS, SCRIPT_VERIFY_WITNESS_PUBKEYTYPE),
  opCount = 0,
  transaction = transaction,
  inputIndex = 0,
  amount = 1,
  sigVersion = SIGVERSION_BASE,
  scriptExecutionStage = ExecutingScriptSig
)

// Exiting paste mode, now interpreting.

import me.hongchao.bitcoin4s.transaction._
import me.hongchao.bitcoin4s.script.ConstantOp._
import me.hongchao.bitcoin4s.script.ScriptFlag._
import me.hongchao.bitcoin4s.script.SigVersion.SIGVERSION_BASE
import me.hongchao.bitcoin4s.script.ScriptExecutionStage.ExecutingScriptSig
import me.hongchao.bitcoin4s.script._
import scodec.bits._
import cats.implicits._
transaction: me.hongchao.bitcoin4s.transaction.Tx = Tx(1,List(TxIn(OutPoint(7ca98806a4b4ab8d2952d3d65ccb450b411def420b3f8f0140bf11d8991ac5ab,0),ByteVector(empty),-1)),List(TxOut(1,ByteVector(empty))),0)
initialState: me.hongchao.bitcoin4s.script.Interprete...

```

then we can run the following code to execute the script

```scala
scala> val interpretedOutcome = Interpreter.create().run(initialState)
interpreterOutcome: me.hongchao.bitcoin4s.script.Interpreter.InterpreterErrorHandler[(me.hongchao.bitcoin4s.script.InterpreterState, Option[Boolean])] = Right((InterpreterState(List(OP_0, OP_PUSHDATA(32), ScriptConstant: List(24, 99, 20, 60, 20, -59, 22, 104, 4, -67, 25, 32, 51, 86, -38, 19, 108, -104, 86, 120, -51, 77, 39, -95, -72, -58, 50, -106, 4, -112, 50, 98)),List(),List(),None,Some(List(OP_PUSHDATA(33), ScriptConstant: List(2, 121, -66, 102, 126, -7, -36, -69, -84, 85, -96, 98, -107, -50, -121, 11, 7, 2, -101, -4, -37, 45, -50, 40, -39, 89, -14, -127, 91, 22, -8, 23, -104), OP_CHECKSIG)),Some(List(ScriptConstant: List(33, 2, 121, -66, 102, 126, -7, -36, -69, -84, 85, -96, 98, -107, -50, -121, 11, 7, 2, -101, -4, -37, 45, -50, 40, -39, 89, -14, -127, 91, 22, -8, 23,...

scala> interpretedOutcome.map{ case (finalState@_, interpretedResult) => interpretedResult }
res4: scala.util.Either[me.hongchao.bitcoin4s.script.InterpreterError,Option[Boolean]] = Right(Some(true))
```

# License

MIT: http://rem.mit-license.org
