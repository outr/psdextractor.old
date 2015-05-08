package com.outr.outrutility.psd

import java.io.File

import scala.sys.process._

/**
 * @author Matt Hicks <matt@outr.com>
 */
object PSDExtractor {
//  def main(args: Array[String]): Unit = {
//    val psd = new File("psdextractor/2 Login.psd")
//    val outputDirectory = new File("extracted")
//    extract(psd, outputDirectory)
//  }

  def extract(psd: File, outputDirectory: File) = {
    val processBuilder = Seq("node", "psdextractor/psdextractor.js", psd.getAbsolutePath, outputDirectory.getAbsolutePath)
    val logger = ProcessLogger(
      (o: String) => println("out " + o),
      (e: String) => println("err " + e)
    )
    val response = processBuilder ! logger
    if (response != 0) throw new RuntimeException(s"PSDExtractor failed with $response for ${processBuilder.mkString(" ")}.")
  }
}
