import NativePackagerHelper.directory

val http4sVersion = "0.23.12"
val circeVersion = "0.13.0"
val circeConfigVersion = "0.8.0"
val doobieVersion = "1.0.0-RC1"

lazy val root = (project in file("."))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(
    name := "eventserver",
    version:= "1.0",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions += "-target:jvm-1.8",
    version in Docker := "latest",
    packageName in Docker := "eventserver",
    dockerExposedPorts in Docker := Seq(8080),
    scalaVersion := "2.13.9",
    mainClass in Compile := Some("server.engine.Engine"),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-config" % circeConfigVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.scalatest" %% "scalatest" % "3.2.12" % "test"
    ),
    unmanagedClasspath in Runtime += baseDirectory.value / "conf",
    mappings in Universal ++= directory("conf"),
    scriptClasspath in bashScriptDefines ~= {cp => "/opt/docker/conf" +: cp},
  )
