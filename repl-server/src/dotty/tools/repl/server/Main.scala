package dotty.tools
package repl
package server

import java.util.function.Consumer

import java.io.{ File => JFile, InputStream, OutputStream, PrintWriter }
import java.net._
import java.nio.channels._

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.services._
import org.eclipse.lsp4j.launch._
import org.eclipse.lsp4j.jsonrpc.Launcher

import jupyterlsp.ReplClient

/** Run the Repl Server.
 */
object Main {
  def main(args: Array[String]): Unit = {

    val serverSocket = new ServerSocket(12555)
    Runtime.getRuntime().addShutdownHook(new Thread(
      new Runnable {
        def run: Unit = {
          serverSocket.close()
        }
      }));

    println(s"Starting REPL server listening on port ${serverSocket.getLocalPort}")
    // val pw = new PrintWriter("../.dotty-repl-dev-port")
    // pw.write(serverSocket.getLocalPort.toString)
    // pw.close()

    val clientSocket = serverSocket.accept()
    println("Received connection")
    val serverIn = clientSocket.getInputStream
    val serverOut = clientSocket.getOutputStream
    System.setOut(System.err)
    scala.Console.withOut(scala.Console.err) {
      startServer(serverIn, serverOut)
    }
  }

  def startServer(in: InputStream, out: OutputStream) = {
    val server = new ReplServer

    val launcher =
      new Launcher.Builder[ReplClient]()
        .setLocalService(server)
        .setRemoteInterface(classOf[ReplClient])
        .setInput(in)
        .setOutput(out)
        // For debugging JSON messages:
        // .traceMessages(new java.io.PrintWriter(System.err, true))
        .create();

    val client = launcher.getRemoteProxy()
    server.connect(client)
    println("REPL server STARTED")
    launcher.startListening()
  }
}
