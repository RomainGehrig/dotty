package jupyterlsp

import org.eclipse.lsp4j.jsonrpc._
import org.eclipse.lsp4j.jsonrpc.services._

import java.net.URI
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap}

@JsonSegment("repl")
trait ReplService {
  // See WorksheetService for examples

  @JsonRequest
  def interpret(params: ReplInterpretParams): CompletableFuture[ReplInterpretResult]

  @JsonRequest
  def interpretResults(params: GetReplResult): CompletableFuture[ReplInterpretResult]

}
