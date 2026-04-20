ThisBuild / version      := "1.0.0"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / organization := "com.earthquake"

lazy val root = (project in file("."))
  .settings(
    name := "earthquake-cooccurrence",

    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
      "org.apache.spark" %% "spark-sql"  % "3.5.0" % "provided"
    ),

    // Produce un fat JAR con sbt assembly
    assembly / mainClass          := Some("EarthquakeApp"),
    assembly / assemblyJarName    := "earthquake-assembly.jar",

    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF")        => MergeStrategy.discard
      case PathList("META-INF", xs @ _*)               => MergeStrategy.discard
      case PathList("reference.conf")                  => MergeStrategy.concat
      case _                                           => MergeStrategy.first
    }
  )