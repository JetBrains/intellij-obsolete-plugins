package com.intellij.lang.puppet.ide.navigation;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightElement;
import com.intellij.lang.puppet.psi.PuppetNodeDefinition;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.NavigatablePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.intellij.lang.puppet.psi.PuppetDefaultWrapper.DEFAULT_NAME;
import static com.intellij.lang.puppet.psi.PuppetDefaultWrapper.DEFAULT_PRESENTABLE_NAME;
import static com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration.HEAVY_NAME;
import static com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration.HEAVY_PRESENTABLE_NAME;
import static com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration.SEPARATOR;
import static com.intellij.openapi.util.NullableLazyValue.lazyNullable;

public abstract class PuppetItemPresentation<T extends NavigatablePsiElement> implements ItemPresentation {
  protected final @NotNull String myName;
  protected final @NotNull T myDelegate;
  private final NullableLazyValue<Icon> myIconProvider = lazyNullable(() -> ReadAction.compute(() -> getDelegate().getIcon(0)));
  private final NullableLazyValue<String> myLocationProvider;

  public static PuppetItemPresentation create(@NotNull String name, @NotNull NavigatablePsiElement delegate) {
    return create(name, null, delegate);
  }

  public static PuppetItemPresentation create(@NotNull String name, @Nullable String location, @NotNull NavigatablePsiElement delegate) {
    if (delegate instanceof PuppetDelegatingLightElement) {
      delegate = ((PuppetDelegatingLightElement<?>)delegate).getDelegate();
    }

    if (delegate instanceof PuppetVariable) {
      return new VariablePresentation(name, location, (PuppetVariable)delegate);
    }
    else if (delegate instanceof PuppetResourceInstanceDeclaration) {
      return new ResourceInstancePresentation(name, location, (PuppetResourceInstanceDeclaration)delegate);
    }
    else if (delegate instanceof PuppetNodeDefinition) {
      return new NodePresentation(name, location, (PuppetNodeDefinition)delegate);
    }
    else if (delegate instanceof PuppetClassDefinition) {
      return new ClassPresentation(name, location, (PuppetClassDefinition)delegate);
    }

    return new DefaultPresentation(name, location, delegate);
  }

  private @NotNull T getDelegate() {
    return myDelegate;
  }

  private PuppetItemPresentation(@NotNull String name, @Nullable String location, @NotNull T delegate) {
    myName = name;
    myDelegate = delegate;
    myLocationProvider = location != null ? lazyNullable(() -> location) :
                         lazyNullable(() -> ReadAction.compute(() -> computeLocationString()));
  }

  @Override
  public @Nullable String getPresentableText() {
    return myName;
  }

  @Override
  public final @Nullable Icon getIcon(boolean unused) {
    return myIconProvider.getValue();
  }

  @Override
  public final @Nullable String getLocationString() {
    return myLocationProvider.getValue();
  }

  protected @Nullable String computeLocationString() {
    if (!myDelegate.isValid()) {
      return null;
    }
    VirtualFile vFile = myDelegate.getContainingFile().getVirtualFile();
    if (vFile != null) {
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myDelegate.getProject()).getFileIndex();
      VirtualFile contentRoot = fileIndex.getContentRootForFile(vFile);
      if (contentRoot == null) {
        contentRoot = fileIndex.getClassRootForFile(vFile);
      }
      if (contentRoot != null) {
        final String relativePath = VfsUtilCore.getRelativePath(vFile, contentRoot);
        if (relativePath != null) {
          return relativePath;
        }
      }
    }
    return null;
  }

  private static class DefaultPresentation extends PuppetItemPresentation<NavigatablePsiElement> {
    DefaultPresentation(@NotNull String name, @Nullable String location, @NotNull NavigatablePsiElement delegate) {
      super(name, location, delegate);
    }
  }

  private static class VariablePresentation extends PuppetItemPresentation<PuppetVariable> {
    VariablePresentation(@NotNull String name, @Nullable String location, @NotNull PuppetVariable delegate) {
      super(name, location, delegate);
    }

    @Override
    public @Nullable String getPresentableText() {
      return "$" + super.getPresentableText();
    }

    @Override
    protected @Nullable String computeLocationString() {
      if (myDelegate.isMetaparameter()) {
        return PuppetBundle.message("puppet.metaparameter");
      }
      else if (myDelegate.isCoreFact()) {
        return PuppetBundle.message("puppet.fact");
      }
      else {
        return super.computeLocationString();
      }
    }
  }

  private static class ClassPresentation extends PuppetItemPresentation<PuppetClassDefinition> {
    private final String myFullQualifiedName;

    ClassPresentation(@NotNull String name, @Nullable String location, @NotNull PuppetClassDefinition delegate) {
      super(name, location, delegate);
      myFullQualifiedName = ReadAction.compute(delegate::getFullQualifiedName);
    }

    @Override
    public @Nullable String getPresentableText() {
      return myFullQualifiedName;
    }
  }

  private static class NodePresentation extends PuppetItemPresentation<PuppetNodeDefinition> {
    NodePresentation(@NotNull String name, @Nullable String location, @NotNull PuppetNodeDefinition delegate) {
      super(name, location, delegate);
    }

    @Override
    public @Nullable String getPresentableText() {
      return DEFAULT_NAME.equals(myName) ? DEFAULT_PRESENTABLE_NAME : myName;
    }
  }

  private static class ResourceInstancePresentation extends PuppetItemPresentation<PuppetResourceInstanceDeclaration> {
    ResourceInstancePresentation(@NotNull String name, @Nullable String location, @NotNull PuppetResourceInstanceDeclaration delegate) {
      super(name, location, delegate);
    }

    @Override
    public @Nullable String getPresentableText() {
      int delimiteroffset = myName.lastIndexOf(SEPARATOR);
      String name = delimiteroffset == -1 ? myName : myName.substring(delimiteroffset + 1);

      if (DEFAULT_NAME.equals(name)) {
        return DEFAULT_PRESENTABLE_NAME;
      }
      else if (HEAVY_NAME.equals(name)) {
        return HEAVY_PRESENTABLE_NAME;
      }

      return name;
    }

    @Override
    protected @Nullable String computeLocationString() {
      return "[" +
             PuppetQualifiedNamesUtil.capitalizePuppetName(StringUtil.notNullize(myDelegate.getEffectiveTypeName())) +
             "] in " +
             super.computeLocationString();
    }
  }
}
