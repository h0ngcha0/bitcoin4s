bitcoin4s
=========
[![Build Status](https://travis-ci.org/liuhongchao/bitcoin4s.svg?branch=master)](https://travis-ci.org/liuhongchao/bitcoin4s)
[![Coverage Status](https://coveralls.io/repos/github/liuhongchao/bitcoin4s/badge.svg?branch=master)](https://coveralls.io/github/liuhongchao/bitcoin4s?branch=master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Scala library for experimenting with Bitcoin scripts.

It provides a step by step debugger, through REST API.

Passes all bitcoin script [test cases](https://github.com/liuhongchao/bitcoin4s/blob/81996cf471ac4a25a28c4bfcb2060d3d0f2cc8bc/src/test/resources/script_test.json) from bitcoin core.

Table of Content
----------------

* [Parse bitcoin script](#parse-bitcoin-script)
* [Run bitcoin script interpreter](#run-bitcoin-script-interpreter)
* [Rest API](#rest-api)
* [How to use](#how-to-use)


Parse bitcoin script
--------------------

```scala
scala> import it.softfork.bitcoin4s.script.Parser
import it.softfork.bitcoin4s.script.Parser

scala> Parser.parse("0x76a9149984a74e6566b5009d47e46949938337081e9e9888ac")
res6: Seq[it.softfork.bitcoin4s.script.ScriptElement] = List(OP_DUP, OP_HASH160, OP_PUSHDATA(20), ScriptConstant: 0x9984a74e6566b5009d47e46949938337081e9e98, OP_EQUALVERIFY, OP_CHECKSIG)

```

Run bitcoin script interpreter
------------------------------

```scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

import it.softfork.bitcoin4s.Boot
val boot = new Boot {}
import boot._
import it.softfork.bitcoin4s.transaction.TxId
import scala.concurrent.Await
import scala.concurrent.duration._

val txId = TxId("bac02148041affd9dec93d30a86d1777d9570ab5d36fa2e0b7a20c0fa208e458")
val interpretedOutcome = Await.result(blockcypherService.interpret(txId, 0), 10.seconds)

interpretedOutcome.map{ case (finalState@_, interpretedResult) => interpretedResult }

// Exiting paste mode, now interpreting.

...

res0: scala.util.Either[it.softfork.bitcoin4s.script.InterpreterError,Option[Boolean]] = Right(Some(true))
```

REST API
--------

Running `sbt reStart` under the project root starts up an HTTP server listening to port `8888` which serves a few REST endpoints.

##### Example 1:
Execute the unlocking script of the first input of transaction `85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac` and its corresponding locking script. 

```bash
(⎈ |internal:swedbank-test)➜  blockchain curl localhost:8888/transaction/85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac/input/0/interpret
{
  "result" : {
    "type" : "Result",
    "value" : true
  },
  "state" : {
    "scriptPubKey" : [ {
      "type" : "OP_DUP",
      "value" : 118
    }, {
      "type" : "OP_HASH160",
      "value" : 169
    }, {
      "type" : "OP_PUSHDATA",
      "value" : 20
    }, {
      "type" : "ScriptConstant",
      "value" : "0x9b29f00db6970f2435c5576479cb5a79da56b078"
    }, {
      "type" : "OP_EQUALVERIFY",
      "value" : 136
    }, {
      "type" : "OP_CHECKSIG",
      "value" : 172
    } ],
    "scriptSig" : [ {
      "type" : "OP_PUSHDATA",
      "value" : 72
    }, {
      "type" : "ScriptConstant",
      "value" : "0x30450221009e1def21a903a4122a36052e0e32d3456ba409df0de45677412c31ea440962b4022036543b57869b3228a6d87f578e0014a10119175183140d7803d9949dfc24a89c01"
    }, {
      "type" : "OP_PUSHDATA",
      "value" : 33
    }, {
      "type" : "ScriptConstant",
      "value" : "0x03338efa2d319a07b8487205f5643f709ebe94fedb2b0ce61bbbf04262242b8ad0"
    } ],
    "currentScript" : [ ],
    "stack" : [ {
      "type" : "ScriptNum",
      "value" : 1
    } ],
    "altStack" : [ ],
    "stage" : {
      "type" : "ExecutingScriptPubKey"
    }
  }
}
```

##### Example 2
Same example as example 1, but stream each of the execution step back using websocket.

```bash
(⎈ |internal:default)➜  blockchain wscat -c ws://localhost:8888/transaction/85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac/input/0/stream-interpret
connected (press CTRL+C to quit)
< {"result":{"type":"NoResult"},"state":{"scriptPubKey":[{"type":"OP_DUP","value":118},{"type":"OP_HASH160","value":169},{"type":"OP_PUSHDATA","value":20},{"type":"ScriptConstant","value":"0x9b29f00db6970f2435c5576479cb5a79da56b078"},{"type":"OP_EQUALVERIFY","value":136},{"type":"OP_CHECKSIG","value":172}],"scriptSig":[{"type":"OP_PUSHDATA","value":72},{"type":"ScriptConstant","value":"0x30450221009e1def21a903a4122a36052e0e32d3456ba409df0de45677412c31ea440962b4022036543b57869b3228a6d87f578e0014a10119175183140d7803d9949dfc24a89c01"},{"type":"OP_PUSHDATA","value":33},{"type":"ScriptConstant","value":"0x03338efa2d319a07b8487205f5643f709ebe94fedb2b0ce61bbbf04262242b8ad0"}],"currentScript":[{"type":"OP_PUSHDATA","value":72},{"type":"ScriptConstant","value":"0x30450221009e1def21a903a4122a36052e0e32d3456ba409df0de45677412c31ea440962b4022036543b57869b3228a6d87f578e0014a10119175183140d7803d9949dfc24a89c01"},{"type":"OP_PUSHDATA","value":33},{"type":"ScriptConstant","value":"0x03338efa2d319a07b8487205f5643f709ebe94fedb2b0ce61bbbf04262242b8ad0"}],"stack":[],"altStack":[],"stage":{"type":"ExecutingScriptSig"}}}
< {"result":{"type":"NoResult"},"state":{"scriptPubKey":[{"type":"OP_DUP","value":118},{"type":"OP_HASH160","value":169},{"type":"OP_PUSHDATA","value":20},{"type":"ScriptConstant","value":"0x9b29f00db6970f2435c5576479cb5a79da56b078"},{"type":"OP_EQUALVERIFY","value":136},{"type":"OP_CHECKSIG","value":172}],"scriptSig":[{"type":"OP_PUSHDATA","value":72},{"type":"ScriptConstant","value":"0x30450221009e1def21a903a4122a36052e0e32d3456ba409df0de45677412c31ea440962b4022036543b57869b3228a6d87f578e0014a10119175183140d7803d9949dfc24a89c01"},{"type":"OP_PUSHDATA","value":33},{"type":"ScriptConstant","value":"0x03338efa2d319a07b8487205f5643f709ebe94fedb2b0ce61bbbf04262242b8ad0"}],"currentScript":[{"type":"OP_PUSHDATA","value":33},{"type":"ScriptConstant","value":"0x03338efa2d319a07b8487205f5643f709ebe94fedb2b0ce61bbbf04262242b8ad0"}],"stack":[{"type":"ScriptConstant","value":"0x30450221009e1def21a903a4122a36052e0e32d3456ba409df0de45677412c31ea440962b4022036543b57869b3228a6d87f578e0014a10119175183140d7803d9949dfc24a89c01"}],"altStack":[],"stage":{"type":"ExecutingScriptSig"}}}
< {"result":{"type":"NoResult"},"state":{"scriptPubKey":[{"type":"OP_DUP","value":118},{"type":"OP_HASH160","value":169},{"type":"OP_PUSHDATA","value":20},{"type":"ScriptConstant","value":"0x9b29f00db6970f2435c5576479cb5a79da56b078"},{"type":"OP_EQUALVERIFY","value":136},{"type":"OP_CHECKSIG","value":172}],"scriptSig":[{"type":"OP_PUSHDATA","value":72},{"type":"ScriptConstant","value":"0x30450221009e1def21a903a4122a36052e0e32d3456ba409df0de45677412c31ea440962b4022036543b57869b3228a6d87f578e0014a10119175183140d7803d9949dfc24a89c01"},{"type":"OP_PUSHDATA","value":33},{"type":"ScriptConstant","value":"0x03338efa2d319a07b8487205f5643f709ebe94fedb2b0ce61bbbf04262242b8ad0"}],"currentScript":[],"stack":[{"type":"ScriptConstant","value":"0x03338efa2d319a07b8487205f5643f709ebe94fedb2b0ce61bbbf04262242b8ad0"},{"type":"ScriptConstant","value":"0x30450221009e1def21a903a4122a36052e0e32d3456ba409df0de45677412c31ea440962b4022036543b57869b3228a6d87f578e0014a10119175183140d7803d9949dfc24a89c01"}],"altStack":[],"stage":{"type":"ExecutingScriptSig"}}}
...
...
< {"result":{"type":"Result","value":true},"state":{"scriptPubKey":[{"type":"OP_DUP","value":118},{"type":"OP_HASH160","value":169},{"type":"OP_PUSHDATA","value":20},{"type":"ScriptContant","value":[-101,41,-16,13,-74,-105,15,36,53,-59,87,100,121,-53,90,121,-38,86,-80,120]},{"type":"OP_EQUALVERIFY","value":136},{"type":"OP_CHECKSIG","value":172}],"scriptSig":[{"type":"OP_PUSHDATA","value":72},{"type":"ScriptContant","value":[48,69,2,33,0,-98,29,-17,33,-87,3,-92,18,42,54,5,46,14,50,-45,69,107,-92,9,-33,13,-28,86,119,65,44,49,-22,68,9,98,-76,2,32,54,84,59,87,-122,-101,50,40,-90,-40,127,87,-114,0,20,-95,1,25,23,81,-125,20,13,120,3,-39,-108,-99,-4,36,-88,-100,1]},{"type":"OP_PUSHDATA","value":33},{"type":"ScriptContant","value":[3,51,-114,-6,45,49,-102,7,-72,72,114,5,-11,100,63,112,-98,-66,-108,-2,-37,43,12,-26,27,-69,-16,66,98,36,43,-118,-48]}],"currentScript":[],"stack":[{"type":"ScriptNum","value":1}],"altStack":[],"stage":{"type":"ExecutingScriptPubKey"}}}
disconnected
```

How to use
----------

Add the following to your build.sbt

```scala
libraryDependencies += "it.softfork" %% "bitcoin4s" % "0.1.0"
```

with the following resolver

```scala
resolvers += Resolver.bintrayRepo("liuhongchao", "maven")
```

# License

MIT: http://rem.mit-license.org
