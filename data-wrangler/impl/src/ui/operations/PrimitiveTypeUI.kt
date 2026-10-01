package com.intellij.dataWrangler.impl.ui.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.annotations.PossibleValuesProvider
import com.intellij.dataWrangler.executor.CodePreviewProvider
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.DataWranglerBundle
import com.intellij.dataWrangler.impl.operations.DWTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc.Field
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc.FieldAccessor
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWVariantTypeDesc
import com.intellij.dataWrangler.impl.operations.MetaStructImpl
import com.intellij.dataWrangler.impl.operations.asCollectionType
import com.intellij.dataWrangler.impl.operations.findAnnotation
import com.intellij.dataWrangler.impl.operations.inherits
import com.intellij.dataWrangler.impl.ui.DWTableDataViewer
import com.intellij.dataWrangler.impl.ui.DataWranglerUiSession
import com.intellij.dataWrangler.impl.view.CodePreviewPanel
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.MultiSelectComboBox
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.launchOnShow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.jetbrains.annotations.Nls
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.event.ListSelectionListener
import javax.swing.event.PopupMenuEvent
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

class ParameterPanelUiBuilder<C : DataWranglerContext>(
  private val session: DataWranglerUiSession<C>,
) {
  private var isReadOnly: Boolean = false
  private val panelChanges = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  @RequiresEdt
  fun setReadOnly(readOnly: Boolean): ParameterPanelUiBuilder<C> {
    return this.apply { isReadOnly = readOnly }
  }

  private fun <P : Any, T : Any> Row.comboBoxFromPrimitiveMultiCollection(
    primitiveType: Field<P, out Collection<T>>, possibleValues: PossibleValuesProvider<T>, instance: () -> P,
  ) = cell(MultiSelectComboBox(possibleValues.getValues(session.backendSession.getContext()), { it.toString() }).apply {
    updateOnDataChanges {
      setItems(possibleValues.getValues(session.backendSession.getContext()))
    }
    val accessor = primitiveType.accessor
    setSelectedItems(accessor.get(instance()))
    addActionListener {
      setItems(instance, accessor, selectedItems)
      panelChanges.tryEmit(Unit)
    }
    isEnabled = !isReadOnly
  })

  private fun <P : Any, T : Any, C: Collection<T>> setItems(instance: () -> P, accessor: FieldAccessor<P, C>, v: Set<T>) {
    val clazz = accessor.type
    @Suppress("UNCHECKED_CAST")
    val cv = when (clazz) {
      List::class -> v.toList()
      Set::class -> v
      else -> throw AssertionError("Unsupported collection $clazz")
    } as C
    accessor.set(instance(), cv)
  }

  fun <P : Any> createPanel(commandFactory: CommandFactory<P, C>, params: P, struct: MetaStruct<P>, parentDisposable: Disposable): DialogPanel {
    val command = commandFactory.createCommand(params)
    return createPanel(commandFactory.commandName, command.getDescription(), struct, params, command, parentDisposable)
  }

  @RequiresEdt
  fun <P : Any> createPanel(commandTitle: @Nls String, commandDescription: @Nls String?, struct: MetaStruct<P>, params: P, command: DataWranglerCommand<C>, parentDisposable: Disposable): DialogPanel {
    // needs reference in makeCodePreview()
    var panel: DialogPanel = panel { }

    @OptIn(FlowPreview::class)
    fun makeCodePreview(row: Row, codePreviewProvider: CodePreviewProvider<C>, command: DataWranglerCommand<C>) {
      row.topGap(TopGap.MEDIUM)
      val codePreviewLabel = CodePreviewPanel(session.backendSession.getContext(), command, codePreviewProvider, parentDisposable)
      row.cell(codePreviewLabel).align(AlignX.FILL)
        .component.launchOnShow(this::class.java.name) {
          codePreviewLabel.updateCodePreview()
          // Dirty hack, temporary for code preview generation
          panelChanges.debounce(300).collect {
            panel.apply()
            codePreviewLabel.updateCodePreview()
          }
        }
      row.bottomGap(BottomGap.SMALL)
    }

    panel = panel {
      row {
        label(commandTitle).bold()

      }
      val description = commandDescription
      if (description != null) {
        row {
          comment(description)
          bottomGap(BottomGap.SMALL)
        }
      }
      buildStruct({ params }, (struct as MetaStructImpl).desc.fields)
      (session.backendSession.getCodePreviewProvider())?.let { codeProvider ->
        if (codeProvider.isCommandApplicable(command)) {
          row { makeCodePreview(this, codeProvider, command) }
        }
      }
    }

    return panel
  }

  private fun <P : Any, V : Any> Panel.buildVariants(params: () -> P, accessor: FieldAccessor<P, V>, desc: DWVariantTypeDesc<V>) {
    fun getVariant(): DWVariantTypeDesc.Item<out V> {
      val obj = accessor.get(params())
      return desc.variants.find { it.type == obj::class }!!
    }
    fun setVariant(v: DWVariantTypeDesc.Item<out V>) {
      if (getVariant() == v) return
      accessor.set(params(), v.type.createInstance())
    }
    val prop = AtomicProperty(getVariant())
    prop.afterChange {
      setVariant(it)
      panelChanges.tryEmit(Unit)
    }

    row {
      segmentedButton(desc.variants) {
        text = it.label
      }.bind(prop)
    }
    desc.variants.forEach { variant ->
      buildVariantsItem(params, accessor, variant, prop)
    }
  }

  private fun <P : Any, V : Any, V2: V> Panel.buildVariantsItem(
    params: () -> P,
    accessor: FieldAccessor<P, V>,
    variant: DWVariantTypeDesc.Item<V2>,
    prop: AtomicProperty<DWVariantTypeDesc.Item<out V>>,
  ) {
    panel {
      buildStruct(
        {
          val r = accessor.get(params())
          @Suppress("UNCHECKED_CAST")
          r.takeIf { it::class == variant.type } as V2? ?: variant.type.createInstance()
        },
        variant.desc.fields
      )
    }.visibleIf(prop.transform { it == variant })

  }

  private fun <P: Any, V: Any> FieldAccessor<P, V>.wrap(params: () -> P): () -> V =
    { get(params()) }

  private fun <P : Any> Panel.buildStruct(params: () -> P, fields: List<Field<P, *>>) {
    for (field in fields) {
      when (field.desc) {
        is DWStructTypeDesc -> {
          buildSubStruct(field, params)
        }
        is DWVariantTypeDesc -> {
          buildSubVariant(field, params)
        }
        else -> {
          if (field.tryCast(Boolean::class) == null) {
            row {
              label(parseParameterValueName(field)) //NON-NLS
            }
          }
          row {
            getDSLElement(field, params, this)/*.onChanged()*/.apply {
              align(AlignX.FILL)
              customize(UnscaledGaps(0, 0, 0, 0))
            }
            bottomGap(BottomGap.SMALL)
          }
        }
      }
    }
  }

  private fun <P: Any, V: Any> Panel.buildSubStruct(
    field: Field<P, V>,
    params: () -> P,
  ) {
    panel {
      buildStruct(field.accessor.wrap(params), (field.desc as DWStructTypeDesc).fields)
    }
  }

  private fun <P: Any, V: Any> Panel.buildSubVariant(
    field: Field<P, V>,
    params: () -> P,
  ) {
    panel {
      buildVariants(params, field.accessor, field.desc as DWVariantTypeDesc<V>)
    }
  }

  private fun Cell<*>.onChanged() = apply {
    onChanged {
      // TODO check condition
      panelChanges.tryEmit(Unit)
    }
  }

  private fun parseParameterValueName(value: Field<*, *>): @Nls String {
    return value.desc.defaultName ?: value.id
  }


  private fun <P : Any, T : Any> Row.createSetEditor(primitiveValue: Field<P, Collection<T>>, instance: () -> P): Cell<*> {
    val itemType = primitiveValue.desc.asCollectionType().itemType
    val possibleValues = itemType.possibleValues ?: run {
      val accessor = primitiveValue.accessor
      @Suppress("UNCHECKED_CAST")
      object: PossibleValuesProvider<T>(itemType.targetType ?: Any::class as KClass<T>) {
        private val values = accessor.get(instance()).toMutableSet()
        override fun getValues(context: DataWranglerContext?): List<T> {
          values.addAll(accessor.get(instance()))
          return values.toList()
        }
      }
      //throw AssertionError("Collection should have possible values for items")
    }

    @Suppress("UNCHECKED_CAST")
    return comboBoxFromPrimitiveMultiCollection(primitiveValue, possibleValues, instance)
  }

  private fun <P : Any, V : Any> getDSLElement(value: Field<P, V>, instance: () -> P, row: Row): Cell<*> {
    value.tryCast(Boolean::class)?.let { primitiveValue ->
      return row.createCheckBoxEditor(primitiveValue, instance).onChanged()
    }
    value.tryCast(Collection::class)?.let { collection ->
      return row.createSetEditor(collection as Field<P, Collection<Any>>, instance)
    }
    (value.desc as? DWTypeDesc.DWPrimitiveTypeDesc)?.possibleValues?.let { values ->
      return row.createComboBoxEditor(value, values, instance).onChanged()
    }
    value.tryCast(String::class)?.let { primitiveValue ->
      return row.createStringEditor(primitiveValue.accessor, instance).onChanged()
    }
    value.tryCast(Int::class)?.let { primitiveValue ->
      return row.createIntEditor(primitiveValue.accessor, instance).onChanged()
    }

    value.tryCast(Double::class)?.let { primitiveValue ->
      return row.createDoubleEditor(primitiveValue.accessor, instance).onChanged()
    }
    value.tryCast(Float::class)?.let { primitiveValue ->
      return row.createFloatEditor(primitiveValue.accessor, instance).onChanged()
    }
    throw AssertionError("Unsupported primitive type ${value.desc}")
  }

  private fun <P : Any> Row.createStringEditor(
    accessor: FieldAccessor<P, String>,
    instance: () -> P,
  ): Cell<*> = textField().bindText({ accessor.get(instance()) }, { accessor.set(instance(), it) })
    .applyToComponent {
      isEnabled = !isReadOnly
    }

  private fun <P : Any> Row.createIntEditor(
    accessor: FieldAccessor<P, Int>,
    instance: () -> P,
  ): Cell<*> = cell(IntegerField()).bindIntText({ accessor.get(instance()) }, { accessor.set(instance(), it) })
    .applyToComponent {
      isEnabled = !isReadOnly
    }

  private fun <P : Any> Row.createDoubleEditor(
    accessor: FieldAccessor<P, Double>,
    instance: () -> P,
  ): Cell<*> = doubleTextField().bindDouble(instance, accessor)
    .applyToComponent {
      isEnabled = !isReadOnly
    }

  private fun <P : Any> Row.createFloatEditor(
    accessor: FieldAccessor<P, Float>,
    instance: () -> P,
  ): Cell<*> = floatTextField().bindFloat(instance, accessor)
    .applyToComponent {
      isEnabled = !isReadOnly
    }

  private fun <P : Any> Row.createCheckBoxEditor(
    primitiveValue: Field<P, Boolean>,
    instance: () -> P,
  ): Cell<JBCheckBox> {
    @NlsSafe
    val text = "<html><p>${parseParameterValueName(primitiveValue)}</p></html>"
    val accessor = primitiveValue.accessor
    return checkBox(text)
      .bindSelected({ accessor.get(instance()) }, { accessor.set(instance(), it) })
      .applyToComponent { isEnabled = !isReadOnly }
  }

  private fun JComponent.updateOnDataChanges(updater: () -> Unit) =
    launchOnShow(ParameterPanelUiBuilder::class.jvmName) {
      updater()
      session.dataChanges.collectLatest {
        updater()
      }
    }
  private fun <P: Any, V: Any> Field<P, V>.isForColumn() = desc.inherits(DWTableColumn::class)

  private fun <P: Any, V: Any> Row.createComboBoxEditor(
    value: Field<P, V>,
    values: PossibleValuesProvider<V>,
    instance: () -> P,
  ): Cell<ComboBox<*>> {
    var initialList = values.getValues(session.backendSession.getContext())
    val accessor = value.accessor
    // A hack to display the current element chosen (which might not exist. Imagine a column chooser, and on the previous step the user dropped col and wants to see the operation).
    // When interactive history is implemented, we wouldn't need this.
    if (isReadOnly) initialList = initialList + accessor.get(instance())

    if(initialList.isEmpty()) {
      return createEmptyComboBoxEditor(value)
    }

    return createComboBoxEditor(value, values, instance, initialList)
  }

  private fun <P : Any, V : Any> Row.createComboBoxEditor(
    value: Field<P, V>,
    values: PossibleValuesProvider<V>,
    instance: () -> P,
    initialList: List<V>
  ): Cell<ComboBox<V>> {
    val displayName = (value.desc as? DWTypeDesc.DWPrimitiveTypeDesc<V>)?.displayName
    val renderer = if (displayName == null) null
    else object : SimpleListCellRenderer<V>() {
      override fun customize(list: JList<out V>, v: V?, index: Int, selected: Boolean, hasFocus: Boolean) {
        text = v?.let { displayName.getDisplayName(session.backendSession.getContext(), v) } ?: ""
      }
    }
    val model = CollectionComboBoxModel(initialList.toMutableList())
    return comboBox(model, renderer)
      .bindItem({  value.accessor.get(instance()) }, {
        if (it != null) {
          value.accessor.set(instance(), it)
        }
      })
      .apply {
        component.isEnabled = !isReadOnly
        component.updateOnDataChanges {
          model.replaceAll(values.getValues(session.backendSession.getContext()))
        }
        if (value.isForColumn()) {
          if (value.tryCast(String::class) != null) {
            val columnIntent = value.desc.findAnnotation(DWColumnIntent::class)?.intent ?: ColumnIntent.REFERENCE
            (component.asSafely<ComboBox<String>>())?.setUpColumnComboBox(session.tableViewer, columnIntent)
          }
        }
      }
  }

  private fun <P: Any, V: Any> Row.createEmptyComboBoxEditor(
    value: Field<P, V>,
  ): Cell<ComboBox<*>> {
    val listWithEmptyDescription: MutableList<String>
    val tooltipText: String
    if(value.isForColumn()) {
      listWithEmptyDescription = mutableListOf("<no column>")
      tooltipText = DataWranglerBundle.message("tooltip.transformations.operation.empty.columns")
    }
    else {
      listWithEmptyDescription = mutableListOf("<no value>")
      tooltipText = DataWranglerBundle.message("tooltip.transformations.operation.empty.value")
    }

    return comboBox(listWithEmptyDescription, textListCellRenderer { it }).apply {
      component.isEnabled = false
      component.toolTipText = tooltipText
    }
  }

}

