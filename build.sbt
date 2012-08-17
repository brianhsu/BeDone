// 檔案：BeDone/build.sbt

name := "BeDone"            // 我們的專案名稱

version := "0.1"            // 我們的專案版本

scalaVersion := "2.9.2"     // 我們要使用的 Scala 版本

seq(webSettings :_*)        // 使用 xsbt-web-plugin 的預設設定

seq(lessSettings:_*)

(LessKeys.filter in (Compile, LessKeys.less)) := 
    ("bootstrap.less" || "styles.less" || "responsive.less")

(resourceManaged in (Compile, LessKeys.less)) <<= 
    (sourceDirectory in Compile)(_ / "webapp" / "bootstrap" / "css")

(compile in Compile) <<= compile in Compile dependsOn (LessKeys.less in Compile)

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
    "Scala-Tools" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "BoneCP" at "http://jolbox.com/bonecp/downloads/maven"
)

libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container",
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "mysql" % "mysql-connector-java" % "5.1.6",
    "net.liftweb" % "lift-webkit_2.9.2" % "2.5-SNAPSHOT" % "compile->default",
    "net.liftweb" % "lift-squeryl-record_2.9.2" % "2.5-SNAPSHOT",
    "org.tautua.markdownpapers" % "markdownpapers-core" % "1.2.7",
    "com.jolbox" % "bonecp" % "0.7.1.RELEASE",
    "org.slf4j" % "slf4j-simple" % "1.6.6"
)

port in container.Configuration := 8081

