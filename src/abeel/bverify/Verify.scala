package abeel.bverify

import java.io.File
import atk.io.PatternFileFilter
import java.io.PrintWriter
import atk.util.Tool

object Verify extends Tool {

  case class Config(val pattern: String = ".*.log", input: File = null, output: File = null, clean: Boolean = false)

  def main(args: Array[String]): Unit = {

    val parser = new scopt.OptionParser[Config]("java -jar bverify.jar") {
      opt[String]('p', "pattern") action { (x, c) => c.copy(pattern = x) } text ("File pattern to search and process.")
      opt[File]('i', "input") required () action { (x, c) => c.copy(input = x) } text ("Input directory where to search for log files.")

      opt[File]('o', "output") action { (x, c) => c.copy(output = x) } text ("File where you want the output to be written")
      opt[Unit]('c', "clean") action { (_, c) =>
        c.copy(clean = true)
      } text ("Remove log files that completed succesfully")

    }
    parser.parse(args, Config()) map { config =>

      assume(config.input.exists(), "Input directory needs to exist")

      val pw = if (config.output == null) new PrintWriter(System.out) else new PrintWriter(config.output)

      pw.println(generatorInfo)

      val files = config.input.listFiles(new PatternFileFilter(config.pattern))

      var good=0
      var fail=0
      files.map(file => {
        val success = tLinesIterator(file).take(30).toList.exists(_.contains("Successfully completed."))

        if (!success) {
          pw.println(file + "\tfailed")
          fail += 1
        } else {
          good += 1
          if (config.clean)
            file.delete()
        }

      })
      pw.println("# "+good+"\t runs succeeded")
      pw.println("# "+fail+"\t runs failed")
      finish(pw)

    }
  }

}