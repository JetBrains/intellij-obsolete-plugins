// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.copyright;

import com.intellij.jsp.copyright.UpdateJspFileCopyright;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.psi.UpdateCopyright;
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider;
import org.jetbrains.plugins.grails.fileType.GspFileType;

final class UpdateGspCopyrightsProvider extends UpdateCopyrightsProvider {
    @Override
    public UpdateCopyright createInstance(Project project, Module module, VirtualFile file, FileType base, CopyrightProfile options) {
        return new UpdateJspFileCopyright(project, module, file, options) {
            @Override
            protected boolean accept() {
                return getFile().getFileType() == GspFileType.GSP_FILE_TYPE;
            }
        };
    }
}