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

package com.intellij.struts.inplace.reference.property;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.core.PsiBeanProperty;
import com.intellij.struts.core.PsiBeanPropertyImpl;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author davdeev
 */
public class SetPropertyReferenceProvider extends PropertyReferenceProvider {

  private final static PsiBeanProperty[] TILES_PROPS = new PsiBeanProperty[]{
          new PsiBeanPropertyImpl("definitions-config", "String"),
          new PsiBeanPropertyImpl("definitions-parser-validate", "boolean"),
          new PsiBeanPropertyImpl("definitions-factory-class", "String"),
          new PsiBeanPropertyImpl("definitions-parser-details", "String"),
          new PsiBeanPropertyImpl("definitions-debug", "String")
  };

  @NonNls private static final String TILES_PLUGIN = "org.apache.struts.tiles.TilesPlugin";

  private final String myClassNameAttribute;
  private final String myFolderAttribute;

  public SetPropertyReferenceProvider(@NonNls String classNameAttribute, @NonNls String folderAttribute) {
    super(null, null);
    myClassNameAttribute = classNameAttribute;
    myFolderAttribute = folderAttribute;
  }

  @NonNls
  protected String getClassNameAttribute(XmlTag tag) {
    return myClassNameAttribute;
  }

  @Override
  protected PropertyReference createReference(PropertyReferenceSet set, int index, TextRange range) {

    return new PropertyReference(set, index, range, this) {

      {
        mySoft = true;
      }

      @Override
      @NotNull
      protected PsiBeanProperty[] getPropertiesForTag(final boolean forVariants) {
        PsiBeanProperty[] result = PsiBeanProperty.EMPTY_ARRAY;
        XmlTag tag = PsiTreeUtil.getParentOfType(myValue, XmlTag.class);
        if (tag != null) {
          XmlTag element = tag.getParentTag();
          if (element != null) {
            String className = element.getAttributeValue(getClassNameAttribute(element));
            if (className == null) {
              // trying to find default mapping set in folder
              if (myFolderAttribute != null) {
                final XmlTag folder = element.getParentTag();
                if (folder != null) {
                  className = folder.getAttributeValue(myFolderAttribute);
                }
              }
              if (className == null) {
                className = StrutsManager.getInstance().getDefaultClassname(myClassNameAttribute, element);
              }
            }
            if (className != null) {
              result = getProperties(className);
              if (result == null || result.length == 0) {
                result = getProperties(className);
              }
              if (className.equals(TILES_PLUGIN)) {
                result = ArrayUtil.mergeArrays(result, TILES_PROPS);
              }
            }
          }
        }
        return result;
      }

    };
  }
}
