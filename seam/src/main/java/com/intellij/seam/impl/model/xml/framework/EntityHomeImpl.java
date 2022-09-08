package com.intellij.seam.impl.model.xml.framework;

import com.intellij.psi.*;
import com.intellij.seam.model.xml.CustomSeamComponent;
import com.intellij.seam.model.xml.framework.EntityHome;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public abstract class EntityHomeImpl extends CustomSeamComponent implements EntityHome {
  @Override
  @Nullable
  public PsiType getComponentType() {
    PsiClass entityClass = getEntityClass().getValue();

    if (entityClass != null) {
      PsiType psiType = super.getComponentType();
      if (psiType instanceof PsiClassType) {
        PsiClass psiClass = ((PsiClassType)psiType).resolve();
        if (psiClass != null) {
          PsiElementFactory elementFactory = JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory();


          PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
          if (typeParameters.length == 1) {
            Map<PsiTypeParameter, PsiType> map =
              Collections.singletonMap(typeParameters[0], elementFactory.createType(entityClass));
            PsiSubstitutor substitutor = elementFactory.createSubstitutor(map);
            return elementFactory.createType(psiClass, substitutor);
          }
        }
      }
    }
    return super.getComponentType();
  }
}
