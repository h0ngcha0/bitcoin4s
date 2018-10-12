package it.softfork.bitcoin4s

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

object Config {
  object Bitcoin4sConfig {
    def load(): Bitcoin4sConfig = {
      ConfigFactory.load().as[Bitcoin4sConfig]("bitcoin4s")
    }
  }

  case class Bitcoin4sConfig(
    `mnemonic-codes`: MnemonicCodesConfig
  )

  case class MnemonicCodesConfig(words: Seq[String])
}
