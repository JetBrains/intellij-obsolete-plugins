// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.jarFinder.AbstractAttachSourceProvider;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.concurrency.ThreadingAssertions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsConfigUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

final class GrailsAttachSourcesProvider extends AbstractAttachSourceProvider {

  private static final Logger LOG = Logger.getInstance(GrailsAttachSourcesProvider.class);

  @Override
  public @NotNull Collection<? extends AttachSourcesAction> getActions(@NotNull List<? extends LibraryOrderEntry> orderEntries,
                                                                       @NotNull PsiFile psiFile) {
    VirtualFile jar = getJarByPsiFile(psiFile);
    if (jar == null) return List.of();

    final Library library = getLibraryFromOrderEntriesList(orderEntries);
    if (library == null) return List.of();

    VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);

    VirtualFile grailsHome = GrailsConfigUtils.getGrailsLibraryHome(files);
    if (grailsHome == null) return List.of();

    final String grailsVersion = GrailsConfigUtils.getGrailsVersion(files);
    if (StringUtil.isEmpty(grailsVersion) || "1.4".compareTo(grailsVersion) > 0) return List.of();

    final String jarNameWithoutExt = jar.getNameWithoutExtension();

    if (!jarNameWithoutExt.endsWith(grailsVersion)) {
      LOG.warn("Grails JAR name don't end of version [jar:" + jar + ", version: " + grailsVersion + "]");
      return List.of();
    }

    String pluginNameWith_ = StringUtil.trimEnd(jarNameWithoutExt, grailsVersion);
    if (!pluginNameWith_.endsWith("-")) {
      LOG.warn("Grails JAR name don't end of version [jar:" + jar + ", version: " + grailsVersion + "]");
      return List.of();
    }

    final String pluginName = StringUtil.trimEnd(pluginNameWith_, "-").trim();

    final String sourceFileName = jarNameWithoutExt + "-sources.jar";

    ThreadingAssertions.assertEventDispatchThread();

    grailsHome.refresh(false, false);

    final VirtualFile grailsHomeSrc = grailsHome.findChild("src");
    if (grailsHomeSrc == null) {
      LOG.warn("Grails home don't contains 'scr' folder");
      return List.of();
    }

    grailsHomeSrc.refresh(false, false);

    VirtualFile srcFile = grailsHomeSrc.findChild(sourceFileName);
    if (srcFile != null) {
      // File already downloaded.
      VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(srcFile);
      if (jarRoot == null || ArrayUtil.contains(jarRoot, library.getFiles(OrderRootType.SOURCES))) {
        return List.of(); // Sources already attached.
      }

      return List.of(
        new AttachExistingSourceAction(
          jarRoot,
          library,
          GrailsBundle.message("attache.source.from.grails.action.title", "$GRAILS_HOME/src/"))
      );
    }

    final String url = "https://repo.grails.org/grails/libs-releases-local/org/grails/" + pluginName + '/' + grailsVersion + '/' + sourceFileName;

    return List.of(new DownloadSourcesAction(psiFile.getProject(), "Downloading Grails Sources", url) {
      @Override
      protected void storeFile(byte[] content) {
        try {
          VirtualFile srcFile = grailsHomeSrc.createChildData(this, sourceFileName);
          srcFile.setBinaryContent(content);

          addSourceFile(JarFileSystem.getInstance().getJarRootForLocalFile(srcFile), library);
        }
        catch (IOException e) {
          new Notification(myMessageGroupId,
                           GrailsBundle.message("notification.title.io.error"),
                           GrailsBundle.message("notification.content.failed.to.save.0.1", grailsHomeSrc.getPath(), sourceFileName),
                           NotificationType.ERROR)
            .notify(myProject);
          LOG.warn(e);
        }
      }
    });
  }

}
