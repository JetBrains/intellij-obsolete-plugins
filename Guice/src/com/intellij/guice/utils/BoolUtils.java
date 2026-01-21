// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.util.PsiUtil;

final class BoolUtils{
    private BoolUtils(){
        super();
    }

    public static boolean isNegated(PsiExpression exp){
        PsiExpression ancestor = exp;
        while(ancestor.getParent() instanceof PsiParenthesizedExpression){
            ancestor = (PsiExpression) ancestor.getParent();
        }
        if(ancestor.getParent() instanceof PsiPrefixExpression prefixAncestor){
          if(prefixAncestor.getOperationTokenType().equals(JavaTokenType.EXCL)){
                return true;
            }
        }
        return false;
    }

    public static PsiExpression findNegation(PsiExpression exp){
        PsiExpression ancestor = exp;
        while(ancestor.getParent() instanceof PsiParenthesizedExpression){
            ancestor = (PsiExpression) ancestor.getParent();
        }
        if(ancestor.getParent() instanceof PsiPrefixExpression prefixAncestor){
          if(JavaTokenType.EXCL.equals(prefixAncestor.getOperationTokenType())){
                return prefixAncestor;
            }
        }
        return null;
    }

    public static boolean isNegation(PsiExpression exp){
        if(!(exp instanceof PsiPrefixExpression prefixExp)){
            return false;
        }
      return prefixExp.getOperationTokenType().equals(JavaTokenType.EXCL);
    }

    public static PsiExpression getNegated(PsiExpression exp){
        final PsiPrefixExpression prefixExp = (PsiPrefixExpression) exp;
        final PsiExpression operand = prefixExp.getOperand();
      return PsiUtil.skipParenthesizedExprDown(operand);
    }
}
