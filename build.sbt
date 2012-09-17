// 檔案：BeDone/build.sbt

name := "BeDone"            // 我們的專案名稱

version := "0.1"            // 我們的專案版本

scalaVersion := "2.9.2"     // 我們要使用的 Scala 版本

seq(webSettings :_*)        // 使用 xsbt-web-plugin 的預設設定

seq(lessSettings:_*)

(LessKeys.filter in (Compile, LessKeys.less)) := ("custom.less")

(resourceManaged in (Compile, LessKeys.less)) <<= 
    (sourceDirectory in Compile)(_ / "webapp" / "bootstrap" / "css")

(compile in Compile) <<= compile in Compile dependsOn (LessKeys.less in Compile)

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
    "Scala-Tools" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "BoneCP" at "http://jolbox.com/bonecp/downloads/maven"
)

libraryDependencies ++= Seq(
    "com.jolbox" % "bonecp" % "0.7.1.RELEASE",
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "mysql" % "mysql-connector-java" % "5.1.6",
    "net.liftweb" % "lift-webkit_2.9.2" % "2.5-SNAPSHOT" % "compile->default",
    "net.liftweb" % "lift-squeryl-record_2.9.2" % "2.5-SNAPSHOT",
    "net.liftmodules" %% "combobox" % "2.5-SNAPSHOT-0.1",
    "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container",
    "org.tautua.markdownpapers" % "markdownpapers-core" % "1.2.7"
)

port in container.Configuration := 8081

