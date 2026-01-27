// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.inspections;

import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.model.xml.beans.Beans;
import com.intellij.spring.osgi.SpringOsgiBundle;
import com.intellij.spring.osgi.model.xml.BaseOsgiReference;
import com.intellij.spring.osgi.model.xml.BaseReferenceCollection;
import com.intellij.spring.osgi.model.xml.Service;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

public final class SpringOsgiElementsInconsistencyInspection extends SpringOsgiBaseInspection {
  @Override
  protected void checkOsgiService(final Service service, final Beans beans, final DomElementAnnotationHolder holder, final CommonSpringModel springModel) {
    super.checkOsgiService(service, beans, holder, springModel);

    if (service.getInterface().getXmlAttribute() != null && DomUtil.hasXml(service.getInterfaces())) {
      holder.createProblem(service, SpringOsgiBundle.message("service.inconsistency.illegal.use.of.interfaces"));
    }
    if (service.getRef().getXmlAttribute() != null && DomUtil.hasXml(service.getBean())) {
      holder.createProblem(service, SpringOsgiBundle.message("service.inconsistency.illegal.use.of.ref.and.bean"));
    }
  }

  @Override
  protected void checkOsgiReferenceCollection(BaseReferenceCollection referenceCollection,
                                              Beans beans,
                                              DomElementAnnotationHolder holder,
                                              CommonSpringModel model) {
    if (referenceCollection.getComparatorRef().getXmlAttribute() != null && DomUtil.hasXml(referenceCollection.getComparator())) {
      holder.createProblem(referenceCollection, SpringOsgiBundle.message("collections.inconsistency.illegal.use.of.comparator"));
    }
  }

  @Override
  protected void checkOsgiReference(BaseOsgiReference reference, final Beans beans, final DomElementAnnotationHolder holder, final CommonSpringModel springModel) {
    super.checkOsgiReference(reference, beans, holder, springModel);

    if (reference.getInterface().getXmlAttribute() != null && DomUtil.hasXml(reference.getInterfaces())) {
      holder.createProblem(reference, SpringOsgiBundle.message("service.inconsistency.illegal.use.of.interfaces"));
    }
  }
}
