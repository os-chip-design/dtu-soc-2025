scalaVersion := "2.13.10"

scalacOptions ++= Seq(
  "-feature",
  "-language:reflectiveCalls",
)

Compile / unmanagedSourceDirectories += baseDirectory.value / "wildcat/src"
Compile / unmanagedSourceDirectories += baseDirectory.value / "wildcat/soc-comm/src"

// Chisel 3.5
val chiselVersion = "3.5.6"
addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.6"

libraryDependencies += "edu.berkeley.cs" % "ip-contributions" % "0.5.4"
libraryDependencies += "net.fornwall" % "jelf" % "0.9.0"
libraryDependencies += "com.fazecast" % "jSerialComm" % "[2.0.0,3.0.0)"