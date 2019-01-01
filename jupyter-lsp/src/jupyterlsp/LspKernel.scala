package jupyterlsp

import scala.io.Source

import java.net.{URLClassLoader, URL}

import almond.util.ThreadUtil.singleThreadedExecutionContext
import almond.channels.zeromq.ZeromqThreads
import almond.kernel.install.{Install, Options}
import almond.kernel.{Kernel, KernelThreads}
import almond.logger.{Level, LoggerContext}

/** Kernel that connects to an LSP server */
object LspKernel {
  def main(args: Array[String]): Unit = {
    val argsStr = args.mkString(",")
    println(s"Arguments: $argsStr")

    if (args.length >= 1 && args(0) == "--install")
      Install.installOrError(
        defaultId = "dotty",
        defaultDisplayName = "Dotty",
        language = "scala",
        options = Options(force = true)
      ) match {
        case Left(e) =>
          Console.err.println(s"Error: $e")
          sys.exit(1)
        case Right(dir) =>
          println(s"Installed dotty kernel under $dir")
          sys.exit(0)
      }

    // TODO better args
    val connectionFile = args(1)

    val logCtx = LoggerContext.stderr(Level.Warning)
    val zeromqThreads = ZeromqThreads.create("dotty-kernel")
    val kernelThreads = KernelThreads.create("dotty-kernel")
    val interpreterDotty = singleThreadedExecutionContext("dotty-interpreter")

    Kernel.create(JupyterReplClient(Consts.lspServerPort), interpreterDotty, kernelThreads, logCtx)
      .flatMap(_.runOnConnectionFile(connectionFile, "dotty", zeromqThreads))
      .unsafeRunSync()

  }

}
