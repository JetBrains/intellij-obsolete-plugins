package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.client.ClientSystemInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.ui.SpeedSearchComparator
import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.PlainDocument
import kotlin.math.max

/**
 * Special base class for speed search. Descendants are required to implement.
 * Class is a modified and simplified version of SpeedSearchBase.
 *
 *
 * fun findNextElement(s: String): Any?
 * fun findPreviousElement(s: String): Any?
 * fun findElement(s: String): Any?
 * fun findFirstElement(s: String): Any?
 * fun findLastElement(s: String): Any?
 *
 * This is suitable at least for WebView, when the search actually occur in WebView component and the speed search is
 * only an input field and handler of special buttons.
 */
abstract class SimpleSpeedSearch<Comp : JComponent>(val component: Comp?) : SpeedSearchSupply() {

  private var mySearchPopup: SearchPopup? = null
  private var myPopupLayeredPane: JLayeredPane? = null
  private val myWindowManagerListener = MyToolWindowManagerListener()
  private val myChangeSupport = PropertyChangeSupport(this)
  private var myRecentEnteredPrefix: String? = null
  private var comparator = SpeedSearchComparator(false)
  private var myClearSearchOnNavigateNoMatch: Boolean = false

  private var myListenerDisposable: Disposable? = null

  val searchField: JTextField?
    get() = if (mySearchPopup != null) {
      mySearchPopup!!.mySearchField
    }
    else null

  private val isSpeedSearchEnabled: Boolean
    get() = true

  private val componentVisibleRect: Rectangle
    get() = component!!.visibleRect

  private val componentLocationOnScreen: Point
    get() = component!!.locationOnScreen

