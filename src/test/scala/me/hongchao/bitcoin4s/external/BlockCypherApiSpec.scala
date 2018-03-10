package me.hongchao.bitcoin4s.external

import com.typesafe.scalalogging.StrictLogging
import me.hongchao.bitcoin4s.Spec
import me.hongchao.bitcoin4s.crypto.Hash
import me.hongchao.bitcoin4s.script._
import org.spongycastle.util.encoders.Hex

import scala.util.control.Exception.allCatch
import cats.implicits._
import me.hongchao.bitcoin4s.script.SigVersion.SIGVERSION_WITNESS_V0

class BlockCypherApiSpec extends Spec with StrictLogging {

  val blockCypherCreditingRawTransaction =
    s"""
       |{
       |  "block_hash": "0000000000000000000d123700875dd3ff100d0b7cc978714f8f2f945763232c",
       |  "block_height": 508427,
       |  "block_index": 385,
       |  "hash": "4c24b3019f117f109ffac46eb406c544f6d5a57fd197e5ef845de71f15b42771",
       |  "addresses": [
       |    "14WCUpKjenh7ZmRnVAkihTkLDH1PCeXwZ6",
       |    "1DydczknH5SpMHDtp48zECWi2GVGYgJ2J6",
       |    "3BKAXzMepun6cBHGrouS95EnnYQAZVfXfz"
       |  ],
       |  "total": 4907350,
       |  "fees": 33600,
       |  "size": 224,
       |  "preference": "high",
       |  "relayed_by": "107.172.9.157:8333",
       |  "confirmed": "2018-02-09T20:39:12Z",
       |  "received": "2018-02-09T20:34:27.018Z",
       |  "ver": 1,
       |  "double_spend": false,
       |  "vin_sz": 1,
       |  "vout_sz": 2,
       |  "confirmations": 2315,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac",
       |      "output_index": 0,
       |      "script": "483045022100fb4454cd8e07c07471233d17ffd0ed2a6913cdf6121e9c6e20c48410308eede502203cb8e7274fb59eb96b6a28d707e06a46d2c9ead4320dafc0837d7ccb91f38add012102a8d373f7745adf9971324012ab0a7399e78e881ea7fa316adb751444bcc18605",
       |      "output_value": 4940950,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "1DydczknH5SpMHDtp48zECWi2GVGYgJ2J6"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 506681
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 2441174,
       |      "script": "a914698f4b7e28e4b66e68783be11bbe059de03cd39f87",
       |      "spent_by": "1191bdc374a460839471cedb284fc82e5a9d2c45c49a4a4658db5ab9c5916911",
       |      "addresses": [
       |        "3BKAXzMepun6cBHGrouS95EnnYQAZVfXfz"
       |      ],
       |      "script_type": "pay-to-script-hash"
       |    },
       |    {
       |      "value": 2466176,
       |      "script": "76a914266e05ddf3c4df4c61db0ed623428d12c9a1342088ac",
       |      "addresses": [
       |        "14WCUpKjenh7ZmRnVAkihTkLDH1PCeXwZ6"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    }
       |  ]
       |}
     """.stripMargin

