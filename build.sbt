enablePlugins(JavaAppPackaging)

name := "bitcoin4s"
organization := "me.hongchao"
version := "0.0.2"

scalaVersion in ThisBuild := "2.12.4"

scalacOptions := Seq(
  "-unchecked",
  "-feature",
  "-deprecation",
  "-Ywarn-dead-code",
  "-Ywarn-extra-implicit",
  "-Ywarn-inaccessible",
  "-Xfatal-warnings",
  "-Ywarn-extra-implicit",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-Ywarn-unused:imports"
)

libraryDependencies ++= Seq(
  "com.madgag.spongycastle" % "core" % "1.58.0.0",
  "org.scodec" %% "scodec-core" % "1.10.3",
  "io.github.yzernik" %% "bitcoin-scodec" % "0.2.9-hc-3-6",
  "com.iheart" %% "ficus" % "1.4.3",
  "org.typelevel" %% "cats-core" % "1.0.1",
  "com.github.mpilquist" %% "simulacrum" % "0.11.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

resolvers += Resolver.bintrayRepo("liuhongchao", "maven")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
