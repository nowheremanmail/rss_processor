package com.dag

import java.io.{File, FileWriter}
import java.text.SimpleDateFormat
import java.util.Locale

import com.dag.news.bo.TempNew
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.slf4j.LoggerFactory

object Counter {
  var x = 0
}

class FileSink extends RichSinkFunction[TempNew] {
  val logger = LoggerFactory.getLogger("com.dag.FileSink");

  val mapper = new ObjectMapper()

  var rootFile: File = null
  //var previous: String = null;
  //var previousWriter: FileWriter = null;

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)

    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    mapper.setLocale(Locale.US)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val conf = getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    val root = conf.get("rootFolder", "./data")

    rootFile = new File(root)
    rootFile.mkdirs()

    //System.out.println("created " + Counter.x + " " + this)
    //Counter.x = Counter.x+1
  }

  //var previousLang: String = null

  override def invoke(value: TempNew, context: SinkFunction.Context[_]): Unit = {
    val lang = value.getLanguage
    val date = value.getDay

    var writer: FileWriter = null
    try {
      val fileLang = new File(rootFile, lang)
      fileLang.mkdir();

      writer = new FileWriter(new File(fileLang, date + ".txt"), true)

      val str = mapper.writeValueAsString(value)
      writer.append(str + "\n")
    }
    finally {
      if (writer != null) writer.close()
    }
  }
}
