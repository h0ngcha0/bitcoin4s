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
  "com.madgag.spongycastle" % "core" % "1.52.0.0"
)
