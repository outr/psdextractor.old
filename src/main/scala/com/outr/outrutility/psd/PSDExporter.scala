package com.outr.outrutility.psd

import java.io.File

import org.hyperscala.css.attributes.Display
import org.powerscala.IO

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
    val nodes = preview.byType[PreviewNode].toList.reverse
    val construction = ListBuffer.empty[String]
    val add = ListBuffer.empty[String]
    nodes.foreach {
      case node => if (node.style.display() != Display.None) {
        val name = node.layer.nodeName.value()
        val scalaName = s"${name.charAt(0).toLower}${name.substring(1)}"
        val constructor = node match {
          case text: PreviewText => {
            val value = text.value
            val color = s"${text.color.hex.rgb}.substring(1)${text.color.hex.alpha}".toUpperCase
            s"""new Label("$value", Styles(Fonts.${text.fontFamily}.${text.fontWeight}.${text.fontSize})).color(Color.valueOf("$color"))"""
          }
          case image: PreviewImage => s"""new Image(Textures.Icons.$name).sized(${node.w}, ${node.h})"""
        }
        construction += s"  lazy val $scalaName = $constructor.positioned(${node.x}, ${node.y})"
        add += s"    stage.addActor($scalaName)"
      }
    }

    // Create Scala code
    val scalaCode =
      s"""
         |package $packageName
         |
         |import com.badlogic.gdx.graphics.Color
         |import com.badlogic.gdx.scenes.scene2d.ui._
         |import com.gotadawul.mobile.visual._
         |import com.outr.gl._
         |import com.outr.gl.screen.BaseScreen
         |
         |object ${page.scalaName.value()} extends BaseScreen {
         |  ${construction.mkString("\n")}
         |
         |  override defin init() = {
         |    ${add.mkString("\n")}
         |  }
         |}
       """.stripMargin

    // Make the directories
    outputDirectory.mkdirs()

    // Write the Scala file to disk
    IO.copy(scalaCode, outputFile)

    // Copy resources to resources directory
    nodes.foreach {
      case image: PreviewImage if image.style.display() != Display.None => IO.copy(image.file, new File(resourceDirectory, s"${image.layer.nodeName.value()}.png"))
      case _ => // Ignore everything else
    }
  }
}