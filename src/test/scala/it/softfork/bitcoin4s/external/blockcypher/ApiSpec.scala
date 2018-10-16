package it.softfork.bitcoin4s.external.blockcypher

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import it.softfork.bitcoin4s.Spec
import it.softfork.bitcoin4s.crypto.Hash
import it.softfork.bitcoin4s.script.SigVersion.SIGVERSION_WITNESS_V0
import it.softfork.bitcoin4s.script._
import org.spongycastle.util.encoders.Hex

import scala.util.control.Exception.allCatch

class ApiSpec extends Spec with StrictLogging {

  "Interpreter" should "be able to interpret raw transactions from BlockCypher API" in {
    Seq(BlockCypherExample1, BlockCypherExample2, BlockCypherExample3).foreach { example =>
      val testDataName = example.getClass.getSimpleName
      withClue(testDataName) {
        runExample(example)
      }
    }
  }

  it should "be able to interpret raw transactions from BlockCypher API Example 4" in {
    Seq(BlockCypherExample4).foreach { example =>
      val testDataName = example.getClass.getSimpleName
      withClue(testDataName) {
        runExample(example)
      }
    }
  }

  private def runExample(blockCypherExample: BlockCypherExample) = {
    val creditingTransaction = Api.parseTransaction(blockCypherExample.creditingRawTransaction)
    val spendingTransaction = Api.parseTransaction(blockCypherExample.spendingRawTransaction)

    // Only look at the first input
    //val firstScriptPutKey = spendingTx.tx_in(0).sig_script
    val spendingTx = spendingTransaction.toTx
    val txIn = spendingTransaction.inputs(0)
    val txOut = creditingTransaction.outputs(txIn.output_index)
    val scriptSig = txIn.script.map(parseHexString _).getOrElse(Seq.empty[ScriptElement])
    val scriptPubKey = parseHexString(txOut.script)
    val witnessesStack = txIn.witness.map { rawWitnesses =>
      rawWitnesses.reverse.flatMap { rawWitness =>
        allCatch.opt(Hex.decode(rawWitness).toSeq).map(ScriptConstant.apply)
      }
    }

    val amount = txOut.value
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

    val interpreterOutcome = Interpreter.create(verbose = true).run(initialState)

    interpreterOutcome match {
      case Right((finalState@_, interpretedResult)) =>
        interpretedResult.value shouldBe true
        logger.info(s"interpretedResult: $interpretedResult")

      case Left(e) =>
        fail("Interpreter failed", e)
    }
  }

  private def parseHexString(hex: String): Seq[ScriptElement] = {
    Parser.parse(Hash.fromHex(hex))
  }

  private def toScriptFlags(scriptFlagsString: String): Seq[ScriptFlag] = {
    scriptFlagsString.split(",").map(_.trim).flatMap(ScriptFlag.fromString)
  }
}

trait BlockCypherExample {
  val creditingRawTransaction: String
  val spendingRawTransaction: String
}

object BlockCypherExample1 extends BlockCypherExample {
  val creditingRawTransaction =
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

  val spendingRawTransaction =
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
}

object BlockCypherExample2 extends BlockCypherExample {
  val creditingRawTransaction =
    s"""
       |{
       |  "block_hash": "00000000000000000019506e76b2e4aec601e4c900bee007e1d6e9b0a8af1efd",
       |  "block_height": 545465,
       |  "block_index": 1724,
       |  "hash": "d9215c081d17b15cdccdc454348a7bd88c55015689cbfc507c5dcb8ee7c35999",
       |  "addresses": [
       |    "1EzjJHDZTcbX7vBGCZzXejMLuKKUEwLYa8",
       |    "3Gmt6odA8AEis2ATiY732qoPNFuG8YsodM",
       |    "3JLw6nxh5RKvtTpZSdWibJdEQvghhoVeYH",
       |    "3NEb2m7P8BSiZ3ethQPWwBZ1HuPn8sYaej"
       |  ],
       |  "total": 1557850,
       |  "fees": 2590,
       |  "size": 204,
       |  "preference": "medium",
       |  "relayed_by": "91.229.23.122:8333",
       |  "confirmed": "2018-10-12T13:52:08Z",
       |  "received": "2018-10-12T13:01:09.411Z",
       |  "ver": 2,
       |  "lock_time": 545460,
       |  "double_spend": false,
       |  "vin_sz": 2,
       |  "vout_sz": 2,
       |  "confirmations": 225,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "6ba163072c6eeff529dc56a5e1158eb457e1b95b532a93e169380a83f01d250a",
       |      "output_index": 0,
       |      "script": "160014898c836f47035a8e493232d0117c2ef3f62783ad",
       |      "output_value": 1106880,
       |      "sequence": 4294967294,
       |      "addresses": [
       |        "3JLw6nxh5RKvtTpZSdWibJdEQvghhoVeYH"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 545458,
       |      "witness": [
       |        "3045022100c09b891022e14a9ebac0fa17df5781df0f694521fc9b911e55129d8005777c2a02206c6a51cfca96422fa9786f6561e3c91a89c2e5efc247ea018fae0fd1d8cd0a6201",
       |        "03f5f329e3d27a4afae8f62e6cbb7891a1923776a1b8b41162266f14149a570704"
       |      ]
       |    },
       |    {
       |      "prev_hash": "3dfcd11d4b0e455347a04ed8faa1cc8f8acdbf220d44fd3c9a247b5d77f9d1a9",
       |      "output_index": 5,
       |      "script": "16001461ae8a75b0f2ebcfc42cc54b2a53762abf6ded2b",
       |      "output_value": 453560,
       |      "sequence": 4294967294,
       |      "addresses": [
       |        "3Gmt6odA8AEis2ATiY732qoPNFuG8YsodM"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 545454,
       |      "witness": [
       |        "3045022100e8b58c2ad18a5d5c3e886e77c818eb7ec855736579771aee44439e18682835b9022023ac778e5dd97285cd9529a991a1ae5f1e82967b1acb2910578fb88c254b17ad01",
       |        "03d513e44eccb2c571fdaa1d3acf11b03511d000ec2d574c92e3434abaf2ead052"
       |      ]
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 555960,
       |      "script": "76a9149984a74e6566b5009d47e46949938337081e9e9888ac",
       |      "spent_by": "be2ba0e4e6d9bb7582390cb2b446d9c0b21d625d3884d947858bd1b3f67771e1",
       |      "addresses": [
       |        "1EzjJHDZTcbX7vBGCZzXejMLuKKUEwLYa8"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    },
       |    {
       |      "value": 1001890,
       |      "script": "a914e15b0c07819557b72d006484ba2785e378d4020187",
       |      "spent_by": "07881d844b1d952036b2988a661a7268b8013a3895ec5030d5a5d45ae612b187",
       |      "addresses": [
       |        "3NEb2m7P8BSiZ3ethQPWwBZ1HuPn8sYaej"
       |      ],
       |      "script_type": "pay-to-script-hash"
       |    }
       |  ]
       |}
     """.stripMargin

