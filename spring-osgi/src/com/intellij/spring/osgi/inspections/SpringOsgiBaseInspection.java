// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.inspections;

import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringModelVisitor;
import com.intellij.spring.model.highlighting.dom.SpringBeanInspectionBase;
import com.intellij.spring.model.xml.beans.Beans;
import com.intellij.spring.osgi.model.xml.BaseOsgiReference;
import com.intellij.spring.osgi.model.xml.BaseReferenceCollection;
import com.intellij.spring.osgi.model.xml.Service;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SpringOsgiBaseInspection extends SpringBeanInspectionBase {

  @Override
  protected SpringModelVisitor createVisitor(final DomElementAnnotationHolder holder, final @NotNull Beans beans, final @Nullable CommonSpringModel model) {
    return new SpringModelVisitor() {

      @Override
      protected boolean visitBean(CommonSpringBean bean) {
        if (bean instanceof Service) {
          checkOsgiService((Service)bean, beans, holder, model);
        } else if (bean instanceof BaseOsgiReference) {
          checkOsgiReference((BaseOsgiReference)bean, beans, holder, model);
        }

        if (bean instanceof BaseReferenceCollection) {
          checkOsgiReferenceCollection((BaseReferenceCollection)bean, beans, holder, model);
        }

        return true;
      }
    };
  }

  protected void checkOsgiReferenceCollection(BaseReferenceCollection baseReferenceCollection, Beans beans, DomElementAnnotationHolder holder, CommonSpringModel model) {}
  protected void checkOsgiService(Service service, final Beans beans, final DomElementAnnotationHolder holder, final CommonSpringModel springModel) {}
  protected void checkOsgiReference(BaseOsgiReference reference, final Beans beans, final DomElementAnnotationHolder holder, final CommonSpringModel springModel) {}
}

