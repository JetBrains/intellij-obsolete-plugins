/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.highlighting.syntax;

import com.intellij.codeInsight.daemon.impl.analysis.InsertRequiredAttributeFix;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.*;
import com.intellij.struts.util.PsiClassUtil;
import com.intellij.util.xml.GenericAttributeValue;

/**
 * Provides additional syntax highlighting for Struts config files.
 *
 * @author Yann C�bron
 */
public class StrutsSyntaxAnnotator extends DomAnnotatorComponentBase<StrutsConfig> {

  public StrutsSyntaxAnnotator() {
    super(StrutsConfig.class);
  }

  private static boolean isStruts13(final Project project) {
    final PsiClass composableRequestProcessor = PsiClassUtil.findClassInProjectScope("org.apache.struts.chain.ComposableRequestProcessor", project);
    return composableRequestProcessor != null;
  }

  @Override
  protected DomAnnotatorVisitor buildVisitor(final AnnotationHolder holder) {
    return new DomAnnotatorVisitor(holder) {

      public void visitAction(final Action action) {

        // check action path for illegal characters
        final String actionPath = action.getPath().getStringValue();
        if (actionPath != null && actionPath.contains("#")) {
          holder.createErrorAnnotation(action.getPath().ensureXmlElementExists(), "Illegal character '#'");
        }

        // check required attributes
        final GenericAttributeValue<FormBean> formBean = action.getName();
        checkRequiredAttribute(action.getAttribute(), formBean);
        checkRequiredAttribute(action.getInput(), formBean);
        checkRequiredAttribute(action.getScope(), formBean);
        checkRequiredAttribute(action.getPrefix(), formBean);
        checkRequiredAttribute(action.getSuffix(), formBean);
        checkRequiredAttribute(action.getValidate(), formBean);

        // check mutually exclusive attributes
        checkMutuallyExclusiveAttributes(action.getForward(), action.getInclude(), action.getType());

        // check DispatchAction configuration
        final PsiClass actionClazz = action.getType().getValue();
        if (actionClazz != null) {
          final PsiClass dispatchActionClazz = PsiClassUtil.findClassInProjectScope("org.apache.struts.actions.DispatchAction",
                                                                                    action.ensureTagExists().getProject());
          if (dispatchActionClazz != null &&
              actionClazz.isInheritor(dispatchActionClazz, true)) {
            final XmlAttribute parameterAttribute = action.getParameter().getXmlAttribute();
            if (parameterAttribute == null) {
              final Annotation annotation = holder.createErrorAnnotation(
                action.ensureTagExists(),
                "Attribute parameter is mandatory for Action-class of type DispatchAction");
              if (!holder.isBatchMode()) annotation.registerFix(new InsertRequiredAttributeFix(action.getXmlTag(), "parameter"));
            } else if (StringUtil.isEmptyOrSpaces(parameterAttribute.getValue())) {
              final XmlAttributeValue element = parameterAttribute.getValueElement();
              if (element != null) {
                holder.createErrorAnnotation(element,
                                             "Attribute parameter must not be empty for Action-class of type DispatchAction");
              }
            }

          }
        }

      }

      /**
       * Ensure correct processor-class when using Tiles.
       *
       * @param controller Controller element to check.
       */
      public void visitController(final Controller controller) {
        final XmlElement controllerElement = controller.getXmlElement();
        if (controllerElement == null) {
          return;
        }

        // Struts 1.3.x does not need TilesRequestProcessor
        final Project project = controllerElement.getProject();
        if (isStruts13(project)) {
          return;
        }

        final TilesModel tilesModel = StrutsManager.getInstance().getTiles(controllerElement);
        if (tilesModel == null) {
          return;
        }

        final PsiClass processorClass = controller.getProcessorClass().getValue();
        final PsiClass tilesRequestProcessor = PsiClassUtil.findClassInProjectScope("org.apache.struts.tiles.TilesRequestProcessor",
                                                                                    project);

        // not set or wrong subclass
        if (processorClass == null || !InheritanceUtil.isInheritorOrSelf(processorClass, tilesRequestProcessor, true)) {
          holder.createErrorAnnotation(controller.ensureTagExists(), "Wrong processor class for use with Tiles, " +
                                                                     "use org.apache.struts.tiles.TilesRequestProcessor or subclass thereof");
        }

      }

      /**
       * Ensure proper controller setup when using Tiles.
       *
       * @param plugIn PlugIn element to check.
       */
      public void visitPlugIn(final PlugIn plugIn) {
        final XmlElement pluginElement = plugIn.getXmlElement();
        if (pluginElement == null) {
          return;
        }

        final Project project = pluginElement.getProject();

        // Struts 1.3.x does not need TilesRequestProcessor
        if (isStruts13(project)) {
          return;
        }

        final PsiClass pluginClass = plugIn.getClassName().getValue();
        final PsiClass tilesPluginClass = PsiClassUtil.findClassInProjectScope("org.apache.struts.tiles.TilesPlugin",
                                                                               project);

        if (pluginClass == null | !InheritanceUtil.isInheritorOrSelf(pluginClass, tilesPluginClass, true)) {
          return;
        }

        final TilesModel tilesModel = StrutsManager.getInstance().getTiles(pluginElement);
        if (tilesModel != null) {
          final StrutsConfig strutsConfig = StrutsManager.getInstance().getStrutsConfig(pluginElement.getContainingFile());
          assert strutsConfig != null;
          final Controller controller = strutsConfig.getController();
          if (controller.getXmlTag() == null) {
            holder.createErrorAnnotation(pluginElement, "Missing <controller> definition for use with Tiles");
          }
        }

      }

    };
  }

}