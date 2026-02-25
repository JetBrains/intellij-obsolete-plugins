/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure

@JvmField
val COMPARATOR: Comparator<GrailsApplication?> = Comparator { app1, app2 ->
  when {
    app1 == null && app2 == null -> 0
    app1 == null -> -1
    app2 == null -> 1
    else -> app1.name.compareTo(app2.name, ignoreCase = true)
  }
}