// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspExprInjection;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public final class ModelVariableDeclarationSearcher extends PomDeclarationSearcher {

  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!(element instanceof GrArgumentLabel)) return;

    PsiElement namedArgument = element.getParent();
    if (!(namedArgument instanceof GrNamedArgument)) return;

    PsiElement listOrMap = namedArgument.getParent();
    if (!(listOrMap instanceof GrListOrMap)) return;

    if (processVariable((GrNamedArgument)namedArgument, findGspByClosureReturn(listOrMap), element, consumer)) {
      return;
    }

    processVariable((GrNamedArgument)namedArgument, findGspByModelMap(listOrMap), element, consumer);
  }

  private static boolean processVariable(GrNamedArgument namedArgument,
                                         @Nullable GspFile gspFile,
                                         PsiElement element,
                                         Consumer<? super PomTarget> consumer) {
    if (gspFile == null) return false;

    PsiVariable variable = GspModelVariableModel.getInstance(gspFile).getVariable(namedArgument.getLabelName());

    if (variable instanceof GrLightVariable && ((GrLightVariable)variable).getDeclarations().contains(element)) {
      consumer.consume(variable);
      return true;
    }

    return false;
  }

  private static @Nullable GspFile findGspByModelMap(PsiElement modelMap) {
    PsiElement fileReferenceElement = null;
    
    PsiElement parent = modelMap.getParent();
    if (parent instanceof GrNamedArgument namedArgument) {
      if ("model".equals(namedArgument.getLabelName())) {
        fileReferenceElement = PsiUtil.getNamedArgumentValue(namedArgument, "view");
        if (fileReferenceElement == null) {
          fileReferenceElement = PsiUtil.getNamedArgumentValue(namedArgument, "template");
        }
      }
    }
    else if (parent instanceof GrGspExprInjection) {
      PsiElement gspElement = parent.getContainingFile().getViewProvider().findElementAt(modelMap.getTextOffset(), GspLanguage.INSTANCE);
      XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(gspElement, XmlAttribute.class);
      if (xmlAttribute != null && "model".equals(xmlAttribute.getName())) {
        XmlTag xmlTag = xmlAttribute.getParent();
        if (xmlTag != null) {
          XmlAttribute attribute = xmlTag.getAttribute("view");
          if (attribute == null) {
            attribute = xmlTag.getAttribute("template");
          }

          if (attribute != null) {
            fileReferenceElement = attribute.getValueElement();
          }
        }
      }
    }

    if (fileReferenceElement != null) {
      for (PsiReference reference : fileReferenceElement.getReferences()) {
        if (reference instanceof FileReference) {
          final FileReference lastReference = ((FileReference)reference).getFileReferenceSet().getLastReference();
          if (lastReference == null) break;

          PsiFileSystemItem resolve = lastReference.resolve();
          if (resolve instanceof GspFile) return (GspFile)resolve;
          break;
        }
      }
    }

    return null;
  }

  private static @Nullable GspFile findGspByClosureReturn(PsiElement returnMap) {
    PsiElement action = PsiTreeUtil.getParentOfType(returnMap, GrField.class, GrMethod.class);
    if (action == null) return null;
    return (GspFile)ContainerUtil.find(GrailsUtils.getViewPsiByAction(action), view -> view instanceof GspFile);
  }

}
