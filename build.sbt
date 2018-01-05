resolvers in ThisBuild ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  "mvnrepostory" at "https://mvnrepository.com",
  Resolver.mavenLocal
)

name := "news"
version := "0.1-SNAPSHOT"
organization := "com.dag"
scalaVersion in ThisBuild := "2.11.7"

val flinkVersion = "1.4.0"

libraryDependencies ++= Seq(
  "com.rometools" % "rome" % "1.6.0" % "compile",
  "org.apache.httpcomponents" % "httpclient" % "4.5" % "compile",
  "org.slf4j" % "slf4j-log4j12" % "1.7.6" % "compile",
  //  "log4j" % "log4j" % "1.2.17" % "compile",
  "com.h2database" % "h2" % "1.4.196" % "compile"
)
//excludeDependencies += "log4j" % "log4j"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided"
)

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies
  )

mainClass in assembly := Some("com.dag.Job")

// make run command include the provided dependencies
run in Compile := Defaults.runTask(fullClasspath in Compile,
  mainClass in(Compile, run),
  runner in(Compile, run)
).evaluated

// exclude Scala library from assembly
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyMergeStrategy in assembly := {
  case x if x.endsWith(".class") => MergeStrategy.last
  case x if x.endsWith(".properties") => MergeStrategy.last
  case x if x.contains("/resources/") => MergeStrategy.last
  case x if x.startsWith("META-INF/mailcap") => MergeStrategy.last
  case x if x.startsWith("META-INF/mimetypes.default") => MergeStrategy.first
  case x if x.startsWith("META-INF/maven/org.slf4j/slf4j-api/pom.") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    if (oldStrategy == MergeStrategy.deduplicate)
      MergeStrategy.first
    else
      oldStrategy(x)
}

// this jar caused issues so I just exclude it completely
assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {
    _.data.getName contains "kkkkk"
  }
}

