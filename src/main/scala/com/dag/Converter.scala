package com.dag

import java.io.{File, FileReader, FileWriter, FilenameFilter}

import scala.io.Source

object Hello extends App {
  val root = new File(args(0))

  def convert(src: File) = {

    val old = new File (src.getParent, src.getName + "old")
    src.renameTo(old)
    val dst = new File(src.getParent, src.getName )

    val dstW = new FileWriter(dst, false)
    var already: Set[String] = Set()

    Source.fromFile(old, "utf-8").getLines().foreach((str: String) => {
      if (!already.contains(str)) {
        already += str;
        dstW.write(str);
        dstW.write("\n")
      }
    })
    System.out.println(src + " -> " + dst)
    dstW.close()
  }

  val languages = root.listFiles(new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = {
      new File(dir, name).isDirectory()
    }
  }).flatMap((folder: File) =>
    folder.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = {
        new File(folder, name).isFile() && name.endsWith(".txt")
      }
    })).foreach((file: File) => convert(file))

}