  init {

    this.component!!.addComponentListener(object : ComponentAdapter() {
      override fun componentHidden(event: ComponentEvent?) {
        manageSearchPopup(null)
      }

      override fun componentMoved(event: ComponentEvent?) {
        moveSearchPopup()
      }

      override fun componentResized(event: ComponentEvent?) {
        moveSearchPopup()
      }
    })
    this.component.addFocusListener(object : FocusAdapter() {
      override fun focusLost(e: FocusEvent?) {
        manageSearchPopup(null)
      }
    })
    this.component.addKeyListener(object : KeyAdapter() {
      override fun keyTyped(e: KeyEvent?) {
        processKeyEvent(e!!)
      }

      override fun keyPressed(e: KeyEvent?) {
        processKeyEvent(e!!)
      }
    })

    object : AnAction() {
      override fun actionPerformed(e: AnActionEvent) {
        val prefix = enteredPrefix!!
        val strings = NameUtil.splitNameIntoWordList(prefix)
        val last = strings[strings.size - 1]
        val i = prefix.lastIndexOf(last)
        mySearchPopup!!.mySearchField.text = prefix.substring(0, i).trim { it <= ' ' }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = isPopupActive && !StringUtil.isEmpty(enteredPrefix)
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }.registerCustomShortcutSet(CustomShortcutSet.fromString(if (ClientSystemInfo.isMac()) "meta BACK_SPACE" else "control BACK_SPACE"),
                                this.component)

    installSupplyTo(component)
  }

  fun setClearSearchOnNavigateNoMatch(clearSearchOnNavigateNoMatch: Boolean) {
    myClearSearchOnNavigateNoMatch = clearSearchOnNavigateNoMatch
  }

  override fun isPopupActive(): Boolean {
    return mySearchPopup != null && mySearchPopup!!.isVisible
  }


  override fun matchingFragments(text: String): Iterable<TextRange>? {
    if (!isPopupActive) return null
    val comparator = comparator
    val recentSearchText = comparator.recentSearchText
    return if (StringUtil.isNotEmpty(recentSearchText)) comparator.matchingFragments(recentSearchText, text) else null
  }

  /*
   * @param element Element to select. Don't forget to convert model index to view index if needed (i.e. table.convertRowIndexToView(modelIndex), etc).
   * @param selectedText search text
   */
  // protected abstract fun selectElement(element:Any?, selectedText:String)


  override fun addChangeListener(listener: PropertyChangeListener) {
    myChangeSupport.addPropertyChangeListener(listener)
  }

  override fun removeChangeListener(listener: PropertyChangeListener) {
    myChangeSupport.removePropertyChangeListener(listener)
  }

  private fun fireStateChanged() {
    val enteredPrefix = enteredPrefix
    myChangeSupport.firePropertyChange(ENTERED_PREFIX_PROPERTY_NAME, myRecentEnteredPrefix, enteredPrefix)
    myRecentEnteredPrefix = enteredPrefix
  }

  protected fun compare(text: String, pattern: String?): Boolean {
    return pattern != null && comparator.matchingFragments(pattern, text) != null
  }

  abstract fun findNextElement(s: String): Any?

  abstract fun findPreviousElement(s: String): Any?

  abstract fun findElement(s: String): Any?

  abstract fun findFirstElement(s: String): Any?

  abstract fun findLastElement(s: String): Any?

  @JvmOverloads
  fun showPopup(searchText: String = "") {
    manageSearchPopup(SearchPopup(searchText))
  }

  fun hidePopup() {
    manageSearchPopup(null)
  }

  protected fun processKeyEvent(e: KeyEvent) {
    if (e.isAltDown) return
    if (e.isShiftDown && isNavigationKey(e.keyCode)) return
    if (mySearchPopup != null) {
      mySearchPopup!!.processKeyEvent(e)
      return
    }
    if (!isSpeedSearchEnabled) return
    if (e.id == KeyEvent.KEY_TYPED) {
      if (!UIUtil.isReallyTypedEvent(e)) return

      val c = e.keyChar
      if (Character.isLetterOrDigit(c) || !Character.isWhitespace(c) && SpeedSearch.PUNCTUATION_MARKS.indexOf(c) != -1) {
        manageSearchPopup(SearchPopup(c.toString()))
        e.consume()
      }
    }
  }

  override fun getEnteredPrefix(): String? {
    return if (mySearchPopup != null) mySearchPopup!!.mySearchField.text else null
  }

  override fun refreshSelection() {
    if (mySearchPopup != null) mySearchPopup!!.refreshSelection()
  }

  override fun findAndSelectElement(searchQuery: String) {
    //        selectElement(findElement(searchQuery), searchQuery)
  }
  //
  //    fun adjustSelection(keyCode:Int, searchQuery:String):Boolean {
  //        if (isUpDownHomeEnd(keyCode))
  //        {
  //            val element = findTargetElement(keyCode, searchQuery)
  //            if (element != null)
  //            {
  //                selectElement(element, searchQuery)
  //                return true
  //            }
  //        }
  //        return false
  //    }

  private fun findTargetElement(keyCode: Int, searchPrefix: String): Any? {
    when (keyCode) {
      KeyEvent.VK_UP -> return findPreviousElement(searchPrefix)
      KeyEvent.VK_DOWN -> return findNextElement(searchPrefix)
      KeyEvent.VK_HOME -> return findFirstElement(searchPrefix)
      else -> {
        assert(keyCode == KeyEvent.VK_END)
        return findLastElement(searchPrefix)
      }
    }
  }

  private inner class SearchPopup(initialString: String) : JPanel() {
    val mySearchField = SearchField()

    init {
      val searchLabel = JLabel(" " + VisMessagesBundle.message("search.popup.search.for.label") + " ")
      searchLabel.font = searchLabel.font.deriveFont(Font.BOLD)
      searchLabel.foreground = FOREGROUND_COLOR
      mySearchField.border = null
      mySearchField.background = BACKGROUND_COLOR
      mySearchField.foreground = FOREGROUND_COLOR

      mySearchField.document = object : PlainDocument() {
        @Throws(BadLocationException::class)
        override fun insertString(offs: Int, str: String?, a: AttributeSet?) {
          var oldText: String
          try {
            oldText = getText(0, length)
          }
          catch (e1: BadLocationException) {
            oldText = ""
          }

          val newText = oldText.substring(0, offs) + str + oldText.substring(offs)
          super.insertString(offs, str, a)
          if (findElement(newText) == null) {
            mySearchField.setForeground(ERROR_FOREGROUND_COLOR)
          }
          else {
            mySearchField.setForeground(FOREGROUND_COLOR)
          }
        }
      }
      mySearchField.text = initialString

      border = BORDER
      background = BACKGROUND_COLOR
      layout = BorderLayout()
      add(searchLabel, BorderLayout.WEST)
      add(mySearchField, BorderLayout.EAST)
      val element = findElement(mySearchField.text)
      onSearchFieldUpdated(initialString)
      updateSelection(element)
    }

    public override fun processKeyEvent(e: KeyEvent) {
      mySearchField.processKeyEvent(e)
      if (e.isConsumed) {
        val s = mySearchField.text
        onSearchFieldUpdated(s)
        val keyCode = e.keyCode
        var element: Any?
        if (isUpDownHomeEnd(keyCode)) {
          element = findTargetElement(keyCode, s)
          if (myClearSearchOnNavigateNoMatch && element == null) {
            manageSearchPopup(null)
            element = findTargetElement(keyCode, "")
          }
        }
        else {
          element = findElement(s)
        }
        updateSelection(element)
      }
    }

    fun refreshSelection() {
      findAndSelectElement(mySearchField.text)
    }

    private fun updateSelection(element: Any?) {
      if (element != null) {
        // selectElement(element, mySearchField.text)
        mySearchField.setForeground(FOREGROUND_COLOR)
      }
      else {
        mySearchField.setForeground(ERROR_FOREGROUND_COLOR)
      }
      if (mySearchPopup != null) {
        mySearchPopup!!.size = mySearchPopup!!.preferredSize
        mySearchPopup!!.validate()
      }

      fireStateChanged()
    }
  }

  protected fun onSearchFieldUpdated(pattern: String) {}

  private inner class SearchField : JTextField() {
    init {
      isFocusable = false
    }

    override fun getPreferredSize(): Dimension {
      val dim = super.getPreferredSize()
      val m = margin
      dim.width = getFontMetrics(font).stringWidth(text) + 10 + m.left + m.right
      return dim
    }

    /**
     * I made this method public in order to be able to call it from the outside.
     * This is needed for delegating calls.
     */
    public override fun processKeyEvent(e: KeyEvent) {
      val i = e.keyCode
      if (i == KeyEvent.VK_BACK_SPACE && document.length == 0) {
        e.consume()
        return
      }
      if (i == KeyEvent.VK_ENTER ||
          i == KeyEvent.VK_ESCAPE ||
          i == KeyEvent.VK_PAGE_UP ||
          i == KeyEvent.VK_PAGE_DOWN ||
          i == KeyEvent.VK_LEFT ||
          i == KeyEvent.VK_RIGHT) {
        manageSearchPopup(null)
        if (i == KeyEvent.VK_ESCAPE) {
          e.consume()
        }
        return
      }

      if (isUpDownHomeEnd(i)) {
        e.consume()
        return
      }

      super.processKeyEvent(e)
      if (i == KeyEvent.VK_BACK_SPACE) {
        e.consume()
      }
    }
  }

  private fun manageSearchPopup(searchPopup: SearchPopup?) {
    var project: Project? = null
    if (ApplicationManager.getApplication() != null && !ApplicationManager.getApplication().isDisposed) {
      project = DataManager.getInstance().getDataContext(component).getData(CommonDataKeys.PROJECT)
    }
    if (project != null && project.isDefault) {
      project = null
    }
    if (mySearchPopup != null) {
      if (myPopupLayeredPane != null) {
        myPopupLayeredPane!!.remove(mySearchPopup!!)
        myPopupLayeredPane!!.validate()
        myPopupLayeredPane!!.repaint()
        myPopupLayeredPane = null
      }

      if (myListenerDisposable != null) {
        Disposer.dispose(myListenerDisposable!!)
        myListenerDisposable = null
      }
    }
    else if (searchPopup != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed("ui.tree.speedsearch")
    }

    mySearchPopup = if (component!!.isShowing) searchPopup else null

    fireStateChanged()

    //select here!

    if (mySearchPopup == null || !component.isDisplayable) return

    if (project != null) {
      myListenerDisposable = Disposer.newDisposable()
      project.messageBus.connect(myListenerDisposable!!).subscribe(ToolWindowManagerListener.TOPIC, myWindowManagerListener)
    }
    val rootPane = component.rootPane
    myPopupLayeredPane = if (rootPane == null) null else rootPane.layeredPane
    if (myPopupLayeredPane == null) {
      LOG.error((this).toString() + " in " + component)
      return
    }
    myPopupLayeredPane!!.add(mySearchPopup!!, JLayeredPane.POPUP_LAYER)
    moveSearchPopup()

    mySearchPopup!!.refreshSelection()
  }

  private fun moveSearchPopup() {
    if (component == null || mySearchPopup == null || myPopupLayeredPane == null) return
    val lPaneP = myPopupLayeredPane!!.locationOnScreen
    val componentP = componentLocationOnScreen
    val r = componentVisibleRect
    val prefSize = mySearchPopup!!.preferredSize
    val window = SwingUtilities.getAncestorOfClass(Window::class.java, component) as Window
    val windowP: Point
    when (window) {
      is JDialog -> windowP = window.contentPane.locationOnScreen
      is JFrame -> windowP = window.contentPane.locationOnScreen
      else -> windowP = window.locationOnScreen
    }
    var y = r.y + componentP.y - lPaneP.y - prefSize.height
    y = max(y, windowP.y - lPaneP.y)
    mySearchPopup!!.setLocation(componentP.x - lPaneP.x + r.x, y)
    mySearchPopup!!.size = prefSize
    mySearchPopup!!.isVisible = true
    mySearchPopup!!.validate()
  }

  private inner class MyToolWindowManagerListener : ToolWindowManagerListener {
    override fun stateChanged() {
      manageSearchPopup(null)
    }
  }

  companion object {
    private val LOG = Logger.getInstance("#com.intellij.ui.SpeedSearchBase")

    private val BORDER = CustomLineBorder(BORDER_COLOR, JBUI.insets(1))

    fun hasActiveSpeedSearch(component: JComponent): Boolean {
      return SpeedSearchSupply.getSupply(component) != null
    }

    private fun isUpDownHomeEnd(keyCode: Int): Boolean {
      return keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN
    }

    private fun isPgUpPgDown(keyCode: Int): Boolean {
      return keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN
    }

    private fun isNavigationKey(keyCode: Int): Boolean {
      return isPgUpPgDown(keyCode) || isUpDownHomeEnd(keyCode)
    }
  }
}

