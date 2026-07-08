package com.intellij.lang.puppet.psi.resolve;

import com.intellij.lang.puppet.ide.navigation.plugins.facts.PuppetFactsIndex;
import com.intellij.lang.puppet.psi.PuppetCompositePsiElement;
import com.intellij.lang.puppet.psi.PuppetFullQualifiedNameOwner;
import com.intellij.lang.puppet.psi.PuppetParametrizedDeclaration;
import com.intellij.lang.puppet.psi.PuppetPolyNamedPsiElement;
import com.intellij.lang.puppet.psi.PuppetScopeHolder;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedPsiElement;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetNamespacesStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTopLevelVariablesStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.lang.puppet.PuppetTokenTypes.CLASS_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.NAMESPACE_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.TYPE_DEFINITION;
import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

public final class PuppetResolveUtil {
  private static final Key<Set<PsiElement>> RECURSION_SET_KEY = new Key<>("puppet.resolve.recursion.map");

  private static final TokenSet NAMED_SYNONIMS_TOKENSET = TokenSet.create(
    CLASS_DEFINITION,
    TYPE_DEFINITION,
    NAMESPACE_DEFINITION
  );


  public static boolean treeWalkUp(@Nullable PsiElement originElement, @NotNull PsiScopeProcessor processor) {
    return treeWalkUp(originElement, false, processor, ResolveState.initial().put(RECURSION_SET_KEY, new HashSet<>()));
  }

  /**
   * Upwalking the PSI tree, processing declarations
   *
   * @param originElement element to start from
   * @param processOrigin by default, orgin element's children are not processed, but we may force this, e.g. for multi-context processing
   * @param processor     delcarations processor
   * @param resolveState  resolve state
   * @return false if walking been stopped by processor
   */
  public static boolean treeWalkUp(@Nullable PsiElement originElement,
                                   boolean processOrigin,
                                   @NotNull PsiScopeProcessor processor,
                                   @NotNull ResolveState resolveState) {
    Set<PsiElement> recursionSet = resolveState.get(RECURSION_SET_KEY);
    assert recursionSet != null;
    PsiElement lastChild = null;
    PsiElement currentElement = originElement;
    while (currentElement != null) {
      if (recursionSet.add(currentElement)) {
        if ((processOrigin || originElement != currentElement) &&
            !currentElement.processDeclarations(processor, resolveState, lastChild, originElement)) {
          return false;
        }
      }
      lastChild = currentElement;
      currentElement = currentElement.getContext();
    }
    return true;
  }

  /**
   * Processes declarations in child objects;
   * IMPORTANT: processing being done with stubs if available and lastChild is null or stub-based.
   * So all resolvable elements must be stubbed; Unsure if this should be controlled form code;
   *
   * @param parentElement    parent element, whose childs we iterating
   * @param processor        elements consumer
   * @param resolveState     resolve ste
   * @param lastChildElement child element to start from. If null - starting from the last child
   * @param originElement    origin element
   * @return false if we should stop iterations
   */
  public static boolean processChildren(@NotNull PsiElement parentElement,
                                        @NotNull PsiScopeProcessor processor,
                                        @NotNull ResolveState resolveState,
                                        @Nullable PsiElement lastChildElement,
                                        @NotNull PsiElement originElement) {
    Set<PsiElement> recursionSet = resolveState.get(RECURSION_SET_KEY);
    assert recursionSet != null;

    PsiScopeProcessor proxyProcessor = new PsiScopeProcessor() {
      @Override
      public boolean execute(@NotNull PsiElement currentElement, @NotNull ResolveState state) {
        if (recursionSet.add(currentElement)) {
          if (currentElement instanceof PsiNamedElement || currentElement instanceof PuppetPolyNamedPsiElement) {
            if (!processor.execute(currentElement, resolveState)) {
              return false;
            }
          }

          if (currentElement instanceof PuppetCompositePsiElement &&
              !(currentElement instanceof PuppetScopeHolder) &&
              !currentElement.processDeclarations(processor, resolveState, null, originElement)
            ) {
            return false;
          }
        }
        return true;
      }
    };

    StubElement parentStubElement = getStubFromElement(parentElement);
    StubElement lastChildStubElement = getStubFromElement(lastChildElement);
    if (parentStubElement != null && (lastChildElement == null || lastChildStubElement != null)) {
      return processChildrenWithStubs(parentStubElement, proxyProcessor, resolveState, lastChildStubElement);
    }
    else {
      return processChildrenWithPsi(parentElement, proxyProcessor, resolveState, lastChildElement);
    }
  }

  /**
   * Processing backwards psi children of the specified parent starting from the last one or lastChildElement
   *
   * @param parentElement    parent element to process
   * @param processor        processor
   * @param resolveState     resolve state
   * @param lastChildElement last element to start from
   * @return false if we should stop processing
   */
  private static boolean processChildrenWithPsi(@NotNull PsiElement parentElement,
                                                @NotNull PsiScopeProcessor processor,
                                                @NotNull ResolveState resolveState,
                                                @Nullable PsiElement lastChildElement
  ) {
    PsiElement currentElement = lastChildElement == null ? parentElement.getLastChild() : lastChildElement.getPrevSibling();

    while (currentElement != null) {
      if (!processor.execute(currentElement, resolveState)) {
        return false;
      }
      currentElement = currentElement.getPrevSibling();
    }
    return true;
  }

