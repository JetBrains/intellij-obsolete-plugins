/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts.tree;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.StrutsTest;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.builders.WebModuleFixtureBuilder;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.xml.tree.DomFileElementNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * @author Dmitry Avdeev
 */
public class StrutsTreeTest extends StrutsTest {

  @Override
  protected void configure(final WebModuleFixtureBuilder moduleBuilder) {
    moduleBuilder.setWebXml(myFixture.getTempDirPath() + "/WEB-INF/web.xml");
    addStrutsJar(moduleBuilder);
  }

  public void testTree() {
    myFixture.configureByFiles("/WEB-INF/web.xml", "/WEB-INF/struts-config.xml", "/WEB-INF/struts-config-additional.xml");
    final StrutsDomTree tree = new StrutsDomTree(myFixture.getProject());
    final DomBrowser browser = new DomBrowser(tree);
    browser.openDefault();
    TreeUtil.expandAll(tree.getTree());
    UIUtil.dispatchAllInvocationEvents();
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getBuilder().getTreeModel().getRoot();
    assertEquals(2, root.getChildCount());
    final TreeNode config = root.getChildAt(0);
    final TreeNode dataSources = config.getChildAt(0);
    assertEquals(3, dataSources.getChildCount());
    final TreeNode dataSource = dataSources.getChildAt(0);
    assertEquals(2, dataSource.getChildCount());
    final TreeNode forms = config.getChildAt(1);
    assertEquals(5, forms.getChildCount());
    final TreeNode loginForm = forms.getChildAt(1);
    assertEquals(2, loginForm.getChildCount());
    Disposer.dispose(browser);
  }

  public void testTreeChange() {
    myFixture.configureByFiles("/WEB-INF/web.xml", "/WEB-INF/struts-config.xml", "/WEB-INF/struts-config-additional.xml");
    final StrutsDomTree tree = new StrutsDomTree(myFixture.getProject());
    final DomBrowser browser = new DomBrowser(tree);
    try {
      browser.openDefault();
      TreeUtil.expandAll(tree.getTree());
      final DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getBuilder().getTreeModel().getRoot();

      assertEquals(2, root.getChildCount());

      final TreeNode config = root.getChildAt(0);
      final XmlFile xmlFile = ((DomFileElementNode)((DefaultMutableTreeNode)config).getUserObject()).getDomElement().getFile();
      try {
        WriteCommandAction.writeCommandAction(myFixture.getProject()).run(() -> xmlFile.delete());
        PlatformTestUtil.waitForAlarm(1000);
      }
      catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
      assertEquals(1, root.getChildCount());
    }
    finally {
      Disposer.dispose(browser);
    }
  }
}
