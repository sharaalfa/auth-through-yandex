import sbt._

/**
 * @author Artur Sharafutdinov on 12.11.2022
 */
object Dependencies {

  def zio(artifact: String): ModuleID = "dev.zio" %% artifact                 % Version.zio

  def zHttp(artifact: String): ModuleID = "io.d11" %% artifact                 % Version.zHttp

  def scalaPB(artifact: String): ModuleID = "com.thesamet.scalapb" %% artifact % Version.scalaPB

  def circe(artifact: String):   ModuleID =   "io.circe"   %% artifact         % Version.circe

  val backendDeps: Seq[sbt.ModuleID] = Seq(
    zio("zio"),
    "dev.zio" %% "zio-logging"                         % Version.zioLogging,
    "dev.zio" %% "zio-interop-cats"                    % Version.zioInteropCats,
    "dev.zio" %% "zio-json"                            % Version.zioJson,
    zHttp("zhttp"),
    "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-core" % Version.zioGrpc,
    scalaPB("scalapb-runtime-grpc")            % Version.scalaPB,

    "com.github.pureconfig" %% "pureconfig-core"       % Version.pureConfig,
    "io.grpc" % "grpc-netty"                           % Version.grpc,

    circe("circe-core"),

    "com.github.jwt-scala" %% "jwt-json4s-native"      % Version.jwtScala,


    zio("zio-test"),
    zio("zio-test-sbt")
  )

}
