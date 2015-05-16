package com.outr.outrutility.psd

import java.io.{FileReader, File}

import com.badlogic.gdx.utils.{JsonValue, JsonReader}
import org.hyperscala.css.attributes._
import org.hyperscala.html.attributes.InputType
import org.hyperscala.html.constraints.BodyChild
import org.hyperscala.html.tag.Br
import org.hyperscala.jquery.Gritter
import org.hyperscala.jquery.ui.jQueryUI
import org.hyperscala.module.EncodedImages
import org.hyperscala.realtime._
import org.hyperscala.ui.{BusyDialog, ConfirmDialog}
import org.hyperscala.ui.module.WebFontLoader
import org.hyperscala.ui.widgets.FileUploader
import org.hyperscala.web.Webpage
import org.powerscala.{IO, Color}
import org.powerscala.property.Property

import scala.collection.JavaConversions._

import org.hyperscala.html._

/**
 * @author Matt Hicks <matt@outr.com>
 */
class PSDPreviewPage extends Webpage {
  require(Realtime)
  require(WebFontLoader)
  require(Gritter)
  require(jQueryUI)

  this.connectStandard()

  val directory = Property[File]()

  body.contents += new FileUploader {
    style.width := 250.px
    style.right := 10.px
    style.position := Position.Absolute

    override def onField(name: String, value: String) = {}

    override def onFile(filename: String, file: File) = processPSD(filename, file)
  }

  val layerManager = new PSDLayerManager
  body.contents += layerManager

  val scalaName = new tag.Input

  def outline(nodeOption: Option[PreviewNode]) = nodeOption match {
    case Some(node) => {
      outlineDiv.style.left := node.style.left()
      outlineDiv.style.top := node.style.top()
      outlineDiv.style.width := node.w.px
      outlineDiv.style.height := node.h.px
      outlineDiv.style.display := Display.Block
    }
    case None => outlineDiv.style.display := Display.None
  }

  val outlineDiv = new tag.Div {
    style.position := Position.Absolute
    style.outlineColor := Color.Red
    style.outlineOffset := 1.px
    style.outlineStyle := LineStyle.Solid
    style.outlineWidth := 2.px
    style.zIndex := ZIndex(5000)
    style.display := Display.None
  }
  body.contents += outlineDiv

  body.contents += new tag.Div {
    style.position := Position.Absolute
    style.right := 20.px
    style.top := 40.px

    contents += new tag.Button(content = "Show All") {
      clickEvent.onRealtime {
        case evt => layerManager.showAll()
      }
    }
    contents += new tag.Button(content = "Only Images") {
      clickEvent.onRealtime {
        case evt => layerManager.onlyImages()
      }
    }
    contents += new tag.Button(content = "Only Text") {
      clickEvent.onRealtime {
        case evt => layerManager.onlyText()
      }
    }
    contents += new tag.Button(content = "Hide All") {
      clickEvent.onRealtime {
        case evt => layerManager.hideAll()
      }
    }
    contents += new Br
    contents += new Br
    scalaName.style.width := 250.px
    contents += scalaName
    contents += new tag.Button(content = "Export") {
      clickEvent.onRealtime {
        case evt => export()
      }
    }
  }

  private def processPSD(filename: String, file: File) = if (file.getName.toLowerCase.endsWith(".psd")) {
    val outputDirectory = new File("extracted")
    val temp = new File("temp.psd")
    IO.copy(file, temp)
    PSDExtractor.extract(temp, outputDirectory)
    directory := outputDirectory
    scalaName.value := filename.substring(0, filename.indexOf('.'))
    load(directory())
  } else {
    Gritter.add(this, "Not a PSD", s"The supplied file ($filename) was not a PSD file!")
  }

  def psdPreview = body.byId[PSDPreview]("preview")
  def psdPreviewOverlay = body.byId[PSDPreviewOverlay]("preview_overlay")

  private def load(directory: File) = {
    psdPreview match {
      case Some(p) => {
        println("Removing preview!")
        p.removeFromParent()
      }
      case None => println("No preview") // Nothing already created
    }
    psdPreviewOverlay match {
      case Some(p) => {
        println("Removing overlay!")
        p.removeFromParent()
      }
      case None => println("No overlay") // Nothing already created
    }

    val outputFile = new File(directory, "output.json")
    if (!outputFile.exists()) throw new RuntimeException(s"output.json not found for ${directory.getAbsolutePath}.")
    val reader = new JsonReader
    val json = reader.parse(new FileReader(outputFile))
    val width = json.getInt("width")
    val height = json.getInt("height")

    val preview = new PSDPreview(width, height)
    body.contents += preview

    val previewOverlay = new PSDPreviewOverlay(this, width, height)
    body.contents += previewOverlay

    layerManager.contents.clear()
    val layers = json.get("layers")
    layers.foreach {
      case entry => {
        val node = entry.getString("type") match {
          case "text" => new PreviewText(entry, this)
          case "image" => new PreviewImage(entry, this)
        }
        preview.contents += node
        layerManager.add(node)
      }
    }
  }

  def export() = ConfirmDialog.show(this, "Are you sure you want to export this?", "Confirm Export") {
    BusyDialog(this, "Exporting...") {
      PSDExporter.export(this, new File("resourceDir"))
    }
  }
}

class PSDPreview(val width: Int, val height: Int) extends tag.Div(id = "preview") {
  style.width := width.px
  style.height := height.px
  style.borderColor := Color.Black
  style.borderStyle := LineStyle.Solid
  style.borderWidth := 1.px
  style.marginAll(10.px)
}

