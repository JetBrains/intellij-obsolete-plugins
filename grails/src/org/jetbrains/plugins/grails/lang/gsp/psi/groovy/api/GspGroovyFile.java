// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api;

import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GspPageSkeleton;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.api.GspLikeFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;

public interface GspGroovyFile extends GroovyFileBase, GspLikeFile {

  GspPageSkeleton getSkeleton();

}
