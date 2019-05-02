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

package com.intellij.struts.core;

import com.intellij.icons.AllIcons;
import com.intellij.psi.*;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.dom.FormProperty;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;

public class PsiBeanPropertyImpl implements PsiBeanProperty, Comparable<PsiBeanProperty> {

  private final PsiElement[] myPsiElements;
  private final String myName;
  private final String myType;
  private final Icon myIcon;

  private final boolean myHasSetter;
  private final boolean myHasGetter;

  private PsiMethod myGetter;

  public PsiBeanPropertyImpl(@NonNls final String name, @NonNls final String type) {
    myPsiElements = null;
    myName = name;
    myType = type;
    myIcon = AllIcons.Nodes.PropertyWrite;
    myHasSetter = true;
    myHasGetter = true;
  }

  @Nullable
  public static PsiBeanProperty create(final FormProperty formProperty) {
    final String name = formProperty.getName().getStringValue();
    return name == null ? null : new PsiBeanPropertyImpl(formProperty);
  }

  private PsiBeanPropertyImpl(final FormProperty formProperty) {
    myName = formProperty.getName().getStringValue();
    final PsiType type = formProperty.getType().getValue();
    if (type != null) {
      myType = type.getCanonicalText();
    }
    else {
      myType = null;
    }
    final XmlTag xmlTag = formProperty.getName().getXmlTag();
    myPsiElements = xmlTag == null ? null : new PsiElement[]{xmlTag};
    myIcon = AllIcons.Nodes.PropertyWrite;
    myHasSetter = true;
    myHasGetter = true;
  }

  public PsiBeanPropertyImpl(final PsiClass clazz, final String name, @Nullable final PsiField field) {
    myName = name;
    String myType = null;
    myGetter = PropertyUtilBase.findPropertyGetter(clazz, name, false, true);
    PsiMethod setter = PropertyUtilBase.findPropertySetter(clazz, name, false, true);
    if (myGetter != null || setter != null) {
      if (myGetter != null && setter != null) {
        myIcon = AllIcons.Nodes.PropertyWrite;
        myHasSetter = true;
        myHasGetter = true;
      }
      else if (myGetter != null) {
        myIcon = AllIcons.Nodes.PropertyRead;
        myHasGetter = true;
        myHasSetter = false;
      }
      else {
        myIcon = AllIcons.Nodes.PropertyWrite;
        myHasSetter = true;
        myHasGetter = false;
      }
    }
    else {
      myGetter = PropertyUtilBase.findPropertyGetter(clazz, name, true, true);
      setter = PropertyUtilBase.findPropertySetter(clazz, name, true, true);
      if (myGetter != null && setter != null) {
        myIcon = AllIcons.Nodes.PropertyReadWriteStatic;
        myHasSetter = true;
        myHasGetter = true;
      }
      else if (myGetter != null) {
        myIcon = AllIcons.Nodes.PropertyReadStatic;
        myHasGetter = true;
        myHasSetter = false;
      }
      else {
        myIcon = AllIcons.Nodes.PropertyWriteStatic;
        myHasSetter = true;
        myHasGetter = false;
      }
    }
    final ArrayList<PsiElement> elements = new ArrayList<>();
    if (field != null) {
      elements.add(field);
      myType = field.getType().getPresentableText();
    }
    if (myGetter != null) {
      elements.add(myGetter);
      final PsiType returnType = myGetter.getReturnType();
      assert returnType != null;
      myType = returnType.getPresentableText();
    }
    if (setter != null) {
      elements.add(setter);
      myType = setter.getParameterList().getParameters()[0].getType().getPresentableText();
    }
    myPsiElements = PsiUtilCore.toPsiElementArray(elements);
    this.myType = myType;
  }


  @Override
  public PsiElement[] getPsiElements() {
    return myPsiElements;
  }

  @Override
  public Icon getIcon() {
    return myIcon;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getType() {
    return myType;
  }

  @Override
  public boolean hasGetter() {
    return myHasGetter;
  }

  @Override
  public PsiMethod getGetter() {
    return myGetter;
  }

  @Override
  public boolean hasSetter() {
    return myHasSetter;
  }

  public String toString() {
    return myName + " (" + myType + ")";
  }

  @Override
  public int compareTo(final PsiBeanProperty o) {
    return myName.compareTo(o.getName());
  }
}
