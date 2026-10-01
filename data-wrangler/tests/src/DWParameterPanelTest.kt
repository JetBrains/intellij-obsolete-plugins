package com.intellij.dataWrangler

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.impl.DWTableDataViewerImpl
import com.intellij.dataWrangler.impl.DataWranglerUiSessionImpl
import com.intellij.dataWrangler.impl.service.DataWranglerService
import com.intellij.dataWrangler.impl.ui.DataWranglerUiSession
import com.intellij.dataWrangler.impl.view.transformation.ParameterPanel
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.database.datagrid.CachedGridDataHookUp
import com.intellij.database.datagrid.DataConsumer
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.DataGridListModel
import com.intellij.database.datagrid.DataGridUtil
import com.intellij.database.run.ui.DataAccessType
import com.intellij.database.run.ui.grid.editors.GridCellEditorHelper
import com.intellij.database.testFramework.dbTestDataHelper
import com.intellij.ide.impl.HeadlessDataManager.fallbackToProductionDataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.use
import com.intellij.scientific.tables.api.DSDataFrameInfo
import com.intellij.scientific.tables.api.DSTableDataType
import com.intellij.scientific.tables.panel.UpdateOrCreateTableContentUtils.forceTableCreationInHeadlessMode
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.UsefulTestCase.assertSameLinesWithFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.dsl.builder.components.SegmentedButtonComponent
import com.intellij.ui.dsl.gridLayout.Constraints
import com.intellij.ui.dsl.gridLayout.Grid
import com.intellij.ui.dsl.gridLayout.GridLayout
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import java.awt.Component
import java.awt.Container
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.text.JTextComponent

@TestDataPath("\$PROJECT_ROOT/plugins/data-wrangler/tests/testData/parametersUi")
@TestApplication
class DWParameterPanelTest {
  companion object {
    val project = projectFixture(openAfterCreation = true)
    val testData = dbTestDataHelper<DWParameterPanelTest>()
  }

  lateinit var disposable: Disposable

  @Test
  fun checkComponents() {
    timeoutRunBlocking(context = Dispatchers.EDT) {
      Disposer.newDisposable("test").use {
        disposable = it
        fallbackToProductionDataManager(disposable)
        forceTableCreationInHeadlessMode(disposable)
        val pythonEngine = DataWranglerEngine.EP.extensionList.find { DWParameterPanelTestUtil.isPythonEngine(it) }!!
        checkEngineComponents(pythonEngine)
      }
    }
  }

  fun <C: DataWranglerContext> checkEngineComponents(engine: DataWranglerEngine<C>) {
    val res = StringBuilder()
    res.dumpEngine(engine)
    assertSameLinesWithFile(testData.resolve(engine.javaClass.simpleName + ".ui.txt").toString(), res.toString())
  }

  private fun <C : DataWranglerContext> StringBuilder.dumpEngine(engine: DataWranglerEngine<C>) {
    val model = DWParameterPanelTestUtil.createTestGridModel()
    val session = DWParameterPanelTestUtil.createSession(project.get(), engine, model, disposable)
    engine.commandsFactories().value.groupBy { it.getGroupName() }.forEach { (g, factories) ->
      DWParameterPanelTestUtil.dumpGroup(session, g, factories, this, 0)
    }
  }
}

object DWParameterPanelTestUtil {

