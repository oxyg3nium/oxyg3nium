import sbt._
import sbt.Keys._
import APIMapping._
import Dependencies._

Global / cancelable := true // Allow cancellation of forked task without killing SBT

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

def baseProject(id: String): Project = {
  Project(id, file(id))
    .settings(commonSettings: _*)
    .settings(name := s"oxyg3nium-$id")
}

val scalastyleCfgFile     = "project/scalastyle-config.xml"
val scalastyleTestCfgFile = "project/scalastyle-test-config.xml"

val genSerde = Command.command("genSerde") { state =>
  Boilerplate.genSerde()
  state
}

lazy val root: Project = Project("oxyg3nium-scala-blockflow", file("."))
  .settings(commonSettings: _*)
  .settings(
    name              := "oxyg3nium",
    scalastyle        := {},
    Test / scalastyle := {},
    publish / skip    := true,
    commands += genSerde
  )
  .aggregate(
    macros,
    util,
    serde,
    io,
    crypto,
    api,
    rpc,
    app,
    benchmark,
    flow,
    json,
    conf,
    protocol,
    ralph,
    http,
    wallet,
    ralphc,
    tools
  )

def mainProject(id: String): Project =
  project(id).enablePlugins(JavaAppPackaging).dependsOn(flow)

def project(path: String): Project = {
  baseProject(path)
    .settings(
      Compile / scalastyleConfig := root.base / scalastyleCfgFile,
      Test / scalastyleConfig    := root.base / scalastyleTestCfgFile,
      apiMappings +=
        APIMapping.scalaDocs(
          classPath = (Compile / fullClasspath).value,
          scalaVersion = scalaVersion.value
        )
    )
}

lazy val macros = project("macros")
  .settings(
    libraryDependencies += `scala-reflect`(scalaVersion.value),
    Compile / compile / wartremoverErrors := Seq()
  )

lazy val util = project("util")
  .dependsOn(macros)
  .settings(
    libraryDependencies ++= Seq(
      akka,
      `akka-slf4j`,
      bcprov,
      `scala-logging`,
      `scala-reflect`(scalaVersion.value)
    )
  )

lazy val serde = project("serde")
  .settings(coverageExcludedFiles := ".*ProductSerde.scala;.*ProductSerializer.scala")
  .dependsOn(util % "test->test;compile->compile")

lazy val crypto = project("crypto")
  .dependsOn(util % "test->test;compile->compile", serde, json % "test->test")

lazy val io = project("io")
  .dependsOn(util % "test->test;compile->compile", serde, crypto)
  .settings(
    libraryDependencies ++= Seq(rocksdb, `scala-logging`)
  )

lazy val rpc = project("rpc")
  .settings(
    libraryDependencies ++= Seq(
      `scala-logging`,
      `akka-test`
    ),
    publish / skip := true
  )
  .dependsOn(json, util % "test->test;compile->compile")

lazy val api = project("api")
  .dependsOn(
    json,
    protocol % "test->test;compile->compile",
    ralph    % "test->test;compile->compile",
    crypto,
    serde,
    util % "test->test;compile->compile"
  )
  .settings(
    libraryDependencies ++= Seq(
      `scala-logging`,
      `tapir-core`,
      `tapir-server`,
      `tapir-openapi`,
      `tapir-openapi-model`
    )
  )

