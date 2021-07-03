import java.nio.file.Files

enablePlugins(JavaAppPackaging)

name := "bitcoin4s"
organization := "it.softfork"
version := "0.1.0"

ThisBuild / scalaVersion := "2.13.6"

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

val akkaHttpVersion = "10.2.4"
val akkaVersion = "2.6.15"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "com.madgag.spongycastle" % "core" % "1.58.0.0",
  "org.scodec" %% "scodec-core" % "1.11.8",
  "com.iheart" %% "ficus" % "1.5.0",
  "org.typelevel" %% "cats-core" % "2.6.1",
  "org.typelevel" %% "simulacrum" % "1.0.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.typesafe.play" %% "play-functional" % "2.9.2",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.36.0",
  "org.julienrf" %% "play-json-derived-codecs" % "7.0.0",
  "com.lihaoyi" %% "pprint" % "0.6.6"
)

resolvers ++= Seq(
  Resolver.jcenterRepo
)

enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)

docker / dockerfile := {
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
docker / imageNames := {
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

Compile / resourceGenerators += Def.task {
  val resourceBase = (Compile / resourceManaged).value / "client"
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
