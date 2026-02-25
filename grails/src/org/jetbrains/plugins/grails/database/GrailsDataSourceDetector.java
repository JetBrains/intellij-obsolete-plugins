// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.database;

import com.intellij.database.autoconfig.DataSourceDetector;
import com.intellij.database.util.DasUtil;
import com.intellij.database.util.DbUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.config.GroovyConfigReader;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

final class GrailsDataSourceDetector extends DataSourceDetector {

  @Override
  public boolean isRelevantFile(@NotNull PsiFile file) {
    if (!file.getName().equals("DataSource.groovy")) return false;

    VirtualFile dataSourceFile = file.getVirtualFile();
    if (dataSourceFile == null) return false;

    VirtualFile confDir = dataSourceFile.getParent();
    if (confDir == null || !confDir.getName().equals("conf")) return false;

    VirtualFile grailsAppDir = confDir.getParent();
    if (grailsAppDir == null || !grailsAppDir.getName().equals("grails-app")) return false;

    return true;
  }

  @Override
  public void collectDataSources(@NotNull Module module, @NotNull Builder builder, boolean onTheFly) {
    GrailsFramework framework = GrailsFramework.getInstance();

    if (!framework.hasSupport(module)) return;

    VirtualFile confDirectory = GrailsUtils.findConfDirectory(module);
    if (confDirectory == null) return;

    VirtualFile dataSource = confDirectory.findChild("DataSource.groovy");
    if (dataSource == null) return;

    PsiFile psiDataSource = PsiManager.getInstance(module.getProject()).findFile(dataSource);
    if (!(psiDataSource instanceof GroovyFile)) return;

    GroovyConfigReader cfg = GroovyConfigReader.read((GroovyFile)psiDataSource);

    PsiElement urlElement = cfg.getValue("development", "dataSource.url");
    if (urlElement == null) return;

    String driver = cfg.getStringValue("development", "dataSource.driverClassName");
    if (StringUtil.isEmpty(driver) || driver.equals("org.h2.Driver")) return;

    String url = cfg.getStringValue("development", "dataSource.url");
    if (StringUtil.isEmpty(url)) return;

    String dataSourceName = module.getName() + " dev";

    if (onTheFly && DbUtil.getDataSources(module.getProject()).filter(DasUtil.byName(dataSourceName)).isNotEmpty()) return;

    String userName = cfg.getStringValue("development", "dataSource.username");
    if (StringUtil.isEmpty(userName)) return;

    String password = cfg.getStringValue("development", "dataSource.password");
    if (StringUtil.isEmpty(password)) return;

    builder.withName(dataSourceName)
      .withDriverClass(driver)
      .withUrl(url)
      .withUser(userName)
      .withPassword(password)
      .withComment("grails")
      .withOrigin(urlElement)
      .commit();
  }

}