lazy val app = mainProject("app")
  .dependsOn(
    json,
    api,
    rpc,
    http % "compile->compile;test->test",
    util % "test->test",
    flow,
    flow % "test->test",
    wallet
  )
  .enablePlugins(sbtdocker.DockerPlugin, BuildInfoPlugin)
  .settings(
    assembly / mainClass       := Some("org.oxyg3nium.app.Boot"),
    assembly / assemblyJarName := s"oxyg3nium-${version.value}.jar",
    assembly / test            := {},
    assemblyMergeStrategy := {
      case "logback.xml" => MergeStrategy.first
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", xs @ _*) =>
        MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties", xs @ _*) =>
        MergeStrategy.first
      case PathList("module-info.class") =>
        MergeStrategy.discard
      case x if x.endsWith("module-info.class") =>
        MergeStrategy.discard
      case other =>
        assemblyMergeStrategy.value(other)
    },
    libraryDependencies ++= Seq(
      vertx,
      `tapir-core`,
      `tapir-vertx`,
      `tapir-openapi`,
      `tapir-swagger-ui`
    ),
    publish / skip := true,
    docker / dockerfile := {
      val artifact: File     = assembly.value
      val artifactTargetPath = "/oxyg3nium.jar"

      val userConf: File = file("docker/user.conf")

      val oxyg3niumHome = "/oxyg3nium-home"

      new Dockerfile {
        from("eclipse-temurin:17-jre")

        // Uncomment the next line and comment the previous one if you want to use GraalVM instead of OpenJDK
        // from("ghcr.io/graalvm/graalvm-ce:java11-21.0.0.2")

        runRaw(
          Seq(
            s"mkdir -p $oxyg3niumHome && usermod -d $oxyg3niumHome nobody && chown nobody $oxyg3niumHome",
            s"mkdir -p $oxyg3niumHome/.oxyg3nium && chown nobody $oxyg3niumHome/.oxyg3nium",
            s"mkdir -p $oxyg3niumHome/.oxyg3nium/mainnet && chown nobody $oxyg3niumHome/.oxyg3nium/mainnet",
            s"mkdir -p $oxyg3niumHome/.oxyg3nium-wallets && chown nobody $oxyg3niumHome/.oxyg3nium-wallets"
          ).mkString(" && ")
        )
        workDir(oxyg3niumHome)

        copy(userConf, file(s"$oxyg3niumHome/.oxyg3nium/user.conf"))

        copy(artifact, artifactTargetPath)

        run("chmod", "644", artifactTargetPath)

        expose(12973) // http
        expose(11973) // ws
        expose(10973) // miner
        expose(9973)  // p2p

        volume(s"$oxyg3niumHome/.oxyg3nium")
        volume(s"$oxyg3niumHome/.oxyg3nium-wallets")

        user("nobody")

        env("JAVA_NET_OPTS", "-Djava.net.preferIPv4Stack=true")
        env(
          "JAVA_MEM_OPTS",
          "-Xms512M -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseNUMA"
        )
        env(
          "JAVA_GC_OPTS",
          "-XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static -XX:+UnlockExperimentalVMOptions -XX:ShenandoahGuaranteedGCInterval=16000 -XX:ShenandoahUncommitDelay=60000 -XX:MaxGCPauseMillis=1 -XX:+ParallelRefProcEnabled -XX:-UseGCOverheadLimit -XX:+DisableExplicitGC"
        )

        env("JAVA_EXTRA_OPTS", "")

        entryPointRaw(
          s"java $${JAVA_NET_OPTS} $${JAVA_MEM_OPTS} $${JAVA_GC_OPTS} $${JAVA_EXTRA_OPTS} -jar ${artifactTargetPath}"
        )
      }
    },
    docker / imageNames := {
      dockerImageNames(
        baseImageName = "oxyg3nium/dev-oxyg3nium",
        versionTag = version.value.replace('+', '_')
      )
    },
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      scalaVersion,
      sbtVersion,
      BuildInfoKey("commitId"       -> git.gitHeadCommit.value.getOrElse("missing-git-commit")),
      BuildInfoKey("branch"         -> git.gitCurrentBranch.value),
      BuildInfoKey("releaseVersion" -> version.value)
    ),
    buildInfoPackage          := "org.oxyg3nium.app",
    buildInfoUsePackageAsPath := true
  )

lazy val json = project("json")
  .dependsOn(util % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      upickle
    )
  )

lazy val conf = project("conf")
  .dependsOn(protocol, util)
  .settings(
    libraryDependencies ++= Seq(
      ficus
    )
  )

lazy val http = project("http")
  .dependsOn(api, json, util % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      `tapir-vertx`,
      `tapir-client`,
      `tapir-files`,
      `sttp-backend`,
      `tapir-prometheus-metrics`
    )
  )

