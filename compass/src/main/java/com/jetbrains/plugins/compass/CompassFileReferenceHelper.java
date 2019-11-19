package com.jetbrains.plugins.compass;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.sass.SASSFileType;
import org.jetbrains.plugins.scss.SCSSFileType;
import org.jetbrains.plugins.scss.references.SassScssImportReference;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CompassFileReferenceHelper extends FileReferenceHelper {
    private static final String COMPASS_IMPORT = "compass";

    @NotNull
    @Override
    public Collection<PsiFileSystemItem> getContexts(Project project, @NotNull VirtualFile file) {
        return Collections.emptyList();
    }

    @Override
    public boolean isMine(Project project, @NotNull VirtualFile file) {
        return FileTypeManager.getInstance().isFileOfType(file, SCSSFileType.SCSS) ||
                FileTypeManager.getInstance().isFileOfType(file, SASSFileType.SASS);
    }

    @NotNull
    @Override
    public List<? extends LocalQuickFix> registerFixes(FileReference reference) {
        if (reference instanceof SassScssImportReference && reference.getIndex() == 0) {
            if (COMPASS_IMPORT.equals(reference.getText()) && reference.resolve() == null) {
                return ContainerUtil.createMaybeSingletonList(getCompassQuickFix(reference.getElement()));
            }
        }
        return super.registerFixes(reference);
    }

    @Nullable
    private LocalQuickFix getCompassQuickFix(@NotNull PsiElement element) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        return module != null ? new ConfigureCompassQuickFix(module, element) : null;
    }
}
