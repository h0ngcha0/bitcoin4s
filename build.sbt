import java.nio.file.Files

enablePlugins(JavaAppPackaging)

name := "bitcoin4s"
organization := "it.softfork"
version := "0.1.0"

scalaVersion in ThisBuild := "2.13.1"

scalacOptions := Seq(
  "-unchecked",
  "-feature",
  "-deprecation",
  "-Ywarn-dead-code",
  "-Ywarn-extra-implicit",
  "-Xfatal-warnings",
  "-Ywarn-extra-implicit",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-Ywarn-unused:imports",
  "-Ymacro-annotations"
)

val akkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "com.madgag.spongycastle" % "core" % "1.58.0.0",
  "org.scodec" %% "scodec-core" % "1.11.4",
  "com.iheart" %% "ficus" % "1.4.7",
  "org.typelevel" %% "cats-core" % "2.1.0-RC3",
  "org.typelevel" %% "simulacrum" % "1.0.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.typesafe.play" %% "play-functional" % "2.8.1",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.30.0",
  "org.julienrf" %% "play-json-derived-codecs" % "6.0.0",
  "com.lihaoyi" %% "pprint" % "0.5.6"
)

resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("minna-technologies", "maven"),
  Resolver.bintrayRepo("minna-technologies", "others-maven")
)

enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)

dockerfile in docker := {
  val appSource = stage.value
  val appTarget = "/app"
  val logsDir = appTarget + "/logs"

  new Dockerfile {
    from("openjdk:8-jre")
    expose(8888)
    workDir(appTarget)
    runRaw(s"mkdir -p $logsDir && chown daemon:daemon $logsDir")
    user("daemon")
    volume(logsDir)
    entryPoint(s"$appTarget/bin/${executableScriptName.value}")
    copy(appSource, appTarget)
  }
}

val baseImageName = "liuhongchao/bitcoin4s"
imageNames in docker := {
  val branchNameOption = sys.env.get("CI_COMMIT_REF_NAME").orElse(Option(git.gitCurrentBranch.value))
  Seq(
    branchNameOption.filter(_ == "master").map { _ =>
      ImageName(baseImageName + ":latest")
    },
    branchNameOption.map { branch =>
      ImageName(baseImageName + ":" + branch)
    },
    git.gitHeadCommit.value.map { commitId =>
      ImageName(baseImageName + ":" + commitId)
    }
  ).flatten
}

val buildFrontend = taskKey[Unit]("Build frontend")
buildFrontend := {
  val exitCode = scala.sys.process.Process(Seq("yarn", "run", "build"), file("client")).run().exitValue()
  if (exitCode != 0) {
    sys.error(s"Client build failed with exit code $exitCode")
  }
}

stage := {
  stage.dependsOn(buildFrontend).value
}

resourceGenerators in Compile += Def.task {
  val resourceBase = (resourceManaged in Compile).value / "client"
  val sourceBase = file("client") / "build"
  IO.delete(resourceBase)

  sourceBase.allPaths.get.filter(_.isFile).map { file =>
    val relative = file.relativeTo(sourceBase).get
    val resourceFile = resourceBase / relative.toString

    Files.createDirectories(resourceFile.toPath.getParent)
    Files.copy(file.toPath, resourceFile.toPath)

    resourceFile
  }
}.taskValue

Revolver.enableDebugging(port = 5050, suspend = false)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
