package it.softfork.bitcoin4s

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

trait Spec extends AnyFlatSpecLike with ScalaFutures with OptionValues with Matchers
