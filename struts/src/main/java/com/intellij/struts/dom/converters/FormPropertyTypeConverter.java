/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom.converters;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiType;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public class FormPropertyTypeConverter extends ResolvingConverter.WrappedResolvingConverter<PsiType> {

  private final Class[] myClasses = new Class[]{BigDecimal.class, BigInteger.class, boolean.class, Boolean.class, byte.class, Byte.class,
    char.class, Character.class, Class.class, double.class, Double.class, float.class, Float.class, int.class, Integer.class, long.class,
    Long.class, short.class, Short.class, String.class, Date.class, Time.class, Timestamp.class};
  @Deprecated
  public static final Converter<PsiType> PSI_TYPE_CONVERTER = new Converter<PsiType>() {
    @Override
    public PsiType fromString(final String s, final ConvertContext context) {
      if (s == null) return null;
      try {
        return JavaPsiFacade.getInstance(context.getFile().getProject()).getElementFactory().createTypeFromText(s, null);
      }
      catch (IncorrectOperationException e) {
        return null;
      }
    }

    @Override
    public String toString(final PsiType t, final ConvertContext context) {
      return t == null? null:t.getCanonicalText();
    }

  };

  public FormPropertyTypeConverter() {
    super(PSI_TYPE_CONVERTER);


  }

  @Override
  @NotNull
  public Collection<? extends PsiType> getVariants(final ConvertContext context) {
    Collection<PsiType> myVariants = new ArrayList<>(myClasses.length);
    final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(context.getPsiManager().getProject()).getElementFactory();
    try {
      for (Class clazz : myClasses) {
        PsiType type = elementFactory.createTypeFromText(clazz.getName(), context.getFile());
        myVariants.add(type);
      }
    }
    catch (IncorrectOperationException e) {
      throw new RuntimeException(e);
    }
    return myVariants;
  }
}
