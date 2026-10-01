package com.intellij.dataWrangler.impl.operations

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.DataWranglerCommand

abstract class DataWranglerCommandBase<P: Any, C : DataWranglerContext>(val factory: CommandFactory<P, C>, val parameters: P): DataWranglerCommand<C>