  val spendingRawTransaction =
    s"""
       |{
       |  "block_hash": "000000000000000000197ec0a982d55e56ecd02d84d514e18b61e13b085b21f8",
       |  "block_height": 545473,
       |  "block_index": 1,
       |  "hash": "be2ba0e4e6d9bb7582390cb2b446d9c0b21d625d3884d947858bd1b3f67771e1",
       |  "addresses": [
       |    "1EzjJHDZTcbX7vBGCZzXejMLuKKUEwLYa8",
       |    "1FLQNemYAD4x33z6izRuWsRPMeB45GAQ2m",
       |    "1HXpg8D9AMGFVZ9FEU2tkZYvAZ8xBhVudo",
       |    "1PAr99E4aWPKXk6rwhumwNPqjjH18ueXMD",
       |    "3BQA7VoRzhrQuwS7LtVCLhkSN4n2kErM9h"
       |  ],
       |  "total": 1882592,
       |  "fees": 134268,
       |  "size": 665,
       |  "preference": "high",
       |  "relayed_by": "194.109.20.68:8333",
       |  "confirmed": "2018-10-12T15:34:01Z",
       |  "received": "2018-10-12T15:29:48.069Z",
       |  "ver": 2,
       |  "lock_time": 545470,
       |  "double_spend": false,
       |  "vin_sz": 4,
       |  "vout_sz": 2,
       |  "confirmations": 104,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "d9215c081d17b15cdccdc454348a7bd88c55015689cbfc507c5dcb8ee7c35999",
       |      "output_index": 0,
       |      "script": "473044022047a44afee68a0c9e5fba6d51a5957dc35c0aa0a235ba8f9fc99681f686beb9230220014225809471cac8828481ca54043264c69b947c3bf01ee255bcfd1c3086f8bd012102873955dabe7c6de3ea021e8c2a62a1ec2309850bcd4335b12ab80d0d73cc7b31",
       |      "output_value": 555960,
       |      "sequence": 4294967294,
       |      "addresses": [
       |        "1EzjJHDZTcbX7vBGCZzXejMLuKKUEwLYa8"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 545465
       |    },
       |    {
       |      "prev_hash": "84fd04628e92916022a63c0aeb8e61c1fcde10f2b20f381fc37d215d51956d95",
       |      "output_index": 0,
       |      "script": "4830450221008730026c3437edad9bbeeb39126110862ff22d9477a7f0b4547f76ce32e6f8ff022060a130ed8c83ae1e6bc82c8f76339f55cf7c70c47a1b9341f296b2511156d036012102881f828bd04b12374f1d3faa26357595e590d499a1217feda42904b745694b71",
       |      "output_value": 272760,
       |      "sequence": 4294967294,
       |      "addresses": [
       |        "1HXpg8D9AMGFVZ9FEU2tkZYvAZ8xBhVudo"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 545465
       |    },
       |    {
       |      "prev_hash": "771194b9733abc9ef7a4a20ca7fe2dddfe1ea0ed7036c1d8e8b7094357245e62",
       |      "output_index": 0,
       |      "script": "4730440220338d74de2dd6f9dd2307a4befca8639fc7ac4fce5637b0c024b5144fdd4b0c8002207835d14f480d27ffba68885c9bab08d8178d4a8812c53c9131bb8c1fab4f52f3012102881f828bd04b12374f1d3faa26357595e590d499a1217feda42904b745694b71",
       |      "output_value": 381860,
       |      "sequence": 4294967294,
       |      "addresses": [
       |        "1HXpg8D9AMGFVZ9FEU2tkZYvAZ8xBhVudo"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 545465
       |    },
       |    {
       |      "prev_hash": "54a19a1a84c50cd81ea3f651cfc53d2959cecdf854dec16dfa081b2f46dff640",
       |      "output_index": 0,
       |      "script": "4730440220299ea23fd3034c3554796b0bf09061ee1bb68c9738214d17494e401365b7fe95022064e79e041d613bdcb4aa83ad50da667e55e7d11db3c8d9588f78813ca086719c01210210dcece02cfbe8148319f4c5f4d1719f02fb31707bd997945d2b081ed4cce61b",
       |      "output_value": 806280,
       |      "sequence": 4294967294,
       |      "addresses": [
       |        "1PAr99E4aWPKXk6rwhumwNPqjjH18ueXMD"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 545469
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 866481,
       |      "script": "76a9149d3d2cdfed35b8b9851e6eab5ff2423ac6fdc1f688ac",
       |      "spent_by": "d1b620fc549cec8330c03bb535098fda11f0359e94d6e788258d84dc41a15bf9",
       |      "addresses": [
       |        "1FLQNemYAD4x33z6izRuWsRPMeB45GAQ2m"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    },
       |    {
       |      "value": 1016111,
       |      "script": "a9146a8105aee2153af64cd8b7190c19ce9789fa4d8287",
       |      "spent_by": "de5d60fd559ff302b319c5411b318919bb12f04e369b6d56ab4c33a150c43196",
       |      "addresses": [
       |        "3BQA7VoRzhrQuwS7LtVCLhkSN4n2kErM9h"
       |      ],
       |      "script_type": "pay-to-script-hash"
       |    }
       |  ]
       |}
     """.stripMargin
}

