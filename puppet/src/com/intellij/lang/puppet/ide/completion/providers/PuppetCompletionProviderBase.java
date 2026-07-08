package com.intellij.lang.puppet.ide.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.puppet.ide.completion.PuppetLookupElements;
import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.lang.puppet.ide.navigation.plugins.facts.PuppetFactsIndex;
import com.intellij.lang.puppet.ide.navigation.plugins.ruby.PuppetRubyPluginsIndex;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.PuppetDataType;
import com.intellij.lang.puppet.psi.PuppetDataTypeParameterInfo;
import com.intellij.lang.puppet.psi.PuppetDataTypes;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.PuppetElementPatterns;
import com.intellij.lang.puppet.psi.PuppetPsiFileImpl;
import com.intellij.lang.puppet.psi.PuppetResourceDeclarationBase;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.PuppetResourceLikeClassDescription;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.lang.puppet.psi.resolve.PuppetVariableScopeProcessor;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetFunctionsStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetResourceInstanceByTypeStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTopLevelVariablesStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.intellij.lang.puppet.ide.completion.PuppetCompletionContributor.PUPPET_DUMMY_IDENTIFIER;
import static com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil.PUPPET_METAPARAMETERS_STUB_TYPE_NAME;
import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

@ApiStatus.Internal
public abstract class PuppetCompletionProviderBase extends CompletionProvider<CompletionParameters> implements PuppetElementPatterns {
  private static final Key<Boolean> STARTS_WITH_SEPARATOR = new Key<>("puppet.completion.separator");

  protected CompletionResultSet adjustPrefixMatcher(@NotNull CompletionParameters parameters,
                                                    @NotNull ProcessingContext context,
                                                    @NotNull CompletionResultSet result) {

    String text = parameters.getPosition().getText();

    int dummyIndex = text.indexOf(PUPPET_DUMMY_IDENTIFIER);

    if (dummyIndex != -1) {
      text = text.substring(0, dummyIndex);
    }

    if (StringUtil.startsWith(text, SEPARATOR)) {
      text = text.substring(SEPARATOR.length());
      context.put(STARTS_WITH_SEPARATOR, true);
    }
    return result.withPrefixMatcher(text);
  }

  private static @NotNull PsiElement getScopeProvider(@NotNull CompletionParameters parameters) {
    PsiElement scopeProvider = parameters.getOriginalPosition();
    return scopeProvider == null ? parameters.getOriginalFile() : scopeProvider;
  }

  protected static void fillWithClasses(@NotNull CompletionParameters parameters,
                                        @NotNull CompletionResultSet result,
                                        boolean capitalize) {

    PsiElement scopeProvider = getScopeProvider(parameters);
    PuppetClassStubsIndex.getInstance().processAllElements(scopeProvider.getProject(), scopeProvider, classDefinition -> {
      LookupElement lookupElement = PuppetLookupElements.forClass(classDefinition, capitalize);
      if (lookupElement != null) {
        result.addElement(lookupElement);
      }
      return true;
    });
  }

  protected static void fillWithClassParameters(@NotNull CompletionParameters parameters,
                                                @NotNull ProcessingContext context,
                                                @NotNull CompletionResultSet result
  ) {
    if (context.get(STARTS_WITH_SEPARATOR) != null) {
      return;
    }

    PsiElement position = parameters.getPosition();
    PuppetResourceLikeClassDescription classDescription = PsiTreeUtil.getParentOfType(position, PuppetResourceLikeClassDescription.class);
    if (classDescription == null) {
      return;
    }

    PsiElement scopeProvider = getScopeProvider(parameters);
    Set<String> addedParameters = new HashSet<>();

    for (String className : classDescription.getNamesList()) {
      for (PuppetClassDefinition classDefinition : PuppetClassStubsIndex.getInstance().find(className, scopeProvider)) {
        classDefinition.processParametersDeclarations(parameterDeclaration -> {
          String parameterName = parameterDeclaration.getName();
          if (StringUtil.isNotEmpty(parameterName) && addedParameters.add(parameterName)) {
            result.addElement(PuppetLookupElements.forExternalParameter(parameterName, "", parameterDeclaration));
          }
          return true;
        });
      }
    }
  }