lazy val tools = mainProject("tools")
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    assembly / assemblyJarName := s"oxyg3nium-tools-${version.value}.jar",
    assembly / test            := {},
    assemblyMergeStrategy := {
      case "logback.xml" => MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties", xs @ _*) =>
        MergeStrategy.first
      case PathList("module-info.class") =>
        MergeStrategy.discard
      case x if x.endsWith("module-info.class") =>
        MergeStrategy.discard
      case other =>
        assemblyMergeStrategy.value(other)
    },
    docker / dockerfile := {
      val artifact: File     = assembly.value
      val artifactTargetPath = "/oxyg3nium-tools.jar"

      val oxyg3niumHome = "/oxyg3nium-home"

      new Dockerfile {
        from("openjdk:17-jdk")

        runRaw(
          Seq(
            s"mkdir -p $oxyg3niumHome && usermod -d $oxyg3niumHome nobody && chown nobody $oxyg3niumHome",
            s"mkdir -p $oxyg3niumHome/.oxyg3nium && chown nobody $oxyg3niumHome/.oxyg3nium",
            s"mkdir -p $oxyg3niumHome/.oxyg3nium/mainnet && chown nobody $oxyg3niumHome/.oxyg3nium/mainnet"
          ).mkString(" && ")
        )

        workDir(oxyg3niumHome)

        copy(artifact, artifactTargetPath)

        volume(s"$oxyg3niumHome/.oxyg3nium")

        user("nobody")

        entryPoint("java", "-cp", artifactTargetPath)
      }
    },
    docker / imageNames := {
      dockerImageNames(
        baseImageName = "oxyg3nium/dev-oxyg3nium-tools",
        versionTag = version.value.replace('+', '_')
      )
    }
  )
  .dependsOn(app)

lazy val benchmark = project("benchmark")
  .enablePlugins(JmhPlugin)
  .dependsOn(flow)
  .settings(
    publish / skip := true,
    scalacOptions += "-Xdisable-assertions",
    Compile / compile / wartremoverErrors := Seq.empty
  )

lazy val flow = project("flow")
  .dependsOn(conf, crypto, io, serde, util % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      akka,
      `akka-slf4j`,
      logback,
      `scala-logging`,
      weupnp,
      `prometheus-simple-client`,
      `prometheus-simple-client-common`,
      `prometheus-simple-client-hotspot`
    ),
    publish / skip := true
  )
  .dependsOn(protocol % "test->test;compile->compile", ralph % "test->test;compile->compile")

lazy val protocol = project("protocol")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys             := Seq[BuildInfoKey](version),
    buildInfoPackage          := "org.oxyg3nium.protocol",
    buildInfoUsePackageAsPath := true
  )
  .dependsOn(
    crypto % "compile->compile;test->test",
    io     % "compile->compile;test->test",
    serde,
    util % "test->test"
  )
  .settings(
    libraryDependencies ++= Seq(
      `prometheus-simple-client`,
      fastparse
    )
  )

lazy val ralph = project("ralph")
  .dependsOn(protocol % "test->test;compile->compile")
  .settings(
    apiMappings +=
      APIMapping.fastParseDocs(
        classPath = (Compile / fullClasspath).value,
        scalaVersion = scalaVersion.value
      )
  )

lazy val wallet = project("wallet")
  .dependsOn(
    conf,
    json,
    api,
    crypto,
    http     % "compile->compile;test->test",
    util     % "test->test",
    protocol % "compile->compile;test->test"
  )
  .settings(
    libraryDependencies ++= Seq(
      vertx,
      `scala-logging`,
      `tapir-core`,
      `tapir-openapi`,
      `tapir-swagger-ui`,
      `scala-logging`,
      logback
    ),
    publish / skip             := true,
    assembly / mainClass       := Some("org.oxyg3nium.wallet.Main"),
    assembly / assemblyJarName := s"oxyg3nium-wallet-${version.value}.jar",
    assembly / test            := {},
    assemblyMergeStrategy := {
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", xs @ _*) =>
        MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties", xs @ _*) =>
        MergeStrategy.first
      case PathList("module-info.class") =>
        MergeStrategy.discard
      case x if x.endsWith("module-info.class") =>
        MergeStrategy.discard
      case other =>
        assemblyMergeStrategy.value(other)
    }
  )

lazy val ralphc = project("ralphc")
  .dependsOn(
    json,
    api,
    crypto,
    ralph,
    util     % "test->test",
    protocol % "compile->compile;test->test"
  )
  .settings(
    libraryDependencies ++= Seq(
      upickle,
      scopt
    ),
    publish / skip             := false,
    assembly / mainClass       := Some("org.oxyg3nium.ralphc.Main"),
    assembly / assemblyJarName := s"oxyg3nium-ralphc-${version.value}.jar",
    assembly / test            := {},
    assemblyMergeStrategy := {
      case PathList("module-info.class") =>
        MergeStrategy.discard
      case x if x.endsWith("module-info.class") =>
        MergeStrategy.discard
      case other =>
        assemblyMergeStrategy.value(other)
    }
  )

