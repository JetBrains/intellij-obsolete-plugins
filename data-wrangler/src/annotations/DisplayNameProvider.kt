package com.intellij.dataWrangler.annotations

import com.intellij.dataWrangler.executor.DataWranglerContext
import org.jetbrains.annotations.Nls
import java.util.function.Supplier
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValueDisplayNameProvider(val value: KClass<out DisplayNameProvider<*>>)

abstract class DisplayNameProvider<T: Any>(val targetType: KClass<T>) {
  @Nls
  abstract fun getDisplayName(context: DataWranglerContext, value: T): String
}

// TODO: change
interface DisplayName {
  fun getDisplayName(): @Nls String

  interface Impl: DisplayName {
    override fun getDisplayName(): @Nls String {
      return displayNamePtr.get() //NON-NLS
    }
    val displayNamePtr: Supplier<@Nls String>
  }
}

class ItemDisplayNameProvider<T: DisplayName>(targetType: KClass<T>): DisplayNameProvider<T>(targetType) {
  override fun getDisplayName(context: DataWranglerContext, value: T): String {
    return value.getDisplayName()
  }
}