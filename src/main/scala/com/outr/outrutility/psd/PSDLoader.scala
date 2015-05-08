package com.outr.outrutility.psd

import java.io.{File, FileReader}

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.JsonReader

import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer

import scala.collection.JavaConversions._

/**
 * @author Matt Hicks <matt@outr.com>
 */
object PSDLoader {
//  def main(args: Array[String]): Unit = {
//    load("StockMarket", new File("work/design/PSD/1 Stock Market.psd.output"))
//    load("InputTrades", new File("work/design/PSD/15 Input Trades.psd.output"))
//  }

  def load(screenName: String, directory: File) = {
    if (!directory.isDirectory) throw new RuntimeException(s"${directory.getAbsolutePath} is not a directory.")

    val output = new File(directory, "output.json")
    if (!output.exists()) throw new RuntimeException(s"Unable to find output.json file.")

    val init = ListBuffer.empty[String]
    val add = ListBuffer.empty[String]

    var names = Map.empty[String, Int]

    val reader = new JsonReader
    val json = reader.parse(new FileReader(output))
    println(s"Size: ${json.size}")
    json.foreach {
      case entry => {
        val name = entry.get("name").asString()
        val scalaNameOriginal = generateScalaName(name)
        val scalaNameCount = names.getOrElse(scalaNameOriginal, 0)
        val scalaName = if (scalaNameCount == 0) {
          scalaNameOriginal
        } else {
          s"$scalaNameOriginal$scalaNameCount"
        }
        names += scalaNameOriginal -> (scalaNameCount + 1)
        val layerType = entry.get("type").asString()
        val opacity = entry.get("opacity").asFloat()
//        val blendingMode = entry.get("blendingMode").asString()
        val left = entry.get("left").asInt()
        val top = entry.get("top").asInt()
        val width = entry.get("width").asInt()
        val height = entry.get("height").asInt()

        val body = layerType match {
          case "text" => {
            val text = entry.get("text")
            val value = text.get("value").asString()
            val font = text.get("font")
            val (fontFamily, fontVariant) = font.get("name").asString() match {
              case s if s.indexOf('-') != -1 => {
                val f = s.substring(0, s.indexOf('-'))
                val v = s.substring(s.indexOf('-') + 1) match {
                  case "Semibold" => "SemiBold"
                  case "Extrabold" => "ExtraBold"
                  case variant => variant
                }
                (f, v)
              }
              case s => (s, "Normal")
            }
            val fontSize = numbers2Words(font.get("sizes").asIntArray()(0).toString)
            val colorArray = font.get("colors").get(0).asIntArray()
            val color = new Color(colorArray(0) / 255.0f, colorArray(1) / 255.0f, colorArray(2) / 255.0f, colorArray(3) / 255.0f)
//            val align = font.get("alignment").asStringArray()(0)

            s"""new Label("$value", Styles(Fonts.$fontFamily.$fontVariant.$fontSize)).color(Color.valueOf("$color"))"""
          }
          case "image" => s"""new Image(Textures.byName(this, "$name")).sized($width, $height)"""
        }
        var line = s"lazy val $scalaName = $body.positioned($left, $top)"
        if (opacity != 1.0f) {
          line = s"$line.alpha(${opacity}f)"
        }
        init += line
        add += s"stage.addActor($scalaName)"
//        println(s"${entry.get("type").asString()} - ${entry.get("name").asString()}")
      }
    }

    println("package com.gotadawul.mobile.screens")
    println()
    println("import com.badlogic.gdx.graphics.Color")
    println("import com.badlogic.gdx.scenes.scene2d.ui._")
    println("import com.gotadawul.mobile.visual._")
    println("import com.outr.gl._")
    println("import com.outr.gl.screen.BaseScreen")
    println()
    println(s"object ${screenName}Screen extends BaseScreen {")
    init.foreach(line => println(s"  $line"))
    println()
    println("  override def init() = {")
    add.reverse.foreach(line => println(s"    $line"))
    println("  }")
    println("}")
  }

  private val changeMap = Map(
    " " -> "",
    "-" -> "",
    "[$]" -> "Dollar",
    "[>]" -> "Greater",
    "[/]" -> "Slash",
    "[.]" -> "Dot",
    "[,]" -> "Comma",
    "%" -> "Percent"
  )
  private def generateScalaName(s: String) = {
    var m = numbers2Words(s)
    changeMap.foreach {
      case (key, value) => m = m.replaceAll(key, value)
    }
    m.charAt(0).toLower + m.substring(1)
  }

  private val numberMap = ListMap(
    "14" -> "Fourteen",
    "24" -> "TwentyFour",
    "28" -> "TwentyEight",
    "30" -> "Thirty",
    "0" -> "Zero",
    "1" -> "One",
    "2" -> "Two",
    "3" -> "Three",
    "4" -> "Four",
    "5" -> "Five",
    "6" -> "Six",
    "7" -> "Seven",
    "8" -> "Eight",
    "9" -> "Nine"
  )
  private def numbers2Words(s: String) = {
    var m = s
    numberMap.foreach {
      case (key, value) => m = m.replaceAll(key, value)
    }
    m
  }
}