package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.ide.navigation.plugins.facts.PuppetFactsIndex;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTopLevelVariablesStubsIndex;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.PsiElementProcessor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PuppetVariableReference extends PuppetPolyVariantCachingReferenceWithFullQualifiedName<PuppetVariable> {

  public PuppetVariableReference(PuppetVariable psiElement, TextRange range) {
    super(psiElement, range, psiElement.getFullQualifiedName());
  }

  @Override
  protected @NotNull String adjustNewName(@NonNls String newElementName) {
    return newElementName;
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {
    if (StringUtil.isEmpty(myFullQualifiedName)) {
      return ResolveResult.EMPTY_ARRAY;
    }

    if (myElement.isFullQualified()) // looking using index to avoid unnecessary project traversing
    {
      // check facts if main?
      String namespaceName = myElement.getNamespaceName();
      assert namespaceName != null;

      String variableName = myElement.getName();
      assert variableName != null;

      List<PsiElement> result = new ArrayList<>();

      if (namespaceName.equals(PuppetQualifiedNamesUtil.MAIN_NAMESPACE)) {
        List<PsiElement> externalFacts =
          PuppetFactsIndex.findElementsByKey(variableName, myElement.getProject(), myElement.getResolveScope(), Integer.MAX_VALUE);
        boolean foundFact = !externalFacts.isEmpty();
        result.addAll(externalFacts);

        for (PuppetVariable puppetVariable : PuppetTopLevelVariablesStubsIndex.getInstance().find(variableName, myElement)) {
          if (!puppetVariable.isCoreFact() || !foundFact) {
            result.add(puppetVariable);
          }
        }
      }
      else {
        PsiElementProcessor<PuppetVariable> variableProcessor = variable -> {
          if (StringUtil.equals(variableName, variable.getName())) {
            result.add(variable);
            return false;
          }
          return true;
        };

        PsiElementProcessor<PuppetClassDefinition> parentClassProcessor =
          parentClass -> parentClass.processVariablesDeclarations(variableProcessor);

        Collection<PuppetClassDefinition> classDefinitions = PuppetClassStubsIndex.getInstance().find(namespaceName, myElement);
        for (PuppetClassDefinition classDefinition : classDefinitions) {
          if (classDefinition.processVariablesDeclarations(variableProcessor)) {
            classDefinition.processParentClasses(parentClassProcessor);
          }
        }
      }

      return PsiElementResolveResult.createResults(result);
    }
    else {
      Collection<PsiElement> lexicalVariableDeclarations = PuppetResolveUtil.getLexicalVariableDeclaration(myElement);
      if (!lexicalVariableDeclarations.isEmpty()) {
        return PsiElementResolveResult.createResults(lexicalVariableDeclarations);
      }
    }

    return ResolveResult.EMPTY_ARRAY;
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.variable");
  }
}
