// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;

public class GrailsJobTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    TempDirTestFixture tdf = myFixture.getTempDirFixture();
    VirtualFile file;
    try {
      file = tdf.findOrCreateDir("grails-app/jobs");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    contentEntry.addSourceFolder(file, false);
  }

  public void testResolve() {
    PsiFile jobFile = myFixture.addFileToProject("grails-app/jobs/JjjJob.groovy", """
      class JjjJob {
        static triggers = {
          if (true) {
            simple [:]
          }
      
          cron [:]
        };
      
        {
          schedule(new Date())
          JjjJob.schedule(new Date())
          def l = getLog()
          l = log
        }
      
        static {
          schedule(new Date())
          JjjJob.schedule(new Date())
          def l = getLog()
          l = log
        }
      
      }
      """);
    GrailsTestCase.checkResolve(jobFile, "getLog", "getLog");
  }
}
