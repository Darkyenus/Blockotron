name := "Blockotron"

version := "0.0-SNAPSHOT"

organization := "darkyenus.blockotron"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-g")

val gdxVersion = "1.9.2"

libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-backend-lwjgl3" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-platform" % gdxVersion classifier "natives-desktop",
  "com.esotericsoftware" % "kryo" % "3.0.3",
  "darkyenus" % "retinazer" % "0.2.2-SNAPSHOT"
)

autoScalaLibrary := false