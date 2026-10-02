// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.GuiceEntryProducer;
import com.intellij.guice.model.GuiceQualifier;
import com.intellij.guice.model.GuiceQualifiers;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contributes Guice bindings for Google flag binders:
 * <ul>
 *   <li>{@code com.google.common.inject.FlagBinder} and {@code @com.google.common.flags.FlagSpec}</li>
 *   <li>{@code com.google.api.server.core.DynamicFlagBinder} and {@code @com.google.api.server.core.DynamicFlag}</li>
 *   <li>{@code com.google.apps.framework.experiments.ExperimentFlagModule} and
 *       {@code @com.google.experiments.framework.ExperimentFlagSpec}</li>
 * </ul>
 */
@ApiStatus.Internal
final class FlagBinderContributor implements GuiceBindingContributor {
  private static final String FLAG_CLASS = "com.google.common.flags.Flag";
  private static final String FLAG_SPEC_ANNO = "com.google.common.flags.FlagSpec";
  private static final String FLAG_BINDER_CLASS = "com.google.common.inject.FlagBinder";

  private static final String DYNAMIC_FLAG_ANNO = "com.google.api.server.core.DynamicFlag";
  private static final String DYNAMIC_FLAG_BINDER_CLASS = "com.google.api.server.core.DynamicFlagBinder";
  private static final String DYNAMIC_FLAG_HANDLE_CLASS =
      "com.google.api.server.core.DynamicFlagManager.DynamicFlag";

  private static final String EXPERIMENT_FLAG_CLASS = "com.google.experiments.framework.ExperimentFlag";
  private static final String EXPERIMENT_FLAG_SPEC_ANNO = "com.google.experiments.framework.ExperimentFlagSpec";
  private static final String EXPERIMENT_FLAG_MODULE_CLASS =
      "com.google.apps.framework.experiments.ExperimentFlagModule";
  private static final String EXPERIMENT_FLAG_MODULE_BUILDER_CLASS =
      "com.google.apps.framework.experiments.ExperimentFlagModule.Builder";
  private static final String EXPERIMENT_VALUE_ANNO =
      "com.google.apps.framework.annotations.ExperimentValue";

  private static final Set<String> NON_VALUE_EXPERIMENT_ANNOTATIONS = Set.of(
      "com.google.apps.framework.annotations.JsExperiment",
      "com.google.apps.framework.annotations.SoyExperiment"
  );

  private enum NamesToBind {
    NONE,
    PRIMARY,
    ALT,
    BOTH
  }

  private record FlagBindingSpec(@NotNull PsiType type, @NotNull GuiceQualifier qualifier) {}

  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    // 1. Register flag field annotations so inspections and implicit-usage checks recognize them
    // without globally scanning unbound flag classes during index discovery.
    registrar.registerBindingFieldAnnotations(
        List.of(FLAG_SPEC_ANNO, DYNAMIC_FLAG_ANNO, EXPERIMENT_FLAG_SPEC_ANNO)
    );

    // 2. FlagBinder module calls.
    registrar.registerCallWithContext(
        GuiceCallPattern.named("createModule")
            .withOwnerClass(FLAG_BINDER_CLASS)
            .minArguments(1),
        (call, context) -> processFlagBinderCall(call, 0, NamesToBind.NONE, context)
    );
    registrar.registerCallWithContext(
        GuiceCallPattern.named("legacyCreateModuleForBindingAnnotationsOrNameAndAltName")
            .withOwnerClass(FLAG_BINDER_CLASS)
            .minArguments(1),
        (call, context) -> processFlagBinderCall(call, 0, NamesToBind.BOTH, context)
    );
    registrar.registerCallWithContext(
        GuiceCallPattern.named("bind")
            .withOwnerClass(FLAG_BINDER_CLASS)
            .minArguments(1),
        (call, context) -> processFlagBinderCall(call, 0, resolveFlagBinderMode(call), context)
    );

    // 3. DynamicFlagBinder module calls.
    registrar.registerCallWithContext(
        GuiceCallPattern.named("dynamicFlagsModule")
            .withOwnerClass(DYNAMIC_FLAG_BINDER_CLASS)
            .minArguments(1),
        (call, context) -> processDynamicFlagBinderCall(call, 0, false, context)
    );
    registrar.registerCallWithContext(
        GuiceCallPattern.named("bind")
            .withOwnerClass(DYNAMIC_FLAG_BINDER_CLASS)
            .minArguments(2),
        (call, context) -> processDynamicFlagBinderCall(call, 1, true, context)
    );

