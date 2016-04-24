name := "Blockotron"

version := "0.0-SNAPSHOT"

organization := "darkyenus.blockotron"

javacOptions ++= Seq("-source","1.8","-target","1.8","-encoding","UTF-8","-g")

val gdxVersion = "1.9.2"

resolvers += "bintray-antag99-maven" at "http://dl.bintray.com/antag99/maven"

libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-backend-lwjgl3" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-platform" % gdxVersion classifier "natives-desktop",
  "com.github.antag99.retinazer" % "retinazer" % "0.2.1"
)

autoScalaLibrary := false