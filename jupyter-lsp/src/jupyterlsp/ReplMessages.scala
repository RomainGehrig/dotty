package jupyterlsp

import org.eclipse.lsp4j.VersionedTextDocumentIdentifier

case class ReplInterpretParams(code: String) {
  def this() = this("")
}

case class GetReplResult(runId: Int) {
  def this() = this(0)
}

case class ReplInterpretResult(runId: Int, output: String, hasMore: Boolean) {
  def this() = this(0, "", false)
}

case class ReplCompletionParams(code: String, position: Int) {
  def this() = this("", 0)
}