  protected static void fillWithTypeParameters(@NotNull CompletionParameters parameters,
                                               @NotNull ProcessingContext context,
                                               @NotNull CompletionResultSet result
  ) {
    if (context.get(STARTS_WITH_SEPARATOR) != null) {
      return;
    }

    PsiElement position = parameters.getPosition();
    PuppetResourceDeclarationBase resourceDeclaration = PsiTreeUtil.getParentOfType(position, PuppetResourceDeclarationBase.class);

    if (resourceDeclaration == null) {
      return;
    }

    String typeName = resourceDeclaration.getEffectiveTypeName();
    if (StringUtil.isEmpty(typeName)) {
      return;
    }

    fillWithTypeParameters(typeName, parameters, context, result);

    // we can't move this to the type PSI element, like in resolve context for TypeDefinition, because we have ruby-defined types
    if (!StringUtil.equals(typeName, PUPPET_METAPARAMETERS_STUB_TYPE_NAME)) {
      fillWithTypeParameters(PUPPET_METAPARAMETERS_STUB_TYPE_NAME, parameters, context, result);
    }
  }

  protected static void fillWithTypeParameters(@NotNull String typeName,
                                               @NotNull CompletionParameters parameters,
                                               @NotNull ProcessingContext context,
                                               @NotNull CompletionResultSet result
  ) {
    if (context.get(STARTS_WITH_SEPARATOR) != null) {
      return;
    }

    PsiElement scopeProvider = getScopeProvider(parameters);

    boolean[] foundInRuby = new boolean[]{false};

    PuppetRubyPluginsIndex.processElementParameters(
      PuppetRubyPluginsIndex.SymbolType.TYPE,
      typeName,
      scopeProvider.getProject(),
      scopeProvider.getResolveScope(),
      (name, element) -> {
        foundInRuby[0] = true;
        result.addElement(PuppetLookupElements.forExternalParameter(name, "", element));
      }
    );

    if (foundInRuby[0]) {
      return;
    }

    for (PuppetTypeDefinition typeDefinition : PuppetTypeStubIndex.getInstance().find(typeName, scopeProvider)) {
      typeDefinition.processParametersDeclarations(parameterDeclaration -> {
        String parameterName = parameterDeclaration.getName();
        if (StringUtil.isNotEmpty(parameterName)) {
          result.addElement(PuppetLookupElements.forExternalParameter(parameterName, "", parameterDeclaration));
        }
        return true;
      });
    }
  }

  protected static void fillWithFunctions(@NotNull CompletionParameters parameters,
                                          @NotNull ProcessingContext context,
                                          @NotNull CompletionResultSet result
  ) {
    // fixme we should adjust functions naming to distinct func from ::func and adjust following algorythm for manual control
    if (context.get(STARTS_WITH_SEPARATOR) != null) {
      return;
    }

    PsiElement scopeProvider = getScopeProvider(parameters);
    Project project = scopeProvider.getProject();

    Map<String, LookupElement> stubElements = new HashMap<>();
    Set<String> foundNonStubNames = new HashSet<>();

    PuppetRubyPluginsIndex
      .processAllElements(PuppetRubyPluginsIndex.SymbolType.FUNCTION, project, scopeProvider.getResolveScope(), (name, element) -> {
        LookupElement lookupElement = PuppetLookupElements.forExternalFunction(name, element);
        if (PuppetLibraryUtil.isFunctionStubElement(element)) {
          stubElements.put(name, lookupElement);
        }
        else {
          foundNonStubNames.add(name);
          result.addElement(lookupElement);
        }
      });

    for (Map.Entry<String, LookupElement> entry : stubElements.entrySet()) {
      if (!foundNonStubNames.contains(entry.getKey())) {
        result.addElement(entry.getValue());
      }
    }

    PuppetFunctionsStubsIndex.getInstance().processAllElements(project, scopeProvider, functionDefinition -> {
      String functionName = functionDefinition.getFullQualifiedName();
      assert functionName != null;
      result.addElement(PuppetLookupElements.forExternalFunction(functionName, functionDefinition));
      return true;
    });
  }

