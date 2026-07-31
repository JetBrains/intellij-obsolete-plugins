package com.intellij.gwt.inspections;

import com.intellij.codeInspection.canBeFinal.CanBeFinalHandler;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.rpc.GwtSerializableUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GwtCanBeFinalHandler extends CanBeFinalHandler{
  @Override
  public boolean canBeFinal(@NotNull PsiMember member) {
    if (member instanceof PsiField) {
      final PsiClass aClass = member.getContainingClass();
      if (aClass != null) {
        final GwtFacet facet = BaseGwtInspection.getFacet(aClass);
        if (facet != null) {
          PsiFile containingFile = aClass.getContainingFile();
          if (containingFile != null) {
            VirtualFile virtualFile = containingFile.getVirtualFile();
            if (virtualFile != null) {
              List<GwtModule> gwtModules = GwtModulesManager.getInstance(member.getProject()).findGwtModulesByClientSourceFile(virtualFile);
              if (!gwtModules.isEmpty()) {
                GwtSerializableUtil.SerializableChecker serializableChecker = GwtSerializableUtil.createSerializableChecker(facet, true);
                return !serializableChecker.isGwtSerializable(aClass);
              }
            }
          }
        }
      }
    }
    return true;
  }
}
