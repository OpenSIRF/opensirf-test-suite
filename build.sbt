lazy val commonSettings = Seq(
  organization := "org.opensirf",
  version := "1.0.0",
  name := "opensirf-test-suite"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "opensirf-test-suite",
    version := "1.0.0",
    crossTarget := new java.io.File("target"),
    artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
        artifact.name + "-" + version.value + "." + artifact.extension
	}
)

crossPaths := false
publishTo := Some(Resolver.url("Artifactory Realm", new URL("http://200.144.189.109:58082/artifactory"))(Resolver.ivyStylePatterns))
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
publishMavenStyle := false
isSnapshot := true

libraryDependencies += "org.glassfish.jersey.core" % "jersey-client" % "2.22.2"
libraryDependencies += "org.opensirf" % "opensirf-java-client" % "1.0.0" changing()
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test

publishArtifact in (Compile, packageDoc) := false
publishArtifact in (Compile, packageSrc) := false

resolvers += Resolver.url("SIRF Artifactory", url("http://200.144.189.109:58082/artifactory"))(Resolver.ivyStylePatterns)
