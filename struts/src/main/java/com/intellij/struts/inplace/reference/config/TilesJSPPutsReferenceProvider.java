/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
