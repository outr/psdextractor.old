package com.outr.outrutility.psd

import java.io.File

import org.hyperscala.css.attributes.Display
import org.powerscala.IO

import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer

/**
 * @author Matt Hicks <matt@outr.com>
 */
object PSDExporter {
  val packageName = "com.gotadawul.mobile.screens"
  val directory = new File(".")

  def export(page: PSDPreviewPage, resourceDirectory: File) = {
    val outputDirectory = new File(directory, ".")    // TODO: support packages
    val outputFile = new File(outputDirectory, s"${page.scalaName.value()}.scala")
    val preview = page.psdPreview.get
    val width = preview.width
    val height = preview.height
    val nodes = preview.byType[PreviewNode].toList.filter(n => n.layer.visibility.checked()).reverse
    val construction = ListBuffer.empty[String]
    val add = ListBuffer.empty[String]
    nodes.foreach {
      case node => {
        val name = node.layer.nodeName.value()
        val scalaName = generateScalaName(name)
        var constructor = node match {
          case text: PreviewText => {
            val value = text.value
            val fontFamily = text.fontFamily.replaceAll(" ", "")
            val color = s"${text.color.hex.rgb.substring(1)}${text.color.hex.alpha}".toUpperCase
            s"""new Label("$value", app.styles(app.fonts.${fontFamily}.${text.fontWeight}.${numbers2Words(text.fontSize.toString)})).color(Color.valueOf("$color"))"""
          }
          case image: PreviewImage => s"""new Image(app.textures.Icons.${name.replaceAll(" ", "")}).sized(${node.w}, ${node.h})"""
        }
        constructor += s".positioned(${node.x}, ${node.y - 6})"
        if (node.opacity != 1.0f) {
          constructor += s".alpha(${node.opacity}f)"
        }
        construction += s"  lazy val $scalaName = $constructor"
        add += s"    stage.addActor($scalaName)"
      }
    }

    // Create Scala code
    val scalaCode =
      s"""package $packageName
         |
         |import com.badlogic.gdx.graphics.Color
         |import com.badlogic.gdx.scenes.scene2d.ui._
         |import com.gotadawul.mobile.AppSupport
         |import com.gotadawul.mobile.visual._
         |import com.outr.gl._
         |import com.outr.gl.screen.BaseScreen
         |
         |class ${page.scalaName.value()} extends BaseScreen with AppSupport {
         |${construction.mkString("\n")}
         |
         |  override def init() = {
         |${add.mkString("\n")}
         |  }
         |}""".stripMargin

    // Make the directories
    outputDirectory.mkdirs()

    // Write the Scala file to disk
    IO.copy(scalaCode, outputFile)

    // Copy resources to resources directory
    resourceDirectory.mkdirs()
    nodes.foreach {
      case image: PreviewImage => IO.copy(image.file, new File(resourceDirectory, s"${image.layer.nodeName.value().toLowerCase}.png"))
      case _ => // Ignore everything else
    }
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
    "20" -> "Twenty",
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