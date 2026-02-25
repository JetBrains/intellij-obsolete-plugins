// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.grails.structure.GrailsCommonClassNames;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.NamedArgumentDescriptorBase;
import org.jetbrains.plugins.groovy.extensions.impl.NamedArgumentDescriptorImpl;
import org.jetbrains.plugins.groovy.extensions.impl.StringTypeCondition;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static com.intellij.psi.CommonClassNames.JAVA_LANG_BOOLEAN;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_COMPARABLE;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_DOUBLE;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_FLOAT;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING;
import static com.intellij.psi.CommonClassNames.JAVA_UTIL_COLLECTION;
import static com.intellij.psi.CommonClassNames.JAVA_UTIL_LIST;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.SIMPLE_ON_TOP;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_BOOL;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_INTEGER;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_STRING;
import static org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames.GROOVY_LANG_CLOSURE;
import static org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames.GROOVY_LANG_INT_RANGE;

public class GrailsConstraintNamedArgumentProvider extends GroovyNamedArgumentProvider {
  record Descriptor(Function<? super PsiElement, String> constraintFn, int mask, Object marker) {
  }
  
  static final Map<String, Descriptor> DESCRIPTORS = new LinkedHashMap<>();

  private static final int SUPPORT_ANY = 1;
  private static final int SUPPORT_STRING = 2;
  private static final int SUPPORT_COLLECTIONS = 4;
  private static final int SUPPORT_NOT_PRIMITIVE = 8;
  private static final int SUPPORT_DECIMAL = 16;
  private static final int SUPPORT_COMPARABLE = 32;

  // #CHECK#
  // @see org.codehaus.groovy.grails.validation.ConstrainedProperty.CREDIT_CARD_CONSTRAINT, etc ...
  // + HibernatePluginSupport.doWithSpring() - register 'unique' constraint (org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint)
  static {
    Function<? super PsiElement, GrailsCommonClassNames> producer = GrailsCommonClassNames::getInstance;

    DESCRIPTORS.put("creditCard", new Descriptor(producer.andThen(GrailsCommonClassNames::getCreditCardConstraint),
                                                 SUPPORT_STRING,
                                                 TYPE_BOOL));

    DESCRIPTORS.put("email", new Descriptor(producer.andThen(GrailsCommonClassNames::getEmailConstraint),
                                            SUPPORT_STRING,
                                            TYPE_BOOL));

    DESCRIPTORS.put("blank", new Descriptor(producer.andThen(GrailsCommonClassNames::getBlankConstraint),
                                            SUPPORT_STRING,
                                            TYPE_BOOL));

    DESCRIPTORS.put("range", new Descriptor(producer.andThen(GrailsCommonClassNames::getRangeConstraint),
                                            SUPPORT_COMPARABLE,
                                            new StringTypeCondition("groovy.lang.Range")));

    DESCRIPTORS.put("inList", new Descriptor(producer.andThen(GrailsCommonClassNames::getInListConstraint),
                                             SUPPORT_ANY,
                                             new StringTypeCondition(JAVA_UTIL_LIST)));

    DESCRIPTORS.put("url", new Descriptor(producer.andThen(GrailsCommonClassNames::getUrlConstraint),
                                          SUPPORT_STRING,
                                          new StringArrayTypeCondition(JAVA_LANG_BOOLEAN, JAVA_LANG_STRING, JAVA_UTIL_LIST)));

    DESCRIPTORS.put("size", new Descriptor(producer.andThen(GrailsCommonClassNames::getSizeConstraint),
                                           SUPPORT_COLLECTIONS,
                                           new StringTypeCondition(GROOVY_LANG_INT_RANGE)));

    DESCRIPTORS.put("matches", new Descriptor(producer.andThen(GrailsCommonClassNames::getMatchesConstraint),
                                              SUPPORT_STRING,
                                              TYPE_STRING));

    DESCRIPTORS.put("min", new Descriptor(producer.andThen(GrailsCommonClassNames::getMinConstraint),
                                          SUPPORT_COMPARABLE,
                                          MinMaxArgumentDescriptor.class));

    DESCRIPTORS.put("max", new Descriptor(producer.andThen(GrailsCommonClassNames::getMaxConstraint),
                                          SUPPORT_COMPARABLE,
                                          MinMaxArgumentDescriptor.class));

    DESCRIPTORS.put("maxSize", new Descriptor(producer.andThen(GrailsCommonClassNames::getMaxSizeConstraint),
                                              SUPPORT_COLLECTIONS,
                                              TYPE_INTEGER));

    DESCRIPTORS.put("minSize", new Descriptor(producer.andThen(GrailsCommonClassNames::getMinSizeConstraint),
                                              SUPPORT_COLLECTIONS,
                                              TYPE_INTEGER));

    DESCRIPTORS.put("scale", new Descriptor(producer.andThen(GrailsCommonClassNames::getScaleConstraint),
                                            SUPPORT_DECIMAL,
                                            TYPE_INTEGER));

    DESCRIPTORS.put("notEqual", new Descriptor(producer.andThen(GrailsCommonClassNames::getNotEqualConstraint),
                                               SUPPORT_ANY,
                                               SIMPLE_ON_TOP));

    DESCRIPTORS.put("nullable", new Descriptor(producer.andThen(GrailsCommonClassNames::getNullableConstraint),
                                               SUPPORT_NOT_PRIMITIVE,
                                               TYPE_BOOL));

    DESCRIPTORS.put("validator", new Descriptor(producer.andThen(GrailsCommonClassNames::getValidatorConstraint),
                                                SUPPORT_ANY,
                                                new StringTypeCondition(GROOVY_LANG_CLOSURE)));

    DESCRIPTORS.put("unique", new Descriptor(producer.andThen(GrailsCommonClassNames::getUniqueConstraint),
                                             SUPPORT_ANY,
                                             new StringArrayTypeCondition(JAVA_LANG_BOOLEAN,
                                                                          JAVA_LANG_STRING,
                                                                          JAVA_UTIL_LIST)));
  }

  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolve = resolveResult.getElement();
    PsiType type = GrailsConstraintsUtil.getValidatedValueType(resolve);
    