object BlockCypherExample3 extends BlockCypherExample {
  val spendingRawTransaction =
    s"""
       |{
       |  "block_hash": "0000000000000000000f69ef7775417a70d06f729914bef1787b7c330634c6bb",
       |  "block_height": 545777,
       |  "block_index": 1,
       |  "hash": "71c9e4aa40e4f799d15b5d1a15926d2bbb1098e3b7ec21ba7e8029b847e0d4ca",
       |  "addresses": [
       |    "17tQ2xVmsou9ZQiLBwMtaqUVpvgbKYJfTV",
       |    "1B3GUJt7zGZVApqBf5BHJUmfWrpXN8UnBZ",
       |    "1GTkVY4GTo8ewQZDE1zgVX6WnUQm7VQMGc"
       |  ],
       |  "total": 29941032,
       |  "fees": 2600,
       |  "size": 226,
       |  "preference": "medium",
       |  "relayed_by": "104.199.47.152:8333",
       |  "confirmed": "2018-10-15T00:03:33Z",
       |  "received": "2018-10-15T00:00:16.958Z",
       |  "ver": 2,
       |  "double_spend": false,
       |  "vin_sz": 1,
       |  "vout_sz": 2,
       |  "confirmations": 126,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "e5a46134c6fea347130fb672e371e2e6868950bbaab442951a05fa7d02ed656d",
       |      "output_index": 1,
       |      "script": "483045022100fecc6ac7dc70f4e0599d74ec607fdd0e85c2dc67d894ea27d6440df8fa2ecb9802204baffd10526e5e682ed84a06ab79ebeaa094201a7c0de5457b89462c117629690121028778345d37c04ad7aac7061893687992b3e26cd89762699c29ad3ac7e6cac7f9",
       |      "output_value": 29943632,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "1GTkVY4GTo8ewQZDE1zgVX6WnUQm7VQMGc"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 545774
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 28997824,
       |      "script": "76a9144b893244063e8dfe80a096f51493fe15f44ac2c088ac",
       |      "spent_by": "bb027d19eae8560bf6dced993b08792e910ecd2991af06e9a89d9427196d87a0",
       |      "addresses": [
       |        "17tQ2xVmsou9ZQiLBwMtaqUVpvgbKYJfTV"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    },
       |    {
       |      "value": 943208,
       |      "script": "76a9146e1f0110c5b08e4ada57ddf4a1116672d4f0ddc188ac",
       |      "spent_by": "7cc852a667c2c4604673e0d37ababa9d721c31b64ed5f5dba4d10fd66f952a9f",
       |      "addresses": [
       |        "1B3GUJt7zGZVApqBf5BHJUmfWrpXN8UnBZ"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    }
       |  ]
       |}
     """.stripMargin

  val creditingRawTransaction =
    s"""
       |{
       |  "block_hash": "0000000000000000000419f5e9a0c52734d6014c1e36fad1c95a58aac31ec627",
       |  "block_height": 545774,
       |  "block_index": 1526,
       |  "hash": "e5a46134c6fea347130fb672e371e2e6868950bbaab442951a05fa7d02ed656d",
       |  "addresses": [
       |    "1GTkVY4GTo8ewQZDE1zgVX6WnUQm7VQMGc",
       |    "1Gz72nPhSYKhwuT5o7kCMHmj41Ney7SuNz",
       |    "1JEisjhJmRv1v3yTCNVtry6WrqvdkULn6M"
       |  ],
       |  "total": 42029620,
       |  "fees": 2600,
       |  "size": 226,
       |  "preference": "medium",
       |  "relayed_by": "213.239.216.162:9001",
       |  "confirmed": "2018-10-14T23:56:28Z",
       |  "received": "2018-10-14T23:40:19.01Z",
       |  "ver": 2,
       |  "double_spend": false,
       |  "vin_sz": 1,
       |  "vout_sz": 2,
       |  "confirmations": 129,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "a26394328d1250314e9092947bccebccd1a10b46ed6ead01c3165da17ffe2fa9",
       |      "output_index": 1,
       |      "script": "483045022100a38ee4ebff13ef0a309f0b5d8bf8392060158113ba160c532e99cfd4610ef82302203981e07885cc234c2f2a84cf75a185a21851f249f32d58fb7430f36546a1effb0121025b9b48e5c3a6cb6446583514e21b7b04f4b1534a97e2e4e643c22457e0837e3c",
       |      "output_value": 42032220,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "1Gz72nPhSYKhwuT5o7kCMHmj41Ney7SuNz"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 545769
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 12085988,
       |      "script": "76a914bd1280493a388f2c878f02e19d470d659e3a262288ac",
       |      "spent_by": "f4b2507d7f39b9b6e97b863ecfe9c215dd3435f72596b52f90245ba2c4c707c7",
       |      "addresses": [
       |        "1JEisjhJmRv1v3yTCNVtry6WrqvdkULn6M"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    },
       |    {
       |      "value": 29943632,
       |      "script": "76a914a99901cdefecf382b1ae31ec70a53987b0cb668988ac",
       |      "spent_by": "71c9e4aa40e4f799d15b5d1a15926d2bbb1098e3b7ec21ba7e8029b847e0d4ca",
       |      "addresses": [
       |        "1GTkVY4GTo8ewQZDE1zgVX6WnUQm7VQMGc"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    }
       |  ]
       |}
     """.stripMargin
}