lazy val integration = project("integration")
  .dependsOn(app % "test->test")
  .settings(
    publish / skip                   := true,
    Test / envVars += "ALEPHIUM_ENV" -> "it"
  )

val publishSettings = Seq(
  organization := "org.oxyg3nium",
  homepage     := Some(url("https://github.com/oxyg3nium/oxyg3nium")),
  licenses     := Seq("LGPL 3.0" -> new URL("https://www.gnu.org/licenses/lgpl-3.0.en.html")),
  developers := List(
    Developer(
      id = "oxyg3nium core dev",
      name = "oxyg3nium core dev",
      email = "dev@oxyg3nium.org",
      url = url("https://oxyg3nium.org/")
    )
  )
)

val commonSettings = publishSettings ++ Seq(
  scalaVersion             := "2.13.14",
  Test / parallelExecution := false,
  scalacOptions ++= Seq(
    "-Xsource:3",
    "-opt:l:method",
//    "-Xdisable-assertions", // TODO: use this properly
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-unchecked",
    "-Xlint:adapted-args",
    "-Xlint:constant",
    "-Xlint:delayedinit-select",
    "-Xlint:doc-detached",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:missing-interpolator",
    "-Xlint:nullary-unit",
    "-Xlint:option-implicit",
    "-Xlint:package-object-classes",
    "-Xlint:poly-implicit-overload",
    "-Xlint:private-shadow",
    "-Xlint:stars-align",
    "-Xlint:type-parameter-shadow",
    "-Xlint:nonlocal-return",
    "-Xfatal-warnings",
    "-Ywarn-dead-code",
    "-Ywarn-extra-implicit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates",
    "-Ywarn-value-discard",
    "-Ymacro-annotations",
    "-Wconf:msg=access modifiers for `.*` method are copied from the case class constructor under Scala 3:silent"
  ),
  Compile / console / scalacOptions --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  Compile / compile / wartremoverErrors := Warts.allBut(wartsCompileExcludes: _*),
  Test / test / wartremoverErrors       := Warts.allBut(wartsTestExcludes: _*),
  fork                                  := true,
  javaOptions += "-Xss2m",
  Test / scalacOptions ++= Seq("-Xcheckinit"),
  Test / envVars += "ALEPHIUM_ENV" -> "test",
  Test / testOptions += Tests.Argument("-oD"),
  libraryDependencies ++= Seq(
    `akka-test`,
    scalacheck,
    scalatest,
    scalatestplus
  ),
  Compile / doc / sources ~= (_ filter (_.getName endsWith ".scala"))
)

val wartsCompileExcludes = Seq(
  Wart.MutableDataStructures,
  Wart.Var,
  Wart.Overloading,
  Wart.ImplicitParameter,
  Wart.NonUnitStatements,
  Wart.Nothing,
  Wart.Null,   // Partially covered by scalastyle, only use _ inside actors
  Wart.Return, // Covered by scalastyle
  Wart.Any,
  Wart.Throw,
  Wart.Equals,
  Wart.StringPlusAny,
  Wart.While,
  Wart.SizeIs,
  Wart.ScalaApp,
  Wart.SeqApply,
  Wart.TripleQuestionMark
)

val wartsTestExcludes = wartsCompileExcludes ++ Seq(
  Wart.PublicInference,
  Wart.IterableOps,
  Wart.OptionPartial
)

def dockerImageNames(baseImageName: String, versionTag: String): Seq[ImageName] = {
  Seq(
    ImageName(baseImageName + ":latest"),
    ImageName(baseImageName + ":" + versionTag),
    ImageName(baseImageName + ":" + versionTag + "-jdk17")
  )
}

addCommandAlias(
  "format",
  "scalafmtSbt;scalafmt;test:scalafmt;scalastyle;test:scalastyle;it:scalafmt;it:scalastyle;doc"
)

addCommandAlias(
  "unitTest",
  "scalafmtSbtCheck;scalafmtCheck;test:scalafmtCheck;scalastyle;test:scalastyle;coverage;test;coverageReport;doc"
)

addCommandAlias(
  "integrationTest",
  "integration/scalafmtCheck;integration/scalastyle;integration/test"
)
