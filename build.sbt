import java.nio.file.Files

enablePlugins(JavaAppPackaging)

name := "bitcoin4s"
organization := "it.softfork"
version := "0.1.0"

scalaVersion in ThisBuild := "2.12.7"

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

val akkaHttpVersion = "10.1.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "com.madgag.spongycastle" % "core" % "1.58.0.0",
  "org.scodec" %% "scodec-core" % "1.10.3",
  "com.iheart" %% "ficus" % "1.4.3",
  "org.typelevel" %% "cats-core" % "1.4.0",
  "com.github.mpilquist" %% "simulacrum" % "0.13.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.play" %% "play-json" % "2.6.10",
  "com.typesafe.play" %% "play-functional" % "2.6.10",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.22.0",
  "org.julienrf" %% "play-json-derived-codecs" % "4.0.1",
  "tech.minna" %% "play-json-macros" % "1.0.1",
  "tech.minna" %% "utilities" % "1.2.0"
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

val scalafmtAll = taskKey[Unit]("Format all Scala and sbt files")
scalafmtAll := {
  (Compile / scalafmt).value
  (Test / scalafmt).value
  (Compile / scalafmtSbt).value
}

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