object BlockCypherExample4 extends BlockCypherExample {
  val spendingRawTransaction =
    s"""
       |{
       |  "block_hash": "0000000000000000000ad72b08f8409835493c12364a62e534fc0e010757b8d5",
       |  "block_height": 545694,
       |  "block_index": 1285,
       |  "hash": "bac02148041affd9dec93d30a86d1777d9570ab5d36fa2e0b7a20c0fa208e458",
       |  "addresses": [
       |    "32APJeyP4SQ7M4KuKVejzVSUprhTrWPKPC",
       |    "32BkL9gtAorKEXznTXH46GShcuoEud73Q9",
       |    "32KUmJa2f3ZPD1bkBVafGDhPznimxrg6qJ",
       |    "32VDue7ej4YAcBrgTFucjYFFTe3woVfzKi",
       |    "33F3JgsCug1oqYHCRvSgtJ5SRWa592QJbu",
       |    "33NoPF1CoTBWo3DTADNyVwwEhqkP999SDq",
       |    "33YQtAb4N72ZY51gjdN1pdtYbXrGzMDZjW",
       |    "33p1AL9YnJcFD5oo6tkGxywuh1B3wXzoGR",
       |    "33pzVhtti7PWrem7naNmy1L5EPvKP8wmZW",
       |    "35RS2ZcMTtcnX1pKSrxDgRd32bVKdUYkPf",
       |    "35oWxMRwCCgkGqpw1xwPPjNEUtTVvkq97W",
       |    "362NDuuiX2PYGfQsVHcCcEzKwRoSBcfzBf",
       |    "36AWqYtiPtJSyoyrCH8GEMtgFmio7x4ogC",
       |    "36T1pBoHiLNJo3yjC3mkX9w28Jy3YNPsrb",
       |    "36k1bAbzrWPMJKGP7v6qqvisyjEyiNRwXc",
       |    "36tXsy3Mb8qG4cDNGnSLSZWvwWc5a2cszp",
       |    "37kmohzRHqdaAvP8Wj9HgJJEUzJa6k5Rsu",
       |    "393hzUTwuykKkDJ7rzBi9gSttQcZ2B2bWK",
       |    "39Znk8TPpRWPT3o3FgXwdEm7rd8wwLmU2v",
       |    "3AKYA1ch35RpD2gXTnNcVF4rdNhzMAVsYk",
       |    "3AQDoSShsWCXvBLHZNdJ1ehiQti55MAk9X",
       |    "3ATSjVEaXQxDtXTgMWwHgt7J5SAzFe3mDT",
       |    "3ApT7DqtajgeECj5R4t7pcWNjVi6wYsjGi",
       |    "3BB89ikKftRcTAWQAFXCcYMWvybfrxa4D8",
       |    "3Bbcxvognrid44XRmi2BuAeBg2MsrRb9uM",
       |    "3CjDTcs1fabLoFUnUkz3ktu7hDFqfFrtXt",
       |    "3CpLZ5j21MCoh8RtrejnvYnRG9tqN5fn2Y",
       |    "3DpguSQfWvhttcbyg5EZMjhVS9WXCkC8G3",
       |    "3Dpmmwx9j7fpZ4Uknsd4XJfDMRPHJYpouh",
       |    "3E3qE71TwhpTBfxgBJKH2sZ7Pws8kmz2BW",
       |    "3ES1afza3HPHeD2Xz1wPCnUvvhnfFLmEDg",
       |    "3EYYYCdcMLQtJVVbZnUeNz5a5VeLzUSNTw",
       |    "3Eub4NaGwqFBXzvJEtpbaGzZEB9zVVJEup",
       |    "3FVgX4QHFUeP8mmVgqinjwBNPBLmiApXiQ",
       |    "3FcM4fyNivfjyH2dyQMsZKBcWwKccUJXj5",
       |    "3GJCVyE5rXej6HwofLrUc2WvoLT3o9idmh",
       |    "3GfJDRqYhtu9EQtr6jYUBojikvPARxMn3W",
       |    "3GwUYFzQVhaUxkReefw9pjjRHiedjMsMa7",
       |    "3HJb7zfLVnR3GofWoNzV19fgn5JW4WaGs5",
       |    "3HT6XWFhbEGFzDLnFN9pB1S41AN1Ai7Kyd",
       |    "3HiZrNhUz6bQFJRTYJ4m1QSQqgGwV31Szs",
       |    "3HkwmWfCRrF82aHfCGGZtdZdSspY6s9Yfi",
       |    "3HqKwySuEYo23CZfmTgz5tRUkHLmnGJRtD",
       |    "3Hqbt6RsdHP7cgbetSuVF1CCJyqVW1trNe",
       |    "3JKwiNPVsM3GEC6PBsVWpVb7LUwKS8kg6a",
       |    "3JYUuoH6jTnXFyvNSsKUgsnURHbF5MmJPA",
       |    "3JbKga2CHEguAxTLN3qj6ZQTEjDawTJVSx",
       |    "3KGE6Uq82y7f39aV6eFmVZQfWnbEuxS6JQ",
       |    "3KJP2d7Y97RMbKZgEbJeHRq9VXoK3zQsCF",
       |    "3LLxF4zFphSNgRNDVjg2zGp18xZyU6TTq6",
       |    "3Lan738TweEL9DDwhohpumyTRgSGa1ud6s",
       |    "3LaxcK6R5kPFU8eh35VNsxgB4vx7rsBBPW",
       |    "3LmU1rM2jCv3X7d6QN657NdS1pDshpQHQH",
       |    "3LtUGjgwK3ysZiX8xY34Ctkd8quDaMrvpS",
       |    "3M9qzpmFJxwD9uAtfdvozuS36TY2jYqGnK",
       |    "3MCvK8DwVrehJ5VduZkCsPq3WxqMeWA66B",
       |    "3MKyBpAXXDT4k8kb5yvo365HoX8d1iVD19",
       |    "3NKDu5fFcXoFKjcKG6S1gGmy4dcU71AMi2",
       |    "3NVLNuFfrhpPdCBFegB1CS36fTNQXUuoXK",
       |    "3NhhTepLvADyEuVpodwnUmWowKyP2xL9wX",
       |    "3NmNvty9pkZx9JAUNtMsmVgXHhaPQndTmA",
       |    "3NswK2AYptaKrLwpJCgcgDorrv4nSY7EFb",
       |    "3NwGNg6qFzu6H4BBkdKZr4ECujAUAVEPgD",
       |    "3PAEhEciibyoxjehfYzMqRXoJFz2ZdPz4F",
       |    "3PNi9qNRJue2Kj3PHCSv3Aw7aYnGEef74q",
       |    "3PmVZTBPm89HWZoGxdKPaWYRjnuRCA9khj",
       |    "3QAo8hxAPbaMtak5Zny8kX35dL1amgnFDP",
       |    "3QMnaWuYSPQV8xNKavacroWCUdMchQNdg5",
       |    "3QbooApUeh8Gs8vtVgVzoPNb6KaFFvHCrK",
       |    "3QjMDhEytccGeX7YqeLopgJiqFd5MyLThg"
       |  ],
       |  "total": 1190780706,
       |  "fees": 54211,
       |  "size": 5362,
       |  "preference": "medium",
       |  "confirmed": "2018-10-14T08:28:45Z",
       |  "received": "2018-10-14T08:28:45Z",
       |  "ver": 2,
       |  "double_spend": false,
       |  "vin_sz": 70,
       |  "vout_sz": 1,
       |  "confirmations": 372,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "f6c7343b9ab72d644230ddf8eb203379b7dc81fbb0fc2fb249fe23984e4e9d35",
       |      "output_index": 1,
       |      "script": "22002053d66e8fd94384a44ed7b3d27e901c293f3dade7f247633b5602d31a1705837c",
       |      "output_value": 831457,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3QAo8hxAPbaMtak5Zny8kX35dL1amgnFDP"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 542076,
       |      "witness": [
       |        "",
       |        "3045022100e17ea619e9da3b89b4d3328d253d22af3f013abbc63970c4921c05db0f80863d02206ef2d08e2a391357503fba819d0efa5c54fc1f70effc47aec203d16bc92acdbb01",
       |        "3045022100fe8a24172be60872408217b59623bd99fefe82d76cbcba50ba4c3f768ff2f861022055c4494a3aec8570e72b0c5091736eb13a106b18d59d468a44a4b8ec9569db7901",
       |        "5221034f1dbe712878f885ed22fae96245d8277aee9f9ccbe03cacbb4057ecfd7870582102e9f8240c8b51a14af6752121653a3fdada7d58357d4bbb01a8536fa8273ccce62103c8b4811f0860537e8bb4c4d02922d2910afd6000edbb164947ed6dcbd6d3c80553ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "fdb8b90f2238e9ba256528630add07e2205bb72806b7d6e1e155e5bd94769d35",
       |      "output_index": 0,
       |      "script": "2200203c99a50161d3819d28f4e57d125ef75dbf914f3dbd59cd7bec9a2334283b3dab",
       |      "output_value": 1097000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "35RS2ZcMTtcnX1pKSrxDgRd32bVKdUYkPf"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 509926,
       |      "witness": [
       |        "",
       |        "3044022040757c76aaa8d58e3036953772be198d18f27652e5269395de906bd0cd1fd0ae0220092f98874efad84da122de62ea2a9d98ebf4e3f11760b3f4edf16b0133dde3f701",
       |        "304402201892a870f284d48cd2943a4b981f6a5f4bcb5d270a4d2394e6d3d3aaa8b8958c02203680863621a2a6bff8ae533a3e96a9b6cacb303105845b3283102bfd2fc251b501",
       |        "522102dc524903159185070d6547cc6a127a776aa6fcce4f0e70a81917bd589af032dc2102fc61fb386fcc76506e119ba653ece4e94d57263f449c4c3ad1992beed16658b02103d7788212171f6ef0abf5c12de591bd676e49b02c58a5cee7a631e4a9df67988253ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "c2e89dda99448b6dd2a5c0694533a7a6ae560452734c8b11c0490b6175929d35",
       |      "output_index": 0,
       |      "script": "2200205487102fcbd5e4ff538ee0af842a05c6e12dea0af4e345633255c04d477add7e",
       |      "output_value": 595000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3LaxcK6R5kPFU8eh35VNsxgB4vx7rsBBPW"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 508388,
       |      "witness": [
       |        "",
       |        "30450221009487a001a9f9af7079ce82048316af60bfb15ed9eba83018feba92bfd7fa7395022033536713f82dbbbe4f4397c887c6f1bc329942dd724f8876bb0e1dcac651510401",
       |        "3045022100b67f155dd6b7675678273d0993561aee6dcf04be105c3b7d94d023681f866cae02200db31962e8a3f70cd4011b398a5f0e8b70698fc1bfe898160f6aef29ff9cd70401",
       |        "522102f7c530f300ca372e977a7da6bb7804f773236e6b21f1407342dbcf69cd5bf818210334b0bbb1045641991b843f1c9dc104649d5cf92fc28955cd7e052505d3d6e891210363c8246a99cee15395b9202f04cd54c6a155f669b7a92d574f5f4b34793bb98353ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "231bc169a10ba48288bf8ad093d8e50f9213912906974162aa033a2992ae9d35",
       |      "output_index": 1,
       |      "script": "220020ff6e6aff39c08aef6b6db7c3a0d89531f0372dbd1b55898b4c69991e703a7d32",
       |      "output_value": 345434,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3MKyBpAXXDT4k8kb5yvo365HoX8d1iVD19"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 512164,
       |      "witness": [
       |        "",
       |        "3045022100c6fa30f1409809e213bd3c3beaf70ec2d819cebbf71019ada5eba6658d71535302204ae8cc6e6812f600216c9c1543266291efa5e7071523c54885e5c3f50ddc5eb401",
       |        "3045022100c53370ab752945fa583e3d3a732b45171df3bf43e2b66264855dddbab47ee6fc0220201f0f7e4086cc7ebf6d7426f0e52310c0888fed7379b0b6f73bda80d901f44901",
       |        "522103d773e6f04350f0113e203d09a9f2fd76d79026fc857746ba6d9b5bdbb93c40ae2103e1c8fae2825f2ff5c163d18e37bc99a4eccca126c5bfcf9c817b28dccec8435021029c79d6bda31df85e01abb818b95ce999bdff33b9a662791f6b93c964d47570ef53ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "41472749d30825a4b2c9212da259efab74883260232f24a69d53214ee7cc9d35",
       |      "output_index": 7,
       |      "script": "2200200ed7b1e8b61da34f14296d154ec96aba8b60f6bd0e4451297f0c1421aa82d385",
       |      "output_value": 1045000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3LLxF4zFphSNgRNDVjg2zGp18xZyU6TTq6"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 496409,
       |      "witness": [
       |        "",
       |        "30450221009b89df9d548a2efbd9fd2d8a155530ebac328a9b5a91a6b1ba56e5e94effc1ec02205ecfc594cd6540b0b106ad1a6a18fda4c477a33fca9810ed812b2f9a417650c401",
       |        "30450221008c2f357e54dca94bb998791cd908a9dc50ec60c4f894402fbd409a40ae22156902203f3538497d0f7a8f938199f9ecd5b262dc40aded6dda085d979bd2f14343893601",
       |        "522102d165b7d8aea714813e0bdc59882329ea97508e4bd55464223201c3201361a4e5210282a903b1423d5f2b08319fba73296c3b83268029811059b3b9bc1e023d81133f2102d8335ea53727224e2b93dcd4a611fefe31b9819e976cb3e5b64312e64ee2d8d053ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "048c6335f6e935a0b11a3474201861c1bd13c9803915d67ed5ca5660f2069e35",
       |      "output_index": 0,
       |      "script": "22002066c66d2ccc9f97a3155156e239de96e97b6e2e65a3316eb8a8b39ef963fec653",
       |      "output_value": 400000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "362NDuuiX2PYGfQsVHcCcEzKwRoSBcfzBf"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 496679,
       |      "witness": [
       |        "",
       |        "3045022100be2e73953dfe95bb3610b3589517789dfcc2dd75d6c519337db7a24ced637c8b02204f57887c18ca99065c6e703d3685e3bb3e8a003cdce34a9017154b87297528b301",
       |        "3044022043cb744b5126bba60bb1af87198ae0b75383885c5d39f8abcaabbf1f7ea24bb30220676c8be9046ea1444d20cabc986609791a4b7a162bb9f1d0f8a2f72914139b5501",
       |        "52210292bda58aba8a2a2278cba3e32449404e9a5400615b2f31d1f61d765d3b3a13602102ab99a8b11c499f88ba91820b2f04a4add902ec8d8cd4861e5e7cf636411b04042103342752a5b41d62435151fe12e57ddd7c44c682fb9557232b4a5b416851ee088a53ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "29897032d08eb90f5dd8a7bd0d3dc1dea946ca9249d43d85086f6fc884359e35",
       |      "output_index": 6,
       |      "script": "220020a68ff6223e23dddfce1610d261987a2b6501541a14278a97490026d250688d40",
       |      "output_value": 822770,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3KGE6Uq82y7f39aV6eFmVZQfWnbEuxS6JQ"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 507821,
       |      "witness": [
       |        "",
       |        "304402205f60888804470667154dbf2b0d7a271a25180d9d0b64d0888bcde00fb13f7322022009a45c49efe9deb7a20df374e2efbd3d15a226536a068cc2fe506a94534da50201",
       |        "30450221008c569a9c24fb84f7b5badc14d08d0722e4b49c0920715f32a5e3e0f2e793fb2102201e3d71dba333ce5d29922067380122a63494692d247a8713a62f92d1391388c001",
       |        "522103cc6b0e2e993bef4ce15b89bb99228bf24826034410933980c987c1677afbd2a521034f2b156f9374a88fe222404f496512f85e7f0e06060ba67c490db639ddce87ba21025d97f15766aad0537399b8497b3ffac849690ffebbdcb8358acdb7b3cd164ce053ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "16c5ac49d784442e82175df47e8a7b8093284988ae15029a3384e20396c89e35",
       |      "output_index": 1,
       |      "script": "2200206fd4bc32779f6f80cf7a3f784c6520c5128997451a6e66b6ca8cdafb14f4241f",
       |      "output_value": 1040000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "32KUmJa2f3ZPD1bkBVafGDhPznimxrg6qJ"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 521110,
       |      "witness": [
       |        "",
       |        "3044022055fde5db3d28d0af3ac4a38a208835e124b3c31d709a9fc101d400b3e2410c3c022013edf2a5e5d83b1724aff1df1b8095fd971b1bc8b5f4d8f9be5c52b94033519701",
       |        "3045022100cdd91084a535eda0728340288d04a1647eb4a67b322b57e929f6eaf93c3cfd9702205d1bb3988eb788f756ff32875e936cf450a346757baf276c87bf4fd9b304d25301",
       |        "522102a48f604327613fcd7bdd669af7f8920aaec8ab7b86fa292e0c3c0e5c9a9d88dd2102fb6a5f80c50509cdf76f46ebcfdf7f65d020763d1e48111bfbc0ae058d06a77221026b9734a9352b8c61fc7f0b5003a01c9af1044e3938e4ab8b3c202d47c44900ae53ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "0d371172de01e921c742b7d4e5fd2e5532b4c571e1684a56c81e70eafb1f9f35",
       |      "output_index": 0,
       |      "script": "2200203159ac1137bbd61d84338f62c28de98d7f5ccc1fbd953508460aff821611d712",
       |      "output_value": 1815896,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3HT6XWFhbEGFzDLnFN9pB1S41AN1Ai7Kyd"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 539806,
       |      "witness": [
       |        "",
       |        "30440220392b209b627d1696edf32e208e61f7dc1f65e380c980a96e36a3ca66406907c902207c69d23c5a5c7bee4730b7d07b476baa6abaa064df45b7ba7c310f238084ca3401",
       |        "3044022077965184e77b4dd9db47ab6f590db7a539d5bb730105038eaf6fff48bb056ae4022000b25ac4e8e8739035dd9e7752f3137d8614135471ad390af00a14821d80279301",
       |        "52210394ea0cf813dbda0022bff8c2336c09dbd708105d3ec7e04996e5c412ab6a925e2102ff28ef77fb1ed9ea5ff5b47216b3280cb92cd86baf7af9ab89eda255e2af9162210261e8c38a6bb634dea579837d490645be8d0f94eaca53c1e95bc97889d091809053ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "6209571dd541701980699c12220a7dd4f4b6f1631ae7ed782f2ba833a6969f35",
       |      "output_index": 1,
       |      "script": "2200205a86a93649d45abd04f5ad81c44ab7bb33547a71a759f9880bf7c9da73f0ab7c",
       |      "output_value": 1171091,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3NmNvty9pkZx9JAUNtMsmVgXHhaPQndTmA"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 508137,
       |      "witness": [
       |        "",
       |        "304402206f298bc19fea72bf10d36cfc6ad755bb4d83f9dae8cf682a73d37493c37e490002202360c448cf951bfbf9afa0a83104c775c72e7db7634306295b1df496e2dee0f401",
       |        "304402204de2e6ef9fec2497d9627ce8675cab91db33cc0ceadb86fc434e51ca1199b325022056c6237026551554a789a0495ad500011435f4a4d4cacbdb608deb3b5209275301",
       |        "5221031020e1f3a18f6608bb7da6488db0d6398f6f186f99d30a5908a3f6d85fd1c5ce2102409a047ce2d4edd2e365cb8e458a1cc6f9026949dce9807a662268205eb7c63e21039daebfc0d941df8f875a1c66fd3bd3d8089232bed7b54058fafe77ab6b8e55f153ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "83ff3a4090bb1d411f0cfc104f65ce56135f8b678048e087f8458ab6a106a035",
       |      "output_index": 0,
       |      "script": "220020c66a9330e8559c73fd27de7fdf6083ec122b51d10a22bbc827c8d02c2e1f61c1",
       |      "output_value": 661044,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3QbooApUeh8Gs8vtVgVzoPNb6KaFFvHCrK"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 499654,
       |      "witness": [
       |        "",
       |        "3045022100b8921f41e49112fa07c7a1306f4b4e74bb67f185c8e17ff4b294fc6b144324d102204d74ef4eddce7219f34b13d74343862529398c30ce77186d3cd35ba9de2bf67401",
       |        "304402200e760a3d0f2a534c8193fa88860024d744a0a5e559befee64f078b3dad402be20220492ddf446525fa411ae38902371bd834d34bf663e4419222dda0bc278cdca4da01",
       |        "5221023a9e4f063d5c22ae06b63c698e4d81426097bb931aaabd4421fec25fe684a5e1210360d3c2f97dc7aa4e67b0bc4ea4a2c28340e44200dc059bba58b96f822e04770321023496644ea236559e73b2fc270dfa45b3f4ccbd403c4bdcd73f0fffba5235bfdc53ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "a15bef9d581b87ef0d7740940d6a4d433cd25afe1c388ed348acac1aba6da035",
       |      "output_index": 16,
       |      "script": "2200207a11068a7a7d84d5372879e2f8b6d311cdd35c2aeaeba1fcfdb0e2be6b28512e",
       |      "output_value": 900000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "33NoPF1CoTBWo3DTADNyVwwEhqkP999SDq"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 506298,
       |      "witness": [
       |        "",
       |        "30440220755846f4e58dbc6df3d48fa46b330c72e61c739e3c382ed054c14d6e09c5e13202200fb7485a3510d2351e7854e2396839ee0314cc7f250fdf457182a6789d9986cb01",
       |        "30450221009c5a786bd98ead121de649d8adcaa9d664780e24cd9a64a845240506dab75036022018de0d738cc77b4777613da2c9b311aebf5dab8acfdcfba7d53a2c03428e6e2f01",
       |        "522103880ce8753c7b32e33d6e80f091db549a3bb26060ee9d05c63aa805bb2b8fc587210209bf756b2a2dc15dd8459bee1b0b2012faba8e44569898273bbe21ebef12d50e210285943bc2e73e24b992fe4d7c739f2850d25d582834fbb06a92d3b98b4f42ae8f53ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "9161857844d74f69cfa2ee252f09d2dcb7bb8c252756060d5908867294eba035",
       |      "output_index": 2,
       |      "script": "22002089c4c6babacec0d3efa830a476fafd4facf52e3bf26228d228a1c2d444557d6f",
       |      "output_value": 770000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3JbKga2CHEguAxTLN3qj6ZQTEjDawTJVSx"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 501815,
       |      "witness": [
       |        "",
       |        "3045022100f2dee8c598dc33e5a47fcb02c1dfdf621fa6e649373ef3bf01cc9bd1b21aedc60220642bb36f35548cec53c0a8ac918db803899a6bd4d1ccd82814b3ed48d50a66a001",
       |        "30440220473c29d3276bb8cfd139d05cc64e41ca8c4886c1bcd339f76e4b83e9577413ed02202bcf0d97faaa2ff35fceb10e45b5f2fecbb561199e5cc6b104037b70db971fb301",
       |        "52210308504e37b89a74a428753ac4295c12d2d323d61a6c578abb187fe6b6df8f4ee821028e42da6a5fc1f179f4776a91ff69eafbd0b823835ac9fafe06f717bace47862b2102fbdfcd9de21fab53eabc1f78c4c5a94074dc7fff1e6c4d8936a4bbd218f651c253ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "9dced3f885e511ca3ca15801b647a675ab0a7a382cb5d5d7d04a0cbd5324a135",
       |      "output_index": 1,
       |      "script": "220020942ecbbcebb2af5104d61cd64ca0453bf45831b7af978a800aea39c81201a307",
       |      "output_value": 663322,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "36T1pBoHiLNJo3yjC3mkX9w28Jy3YNPsrb"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 502550,
       |      "witness": [
       |        "",
       |        "3045022100dc7952f9d6c45a8a30d88e6f4e8e5e9a91ebecc4b49f88f9b58a754fa77e8df002205046bb3ab39bfeec785991ba0f8bd22a068cd71ce58043acb85973d89260b9c301",
       |        "3045022100a9de0781192f51008ce864c5f02b35f26931af2197038649d6e290c78ecdc6d502202ceb74da25fbb93e925efb313ddbe61bae5d250b8a39c27cfe471e3488ffcd4101",
       |        "522103da0b0b1b45082b820a5c106cb7f206177c8c784ff21eb22095420e6293983fdf210311295cd786de5b1cd5704afc210e29a30e1d91ad3cd5f41fa839a898ab4d285e21031630ab68f1968987ea259c887f53eaa0609ebff85dc8d1c544996580a3fa558a53ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "857333f0f8638a1c46f38821548b0571875aa58fac94ace38c1fe7cc9e32a135",
       |      "output_index": 18,
       |      "script": "220020a723f590d38b0c449aa4f749ff2e6e87a8911a3855dc24891ff1cd9435a3e49a",
       |      "output_value": 149677,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3M9qzpmFJxwD9uAtfdvozuS36TY2jYqGnK"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 512726,
       |      "witness": [
       |        "",
       |        "3045022100e7fc98504249dde5b1481c41a9cc515c9ee6b8ebd6e3e007f00f23060176405802200aa830cbe19c2a6c0ef91172c70ffff370d024984942593258e668ff70bb78d601",
       |        "3045022100a7b800fa87f7eed85e4395a827f0cf3b8a8a91f3c231b026fa9d5add47f0f399022064d1d07d7a56dab096046f4fd0df0eb5803a8bf823d03e30e0ba01c9edecf99001",
       |        "5221020c42ea340446ee990c4c5f61a6345b0f2713249430b6776d295d7ede926fe25a2102f2e6c4e7137efb3b7309951855d3c69d12d0e6dbfef3fb1b62246e36a490f0b12103f23e6ecf426df5ae3ebf1843cdf9583464808587bb84cb77a2465e5021ba3a4453ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "fbe3affd29a935bf9daf18b28d8e529256d706e73bfb09096d73c200a348a135",
       |      "output_index": 37,
       |      "script": "220020d443029991a82c533640f9c7d8fd3637ca395505945033839fa5428d52027861",
       |      "output_value": 228065,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3Hqbt6RsdHP7cgbetSuVF1CCJyqVW1trNe"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 529434,
       |      "witness": [
       |        "",
       |        "3045022100ce234308a304094adfc4d3dc8cb3c254ff24e20869fd0dcc376ce45bf3f7bde402200ae277669fa82c1be42b9596a3aed53eea49e6fdcecba8358c9e024a02ca82ee01",
       |        "3045022100f87405ea28df48a1a6329fb0e2807fe96a9d06d177bff26ee7b6ee16a3e909ac02204538d24ab2612620042ab85f1b5359a58b8fefca51e5b349a5faa3ab1512f55401",
       |        "522102175f17db8a491b24862d76cfebcb4dbaa2a89dd96786a2664d00ad4f895dddae210228ef73d015da9cbeb095bef000d8a46b7d66acd19cde0db9275f7b5431dc659a21036b7403fd04dedfb90c96ee2a67b1f9e04e09712ff7e7444bee5d03fd3d0bf42153ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "9f27f5eb93aa028ded54ed1f2a215c6f51ab4f2dfa56b0858a3e2ebf2789a135",
       |      "output_index": 12,
       |      "script": "2200202d3ef559db9decbbbd2c6161a2b59d8106af15efa23d84c166ecf44961d78459",
       |      "output_value": 773787,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3Dpmmwx9j7fpZ4Uknsd4XJfDMRPHJYpouh"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 513345,
       |      "witness": [
       |        "",
       |        "3045022100d936ae87d24cb391924599df8e59a317b437ae4360ba0a965486bd3702b098f402202f92401a73b67c080c04963f44de753f9c10d735c6fa537b96a4cab76eacf9d701",
       |        "3045022100e1adcbb9d44f7db710e93a0f9a4f8c3be6fe8e1b71d3d6b3849eac2874de26fe022014e1273a54e424fbe4a7773d06bbcfb8efac69c59e26b09b790ad9badaae3a4b01",
       |        "522102031bc3d41726a59ee43582d9766e1817060829a351e3c1e37bf72cef831cd2e0210332fd050e5360fd10d4b203867c033ea2a67f51d9531d1d42d3a9c3f040989b84210399b4734a75f91646eb46e7a6be2c2f55387823a897aea000022413fb31d6448853ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "677907f6b8c78d8e245dfe3b6453d68bd19e0ac8fb1eb40cbbce94c7bbeca135",
       |      "output_index": 1,
       |      "script": "22002037ff0ca683128c146d24a4d2491f26398e081cd54a408bf37721d984e099595f",
       |      "output_value": 456000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "35oWxMRwCCgkGqpw1xwPPjNEUtTVvkq97W"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 501843,
       |      "witness": [
       |        "",
       |        "3045022100fddd8fcf2da6dec387e1d3c8ec2637ccaea76cb0fbf412e3248af70bc2647f5102201c340ace04f9a70e372f871f5bdbfe0938a4b80ab6fd0c4d6c571f9ff7dcb24701",
       |        "304402207c61cb91526a4109b6ae74cd530f9721f73fd70227c24aec065fd86b5acd0edd02206adac05249b94882d67f9a22df609b4dcf46e7fa336ae647c4554c6c549d42a101",
       |        "52210319cefb05a11e3d9edcba83b479a3c970bbc8246312811858e8c5be6f7de3d7262103b12176a073155c37ee92941d98799d1fe2672e28b106528ab5a54f2a312861ce2103863a9e9b408e76d1b8d6469687cd5033cbbf12c5285863c092864930909b8afc53ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "8750c05dce9131c3f069bdedaf1f49590451b12166d1d1a17cfddc2c4ef1a235",
       |      "output_index": 1,
       |      "script": "220020f2cbb2e15f3d285872a09523468b35b740800fdea941647a215c362bd310cbc4",
       |      "output_value": 660000,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3PAEhEciibyoxjehfYzMqRXoJFz2ZdPz4F"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 503254,
       |      "witness": [
       |        "",
       |        "30450221008341ec747e051b61a4f3f4167bcdb8032b2fa7b75188d6d4e5c8fac8e373479602202899b8e8c02d431c6d2424b172fcefd1c8c2234f4fa26952acfcfd6aad1c4dfd01",
       |        "30450221009e13cda03af236b52f1963e877b09b9198af337789a7f60bf5c63f3faaba2fba022030fd5f482f55da752fa0d7ad4a71f3773b1fa7d01c599733c18745ae4e4da29d01",
       |        "522103acb7db073bda045b6aef7c9edc8c598c61c5dfb7db1ce433551c357e144f11da2102e0d5b7c745c2d89c33cb2edd3dc334a959e3e9b27f231621cf9e843ada2b9a452102beeea0314a270e13f60b0e7d37a676259760bbbf820773e923762d8ca63aeca353ae"
       |      ]
       |    },
       |    {
       |      "prev_hash": "c78c8ef03a0c5286759837bc2ecbcd3b79bc87180b8a7ca2414ea68177f7a235",
       |      "output_index": 0,
       |      "script": "2200208cfa822d9066134cd121f52a899bfc81dea5bd4fd0e8199872f792bf6e7883a5",
       |      "output_value": 1082524,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "3GfJDRqYhtu9EQtr6jYUBojikvPARxMn3W"
       |      ],
       |      "script_type": "pay-to-script-hash",
       |      "age": 516040,
       |      "witness": [
       |        "",
       |        "30440220260ebbd92bcc7e7a69a61e1c947fe20c7a03675d9e07e8c845360378650cd9550220172dca4de6854b46b083468d7dbf23f6359b597052d2db57e9c43c183b8a6bc101",
       |        "3045022100f12c8ea125d9c8b078d0a1dcdf1f154772942b6a3ff00ac4878cd7d838608b5102203c4c1b2a8c5c466766eacbd38e603d5fc10fe28496d0ea080bd51877cc1a8c3c01",
       |        "52210228c8add4f9fa4cfa030a2d4d39963fcb555ac1c118fb4e39ca858e92dbdd97be21024ffca98b9ce7d4da98ca468b25c85af0bf2c3cf305e209b1971ad257406466a0210370be4be2f0bcc9b7bd1dd17071ba4359507cab80b65c42a6a50c75e99ce19beb53ae"
       |      ]
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 1190780706,
       |      "script": "a914b8dcec2311097fd5a3ada55b9c8b45f0b1456edf87",
       |      "addresses": [
       |        "3JYUuoH6jTnXFyvNSsKUgsnURHbF5MmJPA"
       |      ],
       |      "script_type": "pay-to-script-hash"
       |    }
       |  ],
       |  "next_inputs": "https://api.blockcypher.com/v1/btc/main/txs/bac02148041affd9dec93d30a86d1777d9570ab5d36fa2e0b7a20c0fa208e458?instart=20\u0026outstart=0\u0026limit=20"
       |}
    """.stripMargin

