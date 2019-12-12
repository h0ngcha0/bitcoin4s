package it.softfork.bitcoin4s

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import com.typesafe.scalalogging.StrictLogging
import it.softfork.bitcoin4s.external.HttpSender
import it.softfork.bitcoin4s.external.blockcypher.{Api => BlockcypherApi, CachedApi, Service}
import it.softfork.bitcoin4s.external.bitcoind.{Api => BitcoindApi}

trait Boot extends StrictLogging {
  implicit val system: ActorSystem = ActorSystem("bitcoin4s")

  implicit val materializer: Materializer = ActorMaterializer(ActorMaterializerSettings(system))

  import system.dispatcher

  sys.addShutdownHook {
    logger.info("Bitcoin4s shutting down.")
  }

  val httpSender = new HttpSender()
  val blockcypherApi = new BlockcypherApi(httpSender)
  val bitcoindApi = new BitcoindApi(httpSender)
  val cachedBlockcypherApi = new CachedApi(blockcypherApi)
  val blockcypherService = new Service(cachedBlockcypherApi)

  val routes = new Routes(blockcypherService)
}