    int fieldTypeRestriction = -1;
    if (forCompletion && type != null) {
      fieldTypeRestriction = SUPPORT_ANY;

      if (PsiTypes.byteType().equals(type) || PsiTypes.shortType().equals(type) || PsiTypes.intType().equals(type) || PsiTypes.longType()
        .equals(type) ||
          PsiTypes.floatType().equals(type) || PsiTypes.doubleType().equals(type) ||
          InheritanceUtil.isInheritor(type, JAVA_LANG_COMPARABLE)) {
        fieldTypeRestriction |= SUPPORT_COMPARABLE;
      }

      PsiType boxedType = type;
      
      if (type instanceof PsiPrimitiveType) {
        boxedType = TypesUtil.boxPrimitiveType(type, call.getManager(), call.getResolveScope());
      }
      else {
        fieldTypeRestriction |= SUPPORT_NOT_PRIMITIVE;
      }

      if (boxedType.equalsToText(JAVA_LANG_STRING)) {
        fieldTypeRestriction |= SUPPORT_COLLECTIONS | SUPPORT_STRING;
      }
      else if (boxedType instanceof PsiArrayType || InheritanceUtil.isInheritor(boxedType, JAVA_UTIL_COLLECTION)) {
        fieldTypeRestriction |= SUPPORT_COLLECTIONS;
      }
      else if (boxedType.equalsToText(JAVA_LANG_FLOAT) || boxedType.equalsToText(JAVA_LANG_DOUBLE) || boxedType.equalsToText("java.math.BigDecimal")) {
        fieldTypeRestriction |= SUPPORT_DECIMAL;
      }
    }

    for (final Map.Entry<String, Descriptor> entry : DESCRIPTORS.entrySet()) {
      if (argumentName != null && !argumentName.equals(entry.getKey())) continue;
      
      if ((fieldTypeRestriction & entry.getValue().mask) == 0) continue;

      String name = entry.getKey();

      if ("unique".equals(name) && !isDomainConstraintMethod(call)) continue;

      Object argumentDescriptorMarker = entry.getValue().marker;

      NamedArgumentDescriptor argumentDescriptor;

      if (argumentDescriptorMarker == MinMaxArgumentDescriptor.class) {
        argumentDescriptor = type == null ? SIMPLE_ON_TOP : new MinMaxArgumentDescriptor(type);
      }
      else {
        argumentDescriptor = (NamedArgumentDescriptor)argumentDescriptorMarker;
      }

      result.put(name, new MyArgumentDescriptor(entry.getValue().constraintFn.apply(call), argumentDescriptor, resolve));
    }

    if (argumentName == null || argumentName.equals("shared")) {
      result.put("shared", SIMPLE_ON_TOP);
    }
  }

  private static boolean isDomainConstraintMethod(GrCall method) {
    GrClosableBlock closure = PsiTreeUtil.getParentOfType(method, GrClosableBlock.class);
    if (closure == null) return false;

    PsiElement parent = closure.getParent();
    if (!(parent instanceof GrField field)) return false;

    if (!"constraints".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) return false;

    PsiClass aClass = field.getContainingClass();
    return GormUtils.isGormBean(aClass);
  }

  static class MinMaxArgumentDescriptor extends NamedArgumentDescriptorBase {
    
    private final PsiType myFieldType;
    
    MinMaxArgumentDescriptor(@NotNull PsiType fieldType) {
      myFieldType = fieldType;
    }

    @Override
    public boolean checkType(@NotNull PsiType type, @NotNull GroovyPsiElement context) {
      PsiType boxedType = TypesUtil.boxPrimitiveType(type, context.getManager(), context.getResolveScope());

      return boxedType.isAssignableFrom(myFieldType);
    }
  }

  static class MyArgumentDescriptor extends NamedArgumentDescriptorImpl {
    private final PsiElement myContext;
    private final String myClassToNavigate;
    private final NamedArgumentDescriptor myDelegatedArgumentDescriptor;

    MyArgumentDescriptor(String classToNavigate, NamedArgumentDescriptor delegatedArgumentDescriptor, PsiElement context) {
      myContext = context;
      myClassToNavigate = classToNavigate;
      myDelegatedArgumentDescriptor = delegatedArgumentDescriptor;
    }

    @Override
    public boolean checkType(@NotNull PsiType type, @NotNull GroovyPsiElement context) {
      return myDelegatedArgumentDescriptor.checkType(type, context);
    }

    @Override
    public PsiElement getNavigationElement() {
      return JavaPsiFacade.getInstance(myContext.getProject()).findClass(myClassToNavigate, myContext.getResolveScope());
    }
  }

  static class StringArrayTypeCondition extends NamedArgumentDescriptorBase {

    private final String @NotNull [] myTypeNames;

    StringArrayTypeCondition(String @NotNull ... typeNames) {
      super();
      myTypeNames = typeNames;
    }

    @Override
    public boolean checkType(@NotNull PsiType type, @NotNull GroovyPsiElement context) {
      for (String typeName : myTypeNames) {
        if (InheritanceUtil.isInheritor(type, typeName)) {
          return true;
        }
      }
      return false;
    }
  }
}
