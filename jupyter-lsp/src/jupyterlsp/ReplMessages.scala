package jupyterlsp

import org.eclipse.lsp4j.VersionedTextDocumentIdentifier

case class ReplInterpretParams(code: String) {
  def this() = this("")
}

case class ReplInterpretResult(output: String) {
  def this() = this("")
}
