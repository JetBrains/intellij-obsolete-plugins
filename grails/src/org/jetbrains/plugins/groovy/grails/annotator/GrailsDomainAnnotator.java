// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.grails.references.constraints.GrailsConstraintsUtil;
import org.jetbrains.plugins.grails.references.domain.DomainClassUtils;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GrailsDomainAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
    if (!(psiElement instanceof GroovyFile)) return;

    for (GrTypeDefinition grTypeDefinition : ((GroovyFile)psiElement).getTypeDefinitions()) {
      if (GormUtils.isGormBean(grTypeDefinition)) {
        checkBelongsToAndHasManyRelations(grTypeDefinition, holder);
        checkMappedBy(grTypeDefinition, holder);
        checkConstraints(grTypeDefinition, holder);
      }
    }
  }

  private static void checkConstraints(GrTypeDefinition domainClass, final AnnotationHolder holder) {
    GrField constraintsField = (GrField)domainClass.findFieldByName("constraints", false);
    if (constraintsField == null) return;

    GrExpression initializerGroovy = constraintsField.getInitializerGroovy();
    if (!(initializerGroovy instanceof GrClosableBlock)) return;

    final Map<String, Pair<PsiType, PsiElement>> fields = DomainDescriptor.getDescriptor(domainClass).getPersistentProperties();

    final Map<String, Map<String, GrNamedArgument>> constraints = new HashMap<>();

    // Load constraints and check duplicates.
    initializerGroovy.accept(new GroovyRecursiveElementVisitor() {
      @Override
      public void visitMethodCallExpression(@NotNull GrMethodCallExpression methodCallExpression) {
        PsiMethod method = methodCallExpression.resolveMethod();
        if (GrailsConstraintsUtil.isConstraintsMethod(method)) {
          assert method != null;

          String fieldName = method.getName();

          if (fields.containsKey(fieldName)) {
            Map<String, GrNamedArgument> fieldConstraints = constraints.get(fieldName);
            if (fieldConstraints == null) {
              fieldConstraints = new HashMap<>();
              constraints.put(fieldName, fieldConstraints);
            }

            for (GrNamedArgument argument : PsiUtil.getFirstMapNamedArguments(methodCallExpression)) {
              String constraintType = argument.getLabelName();

              GrNamedArgument oldConstraint = fieldConstraints.put(constraintType, argument);

              if (oldConstraint != null) {
                GrArgumentLabel oldArgumentLabel = oldConstraint.getLabel();
                assert oldArgumentLabel != null;
                holder.newAnnotation(
                  HighlightSeverity.WARNING,
                  GrailsBundle.message("domain.annotator.error.message.already.defined", constraintType, fieldName)
                ).range(oldArgumentLabel).create();
              }
            }
          }
        }

        super.visitMethodCallExpression(methodCallExpression);
      }
    });
  }

  private static void checkBelongsToAndHasManyRelations(PsiClass domainClass, AnnotationHolder holder) {
    final Map<String, Pair<PsiType, PsiElement>> fields = DomainDescriptor.getDescriptor(domainClass).getPersistentProperties();

    PsiField belongs = domainClass.findFieldByName(DomainClassRelationsInfo.BELONGS_TO_NAME, false);
    GrListOrMap lom = getListOrMap(belongs);
    if (lom != null) {
      if (lom.isMap()) {
        for (GrNamedArgument argument : lom.getNamedArguments()) {
          checkNamedArgumentForBelongsTo(argument, fields, holder);
        }
      }
      processDuplicates(holder, lom);
    }

    final PsiField hasMany = domainClass.findFieldByName(DomainClassRelationsInfo.HAS_MANY_NAME, false);
    lom = getListOrMap(hasMany);
    if (lom != null) {
      if (!lom.isMap()) {
        holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("must.contain.map")).range(lom).create();
      }
      processDuplicates(holder, lom);
    }
  }

  private static void checkMappedBy(PsiClass domainClass, AnnotationHolder holder) {
    final PsiField mappedBy = domainClass.findFieldByName(DomainClassRelationsInfo.MAPPED_BY, false);
    if (mappedBy == null || !mappedBy.hasModifierProperty(PsiModifier.STATIC)) return;

    final PsiField hasMany = domainClass.findFieldByName(DomainClassRelationsInfo.HAS_MANY_NAME, false);
    if (hasMany == null || !hasMany.hasModifierProperty(PsiModifier.STATIC)) {
      holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("mapped.by.is.used.without.has.many")).range(mappedBy.getNameIdentifier()).create();
      return;
    }

    GrListOrMap lom = getListOrMap(mappedBy);
    if (lom != null) {
      if (lom.isMap()) {
        Set<String> names = new HashSet<>();
        Map<String, Pair<PsiType, PsiElement>> hasManyMap = DomainDescriptor.getDescriptor(domainClass).getHasMany();

        for (GrNamedArgument argument : lom.getNamedArguments()) {
          final GrArgumentLabel label = argument.getLabel();
          if (label != null) {
            final String name = label.getName();
            if (names.contains(name)) {
              holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("duplicate.property.name")).range(label).create();
            }
            else {
              names.add(name);
              final Pair<PsiType, PsiElement> pair = hasManyMap.get(name);
              if (pair == null) {
                holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("property.is.absent.in.has.many", name)).range(label).create();
              }
              else {
                final GrExpression expression = argument.getExpression();
                if (expression instanceof GrLiteral && ((GrLiteral)expression).getValue() instanceof String) {
                  final PsiReference ref = expression.getReference();
                  if (ref != null) {
                    final Object value = ((GrLiteral)expression).getValue();
                    final PsiElement resolved = ref.resolve();
                    if (resolved == null) {
                      holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("property.is.absent", pair.first.getPresentableText(), value)).range(expression).create();
                    }
                    else {
                      final PsiType propertyType = DomainClassUtils.getDCPropertyType(resolved);
                      if (propertyType instanceof PsiClassType) {
                        final PsiClass resolvedClass = ((PsiClassType)propertyType).resolve();
                        if (!domainClass.getManager().areElementsEquivalent(domainClass, resolvedClass)) {
                          holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("property.has.wrong.type", value, propertyType.getPresentableText())).range(expression).create();
                        }
                      }
                      else {
                        holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("property.has.wrong.type", value,
                                                                                        propertyType != null ? propertyType
                                                                                          .getPresentableText() : "null")).range(expression).create();
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      else {
        holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("must.contain.map")).range(lom).create();
      }
    }
  }

  private static void checkNamedArgumentForBelongsTo(GrNamedArgument argument,
                                                     Map<String, Pair<PsiType, PsiElement>> fields,
                                                     AnnotationHolder holder) {
    final GrArgumentLabel label = argument.getLabel();
    if (label != null) {
      final GrExpression expression = argument.getExpression();
      PsiClass labelClass = null;
      if (expression instanceof GrReferenceExpression) {
        final PsiElement element = ((GrReferenceExpression)expression).resolve();
        if (element instanceof PsiClass) {
          labelClass = (PsiClass)element;
        }
      }
      if (labelClass == null) {
        return;
      }

      if (!GormUtils.isGormBean(labelClass)) {
        holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("must.be.domain.class.name")).range(expression).create();
        return;
      }

      final String name = label.getName();
      Pair<PsiType, PsiElement> pair = fields.get(name);
      if (pair != null) {
        PsiClass fieldClass = PsiTypesUtil.getPsiClass(pair.first);
        if (fieldClass != null &&
            !(fieldClass.getManager().areElementsEquivalent(fieldClass, labelClass) || labelClass.isInheritor(fieldClass, true))) {
          holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("property.is.abmbigous", name, fieldClass.getQualifiedName())).range(argument).create();
        }
      }
    }
  }


  private static @Nullable GrListOrMap getListOrMap(@Nullable PsiField field) {
    if (field instanceof GrField) {
      final GrExpression initializer = ((GrField)field).getInitializerGroovy();
      if (initializer instanceof GrListOrMap) {
        return (GrListOrMap)initializer;
      }
    }
    return null;
  }

  private static void processDuplicates(AnnotationHolder holder, GrListOrMap lom) {
    if (lom.isMap()) {
      Set<String> names = new HashSet<>();
      for (GrNamedArgument argument : lom.getNamedArguments()) {
        final GrArgumentLabel label = argument.getLabel();
        if (label == null) continue;
        final String name = label.getName();
        if (name != null && !names.add(name)) {
          holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("duplicate.property.name")).range(label).create();
        }
      }
    }
    else {
      Set<String> classNames = new HashSet<>();
      for (GrExpression expr : lom.getInitializers()) {
        if (expr instanceof GrReferenceExpression) {
          PsiElement element = ((GrReferenceExpression)expr).resolve();
          if (element instanceof PsiClass) {
            final String qname = ((PsiClass)element).getQualifiedName();
            assert qname != null;
            if (!classNames.add(qname)) {
              holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("domain.annotator.duplicate.type"))
                .range(expr)
                .create();
            }
          } else {
            holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("domain.annotator.class.name.expected"))
              .range(expr)
              .create();
          }
        }
      }
    }
  }
}
