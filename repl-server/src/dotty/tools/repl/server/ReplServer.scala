package dotty.tools
package repl
package server

import java.net.URI
import java.io._
import java.nio.file._
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, CancellationException}
import java.util.function.Function

import org.eclipse.lsp4j
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier

import scala.collection._
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.io.Codec

import lsp4j.services._

//
import dotc._
import ast.{Trees, tpd}
import core._, core.Decorators.{sourcePos => _, _}
import Contexts._, Flags._, Symbols._

import jupyterlsp._


/** A Language Server that runs an instance of the Dotty REPL.
 */
class ReplServer extends LanguageServer
    with TextDocumentService with WorkspaceService with ReplService { thisServer =>
  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}
  import lsp4j._

  private val defaultLoader = Thread.currentThread().getContextClassLoader
  private val resultOutput = new StringBuilder
  private val resultStream = new FunctionOutputStream(2000, 2000, UTF_8, resultOutput.append(_))
  private val replDriver = new ReplDriver(settings=Array[String]("-classpath", "/home/cranium/.ivy2/local/ch.epfl.lamp/dotty-library_2.12/0.11.0-bin-SNAPSHOT-nonbootstrapped/jars/dotty-library_2.12.jar:/home/cranium/.coursier/cache/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.7/scala-library-2.12.7.jar"),
                                          out=resultStream.printStream(), classLoader=Some(defaultLoader))

  private[this] var rootUri: String = _
  @volatile private[this] var currentReplRunId_ : Int = 0
  @volatile private[this] var currentReplRun_ = Option.empty[CompletableFuture[State]]
  @volatile private[this] var currentReplState_ = replDriver.initialState

  private[this] def getReplRun(runId: Int): Option[CompletableFuture[State]] =
    if (runId == currentReplRunId_)
      currentReplRun_
    else {
      None
    }
  private[this] var myClient: ReplClient = _
  def client: ReplClient = myClient

  def connect(client: ReplClient): Unit = {
    myClient = client
  }

  override def exit(): Unit = {
    System.exit(0)
  }

  override def shutdown(): CompletableFuture[Object] = {
    CompletableFuture.completedFuture(new Object)
  }

  def computeAsync[R](fun: CancelChecker => R): CompletableFuture[R] =
    CompletableFutures.computeAsync { cancelToken =>
      // We do not support any concurrent use of the compiler currently.
      thisServer.synchronized {
        cancelToken.checkCanceled()
        try {
          fun(cancelToken)
        } catch {
          case NonFatal(ex) =>
            ex.printStackTrace
            throw ex
        }
      }
    }

  override def initialize(params: InitializeParams) = computeAsync { cancelToken =>
    rootUri = params.getRootUri
    assert(rootUri != null)

    val c = new ServerCapabilities
    c.setTextDocumentSync(TextDocumentSyncKind.Full)
    c.setHoverProvider(true)
    c.setDocumentHighlightProvider(false)
    c.setDocumentSymbolProvider(false)
    c.setDefinitionProvider(false)
    c.setRenameProvider(false)
    c.setWorkspaceSymbolProvider(false)
    c.setReferencesProvider(false)
    c.setCompletionProvider(new CompletionOptions(
      /* resolveProvider = */ false,
      /* triggerCharacters = */ List(".").asJava))

    new InitializeResult(c)
  }

  override def didOpen(params: DidOpenTextDocumentParams): Unit = thisServer.synchronized {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)

    ???
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = thisServer.synchronized {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)

    ???
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = thisServer.synchronized {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)

    ???
  }

  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit =
    /*thisServer.synchronized*/ {}

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit =
    /*thisServer.synchronized*/ {}

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
    /*thisServer.synchronized*/ {}
  }

  // TODO interpret
  override def interpret(params: ReplInterpretParams): CompletableFuture[ReplInterpretResult] = computeAsync { cancelToken =>
    // TODO No concurrent runs !

    // TODO Get state
    // TODO Interprete
    // TODO Add new state
    // TODO Spawn new thread
    val replRun: CompletableFuture[State] = CompletableFutures.computeAsync { replCancelToken =>
      println(s"Received code: ${params.code}")

      val replThread = new Thread {
        override def run(): Unit = {
          currentReplState_ = replDriver.run(params.code)(currentReplState_)
        }
      }

      replThread.start
      while (replThread.isAlive) {
        Thread.sleep(50)

        try replCancelToken.checkCanceled()
        catch { case _: CancellationException => replThread.interrupt }
      }

      currentReplState_
    }


    currentReplRunId_ += 1
    currentReplRun_ = Some(replRun)

    val hasMore = !replRun.isDone()
    val out = resultOutput.result()
    if (!hasMore)
      resultOutput.clear()

    ReplInterpretResult(currentReplRunId_, out, hasMore)
  }

  override def interpretResults(params: GetReplResult): CompletableFuture[ReplInterpretResult] = computeAsync { cancelToken =>
    val runId = params.runId
    getReplRun(runId) match {
      case None =>
        println(s"Found no run for id $runId")
        null // No current run -> we don't reply to the request
      case Some(replRun) =>
        try cancelToken.checkCanceled()
        catch { case _: CancellationException => replRun.cancel(true) }

        // TODO Get result from runId
        var out = resultOutput.result()
        while (out.isEmpty && !replRun.isDone) {
          try cancelToken.checkCanceled()
          catch { case _: CancellationException => replRun.cancel(true) }

          Thread.sleep(100) // TODO change resultOutput to wait for future

          out = resultOutput.result()
        }
        resultOutput.clear()
        println(s"Interpret result for $params is $out")

        ReplInterpretResult(runId, out, !replRun.isDone())
    }
  }

  // override def interruptRun(params: ReplRunId): Unit = {
  //   // TODO
  // }

  // TODO Copied from DottyLanguageServer.scala
  /** Create an lsp4j.CompletionItem from a Symbol */
  def completionItem(sym: Symbol)(implicit ctx: Context): lsp4j.CompletionItem = {
    def completionItemKind(sym: Symbol)(implicit ctx: Context): lsp4j.CompletionItemKind = {
      import lsp4j.{CompletionItemKind => CIK}

      if (sym.is(Package))
        CIK.Module // No CompletionItemKind.Package (https://github.com/Microsoft/language-server-protocol/issues/155)
      else if (sym.isConstructor)
        CIK.Constructor
      else if (sym.isClass)
        CIK.Class
      else if (sym.is(Mutable))
        CIK.Variable
      else if (sym.is(Method))
        CIK.Method
      else
        CIK.Field
    }

    val label = sym.name.show
    val item = new lsp4j.CompletionItem(label)
    try {
      item.setDetail(sym.info.widenTermRefExpr.show)
    } catch {
      case _ =>
    }
    item.setKind(completionItemKind(sym))
    item
  }

  override def completion(params: CompletionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)

    // TODO
    // val replState = getReplState(uri)
    // val pos = params.position;

    // replDriver.completions()

    JEither.forRight(new CompletionList(
      /*isIncomplete = */ false, Nil.asJava))
  }

  override def replCompletion(params: ReplCompletionParams) = computeAsync { cancelToken =>
    val code = params.code
    val pos = params.position

    val completions = replDriver.makeCompletions((sym, ctx) => completionItem(sym)(ctx),
                                                 pos, code, currentReplState_)
    JEither.forRight(new CompletionList(
                       /* isIncomplete = */ false,
                       completions.asJava
                     ))
  }

  override def hover(params: TextDocumentPositionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)

    val markup = new lsp4j.MarkupContent
    markup.setKind("markdown")
    markup.setValue("todo")

    new Hover(markup, null)
  }

  override def getTextDocumentService: TextDocumentService = this
  override def getWorkspaceService: WorkspaceService = this

  // Unimplemented features. If you implement one of them, you may need to add a
  // capability in `initialize`
  override def definition(params: TextDocumentPositionParams) = null
  override def references(params: ReferenceParams) = null
  override def rename(params: RenameParams) = null
  override def documentHighlight(params: TextDocumentPositionParams) = null
  override def documentSymbol(params: DocumentSymbolParams) = null
  override def symbol(params: WorkspaceSymbolParams) = null
  override def codeAction(params: CodeActionParams) = null
  override def codeLens(params: CodeLensParams) = null
  override def formatting(params: DocumentFormattingParams) = null
  override def rangeFormatting(params: DocumentRangeFormattingParams) = null
  override def onTypeFormatting(params: DocumentOnTypeFormattingParams) = null
  override def resolveCodeLens(params: CodeLens) = null
  override def resolveCompletionItem(params: CompletionItem) = null
  override def signatureHelp(params: TextDocumentPositionParams) = null
}