private fun Row.doubleTextField(min: Double = 0.0, max: Double = Double.MAX_VALUE) =
  textField()
    .cellValidation {
      addInputRule(DataWranglerBundle.message("dialog.message.should.be.double")) { // TODO bundle
        val value = it.text.toDoubleOrNull()
        value == null || value < min || value > max
      }
    }

private fun <P : Any> Cell<JBTextField>.bindDouble(instance: () -> P, accessor: FieldAccessor<P, Double>): Cell<JBTextField> {
  onChanged { newValue ->
    accessor.set(instance(), newValue.text.toDoubleOrNull() ?: 0.0)
  }
  applyToComponent { text = accessor.get(instance()).toString() }
  return this
}

private fun Row.floatTextField(min: Float = 0.0f, max: Float = Float.MAX_VALUE) =
  textField()
    .cellValidation {
      addInputRule(DataWranglerBundle.message("dialog.message.should.be.float")) { // TODO bundle
        val value = it.text.toFloatOrNull()
        value == null || value < min || value > max
      }
    }

private fun <P : Any> Cell<JBTextField>.bindFloat(instance: () -> P, primitiveType: FieldAccessor<P, Float>): Cell<JBTextField> {
  onChanged { newValue ->
    primitiveType.set(instance(), newValue.text.toFloatOrNull() ?: 0.0f)
  }

  applyToComponent { text = primitiveType.get(instance()).toString() }

  return this
}

private fun ComboBox<String>.setUpColumnComboBox(tableViewer: DWTableDataViewer, columnIntent: ColumnIntent) = apply {
  addPopupMenuListener(object : PopupMenuListenerAdapter() {
    val listener = ListSelectionListener { e ->
      val myList = (e?.source as? JList<*>)
      myList?.onSelected(myList.selectedIndex)
    }

    private fun JList<*>.onSelected(idx: Int) {
      val colName = idx.takeIf { it >= 0 }?.let { model.getElementAt(it) }?.toString()
      if (colName != null)
        tableViewer.setColumnHighlighted(colName, columnIntent)
      else
        tableViewer.dropColumnHighlights()
    }

    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
      popup?.list?.apply {
        addListSelectionListener(listener)
        onSelected(selectedIndex)
      }
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
      popup?.list?.apply {
        removeListSelectionListener(listener)
        onSelected(-1)
      }
    }
  })
}

@Suppress("UNCHECKED_CAST")
fun <P : Any, V : Any, U: Any> Field<P, V>.tryCast(c: KClass<U>): Field<P, U>? =
  takeIf { it.desc.targetType?.isSubclassOf(c) == true } as Field<P, U>?