  fun <C : DataWranglerContext> createSession(project: Project, engine: DataWranglerEngine<C>, model: DataGridListModel, disposable: Disposable): DataWranglerUiSessionImpl<C> {
    val grid = createGrid(project, model, disposable)
    val context = createContext(engine, grid)
    val session = project.service<DataWranglerService>().createDWSession(engine, context)
    Disposer.register(disposable) {
      session.dispose()
    }
    val tableViewer = DWTableDataViewerImpl(grid)
    return DataWranglerUiSessionImpl(session, tableViewer).apply {
      Disposer.register(disposable, this)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <C : DataWranglerContext> createContext(engine: DataWranglerEngine<C>, grid: DataGrid): C = when  {
    isPythonEngine(engine) -> createPythonContext(grid)
    else -> throw AssertionError("Unsupported engine $engine")
  } as C

  fun isPythonEngine(engine: DataWranglerEngine<*>): Boolean =
    engine.id == PythonDataWranglerEngineUtils.PYTHON_ENGINE_ID

  private fun createPythonContext(grid: DataGrid): PythonDataWranglerContext = object : PythonDataWranglerContext {
    override suspend fun executeCommand(commandCode: String) {
      throw UnsupportedOperationException()
    }

    override fun getDSDataFrameInfo(): DSDataFrameInfo = with(grid.getDataModel(DataAccessType.DATABASE_DATA)) {
      DSDataFrameInfo(
        rowCount, 1,
        columns.map { it.name },
        columns.map { it.typeName },
        null,
        DSTableDataType.PANDAS_DATA_FRAME,
        null,
        null,
        null
      )
    }

    override fun getProject(): Project = grid.project

    override fun getTableName(): String = "table_name"

    override fun dispose() {}
  }

  private fun createGrid(project: Project, model: DataGridListModel, disposable: Disposable): DataGrid =
    DataGridUtil.createDataGrid(project, CachedGridDataHookUp(project, model), DefaultActionGroup()).also {
      Disposer.register(disposable, it)
    }

  private fun createGridModel(
    columns: List<Pair<String, String>>,
    vararg rows: Array<Any>,
  ): DataGridListModel {
    val model = DataGridListModel(GridCellEditorHelper::areValuesEqual)
    model.columns = (listOf("rownum" to "int64") + columns).mapIndexed { idx, (name, type) ->
      DataConsumer.Column(idx, name, 0, type, null)
    }
    model.addRows(rows.mapIndexed { idx, row ->
      DataConsumer.Row.create(idx + 1, row)
    })
    return model
  }

  fun createTestGridModel(): DataGridListModel = createGridModel(
    listOf(
      "column 0" to "object",
      "column 1" to "int64",
      "column 2" to "int64",
    ),
    arrayOf(0, 1.0, 1.1, 1.001),
    arrayOf(1, 2.0, 2.2, 2.002),
    arrayOf(2, 3.0, 3.3, 3.003),
  )

  fun <C : DataWranglerContext> dumpGroup(
    session: DataWranglerUiSession<C>,
    g: CommandFactoryGroup,
    factories: List<CommandFactory<*, C>>,
    res: StringBuilder,
    indent: Int
  ) {
    factories.sortedBy { it.commandName }.forEach {
      res.dumpFactory(session, g, it, indent)
    }
  }

  private fun <C : DataWranglerContext> StringBuilder.dumpFactory(session: DataWranglerUiSession<C>, g: CommandFactoryGroup, factory: CommandFactory<*, C>, indent: Int) {
    indent(indent).append(g.displayName).append(" > ").append(factory.commandName).append("\n")
    ParameterPanel(factory, session).use { panel ->
      indent(indent).apply { StringUtil.repeatSymbol(this, '-', 20) }.append("\n")
      renderRootComponent(panel.getPanel(), indent + 1)
      indent(indent).apply { StringUtil.repeatSymbol(this, '-', 20) }.append("\n\n")
    }
  }

  private fun StringBuilder.renderComponent(c: Component, indent: Int) {
    when (c) {
      is JComboBox<*> -> renderComboBox(indent, c)
      is JTextComponent -> renderTextField(indent, c)
      is JCheckBox -> renderCheckBox(indent, c)
      is JLabel -> renderLabel(indent, c)
      is SeparatorComponent -> Unit
      is SegmentedButtonComponent<*> -> renderSegmentedButton(indent, c)
      else -> renderComponentChildren(c, indent, false)
    }
  }

  private fun StringBuilder.renderLabel(indent: Int, c: JLabel) {
    indent(indent).append(StringUtil.removeHtmlTags(c.text)).append("\n")
  }

  private fun StringBuilder.renderCheckBox(indent: Int, c: JCheckBox) {
    indent(indent).append("\u2714 ").append(StringUtil.removeHtmlTags(c.text)).append("\n")
  }

  private fun StringBuilder.renderTextField(indent: Int, c: JTextComponent) {
    if (!c.isEditable) return
    indent(indent).append("|textField(${c.text})|").append("\n")
  }

  private fun StringBuilder.renderComboBox(indent: Int, c: JComboBox<*>) {
    indent(indent).append("|comboBox\u2304|").append("\n").apply {
      renderComboBoxEntries(c, indent + 2)
    }.append("\n")
  }

  private fun StringBuilder.renderSegmentedButton(indent: Int, c: SegmentedButtonComponent<*>) {
    val buttons = UIUtil.uiTraverser(c).mapNotNull { it as? ActionButtonWithText }
    indent(indent).append("|").append(buttons.joinToString("|") { it.presentation.text }).append("|").append("\n")
  }

  private fun StringBuilder.renderRootComponent(component: DialogPanel, indent: Int) {
    renderComponentChildren(component, indent, true)
  }
  private fun StringBuilder.renderComponentChildren(component: Component, indent: Int, root: Boolean) {
    if (component.javaClass.name.contains("CodePreviewPanel")) return
    if (component is Container && component.componentCount != 0) {
      //indent(indent).append(component.toString()).append("\n")
      val components = component.components.asSequence().run {
        if (root) drop(1) //drop label that duplicates header
        else this
      }
      //val segButton = components.find { it is SegmentedButtonComponent<*> }
      //if (segButton != null) {
      //  renderComponent(segButton, indent)
      //  components.filter { it != segButton }.forEach {
      //    append("<<<<<<<<<<<<<<<<<<\n")
      //    renderComponent(it, indent)
      //  }
      //}
      //else {
      val gridLayout = component.layout as? GridLayout
      if (gridLayout != null) {
        renderGridComponents(gridLayout, components, indent)
      }
      else {
        renderComponents(components, indent)
      }
      //}
    }
    else {
      indent(indent).append(component.toString()).append("\n")
    }
  }

  private fun StringBuilder.renderComponents(components: Sequence<Component>, indent: Int) {
    components.forEach {
      renderComponent(it, indent)
    }
  }

  private fun StringBuilder.renderGridComponents(layout: GridLayout, components: Sequence<Component>, indent: Int) {
    renderGridComponents(layout.rootGrid, buildGridGridHierarchy(components, layout), indent)
  }

  private fun buildGridGridHierarchy(
    components: Sequence<Component>,
    layout: GridLayout,
  ): (Grid) -> List<Any> {
    val grids = mutableMapOf<Grid, MutableList<Pair<Any, Constraints>>>()
    components.mapNotNull { it as? JComponent }.forEach {
      addGrid(layout, grids, layout.getConstraints(it), it)
    }
    grids.values.forEach { g ->
      g.sortWith(Comparator.comparing<Pair<Any, Constraints>, Int> { it.second.y }.thenBy { it.second.x })
    }
    return { g -> grids[g]!!.map { it.first } }
  }

  private fun StringBuilder.renderGridComponents(grid: Grid, hierarchy: (Grid) -> List<Any>, indent: Int) {
    val items = hierarchy(grid)
    val isSegmented = items.first().let { it is Grid && hierarchy(it).singleOrNull() is SegmentedButtonComponent<*> }
    items.forEachIndexed { index, item ->
      when (item) {
        is Component -> renderComponent(item, indent)
        is Grid -> {
          if (isSegmented && index != 0) {
            indent(indent).append(if (index == 1) "<<<<<<<<<<<<<<<<<<\n" else "==================\n")
            renderGridComponents(item, hierarchy, indent + 1)
            if (index == items.lastIndex) indent(indent).append(">>>>>>>>>>>>>>>>>>\n")
          }
          else {
            renderGridComponents(item, hierarchy, indent)
          }
        }
      }
    }
  }

  private fun addGrid(
    layout: GridLayout,
    grids: MutableMap<Grid, MutableList<Pair<Any, Constraints>>>,
    constraints: Constraints?,
    container: Any,
  ) {
    if (constraints == null) return
    var gridData = grids[constraints.grid]
    if (gridData == null) {
      gridData = mutableListOf()
      grids[constraints.grid] = gridData
      addGrid(layout, grids, layout.getConstraints(constraints.grid), constraints.grid)
    }
    gridData.add(Pair(container, constraints))
  }


  private fun <E> StringBuilder.renderComboBoxEntries(c: JComboBox<E>, indent: Int) {
    val tmp = JList(c.model)
    (0..<c.model.size).forEach {
      val c = c.renderer.getListCellRendererComponent(tmp, c.model.getElementAt(it), it, false, false)
      renderComponent(c, indent)
    }
  }

  private fun StringBuilder.indent(w: Int): StringBuilder = apply {
    StringUtil.repeatSymbol(this, ' ', w * 2)
  }
}