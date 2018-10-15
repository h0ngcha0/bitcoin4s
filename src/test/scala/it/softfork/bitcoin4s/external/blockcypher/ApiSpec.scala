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

    val interpreterOutcome = Interpreter.create(verbose = false).run(initialState)

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