  protected static void fillWithDataTypes(@NotNull CompletionResultSet result) {
    for (String typeName : PuppetDataTypes.ALL_DATA_TYPES) {
      result.addElement(PuppetLookupElements.forDataType(typeName));
    }
  }

  protected static void fillWithResourceTypes(@NotNull CompletionParameters parameters,
                                              @NotNull ProcessingContext context,
                                              @NotNull CompletionResultSet result,
                                              boolean capitalize) {

    boolean onlyPuppetDefined = context.get(STARTS_WITH_SEPARATOR) != null;

    PsiElement scopeProvider = getScopeProvider(parameters);

    Set<String> rubyDefinedNames = new HashSet<>();

    if (!onlyPuppetDefined) {
      PuppetRubyPluginsIndex
        .processAllElements(PuppetRubyPluginsIndex.SymbolType.TYPE, scopeProvider.getProject(), scopeProvider.getResolveScope(),
                            (name, element) -> {
                              rubyDefinedNames.add(name);
                              result.addElement(PuppetLookupElements.forTypeDefinition(element, name, capitalize));
                            });
    }

    PuppetTypeStubIndex.getInstance().processAllElements(scopeProvider.getProject(), scopeProvider, typeDefinition -> {
      String typeName = typeDefinition.getFullQualifiedName();
      boolean isInStubFile = PuppetPsiFileImpl.isInBuiltInStubsFile(typeDefinition);
      if (!StringUtil.equals(typeName, PUPPET_METAPARAMETERS_STUB_TYPE_NAME) &&
          (!rubyDefinedNames.contains(typeName) || !isInStubFile) &&
          (!onlyPuppetDefined || !isInStubFile)
        ) {
        LookupElement lookupElement = PuppetLookupElements.forTypeDefinition(typeDefinition, capitalize);
        if (lookupElement != null) {
          result.addElement(lookupElement);
        }
      }
      return true;
    });
  }

  protected static void fillWithResourceInstancesByType(@NotNull CompletionParameters parameters,
                                                        @NotNull CompletionResultSet result,
                                                        String resourceType,
                                                        boolean filterWords) {

    if (!filterWords) {
      result = result.withPrefixMatcher(new PlainPrefixMatcher(result.getPrefixMatcher().getPrefix()));
    }

    for (PuppetResourceInstanceDeclaration instanceDeclaration :
      PuppetResourceInstanceByTypeStubsIndex.getInstance().find(resourceType, getScopeProvider(parameters))) {
      for (PuppetDelegatingLightNamedElement element : instanceDeclaration.getLightElementsList()) {
        LookupElement lookupElement = PuppetLookupElements.forResourceInstance(element, resourceType);
        if (lookupElement != null) {
          result.addElement(lookupElement);
        }
      }
    }
  }

  protected static void fillWithVariables(@NotNull CompletionParameters parameters,
                                          boolean isFullQualified,
                                          @NotNull CompletionResultSet result) {

    if (isFullQualified) {
      fillWithFullQualifiedVariables(parameters, result);
    }
    else {
      fillWithNonFullQualifiedVariables(parameters, result);
    }
  }

  private static void fillWithFullQualifiedVariables(@NotNull CompletionParameters parameters,
                                                     @NotNull CompletionResultSet result) {
    PsiElement scopeProvider = getScopeProvider(parameters);
    Project project = scopeProvider.getProject();

    fillWithClassesVariables(scopeProvider, result, 0);

    Set<String> topLevelNamesSet = new HashSet<>();
    PuppetFactsIndex.processAllElements(project, scopeProvider.getResolveScope(), (name, fact) -> {
      topLevelNamesSet.add(name);
      result.addElement(PuppetLookupElements.forExternalFact(name, fact));
    });

    PuppetTopLevelVariablesStubsIndex.getInstance().processAllElements(project, scopeProvider, puppetVariable -> {
      if (!puppetVariable.isCoreFact() || topLevelNamesSet.add(puppetVariable.getName())) {
        LookupElementBuilder lookupElement = PuppetLookupElements.forVariable(puppetVariable, false);
        if (lookupElement != null) {
          result.addElement(lookupElement);
        }
      }
      return true;
    });
  }

