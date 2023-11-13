import Dependencies._
ThisBuild / scalaVersion     := "3.3.1"
ThisBuild / version          := "1.0"
ThisBuild / organization     := "example"
ThisBuild / organizationName := "Example Co"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "auth-keycloak-yandex",
    idePackagePrefix.withRank(KeyRanks.Invisible) := Some("io.example.account"),
    libraryDependencies ++= backendDeps,
    dockerBaseImage := "openjdk:jre",
    Compile / mainClass := Some("io.example.account.Server"),
    Compile / scalacOptions += "-Xlint",
    Compile / console / scalacOptions --= Seq("-Ywarn-unused", "Ywarn-unused-import", "-language:strictEquality", "-Yretain-trees"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value,
  scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value // "yandex"
)

assembly / assemblyMergeStrategy := {
  case PathList(ps@_*) if ps.last endsWith ".properties" => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
