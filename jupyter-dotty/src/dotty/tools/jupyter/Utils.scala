package dotty.tools
package jupyter

// Copied from Almond internals
import java.io.{OutputStream, PrintStream}
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.{Charset, CoderResult}

import almond.interpreter.api.OutputHandler

class FunctionOutputStream(
  inputBufferSize: Int,
  outputBufferSize: Int,
  internalCharset: Charset,
  f: String => Unit
) extends OutputStream {

  // not thread-safe

  private val decoder = internalCharset.newDecoder()

  private val inArray = Array.ofDim[Byte](inputBufferSize)
  private val outArray = Array.ofDim[Char](outputBufferSize)

  private val writeBuf = ByteBuffer.wrap(inArray)
  private val out = CharBuffer.wrap(outArray)

  private var outputHandler: Option[OutputHandler] = None

  private def flushIfNeeded(): Unit =
    if (!writeBuf.hasRemaining)
      flush()

  def write(b: Int): Unit = {
    writeBuf.put(b.toByte) // hope toByte doesn't box b
    flushIfNeeded()
  }

  def setOutputHandler(newOutputHandler: OutputHandler) = {
    outputHandler = Some(newOutputHandler)
  }

  override def write(b: Array[Byte], off: Int, len: Int) = {
    var off0 = off
    var len0 = len
    while (len0 > 0) {
      val take = math.min(len0, writeBuf.remaining())
      assert(take > 0)
      writeBuf.put(b, off0, take)
      off0 = off0 + take
      len0 = len0 - take
      flushIfNeeded()
    }
    assert(len0 == 0)
    assert(off0 == off + len)
  }

  override def flush(): Unit = {
    super.flush()

    val readBuf = ByteBuffer.wrap(inArray, 0, writeBuf.position())

    var r: CoderResult = null

    while (r == null || r.isOverflow) {
      if (r != null) {
        readBuf.position(0)
        readBuf.limit(writeBuf.position())
      }

      r = decoder.decode(readBuf, out, false)

      val outLen = out.position()

      if (r.isError || (r.isOverflow && outLen == 0))
        r.throwException()
      else {
        if (outLen > 0) {
          val s = new String(outArray, 0, outLen)
          out.clear()
          f(s)
          outputHandler.map(_.stdout(s))
        }
        val read = readBuf.position()
        val avail = writeBuf.position()
        val remaining = avail - read
        writeBuf.position(remaining)
        if (remaining > 0)
          System.arraycopy(inArray, read, inArray, 0, remaining)
      }
    }
  }

  def printStream(): PrintStream =
    new PrintStream(this, true, internalCharset.name())

}
