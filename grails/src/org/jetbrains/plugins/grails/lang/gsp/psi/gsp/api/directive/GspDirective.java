// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;

public interface GspDirective extends GspTag {

  GspDirective[] EMPTY_ARRAY = new GspDirective[0];

  boolean addOrReplaceAttribute(@NotNull GspDirectiveAttribute attribute);

}