    // 4. ExperimentFlagModule calls.
    registrar.registerCallWithContext(
        GuiceCallPattern.named("setFlagContainer")
            .withOwnerClass(EXPERIMENT_FLAG_MODULE_BUILDER_CLASS)
            .argumentCount(1),
        FlagBinderContributor::processExperimentFlagModuleCall
    );
    registrar.registerCallWithContext(
        GuiceCallPattern.named("ExperimentFlagModule")
            .withOwnerClass(EXPERIMENT_FLAG_MODULE_CLASS)
            .minArguments(1)
            .maxArguments(2),
        FlagBinderContributor::processExperimentFlagModuleCall
    );
  }

  // -----------------------------------------------------------------------
  // Binder call handlers
  // -----------------------------------------------------------------------

  private static void processFlagBinderCall(@NotNull UCallExpression call,
                                            int startArgIndex,
                                            @NotNull NamesToBind mode,
                                            @NotNull GuiceCallContext context) {
    List<UExpression> args = call.getValueArguments();
    for (int i = startArgIndex; i < args.size(); i++) {
      context.reportClassArgument(args.get(i), (flagClass, entries) -> {
        addImplementationReference(call, flagClass, entries);
        for (PsiField field : flagClass.getFields()) {
          if (!field.hasModifierProperty(PsiModifier.STATIC)) continue;
          if (hasAnnotation(field, DYNAMIC_FLAG_ANNO)) continue;
          for (FlagBindingSpec spec : collectFlagSpecBindings(field, mode)) {
            entries.add(GuiceEntryProducer.createFieldBindingEntry(field, spec.type(), spec.qualifier()));
          }
        }
      });
    }
  }

  private static void processDynamicFlagBinderCall(@NotNull UCallExpression call,
                                                   int startArgIndex,
                                                   boolean includeLegacyStaticFlags,
                                                   @NotNull GuiceCallContext context) {
    List<UExpression> args = call.getValueArguments();
    for (int i = startArgIndex; i < args.size(); i++) {
      context.reportClassArgument(args.get(i), (flagClass, entries) -> {
        addImplementationReference(call, flagClass, entries);
        for (PsiField field : flagClass.getFields()) {
          if (!field.hasModifierProperty(PsiModifier.STATIC)) continue;
          if (hasAnnotation(field, DYNAMIC_FLAG_ANNO)) {
            for (FlagBindingSpec spec : collectDynamicFlagBindings(field)) {
              entries.add(GuiceEntryProducer.createFieldBindingEntry(field, spec.type(), spec.qualifier()));
            }
          }
          else if (includeLegacyStaticFlags) {
            for (FlagBindingSpec spec : collectFlagSpecBindings(field, NamesToBind.BOTH)) {
              entries.add(GuiceEntryProducer.createFieldBindingEntry(field, spec.type(), spec.qualifier()));
            }
          }
        }
      });
    }
  }

  private static void processExperimentFlagModuleCall(@NotNull UCallExpression call,
                                                      @NotNull GuiceCallContext context) {
    List<UExpression> args = call.getValueArguments();
    if (args.isEmpty()) return;
    context.reportClassArgument(args.getFirst(), (flagClass, entries) -> {
      addImplementationReference(call, flagClass, entries);
      for (PsiField field : flagClass.getFields()) {
        if (!field.hasModifierProperty(PsiModifier.STATIC)) continue;
        for (FlagBindingSpec spec : collectExperimentFlagBindings(field)) {
          entries.add(GuiceEntryProducer.createFieldBindingEntry(field, spec.type(), spec.qualifier()));
        }
      }
    });
  }

  private static void addImplementationReference(@NotNull UCallExpression call,
                                                 @NotNull PsiClass flagClass,
                                                 @NotNull Set<GuiceEntry> entries) {
    GuiceEntry implRef = GuiceEntryProducer.createCallImplementationReferenceEntry(call, flagClass);
    if (implRef != null) {
      entries.add(implRef);
    }
  }

  // -----------------------------------------------------------------------
  // Binding spec extraction per flag type
  // -----------------------------------------------------------------------

  private static @NotNull List<FlagBindingSpec> collectFlagSpecBindings(@NotNull PsiField field,
                                                                        @NotNull NamesToBind mode) {
    String[] nameAndAlt = getFlagSpecNameAndAltName(field);
    if (nameAndAlt == null) return List.of();

    PsiType valueType = extractTypeParameter(field.getType(), FLAG_CLASS);
    if (valueType == null) return List.of();

    GuiceQualifier explicitQualifier = GuiceQualifiers.fromDeclaration(field);
    if (explicitQualifier != null && !(explicitQualifier instanceof GuiceQualifier.Unknown)) {
      return List.of(new FlagBindingSpec(valueType, explicitQualifier));
    }

    if (mode == NamesToBind.NONE) {
      return List.of();
    }

    String name = nameAndAlt[0];
    String altName = nameAndAlt[1];
    boolean hasAlt = altName != null && !altName.isEmpty() && !altName.equals(name);

    List<FlagBindingSpec> result = new ArrayList<>(2);
    switch (mode) {
      case PRIMARY -> {
        if (name != null && !name.isEmpty()) {
          result.add(new FlagBindingSpec(valueType, new GuiceQualifier.Named(name)));
        }
      }
      case ALT -> {
        String chosen = hasAlt ? altName : name;
        if (chosen != null && !chosen.isEmpty()) {
          result.add(new FlagBindingSpec(valueType, new GuiceQualifier.Named(chosen)));
        }
      }
      case BOTH -> {
        if (name != null && !name.isEmpty()) {
          result.add(new FlagBindingSpec(valueType, new GuiceQualifier.Named(name)));
        }
        if (hasAlt) {
          result.add(new FlagBindingSpec(valueType, new GuiceQualifier.Named(altName)));
        }
      }
      case NONE -> {}
    }
    return result;
  }

  private static @NotNull List<FlagBindingSpec> collectDynamicFlagBindings(@NotNull PsiField field) {
    if (!hasAnnotation(field, DYNAMIC_FLAG_ANNO)) return List.of();
    String[] nameAndAlt = getFlagSpecNameAndAltName(field);
    if (nameAndAlt == null) return List.of();

    PsiType valueType = extractTypeParameter(field.getType(), FLAG_CLASS);
    if (valueType == null) return List.of();

    String name = nameAndAlt[0];
    String altName = nameAndAlt[1];
    String bindName = (name == null || name.isEmpty()) ? field.getName() : name;

    List<FlagBindingSpec> result = new ArrayList<>(4);
    GuiceQualifier primaryNamed = new GuiceQualifier.Named(bindName);
    result.add(new FlagBindingSpec(valueType, primaryNamed));

    PsiType dynamicFlagHandleType = createParameterizedType(field, DYNAMIC_FLAG_HANDLE_CLASS, valueType);
    if (dynamicFlagHandleType != null) {
      result.add(new FlagBindingSpec(dynamicFlagHandleType, primaryNamed));
    }

    if (altName != null && !altName.isEmpty() && !altName.equals(bindName)) {
      result.add(new FlagBindingSpec(valueType, new GuiceQualifier.Named(altName)));
    }

    GuiceQualifier explicitQualifier = GuiceQualifiers.fromDeclaration(field);
    if (explicitQualifier != null && !(explicitQualifier instanceof GuiceQualifier.Unknown)) {
      result.add(new FlagBindingSpec(valueType, explicitQualifier));
    }

    return result;
  }

  private static @NotNull List<FlagBindingSpec> collectExperimentFlagBindings(@NotNull PsiField field) {
    String flagName = getAnnotationStringAttribute(field, EXPERIMENT_FLAG_SPEC_ANNO, "name");
    if (flagName == null) return List.of();

    PsiType valueType = extractTypeParameter(field.getType(), EXPERIMENT_FLAG_CLASS);
    if (valueType == null) return List.of();

    GuiceQualifier explicitQualifier = GuiceQualifiers.fromDeclaration(field, NON_VALUE_EXPERIMENT_ANNOTATIONS);
    if (explicitQualifier != null && !(explicitQualifier instanceof GuiceQualifier.Unknown)) {
      return List.of(new FlagBindingSpec(valueType, explicitQualifier));
    }

    if (flagName.isEmpty()) return List.of();
    GuiceQualifier experimentValueQualifier =
        new GuiceQualifier.Instance(EXPERIMENT_VALUE_ANNO, "name=" + flagName + ";");
    return List.of(new FlagBindingSpec(valueType, experimentValueQualifier));
  }

  // -----------------------------------------------------------------------
  // Receiver mode & helper utilities
  // -----------------------------------------------------------------------

  private static @NotNull NamesToBind resolveFlagBinderMode(@NotNull UCallExpression bindCall) {
    UExpression current = bindCall.getReceiver();
    while (current != null) {
      current = GuiceUtils.skipParenthesesAndCasts(current);
      if (current instanceof UQualifiedReferenceExpression qualified) {
        UElement selector = qualified.getSelector();
        if (selector instanceof UCallExpression receiverCall) {
          NamesToBind mode = modeFromMethodName(receiverCall.getMethodName());
          if (mode != null) return mode;
        }
        current = qualified.getReceiver();
      }
      else if (current instanceof UCallExpression receiverCall) {
        NamesToBind mode = modeFromMethodName(receiverCall.getMethodName());
        if (mode != null) return mode;
        current = receiverCall.getReceiver();
      }
      else if (current instanceof UReferenceExpression ref) {
        PsiElement resolved = ref.resolve();
        if (resolved instanceof PsiVariable variable && !(resolved instanceof PsiField)) {
          UElement uVar = UastContextKt.toUElement(variable);
          if (uVar instanceof UVariable uVariable && uVariable.getUastInitializer() != null) {
            current = uVariable.getUastInitializer();
            continue;
          }
        }
        break;
      }
      else {
        break;
      }
    }
    return NamesToBind.NONE;
  }

  private static @Nullable NamesToBind modeFromMethodName(@Nullable String methodName) {
    if (methodName == null) return null;
    return switch (methodName) {
      case "legacyForBindingAnnotationsOrNameAndAltName" -> NamesToBind.BOTH;
      case "legacyForBindingAnnotationsOrName" -> NamesToBind.PRIMARY;
      case "legacyForBindingAnnotationsOrAltName" -> NamesToBind.ALT;
      default -> null;
    };
  }

  private static @Nullable PsiType extractTypeParameter(@NotNull PsiType fieldType, @NotNull String containerFqn) {
    PsiType substituted = PsiUtil.substituteTypeParameter(fieldType, containerFqn, 0, false);
    if (substituted instanceof PsiWildcardType wildcard && wildcard.isExtends()) {
      return wildcard.getExtendsBound();
    }
    return substituted;
  }

  private static @Nullable PsiType createParameterizedType(@NotNull PsiElement context,
                                                           @NotNull String rawClassFqn,
                                                           @NotNull PsiType typeParameter) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(context.getProject());
    PsiClass rawClass = facade.findClass(rawClassFqn, context.getResolveScope());
    if (rawClass == null) return null;
    return facade.getElementFactory().createType(rawClass, typeParameter);
  }

  private static boolean hasAnnotation(@NotNull PsiField field, @NotNull String annotationFqn) {
    if (AnnotationUtil.isAnnotated(field, annotationFqn, 0)) {
      return true;
    }
    UAnnotation uAnnotation = findKotlinPropertyAnnotation(field, annotationFqn);
    return uAnnotation != null;
  }

  /**
   * Returns a two-element array {@code [name, altName]} if the field has {@code @FlagSpec},
   * or {@code null} if it is not annotated with {@code @FlagSpec}.
   */
  private static String @Nullable [] getFlagSpecNameAndAltName(@NotNull PsiField field) {
    PsiAnnotation annotation = field.getAnnotation(FLAG_SPEC_ANNO);
    if (annotation != null) {
      String name = AnnotationUtil.getStringAttributeValue(annotation, "name");
      String altName = AnnotationUtil.getStringAttributeValue(annotation, "altName");
      return new String[]{name != null ? name : "", altName != null ? altName : ""};
    }
    UAnnotation uAnnotation = findKotlinPropertyAnnotation(field, FLAG_SPEC_ANNO);
    if (uAnnotation != null) {
      UExpression nameExpr = uAnnotation.findAttributeValue("name");
      UExpression altExpr = uAnnotation.findAttributeValue("altName");
      String name = nameExpr != null ? UastUtils.evaluateString(nameExpr) : "";
      String altName = altExpr != null ? UastUtils.evaluateString(altExpr) : "";
      return new String[]{name != null ? name : "", altName != null ? altName : ""};
    }
    return null;
  }

  private static @Nullable String getAnnotationStringAttribute(@NotNull PsiField field,
                                                               @NotNull String annotationFqn,
                                                               @NotNull String attributeName) {
    PsiAnnotation annotation = field.getAnnotation(annotationFqn);
    if (annotation != null) {
      String value = AnnotationUtil.getStringAttributeValue(annotation, attributeName);
      return value != null ? value : "";
    }
    UAnnotation uAnnotation = findKotlinPropertyAnnotation(field, annotationFqn);
    if (uAnnotation != null) {
      UExpression expr = uAnnotation.findAttributeValue(attributeName);
      String value = expr != null ? UastUtils.evaluateString(expr) : "";
      return value != null ? value : "";
    }
    return null;
  }

  private static @Nullable UAnnotation findKotlinPropertyAnnotation(@NotNull PsiField field,
                                                                    @NotNull String annotationFqn) {
    PsiElement navElem = field.getNavigationElement();
    if (navElem == null || navElem == field) return null;
    UElement uElement = UastContextKt.toUElement(navElem);
    if (uElement instanceof org.jetbrains.uast.UAnnotated annotated) {
      return annotated.findAnnotation(annotationFqn);
    }
    return null;
  }
}
