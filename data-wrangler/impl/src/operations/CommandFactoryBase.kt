package com.intellij.dataWrangler.impl.operations

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.Nls
import java.util.function.Supplier
import kotlin.reflect.KClass

abstract class CommandFactoryBase<P : Any, C : DataWranglerContext>(parametersClass: KClass<P>, private val commandNamePtr: Supplier<@Nls String>): CommandFactory<P, C> {
  final override val commandName: @NlsSafe String
    get() = commandNamePtr.get()
  final override val parametersMetaType: MetaStruct<P> = toMetaStructure(parametersClass)
}