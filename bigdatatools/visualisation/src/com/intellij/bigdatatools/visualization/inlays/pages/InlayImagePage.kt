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
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.io.path.name
import kotlin.math.min

class InlayImagePage(data: String) : InlayPage {

  override var indexInResults = -1

  override val title
    get() = VisMessagesBundle.message("page.title.image")

  override val contentType = InlayPageContentType.IMAGE

  private var image: Image? = null

  override val component: JComponent

  init {

    var errorMessage: String? = null

    try {
      // data - string with prefix and format
      // data:image/png;base64,iVBORw
      image = ImageIO.read(ByteArrayInputStream(Base64.getMimeDecoder().decode(data)))
    }
    catch (e: Exception) {
      errorMessage = e.message
      logger.error(e)
    }

    component = if (image == null) {
      val label = JLabel("<html>${VisMessagesBundle.message("image.loadFailed", errorMessage ?: "")}</html>")
      val panel = JPanel(GridBagLayout()).apply {
        add(label)
      }
      panel.preferredSize = label.preferredSize
      panel
    }
    else {
      val imageComponent = createImageComponent()

      val imgWidth = image!!.getWidth(null)
      val imgHeight = image!!.getHeight(null)
      val scale = imgWidth.toDouble() / imgHeight.toDouble()

      val preferredHeight = min(InlayDimensions.maxHeight, imgHeight)

      imageComponent.preferredSize = Dimension((preferredHeight * scale).toInt(), preferredHeight)

      imageComponent
    }
    component.isOpaque = false
  }

  private fun createImageComponent(): JComponent = object : JComponent() {
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
  }

  override fun getCollapsedDescription(): String? {
    val image = image
    return if (image == null) null else "Collapsed image: ${image.getWidth(null)}x${image.getHeight(null)}"
  }

  override fun createActions(): List<AnAction> {
    image ?: return emptyList()
    val actionSaveAsTxt = DumbAwareAction.create(VisMessagesBundle.message("image.exportAs.text"), AllIcons.Actions.MenuSaveall) {
      saveAs()
    }
    return listOf(actionSaveAsTxt)
  }

  private fun saveAs() {
    val image = image ?: return
    val descriptor = FileSaverDescriptor(VisMessagesBundle.message("image.exportAs.title"), VisMessagesBundle.message("image.exportAs.descr")).apply {
      withExtensionFilter(VisMessagesBundle.message("image.exportAs.label"), "png", "jpeg")
    }
    val chooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, component)
    val exportPath = InlaysSettings.getInstance().getImageExportPath(listOf("png", "jpeg"))
    val fileWrapper = chooser.save(exportPath.parent, exportPath.name) ?: return
    InlaysSettings.getInstance().imagesExportPath = fileWrapper.file.path

    ApplicationManager.getApplication().runWriteAction {
      try {
        ImageIO.write(toBufferedImage(image), fileWrapper.file.extension, fileWrapper.file)
      }
      catch (e: Exception) {
        invokeLater {
          Messages.showErrorDialog(this.component, e.message, VisMessagesBundle.message("image.exportFailed"))
        }
      }
    }
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    fun toBufferedImage(image: Image): BufferedImage {
      if (image is BufferedImage) {
        return image
      }

      @Suppress("UndesirableClassUsage")
      val bufferedImage = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB)

      val g = bufferedImage.createGraphics()
      g.drawImage(image, 0, 0, null)
      g.dispose()

      return bufferedImage
    }
  }
}
