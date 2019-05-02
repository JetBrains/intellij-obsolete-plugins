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

package com.intellij.struts.inplace.reference.config;

import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.inplace.reference.XmlReferenceUtil;

/**
 * @author davdeev
 */
public class TilesJSPPutsReferenceProvider extends TilesPutsReferenceProvider {

  public TilesJSPPutsReferenceProvider() {
    super();
    setSoft(false);
  }

  @Override
  protected String getDefinitionName(XmlTag putTag) {

    XmlTag insert = XmlReferenceUtil.findEnclosingTag(putTag, "insert");
    if (insert != null) {
      String definition = insert.getAttributeValue("definition");
      if (definition != null) {
        return definition;
      }
      String layout = insert.getAttributeValue("page");
      if (layout == null) {
        layout = insert.getAttributeValue("template");
        if (layout == null) {
          layout = insert.getAttributeValue("component");
        }
      }
      if (layout != null) {
/*
              @todo implement
                TilesModel model = StrutsManager.getInstance().getTiles(ModuleUtil.findModuleForPsiElement(putTag));
                return model.getDefinitionByPage(layout);
*/
      }
    }
    return null;
  }
}
