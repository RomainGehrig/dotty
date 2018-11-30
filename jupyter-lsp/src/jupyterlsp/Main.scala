package jupyterlsp

import scala.io.Source

object Main {
  def main(args: Array[String]): Unit = {
    val port = args.toList match {
      case port :: Nil =>
        println(s"Reading port from input")
        Integer.parseInt(port)
      case Nil =>
        println(s"Reading port from .dotty-repl-dev-port file")
        val file = Source.fromFile(".dotty-repl-dev-port")
        Integer.parseInt(file.getLines.toList(0))
      case _ =>
        Console.err.println("Invalid arguments: expected {port}")
        System.exit(1)
        0
    }
    println(s"Launching client on port = $port")
    val client = JupyterReplClient(port)
    println("Client launched")
  }
}
