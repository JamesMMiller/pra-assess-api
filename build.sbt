name := """pra-assess-api"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  coverageMinimumStmtTotal := 90,
  coverageFailOnMinimum := false,
  coverageHighlighting := true,
  coverageExcludedFiles :=
    """|.*handlers.*;
       |.*queries.*;
       |.*viewmodels.*;
       |.*components.*;
       |.*config.*;
       |.*models.*;
       |.*mapping.*;
       |.*stubsonly.*;
       |.*utils.*;
       |.*Routes.*;
       |.*views.xml.pdf.*;
       |.*views.ViewUtils;
       |.*views.html.templates.*;
       |""".stripMargin.replaceAll("\n", ";")
)

scalaVersion := "3.3.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test

// Coverage settings


// Linting alias
addCommandAlias("lint", ";scalafmtAll; scalafixAll")

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
