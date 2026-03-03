// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger;

/**
 * Thrown by the EDTGuard when some method cannot be completed due to the target VM being paused.
 */
public class VMPausedException extends RuntimeException {
}
