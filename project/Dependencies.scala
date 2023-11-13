import sbt.*

/**
 * @author Artur Sharafutdinov on 12.11.2022
 */
object Dependencies {

  def zio(artifact: String)(implicit version: String = Version.zio): ModuleID =
    "dev.zio" %% artifact                 % version

  def zioHttp(artifact: String): ModuleID =
    "dev.zio" %% artifact                  % Version.zioHttp

  def cir(artifact: String): ModuleID =
    "is.cir" %% s"ciris$artifact"          % Version.cir

  def circe(artifact: String):   ModuleID =
    "io.circe"   %% artifact               % Version.circe

  def skunk(artifact: String): ModuleID =
    "org.tpolecat" %% artifact             % Version.skunk

  def timePit(artifact: String): ModuleID =
    "eu.timepit"    %% artifact             % Version.refined

  def catsEffect(artifact: String)(implicit prefix: String = "",
                                   name: String = "cats-effect",
                                   version: String = Version.catsEffect): ModuleID =
    "org.typelevel" %% s"$prefix$name$artifact" % version
  val backendDeps: Seq[sbt.ModuleID] = Seq(
    zio("zio"),
    zio("zio-json")(Version.zioJson),
    zio("zio-json-interop-refined")(Version.zioJson),
    zio("zio-logging")(Version.zioLogging),
    zio("zio-interop-cats")(Version.zioInteropCats),
    zio("zio-http")(Version.zioHttp),
    zio("zio-redis")(Version.redis),
    zio("zio-schema-protobuf")(Version.schema),
    timePit("refined"),
    timePit("refined-cats"),
    timePit("refined-jsonpath"),
    timePit("refined-pureconfig"),
    timePit("refined-scalacheck"),
    cir(""),
    cir("-refined"),
    skunk("skunk-core"),
    skunk("skunk-circe"),
    "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-core" % Version.zioGrpc,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc"   % Version.scalaPB,

    "com.github.pureconfig" %% "pureconfig-core"       % Version.pureConfig,
    "io.grpc" % "grpc-netty"                           % Version.grpc,

    circe("circe-core"),

    "com.github.jwt-scala" %% "jwt-json4s-native"      % Version.jwtScala,

    catsEffect(""),
    catsEffect("-kernel"),
    catsEffect("-std"),
    catsEffect("")("", "log4cats-noop", Version.catsLog4),
    catsEffect("")("", "log4cats-slf4j", Version.catsLog4),

    zio("zio-test"),
    zio("zio-test-sbt")
  )

}
