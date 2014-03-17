package org.bedone.installer

object ConsoleReader 
{
  val consoleReader = System.console

  def getChoice(prompt: String, choice: List[String]): String = {

    val line = Option(consoleReader.readLine(prompt + choice.mkString(" [", ",", "]:") )).map(_.toLowerCase)

    line match {
      case Some(text) if (choice.map(_.toLowerCase).contains(text))=> text
      case _ => getChoice(prompt, choice)
    }

  }

  def getYesOrNo(prompt: String): String = {
    val line = Option(consoleReader.readLine(prompt))

    line.map(_.toLowerCase).filter(x => x == "yes" || x == "no" ) match {
      case Some(text) => text.toLowerCase
      case None => getYesOrNo(prompt)
    }
  }

  def getLine(prompt: String): String = {
    val line = Option(consoleReader.readLine(prompt))

    line.filter(_.length > 0) match {
      case Some(text) => text
      case None => getLine(prompt)
    }
  }

  def getLine(prompt: String, defaultValue: String): String = {
    val line = Option(consoleReader.readLine(prompt))

    line.filter(_.length > 0) match {
      case Some(text) => text
      case None => defaultValue
    }
  }

}

