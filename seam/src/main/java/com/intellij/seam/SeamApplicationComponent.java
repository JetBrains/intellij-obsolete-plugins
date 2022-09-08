package com.intellij.seam;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.meta.MetaDataContributor;
import com.intellij.psi.meta.MetaDataRegistrar;
import com.intellij.psi.xml.XmlTag;
import com.intellij.seam.constants.SeamConstants;
import com.intellij.seam.highlighting.jam.*;
import com.intellij.seam.highlighting.xml.SeamDomModelInspection;
import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SeamApplicationComponent
    implements FileTemplateGroupDescriptorFactory, MetaDataContributor {

  @Override
  public void contributeMetaData(@NotNull MetaDataRegistrar registrar) {
    registrar.registerMetaData(new ElementFilter() {
      @Override
      public boolean isAcceptable(Object element, PsiElement context) {
        if (element instanceof XmlTag) {
          final XmlTag tag = (XmlTag)element;
          final DomElement domElement = DomManager.getDomManager(tag.getProject()).getDomElement(tag);

          return domElement instanceof BasicSeamComponent;
        }
        return false;
      }

      @Override
      public boolean isClassAcceptable(Class hintClass) {
        return XmlTag.class.isAssignableFrom(hintClass);
      }
    }, BasicSeamComponentElementMetaData.class);
  }

  public static class BasicSeamComponentElementMetaData extends DomMetaData<BasicSeamComponent> {

    @Override
    @Nullable
    protected GenericDomValue getNameElement(final BasicSeamComponent element) {
      final GenericAttributeValue<String> id = element.getName();
      if (DomUtil.hasXml(id)) {
        return id;
      }
      return null;
    }

    @Override
    public void setName(final String name) throws IncorrectOperationException {
      getElement().getName().setStringValue(name);
    }
  }

  public static Class[] getInspectionClasses() {
    return new Class[]{SeamDomModelInspection.class, SeamAnnotationIncorrectSignatureInspection.class,
        SeamAnnotationsInconsistencyInspection.class, SeamBijectionUndefinedContextVariableInspection.class,
        SeamBijectionIllegalScopeParameterInspection.class, SeamBijectionTypeMismatchInspection.class, SeamJamComponentInspection.class,
        SeamIllegalComponentScopeInspection.class, SeamDuplicateComponentsInspection.class};
  }

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    final FileTemplateGroupDescriptor groupDescriptor =
        new FileTemplateGroupDescriptor(SeamBundle.SEAM_FRAMEWORK, SeamIcons.Seam);

    groupDescriptor.addTemplate(new FileTemplateDescriptor(SeamConstants.FILE_TEMPLATE_NAME_SEAM_1_2, SeamIcons.Seam));
    groupDescriptor.addTemplate(new FileTemplateDescriptor(SeamConstants.FILE_TEMPLATE_NAME_SEAM_2_0, SeamIcons.Seam));
    groupDescriptor.addTemplate(new FileTemplateDescriptor(SeamConstants.FILE_TEMPLATE_NAME_PAGES_2_0, SeamIcons.Seam));
    groupDescriptor.addTemplate(new FileTemplateDescriptor(SeamConstants.FILE_TEMPLATE_NAME_PAGEFLOW_2_0, SeamIcons.Seam));

    return groupDescriptor;
  }
}