  val creditingRawTransaction =
    s"""
       |{
       |  "block_hash": "0000000000000000000301efd5c36dd57c118bfe99bfb4edd7739ae9a020347e",
       |  "block_height": 542076,
       |  "block_index": 973,
       |  "hash": "f6c7343b9ab72d644230ddf8eb203379b7dc81fbb0fc2fb249fe23984e4e9d35",
       |  "addresses": [
       |    "1CAkNFBsv7ajnQuEDMyrvSFvbGj9TjTGLB",
       |    "1M5SpyYdgea7NjvKiqphVD61HPsXt3L4Ws",
       |    "3QAo8hxAPbaMtak5Zny8kX35dL1amgnFDP"
       |  ],
       |  "total": 36787335,
       |  "fees": 972,
       |  "size": 224,
       |  "preference": "low",
       |  "confirmed": "2018-09-19T10:54:10Z",
       |  "received": "2018-09-19T10:54:10Z",
       |  "ver": 1,
       |  "double_spend": false,
       |  "vin_sz": 1,
       |  "vout_sz": 2,
       |  "confirmations": 3991,
       |  "confidence": 1,
       |  "inputs": [
       |    {
       |      "prev_hash": "d9a21fa12b945f235402f106ae05c9ed6bc2df91377b5f73ea9592aaaba26a17",
       |      "output_index": 0,
       |      "script": "48304502210081eb6d3a6b77c9f34d302722c7a9e160b11af9fe98b74685b3f45eee72d11e4c022021d2beff8be8b318026816168fba54c3a48295f11a65f334cd4757c4eeec70510121039d45b086991d8fe049893ef010b702df168bd004b512ff575d8f35b9d36cf864",
       |      "output_value": 36788307,
       |      "sequence": 4294967295,
       |      "addresses": [
       |        "1M5SpyYdgea7NjvKiqphVD61HPsXt3L4Ws"
       |      ],
       |      "script_type": "pay-to-pubkey-hash",
       |      "age": 542076
       |    }
       |  ],
       |  "outputs": [
       |    {
       |      "value": 35955878,
       |      "script": "76a9147a8153df9832d7c0ffd0286df3d9f9cb2136886c88ac",
       |      "spent_by": "f8e40f469cebbddefa05666355b4583fd71889c65403f57dfab6436b497f98c9",
       |      "addresses": [
       |        "1CAkNFBsv7ajnQuEDMyrvSFvbGj9TjTGLB"
       |      ],
       |      "script_type": "pay-to-pubkey-hash"
       |    },
       |    {
       |      "value": 831457,
       |      "script": "a914f693bcc4aa3ee8c4f6a347120341bb11b4029d1987",
       |      "spent_by": "bac02148041affd9dec93d30a86d1777d9570ab5d36fa2e0b7a20c0fa208e458",
       |      "addresses": [
       |        "3QAo8hxAPbaMtak5Zny8kX35dL1amgnFDP"
       |      ],
       |      "script_type": "pay-to-script-hash"
       |    }
       |  ]
       |}
     """.stripMargin
}