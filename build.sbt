// 檔案：BeDone/build.sbt

seq(webSettings :_*)        // 使用 xsbt-web-plugin 的預設設定

name := "BeDone"            // 我們的專案名稱

version := "0.1"            // 我們的專案版本

scalaVersion := "2.9.2"     // 我們要使用的 Scala 版本

libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container",
    "javax.servlet" % "servlet-api" % "2.5" % "provided"
)


