// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ProcessingContext;
import icons.JetgroovyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.references.domain.detachedCriteria.DetachedCriteriaUtil;
import org.jetbrains.plugins.groovy.lang.completion.GroovyCompletionUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.HashSet;
import java.util.Set;

import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_CONNECTIVES;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FINDER_EXPRESSIONS_2_0;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FINDER_EXPRESSIONS_OLD;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FIND_OR_CREATE;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FIND_OR_SAVE;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.FINDER_PREFIXES;

public final class GormDynamicFinderCompletionProvider extends CompletionProvider<CompletionParameters> {
  private static final Logger LOG = Logger.getInstance(GormDynamicFinderCompletionProvider.class);

  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
    final GrReferenceExpression refExpr = (GrReferenceExpression)parameters.getPosition().getParent();
    final GrExpression qualifier = refExpr.getQualifierExpression();
    if (qualifier == null) return;
    final PsiType type = GroovyCompletionUtil.getQualifierType(qualifier);

    if (!(type instanceof PsiClassType)) return;

    PsiClass domainClass;

    if (isStaticMemberReference(qualifier)) {
      domainClass = ((PsiClassType)type).resolve();
      if (!GormUtils.isGormBean(domainClass)) return;

      assert domainClass != null;
    }
    else {
      domainClass = DetachedCriteriaUtil.getDomainClassByDetachedCriteriaExpression(type);
      if (domainClass == null) return;
    }

    if (refExpr.getReferenceName() != null) {
      DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);