  val blockCypherSpendingRawTransaction =
    s"""
       |{
       |  "block_hash": "000000000000000000489e24c0f64b7027cc80d3d20faef1f1cdc3b0daac4019",
       |  "block_height": 508439,
       |  "block_index": 5,
       |  "hash": "1191bdc374a460839471cedb284fc82e5a9d2c45c49a4a4658db5ab9c5916911",
       |  "addresses": [
       |    "1FAt1gokixe9Y6QdHhXeJ5bjXmKCeNp6a6",
       |    "1Gez1oJoa5hwgfcXkYHj68298C4RQwApQA",
       |    "3BKAXzMepun6cBHGrouS95EnnYQAZVfXfz"
       |  ],
       |  "total": 2386774,
       |  "fees": 54400,
       |  "size": 142,
       |  "preference": "high",
       |  "relayed_by": "24.103.166.34:8333",
       |  "confirmed": "2018-02-09T22:44:55Z",
       |  "received": "2018-02-09T22:44:16.322Z",
       |  "ver": 2,
       |  "lock_time": 508438,
       |  "double_spend": false,
       |  "vin_sz": 1,
       |  "vout_sz": 2,
       |  "confirmations": 137,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "4c24b3019f117f109ffac46eb406c544f6d5a57fd197e5ef845de71f15b42771",
       |      "output_index": 0,
       |      "script": "16001413ecf507209720bc23e47368d98303629eed74e3",
       |      "output_value": 2441174,
       |      "sequence": 4294967294,
       |      "addresses": [
       |        "3BKAXzMepun6cBHGrouS95EnnYQAZVfXfz"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 508427,
       |      "witness": [
       |        "3044022061d6ef24187f5666ed1d2c771d68f2ce8a0b539df632bd54ad757c9654d07f0202204cf5c998e43a934c8e241cb1a9ebff5ccecc06072dea47fe6e691d8ea61d10c701",
       |        "027bb5772df97bbfc616b7a1ae91a986ea4d295f4e27d26ecf43e073f92d721b7a"
       |      ]
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 977703,
       |      "script": "76a9149b7016504b9b035c0ffbc54853d8758faf85271488ac",
       |      "spent_by": "b945cc88c7295a0fe53de3d654be277135957e8aaf9f243db94e19d889223a17",
       |      "addresses": [
       |        "1FAt1gokixe9Y6QdHhXeJ5bjXmKCeNp6a6"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    },
       |    {
       |      "value": 1409071,
       |      "script": "76a914abb8debac72eef4b426d81fac4810c5cb3971f9f88ac",
       |      "spent_by": "dd1e147eaa6715f70e6be23d9af391b51fd19c76d26ba0ece865cf7539ab4f9b",
       |      "addresses": [
       |        "1Gez1oJoa5hwgfcXkYHj68298C4RQwApQA"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    }
       |  ]
       |}
     """.stripMargin

  "Interpreter" should "be able to interpret raw transaction fetched from BlockCypher API" in {

    val creditingTransaction = BlockCypherApi.parseTransaction(blockCypherCreditingRawTransaction)
    val spendingTransaction = BlockCypherApi.parseTransaction(blockCypherSpendingRawTransaction)

    // Only look at the first input
    //val firstScriptPutKey = spendingTx.tx_in(0).sig_script
    val spendingTx = spendingTransaction.toTxWitness
    val txIn = spendingTransaction.inputs(0)
    val txOut = creditingTransaction.outputs(0)
    val scriptSig = parseHexString(txIn.script)
    val scriptPubKey = parseHexString(txOut.script)
    val witnessesStack = txIn.witness.map { rawWitnesses =>
      rawWitnesses.reverse.flatMap { rawWitness =>
        allCatch.opt(Hex.decode(rawWitness).toSeq).map(ScriptConstant.apply)
      }
    }
    val amount = 2441174
    val flags = toScriptFlags("P2SH,WITNESS")

    val initialState = InterpreterState(
      scriptPubKey = scriptPubKey,
      scriptSig = scriptSig,
      scriptWitnessStack = witnessesStack,
      flags = flags,
      transaction = spendingTx,
      inputIndex = 0,
      amount = amount,
      sigVersion = SIGVERSION_WITNESS_V0
    )

    val interpreterOutcome = Interpreter.create(verbose = false).run(initialState)

    interpreterOutcome match {
      case Right((finalState@_, interpretedResult)) =>
        logger.info(s"interpretedResult: $interpretedResult")

      case Left(e) =>
        throw e
    }
  }

  private def parseHexString(hex: String): Seq[ScriptElement] = {
    Parser.parse(Hash.fromHex(hex))
  }

  private def toScriptFlags(scriptFlagsString: String): Seq[ScriptFlag] = {
    scriptFlagsString.split(",").map(_.trim).flatMap(ScriptFlag.fromString)
  }
}
