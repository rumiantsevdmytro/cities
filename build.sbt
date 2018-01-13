val globalSettings = Seq[SettingsDefinition](
  version := "0.1",
  scalaVersion := "2.12.4"
)

val model = Project("model", file("model"))
  .settings(globalSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % "0.8.0"),
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
    )
  )

val repositories = Project("repositories", file("repositories"))
  .dependsOn(model)
  .settings(globalSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "org.slf4j" % "slf4j-nop" % "1.6.4",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      "org.postgresql" % "postgresql" % "42.1.4"
    )
  )

val api = Project("api", file("api"))
  .dependsOn(repositories)
  .settings(globalSettings: _*)
  .settings(
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.11",
      "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % Test,
      "de.heikoseeberger" %% "akka-http-circe" % "1.18.0",
      "com.github.tototoshi" %% "scala-csv" % "1.3.4")
  )


val bot = Project("bot", file("bot"))
  .dependsOn(repositories)
  .settings(globalSettings: _*)
  .settings(
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    libraryDependencies ++= Seq(
      "info.mukel" %% "telegrambot4s" % "3.0.14",
      "com.typesafe.akka" %% "akka-actor" % "2.4.19",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.19" % Test,
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "com.typesafe.play" %% "play-json" % "2.6.8"
    )
  )




