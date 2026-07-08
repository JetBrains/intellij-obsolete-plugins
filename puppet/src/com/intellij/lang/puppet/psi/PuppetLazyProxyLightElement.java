package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.NullableLazyValue.atomicLazyNullable;

public class PuppetLazyProxyLightElement extends LightElement implements PuppetCompositePsiElement, PsiNamedElement {
  private final @NotNull String myName;
  private final @NotNull VirtualFile myFile;
  private final @NotNull Project myProject;
  private final int myFileOffset;
  private final @Nullable @Nls String myTypeName;
  private final NullableLazyValue<PsiFile> myTargetPsiFileProvider;
  private final NullableLazyValue<PsiElement> myTargetProvider;

  public PuppetLazyProxyLightElement(@NotNull Project project,
                                     @NotNull String name,
                                     @NotNull VirtualFile file,
                                     int fileOffset,
                                     @Nullable @Nls String typeName) {
    super(PsiManager.getInstance(project), PuppetLanguage.INSTANCE);
    myName = name;
    myFile = file;
    myFileOffset = fileOffset;
    myProject = project;
    myTypeName = typeName;
    myTargetPsiFileProvider = atomicLazyNullable(() -> !myFile.isValid() ? null : getManager().findFile(myFile));
    myTargetProvider = atomicLazyNullable(() -> {
      PsiFile psiFile = myTargetPsiFileProvider.getValue();
      return psiFile == null ? null : psiFile.findElementAt(myFileOffset);
    });
  }

  public @Nullable @Nls String getTypeName() {
    return myTypeName;
  }

  @Override
  public String toString() {
    VirtualFile targetFile = getVirtualFile();
    return "PuppetLazyProxyLightElement: " + myName + " at " + myFileOffset + " in " + (targetFile == null ? null : targetFile.getName());
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  @Override
  public PsiFile getContainingFile() {
    return myTargetPsiFileProvider.getValue();
  }

  public @Nullable VirtualFile getVirtualFile() {
    return myFile;
  }

  @Override
  public @NotNull PsiElement getNavigationElement() {
    PsiElement computedValue = myTargetProvider.getValue();
    return computedValue == null ? super.getNavigationElement() : computedValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PuppetLazyProxyLightElement element = (PuppetLazyProxyLightElement)o;

    if (myFileOffset != element.myFileOffset) return false;
    if (!myName.equals(element.myName)) return false;
    if (!myFile.equals(element.myFile)) return false;
    if (!myProject.equals(element.myProject)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + myFile.hashCode();
    result = 31 * result + myProject.hashCode();
    result = 31 * result + myFileOffset;
    return result;
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Unavailable");
  }
}
