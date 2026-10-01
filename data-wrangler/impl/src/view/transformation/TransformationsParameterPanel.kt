package com.intellij.dataWrangler.impl.view.transformation

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.operations.setDefaultsFromPossibleValues
import com.intellij.dataWrangler.impl.ui.DataWranglerUiSession
import com.intellij.dataWrangler.impl.ui.operations.ParameterPanelUiBuilder
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer

/**
 * Builds a parameter panel out of [commandFactory] and params.
 * If no params is provided, it creates default params from [commandFactory]
 */
class ParameterPanel<P : Any, C : DataWranglerContext>(
  private val commandFactory: CommandFactory<P, C>,
  private val params: P,
  private val session: DataWranglerUiSession<C>,
  private val readOnly: Boolean = false,
) : Disposable.Default {

  constructor(commandFactory: CommandFactory<P, C>, session: DataWranglerUiSession<C>, readOnly: Boolean = false)
    : this(commandFactory, commandFactory.parametersMetaType.getNewInstance(), session, readOnly) {
    commandFactory.parametersMetaType.setDefaultsFromPossibleValues(params, session.backendSession.getContext())
    commandFactory.initDefaultParameters(session.backendSession.getContext(), params)
  }

  // for code preview, we use a dirty hack, namely panel.apply(), so we need to initialize myPanel to avoid null apply.
  private var myPanel: DialogPanel

  init {
    myPanel = createPanel()
    Disposer.register(session, this)
  }

  fun getPanel(): DialogPanel = myPanel
  private fun createPanel(): DialogPanel {
    val parameterPanel = ParameterPanelUiBuilder(session)
      .setReadOnly(readOnly)
    return parameterPanel.createPanel(commandFactory, params, commandFactory.parametersMetaType, this@ParameterPanel).apply {
      registerValidators(this@ParameterPanel)
    }
  }

  fun createTransformationStep(): TransformationStep<P, C> = TransformationStep(commandFactory, params)
}