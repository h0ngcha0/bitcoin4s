ThisBuild / resolvers ++= Seq(
  "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
  Resolver.jcenterRepo
)
