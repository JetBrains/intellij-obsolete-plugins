/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.codeInspection;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.MobileModuleUtil;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MissedExecutableInspection extends AbstractBaseJavaLocalInspectionTool {
  private static final Logger LOG = Logger.getInstance(MissedExecutableInspection.class);

  @Override
  @Nls
  @NotNull
  public String getGroupDisplayName() {
    return GroupNames.J2ME_GROUP_NAME;
  }

  @Override
  @Nls
  @NotNull
  public String getDisplayName() {
    return J2MEBundle.message("executable.class.misconfiguration.display.name");
  }

  @Override
  @NonNls
  @NotNull
  public String getShortName() {
    return "MissedExecutable";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }


  @Override
  @NotNull
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override public void visitClass(final PsiClass aClass) {
        super.visitClass(aClass);
        final Module module = ModuleUtil.findModuleForPsiElement(aClass);
        if (module != null && MobileModuleUtil.isExecutable(aClass, module)) {
          final String fqName = aClass.getQualifiedName();
          final J2MEModuleProperties moduleProperties = J2MEModuleProperties.getInstance(module);
          if (moduleProperties != null) {
            final MobileModuleSettings moduleSettings = MobileModuleSettings.getInstance(module);
            LOG.assertTrue(moduleSettings != null);
            if (!moduleSettings.containsMidlet(fqName)) {
              final MobileApplicationType applicationType = moduleProperties.getMobileApplicationType();
              holder.registerProblem(aClass.getNameIdentifier(),
                                     J2MEBundle.message("midlet.undefined.problem.description", applicationType.getPresentableClassName()),
                                     new AddExecutable2Configuration(aClass));
            }
          }
        }
      }
    };
  }

  private static class AddExecutable2Configuration implements LocalQuickFix {
    private final String myClassFQN;

    private final Module myModule;

    AddExecutable2Configuration(final PsiClass aClass) {
      myClassFQN = aClass.getQualifiedName();
      myModule = ModuleUtil.findModuleForPsiElement(aClass);
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return "Append midlet to suite";
    }

    @Nullable
    @Override
    public PsiElement getElementToMakeWritable(@NotNull PsiFile file) {
      return null;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
      return J2MEBundle.message("append.to.suite.quickfix.text", StringUtil.getShortName(myClassFQN), myModule.getName());
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final MobileModuleSettings moduleSettings = MobileModuleSettings.getInstance(myModule);
      LOG.assertTrue(moduleSettings != null);
      moduleSettings.addMidlet(myClassFQN);
    }
  }
}
