package com.dag

import java.io.{File, FileWriter}

import com.dag.news.bo.TempNew
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature
import java.text.SimpleDateFormat
import java.util.Locale

import org.slf4j.LoggerFactory

object Counter {
  var x = 0
}

class FileSink extends RichSinkFunction[(String, String, TempNew)] {
  val logger = LoggerFactory.getLogger("com.dag.FileSink");

  val mapper = new ObjectMapper()

  var rootFile: File = null
  var previous: String = null;
  var previousWriter: FileWriter = null;

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)

    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    mapper.setLocale(Locale.US)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val conf = getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    val root = conf.get("rootFolder")

    rootFile = new File(root)
    rootFile.mkdirs()

    //System.out.println("created " + Counter.x + " " + this)
    //Counter.x = Counter.x+1
  }

  var previousLang: String = null

  override def invoke(value: (String, String, TempNew), context: SinkFunction.Context[_]): Unit = {
    val lang = value._1
    val date = value._2
    val content = value._3

    try {
      var writer: FileWriter = previousWriter

      if (previousLang != null && previousLang != lang) {
        logger.error("detected language change")
      }
      previousLang = lang

      if (previous == null || previous != lang + date) {
        if (previousWriter != null) {
          previousWriter.close()
        }
        val fileLang = new File(rootFile, lang)
        fileLang.mkdir();
        val file = new File(fileLang, date + ".txt")
        writer = new FileWriter(file, true)
        previousWriter = writer
        previous = lang + date
      }
      else {
        writer = previousWriter
      }

      val str = mapper.writeValueAsString(content)

      writer.append(str)
      writer.append("\n")
      writer.flush()
    }
    finally {
      //if (writer != null) writer.close()
    }
  }
}
