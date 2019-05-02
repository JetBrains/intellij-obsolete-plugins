/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

package com.intellij.struts;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.struts.diagram.StrutsGraphDataModel;
import com.intellij.struts.diagram.StrutsObject;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.ActionMappings;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.dom.tiles.TilesDefinitions;

import java.util.Collection;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class StrutsModelTest extends StrutsTest {

  public void testStrutsConfig() {
    List<StrutsModel> model = StrutsManager.getInstance().getAllStrutsModels(getModule());
    assertEquals("Config not found", 1, model.size());
    StrutsConfig config = model.get(0).getMergedModel();

    ActionMappings mappings = config.getActionMappings();
    assertEquals(10, mappings.getActions().size());
  }

  public void testTilesDefinitions() {
    List<TilesModel> model = StrutsManager.getInstance().getAllTilesModels(getModule());
    assertSize(1, model);
    TilesDefinitions config = model.get(0).getMergedModel();
    List<Definition> defs = config.getDefinitions();
    assertSize(5, defs);
  }

  public void testStrutsModel() {
    StrutsModel model = StrutsManager.getInstance().getAllStrutsModels(getModule()).get(0);
    assertNotNull("Config not found", model);

    final int configsCount = 2;
    assertSize(configsCount, model.getConfigFiles());

    final int actionsCount = 10;
    List<Action> actions = model.getActions();
    assertSize(actionsCount, actions);

    Action login = model.findAction("/login");
    assertNotNull("Login action not found", login);

    Action another = model.findAction("/anotherAction");
    assertNotNull("Another action not found", another);

    FormBean form = model.findFormBean("loginForm");
    assertNotNull("Login form not found", form);
  }

  public void testDiagramModel() {
    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(getTestDataPath() + "/WEB-INF/struts-config.xml");
    assertNotNull(virtualFile);
    final PsiFile xmlFile = myFixture.getPsiManager().findFile(virtualFile);
    assertNotNull(xmlFile);
    StrutsConfig config = StrutsManager.getInstance().getStrutsConfig(xmlFile);

    final StrutsGraphDataModel graphDataModel = new StrutsGraphDataModel(config);
    final Collection<StrutsObject> nodes = graphDataModel.getNodes();
    assertSize(27, nodes);
    final Collection<StrutsObject> edges = graphDataModel.getEdges();
    assertSize(13, edges);
  }
}
