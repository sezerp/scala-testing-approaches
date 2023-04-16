import sbt._
import Libs.Versions._

object Libs {

  private lazy val coreDeps = Seq(
    "dev.zio"                 %% "zio"              % zioVersion,
    "dev.zio"                 %% "zio-interop-cats" % "23.0.0.0",
    "com.softwaremill.common" %% "tagging"          % "2.3.4"
  )

  private lazy val webDeps = Seq(
    "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server-zio"       % tapirVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server"           % tapirVersion,
    "org.http4s"                    %% "http4s-blaze-server"           % http4sVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics"      % tapirVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"       % tapirVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"              % tapirVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "slf4j-backend"                 % sttpVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"        % tapirVersion
  )

  private lazy val loggingDeps = Seq(
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.5",
    "ch.qos.logback"              % "logback-classic" % "1.4.5",
    "org.codehaus.janino"         % "janino"          % "3.1.9",
    "de.siegmar"                  % "logback-gelf"    % "4.0.2",
    "dev.zio"                    %% "zio-logging"     % "2.1.9"
  )

  private lazy val testDeps = Seq(
    "dev.zio"                       %% "zio-test"               % zioVersion   % Test,
    "dev.zio"                       %% "zio-test-sbt"           % zioVersion   % Test,
    "com.softwaremill.sttp.client3" %% "circe"                  % "3.8.11"     % Test,
    "org.scalatest"                 %% "scalatest"              % "3.2.15"     % Test,
    "com.softwaremill.quicklens"    %% "quicklens"              % "1.8.10"     % Test,
    "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server" % tapirVersion % Test,
    "io.zonky.test"                  % "embedded-postgres"      % "2.0.3"      % Test
  )

  private val configDeps = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.17.2"
  )

  private val monitoringDeps = Seq(
    "io.prometheus"                  % "simpleclient"              % prometheusVersion,
    "io.prometheus"                  % "simpleclient_hotspot"      % prometheusVersion,
    "com.softwaremill.sttp.client3" %% "prometheus-backend"        % sttpVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics"  % tapirVersion,
    "org.http4s"                    %% "http4s-prometheus-metrics" % http4sVersion
  )

  private val dbDependencies = Seq(
    "org.tpolecat" %% "doobie-core"     % doobieVersion,
    "org.tpolecat" %% "doobie-hikari"   % doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % doobieVersion,
    "org.flywaydb"  % "flyway-core"     % flywayVersion
  )

  private val securityDeps = Seq(
    "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0"
  )

  lazy val allDeps: Seq[ModuleID] = coreDeps ++
    webDeps ++
    loggingDeps ++
    testDeps ++
    configDeps ++
    monitoringDeps ++
    dbDependencies ++
    securityDeps

  object Versions {
    val zioVersion        = "2.0.9"
    val tapirVersion      = "1.2.9"
    val prometheusVersion = "0.16.0"
    val sttpVersion       = "3.8.11"
    val doobieVersion     = "1.0.0-M5"
    val flywayVersion     = "9.15.0"
    val http4sVersion     = "0.23.12"
  }
}
