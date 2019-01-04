package jupyterlsp

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc._
import org.eclipse.lsp4j.jsonrpc.services._
import org.eclipse.lsp4j.jsonrpc.messages.{Either => JEither}

import java.net.URI
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap}

@JsonSegment("repl")
trait ReplService {
  // See WorksheetService for examples

  @JsonRequest
  def interpret(params: ReplInterpretParams): CompletableFuture[ReplInterpretResult]

  @JsonRequest
  def interpretResults(params: GetReplResult): CompletableFuture[ReplInterpretResult]

  @JsonRequest
  def replCompletion(params: ReplCompletionParams): CompletableFuture[JEither[java.util.List[CompletionItem],CompletionList]]

}
