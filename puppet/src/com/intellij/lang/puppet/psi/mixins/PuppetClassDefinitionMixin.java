package com.intellij.lang.puppet.psi.mixins;

import com.intellij.icons.AllIcons;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightElement;
import com.intellij.lang.puppet.psi.PuppetNameWrapper;
import com.intellij.lang.puppet.psi.PuppetScopeHolder;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.PuppetVariableLightElement;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.lang.puppet.psi.stubs.PuppetClassDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedClassContainedPsiElementBase;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetIncludeClassStatementsStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetSubClassStubsIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PuppetClassDefinitionMixin extends PuppetStubBasedClassContainedPsiElementBase<PuppetClassDefinitionStub>
  implements PuppetClassDefinition {

  private static final List<String> IMPLICIT_VARIABLE_NAMES = Arrays.asList("name", "title");

  public PuppetClassDefinitionMixin(@NotNull PuppetClassDefinitionStub stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetClassDefinitionMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable String getParentClassName() {
    PuppetClassDefinitionStub stub = getStub();
    if (stub != null) {
      return stub.getParentClassName();
    }

    PuppetNameWrapper parentClassReference = getAnyNameWrapper();
    return parentClassReference == null ? null : StringUtil.toLowerCase(parentClassReference.getText());
  }

  private List<PuppetVariable> getImplicitVariables() {
    return CachedValuesManager.getCachedValue(this, () -> {
      List<PuppetVariable> result = new ArrayList<>();

      for (String variableName : IMPLICIT_VARIABLE_NAMES) {
        result.add(new PuppetVariableLightElement(getManager(), getFullQualifiedName(), variableName) {
          @Override
          public @NotNull PuppetScopeHolder getScopeHolder() {
            return PuppetClassDefinitionMixin.this;
          }
        });
      }

      return CachedValueProvider.Result.create(result, this);
    });
  }

  @Override
  public @Nullable PuppetClassDefinition getParentClass() {
    String parentClassName = getParentClassName();
    if (parentClassName == null) {
      return null;
    }

    Collection<PuppetClassDefinition> definitions = PuppetClassStubsIndex.getInstance().find(parentClassName, this);
    return definitions.isEmpty() ? null : definitions.iterator().next();
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    return AllIcons.Nodes.Class;
  }

  @Override
  public @Nullable String getScopeFullQualifiedName() {
    return getFullQualifiedName();
  }

  @Override
  public PsiElement getContext() {
    return new ResolveContext(this);
  }

  @Override
  public boolean processSubClasses(PsiElementProcessor<? super PuppetClassDefinition> processor) {
    return processSubClasses(processor, new HashSet<>());
  }

  private boolean processSubClasses(PsiElementProcessor<? super PuppetClassDefinition> processor,
                                    @NotNull Set<PuppetClassDefinition> recursionSet) {
    String fullQualifiedName = getFullQualifiedName();
    if (StringUtil.isEmpty(fullQualifiedName)) {
      return true;
    }

    for (PuppetClassDefinition subClass : PuppetSubClassStubsIndex.find(fullQualifiedName, this)) {
      if (recursionSet.add(subClass)) {
        if (!processor.execute(subClass)) {
          return false;
        }
        ((PuppetClassDefinitionMixin)subClass).processSubClasses(processor, recursionSet);
      }
    }
    return true;
  }

  @Override
  public boolean processVariablesDeclarations(PsiElementProcessor<? super PuppetVariable> processor) {
    for (PuppetVariable variable : getImplicitVariables()) {
      if (!processor.execute(variable)) {
        return false;
      }
    }
    return PuppetClassDefinition.super.processVariablesDeclarations(processor);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState resolveState,
                                     @Nullable PsiElement lastChildElement,
                                     @NotNull PsiElement originElement) {
    if (super.processDeclarations(processor, resolveState, lastChildElement, originElement)) {
      for (PuppetVariable variable : getImplicitVariables()) {
        if (!processor.execute(variable, resolveState)) {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  @Override
  public boolean processParentClasses(PsiElementProcessor<? super PuppetClassDefinition> processor) {
    return processParentClasses(processor, new HashSet<>());
  }

  public boolean processParentClasses(PsiElementProcessor<? super PuppetClassDefinition> processor,
                                      @NotNull Set<PuppetClassDefinition> recursionSet) {
    PuppetClassDefinition parentClass = getParentClass();
    if (parentClass == null || !recursionSet.add(parentClass)) {
      return true;
    }

    return processor.execute(parentClass) && ((PuppetClassDefinitionMixin)parentClass).processParentClasses(processor, recursionSet);
  }

  private static class ResolveContext extends PuppetDelegatingLightElement<PuppetClassDefinition> {
    ResolveContext(@NotNull PuppetClassDefinition delegate) {
      super(delegate);
    }

    @Override
    public PsiElement getContext() {
      return null;
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                       @NotNull ResolveState resolveState,
                                       @Nullable PsiElement lastChild,
                                       @NotNull PsiElement originElement) {

      // fixme add implicit $name && $title vars

      // traverse parent classes, stop if found  (use processor?)
      Set<String> classSet = new HashSet<>();
      classSet.add(getDelegate().getFullQualifiedName());

      if (!getDelegate().processParentClasses(
        parentClass -> !classSet.add(parentClass.getFullQualifiedName()) ||
                       PuppetResolveUtil.processChildren(parentClass, processor, resolveState, null, originElement)

      )) {
        return false;
      }

      // Add sublcasses for entry points scanning
      getDelegate().processSubClasses(subClass -> {
        classSet.add(subClass.getFullQualifiedName());
        return true;
      });

      // traverse classes instantiation points even if something is found in one
      for (String className : classSet) {
        if (StringUtil.isEmpty(className)) {
          continue;
        }

        for (PsiElement includeStatement : PuppetIncludeClassStatementsStubsIndex.find(className, getDelegate())) {
          PuppetResolveUtil.treeWalkUp(includeStatement, false, processor, resolveState);
        }
      }

      // fixme: ideally, we should collect instantiation points separately and filter out all occurances except first ones
      return true;
    }
  }
}
