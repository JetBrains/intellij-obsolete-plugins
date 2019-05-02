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

package com.intellij.struts.highlighting.syntax;

import com.intellij.codeInsight.daemon.impl.analysis.InsertRequiredAttributeFix;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementVisitor;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for annotating {@link DomElementVisitor}s.
 * <p/>
 * Contains various utility methods to create "standard" annotations.
 *
 * @author Yann C�bron
 */
abstract class DomAnnotatorVisitor implements DomElementVisitor {

  private final AnnotationHolder holder;

  protected DomAnnotatorVisitor(final AnnotationHolder holder) {
    this.holder = holder;
  }

  /**
   * Visit child elements recursively.
   *
   * @param element Start element.
   */
  @Override
  public final void visitDomElement(final DomElement element) {
    element.acceptChildren(this);
  }

  /**
   * Annotates missing required attribute if existing attribute is present and adds intention to add required attribute.
   *
   * @param existing The existing attribute value.
   * @param required The required attribute value if existing is present.
   */
  protected final void checkRequiredAttribute(@NotNull final GenericAttributeValue existing,
                                              @NotNull final GenericAttributeValue required) {
    if (existing.getXmlAttribute() != null && required.getXmlAttribute() == null) {
      final Annotation annotation = holder.createErrorAnnotation(existing.getXmlAttribute(),
                                                                 '\'' + existing.getXmlElementName() + "' specified without '" +
                                                                 required.getXmlElementName() + '\'');
      if (!holder.isBatchMode()) annotation.registerFix(new InsertRequiredAttributeFix(existing.getXmlTag(), required.getXmlElementName()));
    }
  }

  /**
   * Annotates mutually exclusive attributes (n>1 found).
   *
   * @param values Values to check.
   */
  protected final void checkMutuallyExclusiveAttributes(@NotNull final GenericAttributeValue... values) {
    int found = 0;
    for (GenericAttributeValue value : values) {
      if (value.getXmlAttribute() != null) {
        found++;
        if (found > 1) {
          StringBuilder attributeNames = new StringBuilder();
          for (int i = 0; i < values.length; i++) {
            attributeNames.append(values[i].getXmlElementName());
            if (i < values.length - 1) {
              attributeNames.append('|');
            }
          }
          holder.createErrorAnnotation(value.ensureTagExists(), "Only one of " + attributeNames + " may be specified");
          return;
        }
      }
    }
  }

  /**
   * Annotates deprecated attribute and adds quickfix to replace it.
   *
   * @param deprecatedAttribute  Deprecated attribute.
   * @param replacementAttribute Attribute to replace deprecated attribute.
   * @param replacementValue     Optional default value to be used with replacement attribute.
   */
  protected final void checkDeprecatedAttribute(@NotNull final GenericAttributeValue deprecatedAttribute,
                                                @NotNull final GenericAttributeValue replacementAttribute,
                                                @Nullable final String replacementValue) {
    if (deprecatedAttribute.getXmlAttribute() != null) {
      final Annotation annotation = holder.createWarningAnnotation(
        deprecatedAttribute.getXmlAttribute(),
        "Deprecated attribute " + deprecatedAttribute.getXmlElementName() +
        ", use " + replacementAttribute.getXmlElementName() +
        (replacementValue == null ? "" : "=\"" + replacementValue + "\"") +
        " instead");
      annotation.setTextAttributes(CodeInsightColors.DEPRECATED_ATTRIBUTES);
      // STRUTS-152: register fix only if replacement attribute not present yet
      if (replacementAttribute.getXmlAttribute() == null) {
        annotation.registerFix(new ReplaceDeprecatedAttributeIntentionAction(deprecatedAttribute, replacementAttribute, replacementValue));
      }
    }
  }

  /**
   * Quickfix for replacing deprecated attribute.
   */
  private static class ReplaceDeprecatedAttributeIntentionAction extends BaseIntentionAction {

    private final GenericAttributeValue deprecatedAttribute;
    private final GenericAttributeValue replacementAttribute;
    private final String replacementValue;

    ReplaceDeprecatedAttributeIntentionAction(@NotNull final GenericAttributeValue deprecatedAttribute,
                                              @NotNull final GenericAttributeValue replacementAttribute,
                                              @Nullable final String replacementValue) {
      this.deprecatedAttribute = deprecatedAttribute;
      this.replacementAttribute = replacementAttribute;
      this.replacementValue = replacementValue != null ? replacementValue : ((XmlAttribute) deprecatedAttribute.ensureXmlElementExists()).getValue();
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return getText();
    }

    @Override
    @NotNull
    public String getText() {
      return "Replace deprecated attribute";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
      final XmlAttribute xmlAttribute = XmlElementFactory.getInstance(project).
        createXmlAttribute(replacementAttribute.getXmlElementName(), replacementValue);
      deprecatedAttribute.ensureXmlElementExists().replace(xmlAttribute);
    }
  }

}