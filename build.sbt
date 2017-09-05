enablePlugins(JavaAppPackaging)

name := "bitcoin4s"
organization := "me.hongchao"
version := "0.0.1"

scalaVersion := "2.12.3"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-Ywarn-dead-code"
)

libraryDependencies ++= Seq(
  "com.madgag.spongycastle" % "core" % "1.52.0.0",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scodec" %% "scodec-core" % "1.10.3",
  "io.github.yzernik" %% "bitcoin-scodec" % "0.2.9-hc",
  "com.iheart" %% "ficus" % "1.4.1",
  "org.typelevel" %% "cats-core" % "1.0.0-MF",
  "com.github.mpilquist" %% "simulacrum" % "0.10.0",
  "eu.timepit" %% "refined" % "0.8.2",
  "org.scalatest" %% "scalatest" % "3.0.3" % "test"
)

resolvers += Resolver.bintrayRepo("liuhongchao", "maven")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