class PSDPreviewOverlay(page: PSDPreviewPage, width: Int, height: Int) extends tag.Img(id = "preview_overlay") {
  style.position := Position.Absolute
  style.left := 8.px
  style.top := 0.px
  style.width := width.px
  style.height := height.px
  style.borderColor := Color.Black
  style.borderStyle := LineStyle.Solid
  style.borderWidth := 1.px
  style.backgroundColor := Color.Red
  style.opacity := Opacity(0.0)
  style.zIndex := ZIndex(2000)
  style.marginAll(10.px)

  src := EncodedImages.encode(new File(page.directory(), "preview.png"))

  clickEvent.onRealtime {
    case evt => if (style.opacity().n == 0.0) {
      style.opacity := Opacity(0.5)
    } else if (style.opacity().n == 0.5) {
      style.opacity := Opacity(1.0)
    } else {
      style.opacity := Opacity(0.0)
    }
  }
}

class PSDLayerManager extends tag.Div(id = "layer_manager") {
  style.width := 350.px
  style.right := 10.px
  style.top := 120.px
  style.position := Position.Absolute
  style.borderStyle := LineStyle.Solid
  style.borderWidth := 1.px
  style.borderColor := Color.Black
  style.borderRadius := 5.px
  style.minHeight := 25.px
  style.paddingAll(5.px)
  style.maxHeight := 500.px
  style.overflowY := Overflow.Auto

  def add(node: PreviewNode) = contents += new PSDNodeLayer(node)

  def showAll() = byType[PSDNodeLayer].foreach(l => l.visibility.checked := true)
  def hideAll() = byType[PSDNodeLayer].foreach(l => l.visibility.checked := false)
  def onlyImages() = byType[PSDNodeLayer].foreach(l => l.visibility.checked := l.node.isInstanceOf[PreviewImage])
  def onlyText() = byType[PSDNodeLayer].foreach(l => l.visibility.checked := l.node.isInstanceOf[PreviewText])
}

class PSDNodeLayer(val node: PreviewNode) extends tag.Div {
  val visibility = new tag.Input(inputType = InputType.CheckBox, checked = true)
  val nodeName = new tag.Input(value = node.entry.getString("name"))
  nodeName.focusEvent := RealtimeEvent()
  nodeName.blurEvent := RealtimeEvent()

  nodeName.focusEvent.on {
    case evt => node.preview.outline(Some(node))
  }
  nodeName.blurEvent.on {
    case evt => node.preview.outline(None)
  }

  style.marginAll(5.px)
  node match {
    case n: PreviewText => nodeName.style.backgroundColor := Color.immutable(0.0, 1.0, 0.0, 0.2)
    case n: PreviewImage => nodeName.style.backgroundColor := Color.immutable(0.0, 0.0, 1.0, 0.2)
  }

  visibility.changeEvent := RealtimeEvent()

  visibility.checked.change.on {
    case evt => if (visibility.checked()) {
      node.style.display := Display.Block
    } else {
      node.style.display := Display.None
    }
  }

  visibility.style.marginAll(4.px)

  nodeName.style.width := 280.px

  contents += visibility
  contents += nodeName

  node.layer = this
}

trait PreviewNode extends BodyChild {
  def preview: PSDPreviewPage
  var layer: PSDNodeLayer = _

  val adjustX = 19
  val adjustY = 11
  val x = entry.getInt("left")
  val y = entry.getInt("top")
  val w = entry.getInt("width")
  val h = entry.getInt("height")
  val opacity = entry.getFloat("opacity")

  style.position := Position.Absolute
  style.left := (x + adjustX).px
  style.top := (y + adjustY).px
//  style.width := w.px
//  style.height := h.px
  style.zIndex := ZIndex(1000 - entry.getInt("index"))
  style.opacity := Opacity(entry.getDouble("opacity"))

  def entry: JsonValue
}

class PreviewText(val entry: JsonValue, val preview: PSDPreviewPage) extends tag.Div with PreviewNode {
  val text = entry.get("text")
  val value = text.getString("value")
  val font = text.get("font")
  val fontName = font.getString("name")
  val (fontFamilySimple, fontWeight) = fontName match {
    case s if s.indexOf('-') != -1 => (cleanFamily(s.substring(0, s.lastIndexOf('-'))), s.substring(s.lastIndexOf('-') + 1))
    case s => (cleanFamily(s), "Normal")
  }
  val fontFamily = fontFamilySimple match {
    case "Semibold" => "SemiBold"
    case s => s
  }
  val fontWeightValue = fontWeight.toLowerCase match {
    case "ultralight" => 100
    case "extralight" => 200
    case "light" => 300
    case "normal" | "regular" => 400
    case "medium" => 500
    case "semibold" => 600
    case "bold" => 700
    case "extrabold" => 800
    case "ultrabold" => 900
    case _ => 400
  }
  val fontSize = font.get("sizes").asIntArray()(0)
  val colorArray = font.get("colors").get(0).asIntArray()
  val color = Color.immutable(colorArray(0), colorArray(1), colorArray(2), colorArray(3) / 255.0)

  WebFontLoader(preview).google(List(s"$fontFamily:$fontWeightValue"))

  val transform = text.get("transform")

  val xx = math.round(transform.getDouble("xx")).toInt
  val yy = math.round(transform.getDouble("yy")).toInt

  style.left := (x + adjustX - xx).px
  style.top := (y + adjustY - yy).px
  style.fontFamily := fontFamily
  style.fontWeight := FontWeight(fontWeightValue.toString)
  style.fontSize := fontSize.px
  style.color := color
  style.lineHeight := h.px

  contents += value

  def cleanFamily(f: String) = f.replaceAll("([A-Z]{1})", " $1").trim
}

class PreviewImage(val entry: JsonValue, val preview: PSDPreviewPage) extends tag.Img with PreviewNode {
  val file = new File(preview.directory(), s"${entry.getString("name")}_${entry.getInt("index")}.png")
  src := EncodedImages.encode(file)
}