package it.softfork.bitcoin4s

import org.scalatest.{FlatSpecLike, Matchers, OptionValues}
import org.scalatest.concurrent.ScalaFutures

trait Spec extends FlatSpecLike with ScalaFutures with OptionValues with Matchers
