package jupyterlsp

import java.net._
import java.io._
import java.nio.file._
import java.util.concurrent.CompletableFuture
import java.util.function.Function

import org.eclipse.lsp4j
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.services._
import org.eclipse.lsp4j.jsonrpc.Launcher

import scala.collection._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.io.Codec
import scala.util.Properties

// Almond specifics
import almond.protocol.KernelInfo
import almond.interpreter.api.{DisplayData, OutputHandler}
import almond.interpreter.input.InputManager
import almond.interpreter.{ExecuteResult, Interpreter, Completion}

trait Server extends LanguageServer with ReplService

object JupyterReplClient {
  /** Create a new client connected to the REPL server at the given port number. */
  def apply(port: Int): JupyterReplClient = {
    val client = new JupyterReplClient

    // val writer = new PrintWriter(new File("lsp-client.log"))
    val writer = new PrintWriter(System.err, true)

    val socket = new Socket("localhost", port)

    val launcher = Launcher.createLauncher(client, classOf[Server],
      socket.getInputStream, socket.getOutputStream, /*validate =*/ false,  writer)
    launcher.startListening()
    val server = launcher.getRemoteProxy
    client.server = server

    val params = new InitializeParams
    // TODO What should the rootUri be ? (question for all ReplClients)
    params.setRootUri(System.getProperty("user.dir"))
    server.initialize(params)

    client
  }
}

class JupyterReplClient extends ReplClient with Interpreter { thisClient =>

  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}

  private var server: Server = _

  override def currentLine: Int = count
  @volatile private var count = 0

  override def kernelInfo(): KernelInfo = KernelInfo(
    implementation="dotty",
    implementation_version="0.1",
    language_info=KernelInfo.LanguageInfo(
      name="dotty",
      version="2.14.0", // TODO ?
      mimetype="text/scala",
      file_extension=".scala",
      nbconvert_exporter="", // TODO ?
      pygments_lexer=None, // TODO ?
      codemirror_mode=None, // TODO ?
      ),
    banner="Dotty kernel"
      // helper_links=None,
  )

  override def execute(
    code: String,
    storeHistory: Boolean,
    inputManager: Option[InputManager],
    outputHandler: Option[OutputHandler]
  ): ExecuteResult = {
    val result = server.interpret(ReplInterpretParams(code))

    try {
      ExecuteResult.Success(DisplayData.text(result.get().output))
    } catch {
      case e: Throwable =>
        ExecuteResult.Error(e.toString())
    }
  }

  override def logMessage(params: MessageParams): Unit = {}
  override def showMessage(params: MessageParams): Unit = {}
  override def showMessageRequest(params: ShowMessageRequestParams): CompletableFuture[MessageActionItem] = null
  override def publishDiagnostics(params: PublishDiagnosticsParams): Unit = {}
  override def telemetryEvent(params: Any): Unit = {}
}
