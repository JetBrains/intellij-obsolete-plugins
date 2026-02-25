// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.EqTailType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsPluginCondition;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.GrailsVersionCondition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.plugins.groovy.util.FieldInitializerTailTypes.EQ_ARRAY;
import static org.jetbrains.plugins.groovy.util.FieldInitializerTailTypes.EQ_CLOSURE;
import static org.jetbrains.plugins.groovy.util.FieldInitializerTailTypes.EQ_STRING;
import static org.jetbrains.plugins.groovy.util.FieldInitializerTailTypes.EQ_STRING_ARRAY;

public class GrailsStaticFieldCompletionProvider extends CompletionProvider<CompletionParameters> {

  private record FieldCompletion(@NotNull String fieldName, @NotNull TailType tailType, @Nullable Condition<? super PsiClass> condition) {
  }
  
  private static final Map<GrailsArtifact, FieldCompletion[]> ALLOWED_STATIC_FIELDS = GrailsUtils.createEnumMap(
    GrailsArtifact.DOMAIN, new FieldCompletion[]{
      new FieldCompletion("mapWith", EQ_STRING, null),
      new FieldCompletion("hasMany", EQ_ARRAY, null),
      new FieldCompletion("hasOne", EQ_ARRAY, null),
      new FieldCompletion("belongsTo", EQ_ARRAY, null),
      new FieldCompletion("relatesToMany", EQ_ARRAY, null),
      new FieldCompletion("constraints", EQ_CLOSURE, null),
      new FieldCompletion("mappedBy", EQ_ARRAY, null),
      new FieldCompletion("mapping", EQ_CLOSURE, null),
      new FieldCompletion("embedded", EQ_STRING_ARRAY, null),
      new FieldCompletion("transients", EQ_STRING_ARRAY, null),
      new FieldCompletion("evanescent", EQ_STRING_ARRAY, null),
      new FieldCompletion("optionals", EqTailType.INSTANCE, null),
      new FieldCompletion("namedQueries", EQ_CLOSURE, null),
      new FieldCompletion("fetchMode", EQ_ARRAY, null),
      new FieldCompletion("searchable", EqTailType.INSTANCE, new GrailsPluginCondition("searchable")),
      new FieldCompletion("auditable", EqTailType.INSTANCE, new GrailsPluginCondition("audit-logging"))
    },

    GrailsArtifact.JOB, new FieldCompletion[]{
      new FieldCompletion("triggers", EQ_CLOSURE, null)
    },

    GrailsArtifact.CONTROLLER, new FieldCompletion[]{
      new FieldCompletion("layout", EQ_STRING, null),
      new FieldCompletion("defaultAction", EQ_STRING, null),
      new FieldCompletion("allowedMethods", EqTailType.INSTANCE, null),
      new FieldCompletion("accessControl", EQ_CLOSURE, Conditions.or(new GrailsPluginCondition("shiro"), new GrailsPluginCondition("jsecurity"))),
      new FieldCompletion("namespace", EQ_STRING, new GrailsVersionCondition("2.3.0"))
    },

    GrailsArtifact.TAGLIB, new FieldCompletion[]{
      new FieldCompletion("namespace", EQ_STRING, null),
      new FieldCompletion("supportsController", EqTailType.INSTANCE, null),
      new FieldCompletion("returnObjectForTags", EQ_STRING_ARRAY, null)
    },

    GrailsArtifact.SERVICE, new FieldCompletion[]{
      new FieldCompletion("transactional", EqTailType.INSTANCE, null),
      new FieldCompletion("scope", EqTailType.INSTANCE, null)
    },

    GrailsArtifact.URLMAPPINGS, new FieldCompletion[]{
      new FieldCompletion("mappings", EQ_CLOSURE, null),
    },

    GrailsArtifact.REALM, new FieldCompletion[]{
      new FieldCompletion("authTokenClass", EqTailType.INSTANCE, null)
    }
  );

  private static final Map<GrailsArtifact, FieldCompletion[]> ALLOWED_FIELDS = GrailsUtils.createEnumMap(
    GrailsArtifact.CONTROLLER, new FieldCompletion[]{
      new FieldCompletion("beforeInterceptor", EqTailType.INSTANCE, null),
      new FieldCompletion("afterInterceptor", EqTailType.INSTANCE, null),
      new FieldCompletion("scaffold", EqTailType.INSTANCE, null)
    },

    GrailsArtifact.FILTER, new FieldCompletion[]{
      new FieldCompletion("filters", EqTailType.INSTANCE, null),
      new FieldCompletion("dependsOn", EqTailType.INSTANCE, null)
    },

    GrailsArtifact.JOB, new FieldCompletion[] {
      new FieldCompletion("sessionRequired", EqTailType.INSTANCE, null),
      new FieldCompletion("concurrent", EqTailType.INSTANCE, null),
    }
  );

  private static final Set<String> ourFieldNameCache;
  static {
    Set<String> res = new HashSet<>();

    for (FieldCompletion[] completions : ALLOWED_FIELDS.values()) {
      for (FieldCompletion completion : completions) {
        res.add(completion.fieldName);
      }
    }
    
    for (FieldCompletion[] completions : ALLOWED_STATIC_FIELDS.values()) {
      for (FieldCompletion completion : completions) {
        res.add(completion.fieldName);
      }
    }
    
    ourFieldNameCache = res;
  }
  
  private final Map<GrailsArtifact, FieldCompletion[]> myMap;

  public GrailsStaticFieldCompletionProvider(boolean aStatic) {
    myMap = aStatic ? ALLOWED_STATIC_FIELDS : ALLOWED_FIELDS;
  }

  public static boolean isGrailsField(@NotNull GrField field) {
    String fieldName = field.getName();
    if (!ourFieldNameCache.contains(fieldName)) return false;

    PsiClass containingClass = field.getContainingClass();
    GrailsArtifact grailsArtifact = GrailsArtifact.getType(containingClass);
    if (grailsArtifact == null) return false;

    Map<GrailsArtifact, FieldCompletion[]> map = field.hasModifierProperty(PsiModifier.STATIC) ? ALLOWED_STATIC_FIELDS : ALLOWED_FIELDS;

    FieldCompletion[] trinities = map.get(grailsArtifact);
    if (trinities == null) return false;

    for (FieldCompletion trinity : trinities) {
      if (trinity.fieldName.equals(fieldName) && (trinity.condition == null || trinity.condition.value(containingClass))) {
        return true;
      }
    }

    return false;
  }
  
  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
    final PsiClass psiClass = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiClass.class);
    if (psiClass == null) return;

    GrailsArtifact artifact = GrailsUtils.calculateArtifactType(psiClass);
    if (artifact == null) return;

    FieldCompletion[] allowedFields = myMap.get(artifact);
    if (allowedFields == null) return;

    for (FieldCompletion completion : allowedFields) {
      String fieldName = completion.fieldName;

      if (psiClass.findFieldByName(fieldName, false) == null) {
        if (completion.condition == null || completion.condition.value(psiClass)) {
          result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(fieldName), completion.tailType));
        }
      }
    }
  }

}
