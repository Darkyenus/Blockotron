name := "Blockotron"

version := "0.0-SNAPSHOT"

organization := "darkyenus.blockotron"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-g")

val gdxVersion = "1.9.2"

lazy val blockotron = project in file(".") dependsOn retinazer

lazy val retinazer = project in file("retinazer") / "retinazer" settings (
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-g"),
  libraryDependencies += "com.badlogicgames.gdx" % "gdx" % gdxVersion
  )

libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-backend-lwjgl3" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-platform" % gdxVersion classifier "natives-desktop"
)

autoScalaLibrary := false