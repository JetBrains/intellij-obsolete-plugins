package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.visualization.inlays.InlayDimensions
import com.intellij.bigdatatools.visualization.inlays.settings.InlaysSettings
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.scale.JBUIScale
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.RenderingHints
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.io.path.name
import kotlin.math.min

class InlaySvgImagePage(private val data: String) : InlayPage {
  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }

  override var indexInResults = -1

  override val title
    get() = VisMessagesBundle.message("page.title.image")

  override val contentType = InlayPageContentType.IMAGE

  private var image: Image? = null

  private var originalImageWidth = 0f
  private var originalImageHeight = 0f

  // To prevent recreation of svg on component move.
  private var lastScale = -1f

  override val component: JComponent

  init {

    // We need to get unit type of svg image to properly convert it to pixels
    //     SVGLength.SVG_LENGTHTYPE_MM -> v / ctx.getPixelUnitToMillimeter()
    //     SVGLength.SVG_LENGTHTYPE_CM -> v * 10f / ctx.getPixelUnitToMillimeter()
    //     SVGLength.SVG_LENGTHTYPE_IN -> v * 25.4f / ctx.getPixelUnitToMillimeter()
    //     SVGLength.SVG_LENGTHTYPE_PT -> v * 25.4f / (72f * ctx.getPixelUnitToMillimeter())
    //     SVGLength.SVG_LENGTHTYPE_PC -> v * 25.4f / (6f * ctx.getPixelUnitToMillimeter())
    var errorMessage: String? = null

    try {
      reinitImage(JBUIScale.sysScale())
    }
    catch (e: Exception) {
      errorMessage = e.message
      logger.error(e)
    }

    component = if (image == null) {
      JPanel(GridBagLayout()).apply {
        add(JLabel(errorMessage))
      }
    }
    else {
      val imageComponent = createImageComponent()
      imageComponent.preferredSize = Dimension(imageComponent.preferredSize.width, min(InlayDimensions.maxHeight, image!!.getHeight(null)))
      imageComponent
    }

    component.isOpaque = false
  }

  private fun createImageComponent(): JComponent {

    return object : JComponent() {
      override fun paintComponent(g: Graphics) {
        if (width <= 0 || height <= 0) return
        val image = image ?: return

        val imgWidth = image.getWidth(null)
        val imgHeight = image.getHeight(null)

        // To preserve proportions.
        val scale = min(width.toDouble() / imgWidth, height.toDouble() / imgHeight)

        val g2 = g.create() as Graphics2D
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
          g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
          g2.drawImage(image, 0, 0, (imgWidth * scale).toInt(), (imgHeight * scale).toInt(), 0, 0, imgWidth, imgHeight, null)
        }
        finally {
          g2.dispose()
        }
      }

      override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        if (width <= 0 || height <= 0) return
        image?.let {
          reinitImage(min(width.toFloat() / originalImageWidth, height.toFloat() / originalImageHeight) * JBUIScale.sysScale())
        }
      }
    }
  }

  private fun reinitImage(scale: Float = 1f) {
    lastScale = scale
  }

  override fun getCollapsedDescription(): String? {
    val image = this.image
    return if (image == null) null else "Collapsed SVG image: ${image.getWidth(null)}x${image.getHeight(null)}"
  }

  override fun createActions(): List<AnAction> {
    val actionSaveAsTxt = DumbAwareAction.create(VisMessagesBundle.message("image.exportAs.text"), AllIcons.Actions.MenuSaveall) {
      saveAs()
    }
    return listOf(actionSaveAsTxt)
  }

  private fun saveAs() {
    val image = image ?: return
    val descriptor = FileSaverDescriptor(VisMessagesBundle.message("image.exportAs.title"), VisMessagesBundle.message("image.exportAs.descr")).apply {
      withExtensionFilter(VisMessagesBundle.message("image.exportAs.label"), "svg", "png", "jpeg")
    }
    val chooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, component)
    val exportPath = InlaysSettings.getInstance().getImageExportPath(listOf("svg", "png", "jpeg"))
    val fileWrapper = chooser.save(exportPath.parent, exportPath.name) ?: return
    InlaysSettings.getInstance().imagesExportPath = fileWrapper.file.path

    ApplicationManager.getApplication().runWriteAction {
      try {
        if (fileWrapper.file.extension == "svg") {
          fileWrapper.file.bufferedWriter().use { out ->
            out.write(data)
          }
        }
        else {
          ImageIO.write(InlayImagePage.toBufferedImage(image), fileWrapper.file.extension, fileWrapper.file)
        }
      }
      catch (e: Exception) {
        invokeLater {
          Messages.showErrorDialog(this.component, e.message, VisMessagesBundle.message("image.exportFailed"))
        }
      }
    }
  }
}
