package com.intellij.bigdatatools.zeppelin.constants

import com.intellij.bigdatatools.jupyter.icons.BigdatatoolsJupyterIcons
import com.intellij.bigdatatools.zeppelin.icons.BigdatatoolsZeppelinIcons
import com.intellij.icons.AllIcons
import javax.swing.Icon

object ZeppelinIcons {
  @JvmField
  val INTERPRETER_BINDINGS: Icon = AllIcons.Actions.Properties

  @JvmField
  val CLEAR_OUTPUTS: Icon = BigdatatoolsJupyterIcons.ClearOutputs

  @JvmField
  val STOP_EXECUTION: Icon = AllIcons.Actions.Suspend

  @JvmField
  val ZEPPELIN_FILE: Icon = BigdatatoolsZeppelinIcons.ZeppelinFile

  @JvmField
  val TRASH_ICON: Icon = AllIcons.Actions.GC

  @JvmField
  val ZEPPELIN: Icon = BigdatatoolsZeppelinIcons.Zeppelin

  @JvmField
  val RUN_ALL: Icon = AllIcons.Actions.RunAll

  @JvmField
  val INTERACTIVE_NOTE_ICON: Icon = BigdatatoolsZeppelinIcons.InteractiveNote

  @JvmField
  val INSERT_ROW_ABOVE: Icon = BigdatatoolsZeppelinIcons.InsertRowAbove

  @JvmField
  val INSERT_ROW_BELOW: Icon = BigdatatoolsZeppelinIcons.InsertRowBelow
}