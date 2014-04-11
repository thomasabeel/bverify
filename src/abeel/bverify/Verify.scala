package abeel.bverify

import java.io.File
import atk.io.PatternFileFilter
import java.io.PrintWriter
import atk.util.Tool
import scala.collection.mutable.MutableList
import atk.io.IOTools

object Verify extends Tool {

  case class Config(val recursive: Boolean = false, val pattern: Seq[String] = Seq(), input: File = new File("."), output: File = null, clean: Boolean = false)

  def main(args: Array[String]): Unit = {

    val parser = new scopt.OptionParser[Config]("java -jar bverify.jar") {
      opt[String]('p', "pattern") unbounded () action { (x, c) => c.copy(pattern = c.pattern :+ x) } text ("File patterns to search and process. You can specify this option multiple times.")
      opt[File]('i', "input") action { (x, c) => c.copy(input = x) } text ("Input directory where to search for log files. Default is current directory.")

      opt[File]('o', "output") action { (x, c) => c.copy(output = x) } text ("File where you want the output to be written. Default is screen")
      opt[Unit]('c', "clean") action { (_, c) => c.copy(clean = true) } text ("Remove log files that completed succesfully")
      opt[Unit]('r', "recursive") action { (_, c) => c.copy(recursive = true) } text ("Recursively search all sub-directories")

    }
    parser.parse(args, Config()) map { config =>

      assume(config.input.exists(), "Input directory needs to exist")

      val pw = if (config.output == null) new PrintWriter(System.out) else new PrintWriter(config.output)

      pw.println(generatorInfo)
      pw.println("# Pattern\tSuccess\tFailure")
      val patterns=if(config.pattern.size>0)config.pattern.toList else List(".*.log")
      for (pattern <- patterns) {
        val files = if(config.recursive) IOTools.recurse(config.input, new PatternFileFilter(pattern))   else config.input.listFiles(new PatternFileFilter(pattern))

        var good = 0
        var fail = 0
        val failureList=MutableList.empty[File]
        files.map(file => {
          val success = tLinesIterator(file).take(30).toList.exists(_.contains("Successfully completed."))

          if (!success) {
//            pw.println(file + "\tfailed")
            failureList +=file
            fail += 1
          } else {
            good += 1
            if (config.clean)
              file.delete()
          }

        })
        pw.println(pattern+"\t"+good+"\t"+fail)
        if(failureList.size>0)
        	pw.println("\t Failed runs: \n\t\t"+failureList.mkString("\n\t\t"))
//        pw.println("# " + good + "\t runs succeeded")
//        pw.println("# " + fail + "\t runs failed")
      }
      finish(pw)

    }
  }

}