  private static void fillWithNonFullQualifiedVariables(@NotNull CompletionParameters parameters,
                                                        @NotNull CompletionResultSet result) {
    PsiElement scopeProvider = getScopeProvider(parameters);
    Set<PsiElement> addedElements = new HashSet<>();

    PuppetVariableScopeProcessor completionProcessor = new PuppetVariableScopeProcessor() {
      private final Set<String> myNamesSet = new HashSet<>();

      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (!(element instanceof PuppetVariable) || !((PuppetVariable)element).isDeclaration()) {
          return true;
        }

        String name = ((PuppetVariable)element).getName();
        if (StringUtil.isEmpty(name)) {
          return true;
        }

        if (myNamesSet.add(name)) {
          LookupElementBuilder lookupElement = PuppetLookupElements.forVariable((PuppetVariable)element, false);
          if (lookupElement != null) {
            addedElements.add(element);
            result.addElement(lookupElement);
          }
        }

        return true;
      }

      @Override
      public void executeWithName(@NotNull String name, @NotNull PsiElement element) {
        addedElements.add(element);
        result.addElement(PuppetLookupElements.forExternalFact(name, element));
        myNamesSet.add(name);
      }
    };

    if (PuppetResolveUtil.treeWalkUp(scopeProvider, completionProcessor)) {
      PuppetResolveUtil.processTopScopeVariablesAndFacts(completionProcessor, scopeProvider);
    }

    fillWithClassesVariables(scopeProvider, result, -1, addedElements);
  }

  private static void fillWithClassesVariables(@NotNull PsiElement scopePosition,
                                               @NotNull CompletionResultSet resultSet,
                                               int weight) {
    fillWithClassesVariables(scopePosition, resultSet, weight, null);
  }

  private static void fillWithClassesVariables(@NotNull PsiElement scopePosition,
                                               @NotNull CompletionResultSet resultSet,
                                               int weight,
                                               @Nullable Set<PsiElement> addedElements
  ) {
    PsiElementProcessor<PuppetVariable> variableProcessor = puppetVariable -> {
      if (!puppetVariable.isLexicalDeclaration()) {
        if (addedElements == null || addedElements.add(puppetVariable)) {
          LookupElementBuilder lookupElement = PuppetLookupElements.forVariable(puppetVariable, true);
          if (lookupElement != null) {
            resultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement, weight));
          }
        }
      }
      return true;
    };

    PuppetClassStubsIndex.getInstance().processAllElements(
      scopePosition.getProject(),
      scopePosition,
      classDefinition -> classDefinition.processVariablesDeclarations(variableProcessor)
    );
  }


  protected static void computeDataTypeCompletion(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result,
                                                  boolean capitalize) {
    PsiElement nameElement = parameters.getPosition();
    PuppetDataType containingType = PsiTreeUtil.getParentOfType(nameElement, PuppetDataType.class);
    if (containingType == null) {
      return;
    }
    PuppetDataTypeParameterInfo info = containingType.getParameterInfo(nameElement.getParent());
    if (info == null) {
      return;
    }

    if (info.isResourceType()) {
      fillWithResourceTypes(parameters, context, result, capitalize);
    }
    if (info.isDataType()) {
      fillWithDataTypes(result);
    }
    if (info.isClass()) {
      fillWithClasses(parameters, result, capitalize);
    }
    if (info.isResourceInstance()) {
      fillWithResourceInstancesByType(parameters, result, info.getResourceDataType(), PsiUtilCore.getElementType(nameElement) == NAME);
    }
  }
}
