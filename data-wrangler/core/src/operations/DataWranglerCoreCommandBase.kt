package com.intellij.dataWrangler.core.operations

import com.intellij.dataWrangler.core.engine.DataWranglerCoreContext
import com.intellij.dataWrangler.impl.operations.DataWranglerCommandBase
import com.intellij.dataWrangler.operations.CommandFactory

internal abstract class DataWranglerCoreCommandBase<P: Any>(factory: CommandFactory<P, DataWranglerCoreContext>, parameters: P) : DataWranglerCommandBase<P, DataWranglerCoreContext>(factory, parameters)