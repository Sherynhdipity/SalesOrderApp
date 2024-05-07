ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.10.2"

lazy val root = (project in file("."))
  .settings(
    name := "SalesOrderApp"
  )
