package com.dag

import java.lang.reflect.{ParameterizedType, Type}

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.`type`.TypeReference
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper

//http://stackoverflow.com/questions/12591457/scala-2-10-json-serialization-and-deserialization
object JacksonWrapper {
  val mapper = new ObjectMapper()
  //mapper.registerModule(DefaultScalaModule)

  def serialize(value: Any): String = {
    import java.io.StringWriter
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }

  def deserialize[T: Manifest](value: String) : T =
    mapper.readValue(value, typeReference[T])

  private [this] def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType = typeFromManifest(manifest[T])
  }

  private [this] def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) { m.erasure }
    else new ParameterizedType {
      def getRawType = m.erasure
      def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray
      def getOwnerType = null
    }
  }
}