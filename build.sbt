name := "Blockotron"

version := "0.0-SNAPSHOT"

organization := "darkyenus.blockotron"

javacOptions ++= Seq("-source","1.8","-target","1.8","-encoding","UTF-8")

val gdxVersion = "1.9.2"

libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-platform" % gdxVersion classifier "natives-desktop"
)

autoScalaLibrary := false