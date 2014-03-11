// BeDone/project/plugins.scala

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.7.0")

resolvers += Resolver.url("sbt-plugin-releases 2",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "less-sbt" % "0.1.10")
