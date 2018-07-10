// 檔案：BeDone/build.sbt

name := "BeDone"            // 我們的專案名稱

version := "0.2"            // 我們的專案版本

scalaVersion := "2.11.12"     // 我們要使用的 Scala 版本

seq(webSettings :_*)        // 使用 xsbt-web-plugin 的預設設定

seq(lessSettings:_*)

(LessKeys.filter in (Compile, LessKeys.less)) := ("custom.less")

(resourceManaged in (Compile, LessKeys.less)) <<= 
  (sourceDirectory in Compile)(_ / "webapp" / "bootstrap" / "custom")

(compile in Compile) <<= compile in Compile dependsOn (LessKeys.less in Compile)

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps", "-language:implicitConversions")

resolvers ++= Seq(
  "Scala-Tools" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "BoneCP" at "http://jolbox.com/bonecp/downloads/maven",
  "brianhsu" at "http://brianhsu.moe/ivy"
)

libraryDependencies ++= Seq(
  "com.jolbox" % "bonecp" % "0.7.1.RELEASE",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container",
  "org.tautua.markdownpapers" % "markdownpapers-core" % "1.2.7",
  "org.scribe" % "scribe" % "1.3.2",
  "org.slf4j" % "slf4j-nop" % "1.6.6"
)

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "mysql" % "mysql-connector-java" % "5.1.6"
)

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-webkit" % "2.6.3" % "compile->default",
  "net.liftweb" %% "lift-squeryl-record" % "2.6.3",
  "net.liftmodules" %% "combobox" % "2.6.3-0.7.2"
)

port in container.Configuration := 8081
