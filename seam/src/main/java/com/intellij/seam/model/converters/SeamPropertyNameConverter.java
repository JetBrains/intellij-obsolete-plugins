package com.intellij.seam.model.converters;

import com.intellij.codeInsight.lookup.LookupValueFactory;
import com.intellij.psi.*;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SeamPropertyNameConverter extends Converter<BeanProperty> implements CustomReferenceConverter<BeanProperty> {
  @Override
  public BeanProperty fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s == null) return null;

    final PsiClass psiClass = getComponentClass(context);
    if (psiClass != null) {
      final PsiMethod method = PropertyUtilBase.getAllProperties(psiClass, true, false).get(s);
      if (method != null) {
        return BeanProperty.createBeanProperty(method);
      }
    }
    return null;
  }

  @Nullable
  private static PsiClass getComponentClass(final ConvertContext context) {
    final SeamDomComponent component = getSeamDomComponent(context);
    if (component != null) {
      final PsiType psiType = component.getComponentType();

      if (psiType instanceof PsiClassType) {
        return ((PsiClassType)psiType).resolve();
      }
    }

    return null;
  }

  @Nullable
  private static SeamDomComponent getSeamDomComponent(final ConvertContext context) {
    return context.getInvocationElement().getParentOfType(SeamDomComponent.class, false);
  }

  @Override
  public String toString(@Nullable final BeanProperty beanProperty, final ConvertContext context) {
    return beanProperty == null ? null : PropertyUtilBase.getPropertyNameBySetter(beanProperty.getMethod());
  }

  @Override
  public PsiReference @NotNull [] createReferences(final GenericDomValue<BeanProperty> genericDomValue,
                                                   final PsiElement element,
                                                   final ConvertContext context) {

    return new PsiReference[]{
      new MyPsiReferenceBase(element, genericDomValue, getUndefinedProperties(getSeamDomComponent(context), getComponentClass(context)))};
  }

  private static List<BeanProperty> getUndefinedProperties(@Nullable final SeamDomComponent seamDomComponent,
                                                           @Nullable final PsiClass componentClass) {
    List<BeanProperty> properties = new ArrayList<>();
    if (seamDomComponent != null && componentClass != null) {

      List<String> alreadyDefinedProperties =
        ContainerUtil.mapNotNull(seamDomComponent.getProperties(), seamProperty -> seamProperty.getPropertyName());

      final Map<String, PsiMethod> map = PropertyUtilBase.getAllProperties(componentClass, true, false);
      for (String propertyName : map.keySet()) {
        if (!alreadyDefinedProperties.contains(propertyName)) {
          properties.add(BeanProperty.createBeanProperty(map.get(propertyName)));
        }
      }

    }
    return properties;
  }

  private static class MyPsiReferenceBase extends PsiReferenceBase<PsiElement> {
    private final GenericDomValue<BeanProperty> myGenericDomValue;
    private final List<BeanProperty> myUndefinedProperties;

    MyPsiReferenceBase(PsiElement element,
                              final GenericDomValue<BeanProperty> genericDomValue,
                              final List<BeanProperty> undefinedProperties) {
      super(element);
      myGenericDomValue = genericDomValue;
      myUndefinedProperties = undefinedProperties;
    }

    @Override
    public PsiElement resolve() {
      final BeanProperty value = myGenericDomValue.getValue();
      return value == null ? null : value.getMethod();
    }

    @Override
    public PsiElement handleElementRename(@NotNull final String newElementName) throws IncorrectOperationException {
      final String name = PropertyUtilBase.getPropertyName(newElementName);
      return super.handleElementRename(name == null ? newElementName : name);
    }

    @Override
    public PsiElement bindToElement(@NotNull final PsiElement element) throws IncorrectOperationException {
      if (element instanceof PsiMethod) {
        final String propertyName = PropertyUtilBase.getPropertyName((PsiMember)element);
        if (propertyName != null) {
          return super.handleElementRename(propertyName);
        }
      }
      return getElement();
    }

    @Override
    public Object @NotNull [] getVariants() {
      return ContainerUtil.map2Array(myUndefinedProperties, beanProperty -> LookupValueFactory.createLookupValueWithHint(beanProperty.getName(), beanProperty.getIcon(0),
                                                                                                                       beanProperty.getPropertyType().getPresentableText()));
    }
  }
}
