package it.softfork.bitcoin4s.transaction

import it.softfork.bitcoin4s.Spec

import scala.io.Source

class TxRawSpec extends Spec {
  "Tx raw encoder" should "be able to encode a transaction into its structured raw format" in {
    val nonWitnessTxHex =
      """
        |01000000010b37507539abc8eb903d39be9803efe09db5b02fb0cfe012
        |ab6d99ceda98f3f2000000006a47304402205c5876144bf491eb6aece2
        |625cbc3049819f35094e8feaf808399de0c29b593d022048267261596d
        |cdb8a49659f0a9c74f2a423d6c7bef02058b56a8b90fb39e8ff9012102
        |b621afa86afdb74d874e876413cf199833f4a5f68e10335134876eebe2
        |9bbe6dffffffff0207fa0000000000001976a91426fcf3b9cc3e0d2fc5
        |1fc69e58b63b41e2094f4488ac404b4c00000000001976a914547a369b
        |70f0241ebd1e8288397dd34f2c11ac6b88ac00000000
        |""".stripMargin.split('\n').mkString

    val witnessTxHex =
      """
        |010000000001017a3d119055e0e854c5f350720b5df4410f347775f413
        |050a1d5f08e67b403de1010000002322002087a59be084440ce7b1ccc9
        |65cb53cee54fdc059855107f5c986f80c7a60db3dfffffffff024f934e
        |00000000001976a9146f3f0b93b060ea9c0d76989c9747c9b6cfad617d
        |88ac2720853a0000000017a9147a1b6b1dbd9840fcf590e13a8a6e2ce6
        |d55ecb89870400483045022100ba14118c99631e0b07bfc811a89789c7
        |3df114cfa6305950a519f8efbfbd84de0220640288fd1bf917d8b6a08e
        |2e94eb222859c4225f60a6825af5704549f9218aa901483045022100dc
        |bf285834e8d6ebec4b981fa77aaf88af7d8172a03c62d645c653caf0df
        |6212022014f66da2f03a6c5a228c2f256988aa81ef1006a53cff6d6d18
        |30c4a0bd51d02d0147522102617401497ecbfa70e0bff49ff534667ab7
        |0886ba4720e74fac5110b1af668e7421027a900cf1bc28805f5747fd9d
        |4b9fd3ff8ff10c0ba15b8caba8dd750f1e22db0452ae00000000
        |""".stripMargin.split('\n').mkString

    // This hex is too big that it exceeds the compiler limits.
    val nonWitnessHugeHex = Source
      .fromURI(getClass.getResource(s"/transaction/d519b6cb8d696585f2107b6d3cc464fad995f91c1619b66bc5254de7e6c43398.hex").toURI)
      .mkString

    Seq(nonWitnessTxHex, witnessTxHex, nonWitnessHugeHex).foreach { hex =>
      TxRaw(Tx.fromHex(hex)).toOption.value.hex shouldEqual hex
    }
  }
}