      addDynamicFinderMethods(result, descriptor, result.getPrefixMatcher().getPrefix());
    }
  }

  public static boolean isStaticMemberReference(GrExpression qual) {
    final PsiReference ref = qual.getReference();
    if (ref == null) return false;
    return ref.resolve() instanceof PsiClass;
  }

  private static void addDynamicFinderMethods(CompletionResultSet result, DomainDescriptor descriptor, String pref) {
    String[] names = ArrayUtilRt.toStringArray(descriptor.getPersistentProperties().keySet());
    for (int i = 0; i < names.length; i++) {
      names[i] = StringUtil.capitalize(names[i]);
    }

    String p;

    p = findPrefix(pref, 0, FINDER_PREFIXES);
    if (p == null) {
      for (String prefix : FINDER_PREFIXES) {
        addIncompleteLookup(result, prefix, descriptor);
      }

      return;
    }

    int offset = p.length();

    if (p.equals(DOMAIN_FIND_OR_CREATE) || p.equals(DOMAIN_FIND_OR_SAVE)) {
      completeFindOrSave(result, pref, offset, names, descriptor);
      return;
    }

    boolean isGrails2_0 = true;
    GrailsStructure structure = GrailsStructure.getInstance(descriptor.getDomainClass());
    if (structure != null) {
      isGrails2_0 = structure.isAtLeastGrails1_4();
    }

    String[] expressions = isGrails2_0 ? DOMAIN_FINDER_EXPRESSIONS_2_0 : DOMAIN_FINDER_EXPRESSIONS_OLD;

    String connector = null;

    while (true) {
      p = findPrefix(pref, offset, names);
      if (p == null) {
        for (String name : names) {
          addIncompleteLookup(result, pref.substring(0, offset) + name, descriptor);
        }
        break;
      }

      offset += p.length();

      int notLength = 0;
      p = findPrefix(pref, offset, expressions);
      if (p == null) {
        if (pref.startsWith("Not", offset)) {
          notLength = "Not".length();
          p = findPrefix(pref, offset + 3, expressions);
        }
      }

      if (p == null) {
        if (findPrefix(pref, offset, DOMAIN_CONNECTIVES) == null) {
          String start = pref.substring(0, offset);

          addCompletedLookup(result, start, descriptor);

          for (String expression : expressions) {
            if (expression.equals("Equal")) {
              continue;
            }

            if (expression.equals("NotEqual")) {
              addIncompleteLookup(result, start + expression, descriptor);
              continue;
            }

            addIncompleteLookup(result, start + expression, descriptor);
            addIncompleteLookup(result, start + "Not" + expression, descriptor);
          }

          if (connector == null) {
            for (String c : DOMAIN_CONNECTIVES) {
              addIncompleteLookup(result, start + c, descriptor);
            }
          }
          else {
            if (isGrails2_0) { // Grails <= 1.2.7  allow only 2 conditions.
              addIncompleteLookup(result, start + connector, descriptor);
            }
          }

          break;
        }
      }
      else {
        offset += p.length() + notLength;
      }

      p = findPrefix(pref, offset, DOMAIN_CONNECTIVES);
      if (p == null) {
        String start = pref.substring(0, offset);

        addCompletedLookup(result, start, descriptor);

        if (connector == null) {
          for (String name : names) {
            for (String c : DOMAIN_CONNECTIVES) {
              addIncompleteLookup(result, start + c + name, descriptor);
            }
          }
        }
        else {
          if (isGrails2_0) { // Grails <= 1.2.7  allow only 2 conditions.
            for (String name : names) {
              addIncompleteLookup(result, start + connector + name, descriptor);
            }
          }
        }

        break;
      }

      if (connector == null) {
        connector = p;
      }
      else {
        if (!isGrails2_0) {
          // Grails <= 1.2.7  allow only 2 conditions.
          addCompletedLookup(result, pref.substring(0, offset), descriptor);
          break;
        }
      }

      offset += p.length();
    }
  }

  private static void completeFindOrSave(CompletionResultSet result,
                                         String pref,
                                         int offset,
                                         String[] names,
                                         @NotNull DomainDescriptor descriptor) {
    String p;

    Set<String> alreadyUsed = new HashSet<>();

    while (true) {
      p = findPrefix(pref, offset, names);
      if (p == null) {
        for (String name : names) {
          if (!alreadyUsed.contains(name)) {
            addIncompleteLookup(result, pref.substring(0, offset) + name, descriptor);
          }
        }
        break;
      }

      alreadyUsed.add(p);

      offset += p.length();

      if (pref.startsWith("Equal", offset)) {
        offset += "Equal".length();
      }

      if (!pref.startsWith("And", offset)) {
        String start = pref.substring(0, offset);

        addCompletedLookup(result, start, descriptor);

        for (String name : names) {
          if (!alreadyUsed.contains(name)) {
            addIncompleteLookup(result, start + "And" + name, descriptor);
          }
        }

        return;
      }

      offset += "And".length();
    }
  }

  private static @Nullable String findPrefix(String s, int offset, String[] prefixes) {
    for (String prefix : prefixes) {
      if (s.startsWith(prefix, offset)) {
        return prefix;
      }
    }

    return null;
  }

  private static void addCompletedLookup(CompletionResultSet result, String text, @NotNull DomainDescriptor descriptor) {
    GrLightMethodBuilder method = DomainMembersProvider.parseFinderMethod(text, descriptor);
    if (method == null) {
      LOG.error("Invalid method name generated: " + text);
      return;
    }

    GrParameter[] parameters = method.getParameters();
    if (parameters.length > 0 && parameters[parameters.length - 1].isOptional()) {
      method.getParameterList().removeParameter(parameters.length - 1);
    }

    result.addElement(GroovyCompletionUtil.createLookupElement(method));
  }

  private static void addIncompleteLookup(CompletionResultSet result, String text, @NotNull DomainDescriptor descriptor) {
    LookupElementBuilder element= LookupElementBuilder.create(text).withIcon(JetgroovyIcons.Groovy.Method);
    String typeText = "";

    if (text.startsWith("findAll")) {
      typeText = "List<" + descriptor.getDomainClass().getName() + ">";
    }
    else if (text.startsWith("count")) {
      typeText = "int";
    }
    else if (text.startsWith("find")) {
      typeText = descriptor.getDomainClass().getName();
    }

    element = element.withTypeText(typeText);

    element = element.withTailText("...", true);

    element = element.withInsertHandler(new InsertHandler<>() {
      @Override
      public void handleInsert(final @NotNull InsertionContext context, @NotNull LookupElement item) {
        context.setLaterRunnable(
          () -> new CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(context.getProject(), context.getEditor()));
      }
    });

    result.addElement(element);
  }

}