  /**
   * Processing backwards stub based children of the current stubbased element starting from the last one or lastChild
   *
   * @param parentStubElement    parent element's stub
   * @param processor            processor
   * @param resolveState         resolve state
   * @param lastChildStubElement last child if any
   * @return false if we should stop processing
   */
  private static boolean processChildrenWithStubs(@NotNull StubElement parentStubElement,
                                                  @NotNull PsiScopeProcessor processor,
                                                  @NotNull ResolveState resolveState,
                                                  @Nullable StubElement lastChildStubElement
  ) {
    @SuppressWarnings("unchecked") List<StubElement> childrenStubElements = parentStubElement.getChildrenStubs();
    if (childrenStubElements.isEmpty()) {
      return true;
    }

    int startIndex = childrenStubElements.size() - 1;
    if (lastChildStubElement != null) {
      int lastIndex = childrenStubElements.lastIndexOf(lastChildStubElement);
      if (lastIndex != -1) {
        startIndex = lastIndex;
      }
    }

    for (int i = startIndex; i >= 0; i--) {
      if (!processor.execute(childrenStubElements.get(i).getPsi(), resolveState)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Reads a stub from an element and returns it
   *
   * @param element element to check
   * @return stub for this element or null if element is null, element has no stub
   */
  public static @Nullable StubElement getStubFromElement(@Nullable PsiElement element) {
    if (element == null) {
      return null;
    }
    if (element instanceof StubBasedPsiElement) {
      return ((StubBasedPsiElement<?>)element).getStub();
    }
    else if (element instanceof PsiFileImpl) {
      return ((PsiFileImpl)element).getStub();
    }
    return null;
  }

  /**
   * Processing children using stubs or psi, filtering children with tokenset without entering certain tokens
   *
   * @param element        element to process
   * @param typesToProcess tokenset of children of interest
   * @param processor      processor
   */
  public static boolean processStubBasedChildrenWithSmartStop(@NotNull PsiElement element,
                                                              final @NotNull TokenSet typesToProcess,
                                                              final @NotNull Processor<? super PuppetStubBasedPsiElement> processor) {
    StubElement stub = getStubFromElement(element);

    if (stub != null) {
      for (PuppetStubBasedPsiElement child : (PuppetStubBasedPsiElement[])stub
        .getChildrenByType(typesToProcess, new PuppetStubBasedPsiElement[0])) {
        if (!processor.process(child)) {
          return false;
        }
      }
    }
    else {
      boolean[] result = new boolean[]{true};

      element.acceptChildren(new PsiElementVisitor() {

        @Override
        public void visitElement(@NotNull PsiElement childElement) {

          if (!result[0]) {
            return;
          }

          if (childElement instanceof PuppetStubBasedPsiElement stubBasedPsiElement) {

            if (typesToProcess.contains((stubBasedPsiElement.getElementType()))) {
              if (!processor.process(stubBasedPsiElement)) {
                result[0] = false;
              }
            }
          }
          else if (element.equals(childElement) ||
                   !(childElement instanceof PuppetParametrizedDeclaration)) { // avoids entering nested classes
            childElement.acceptChildren(this);
          }
        }
      });
      return result[0];
    }
    return true;
  }

  public static @NotNull Collection<PsiElement> getLexicalVariableDeclaration(@NotNull PuppetVariable sourceVariable) {
    if (sourceVariable.isDeclaration()) {
      return Collections.emptyList();
    }

    String fullQualifiedName = sourceVariable.getFullQualifiedName();

    if (StringUtil.isEmpty(fullQualifiedName) || StringUtil.contains(fullQualifiedName, SEPARATOR)) {
      return Collections.emptyList();
    }

    PuppetVariableResolveProcessor processor = new PuppetVariableResolveProcessor(fullQualifiedName);
    if (treeWalkUp(sourceVariable, processor)) {
      processTopScopeVariablesAndFacts(processor, sourceVariable);
    }

    return processor.getResult();
  }

  public static void processTopScopeVariablesAndFacts(@NotNull PuppetVariableScopeProcessor processor, @NotNull PsiElement scopeElement) {
    Set<PsiFile> processedFiles = processor.getProcessedFiles();
    Project project = scopeElement.getProject();

    PuppetFactsIndex.processAllElements(project, scopeElement.getResolveScope(), (processor));
    PuppetTopLevelVariablesStubsIndex.getInstance()
      .processAllElements(project, scopeElement, variable -> {
        if (processedFiles.contains(variable.getContainingFile())) {
          return true;
        }
        return processor.execute(variable, ResolveState.initial());
      });
  }

  /**
   * Processes synonims for given main target element.
   *
   * @param targetElement  target to find synonims for
   * @param scopeProvider  used to get project and resolve scope
   * @param processedNames set of already processed names to avoid duplications
   * @param processor      processor
   */
  public static boolean processElementSynonims(@NotNull PsiElement targetElement,
                                               @NotNull PsiElement scopeProvider,
                                               @NotNull Set<? super String> processedNames,
                                               @NotNull PsiElementProcessor<? super PsiElement> processor) {
    if (!NAMED_SYNONIMS_TOKENSET.contains(PsiUtilCore.getElementType(targetElement))) {
      return true;
    }
    assert targetElement instanceof PuppetFullQualifiedNameOwner;
    String fullQualifiedName = ((PuppetFullQualifiedNameOwner)targetElement).getFullQualifiedName();
    if (StringUtil.isEmpty(fullQualifiedName) || !processedNames.add(fullQualifiedName)) {
      return true;
    }

    List<PsiElement> allSynonims = new ArrayList<>(PuppetClassStubsIndex.getInstance().find(fullQualifiedName, scopeProvider));
    allSynonims.addAll(PuppetTypeStubIndex.getInstance().find(fullQualifiedName, scopeProvider));
    allSynonims.addAll(PuppetNamespacesStubsIndex.getInstance().find(fullQualifiedName, scopeProvider));

    for (PsiElement synonim : allSynonims) {
      if (!processor.execute(synonim)) {
        return false;
      }
    }
    return true;
  }
}
