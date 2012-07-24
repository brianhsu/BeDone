// 檔案：BeDone/build.sbt

seq(webSettings :_*)        // 使用 xsbt-web-plugin 的預設設定

name := "BeDone"            // 我們的專案名稱

version := "0.1"            // 我們的專案版本

scalaVersion := "2.9.2"     // 我們要使用的 Scala 版本

resolvers += "Scala-Tools Maven2 Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container",
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "mysql" % "mysql-connector-java" % "5.1.6",
    "net.liftweb" % "lift-webkit_2.9.2" % "2.5-SNAPSHOT" % "compile->default",
    "net.liftweb" % "lift-squeryl-record_2.9.2" % "2.5-SNAPSHOT",
    "org.tautua.markdownpapers" % "markdownpapers-core" % "1.2.7"
)

port in container.Configuration := 8081

