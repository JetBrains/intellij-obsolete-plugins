// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

public class LibraryLightProjectDescriptor extends DefaultLightProjectDescriptor {
    public LibraryLightProjectDescriptor(TestLibrary library) {
        myLibrary = library;
    }

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
        super.configureModule(module, model, contentEntry);
        myLibrary.addTo(module, model);
        IndexingTestUtil.waitUntilIndexesAreReady(model.getProject());
    }

    private final TestLibrary myLibrary;
}
