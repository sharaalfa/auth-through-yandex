logLevel := Level.Warn

val zioGrpcVersion = "0.6.0-rc6"

addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.1")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager"      % "1.8.1")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")

libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion




