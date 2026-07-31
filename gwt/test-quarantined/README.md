# Quarantined tests

These test sources were extracted from the original `intellij.gwt` plugin but are **not compiled**
by the standalone Gradle build. They depend on JetBrains test frameworks and/or SDKs that are **not
distributed** with the released IntelliJ IDEA Ultimate build, so they cannot compile against the
public IntelliJ Platform artifact:

- **JavaEE-web test framework** — `com.intellij.ee.core.javaee.JavaeeTestUtil`,
  `com.intellij.testFramework.builders.WebModuleFixtureBuilder`
  (base classes `GwtFixtureTestCase` → `GwtCodeInsightTestCase` and all their subclasses).
- **JavaScript test framework** — `com.intellij.lang.javascript.Base*/…TestCase`,
  `com.intellij.javascript.testFramework.*`, `org.intellij.idea.lang.javascript.intention.*`.
- **Spring test framework** — `com.intellij.spring.testFramework.*`.
- **Grazie / spellchecker test support** — `com.intellij.grazie.*`.
- **Artifact-compiler test base** — `com.intellij.compiler.artifacts.ArtifactCompilerTestCase`.
- **GWT SDK** — `com.google.gwt.dev.*` (requires a real GWT distribution on the test classpath).
- **Maven test-framework fixtures** newer than the released platform ships
  (`getFacet`, `waitForImportWithinTimeout`) — `GwtLtgtFacetImporterTest.kt`.

The tests that only need the publicly available Platform / Java / Maven test frameworks remain under
`../test/` and are compiled by the build.

To revive any of these, move the file back under `../test/` (preserving its package path) and add the
required test-framework dependency in `../build.gradle.kts`. If the needed framework is not published
for standalone consumption, the test must stay here.

## Quarantined files
- com/intellij/gwt/actions/GenerateUiHandlerMethodHandlerTest.java
- com/intellij/gwt/css/GwtCssCompletionTest.java
- com/intellij/gwt/css/GwtCssFindUsagesTest.java
- com/intellij/gwt/css/GwtCssInspectionTest.java
- com/intellij/gwt/css/GwtCssTypingTest.java
- com/intellij/gwt/html/CssClassReferencesTest.java
- com/intellij/gwt/html/HtmlIdRefactoringTest.java
- com/intellij/gwt/injected/GwtInjectedLanguageHighlightingTest.java
- com/intellij/gwt/injected/GwtJsEditingTest.java
- com/intellij/gwt/injected/GwtJsEditorTest.java
- com/intellij/gwt/injected/GwtJsIntentionTest.java
- com/intellij/gwt/injected/GwtJsIntroduceVariableTest.java
- com/intellij/gwt/inspections/GwtAllowedClientCodeInspectionsTest.java
- com/intellij/gwt/inspections/GwtClientBundleHighlightingTest.java
- com/intellij/gwt/inspections/GwtI18nInspectionTest.java
- com/intellij/gwt/inspections/GwtReferencesTest.java
- com/intellij/gwt/inspections/GwtRpcInspectionsTest.java
- com/intellij/gwt/inspections/GwtSerializableTest.java
- com/intellij/gwt/inspections/GwtToCssClassReferencesInspectionTest.java
- com/intellij/gwt/inspections/GwtToHtmlTagReferencesInspectionTest.java
- com/intellij/gwt/inspections/GwtUiBinderInspectionsTest.java
- com/intellij/gwt/intentions/CreateUiFieldIntentionTest.java
- com/intellij/gwt/intentions/CreateUiHandlerIntentionTest.java
- com/intellij/gwt/intentions/GwtIntentionTestBase.java
- com/intellij/gwt/javascript/GwtJavaScriptParsingTest.java
- com/intellij/gwt/jsni/CompleteJsniMethodBodyTest.java
- com/intellij/gwt/jsni/CompletionInInjectedTest.java
- com/intellij/gwt/jsni/GwtJSCompletionTest.java
- com/intellij/gwt/jsni/GwtJavaFromJSMethodCallsInspectionTest.java
- com/intellij/gwt/jsni/GwtJsLiveTemplateTest.java
- com/intellij/gwt/jsni/GwtJsniInspectionTest.java
- com/intellij/gwt/jsni/JsniRefactoringTest.java
- com/intellij/gwt/maven/GwtLtgtFacetImporterTest.kt
- com/intellij/gwt/model/GwtFacetConfigurationTest.java
- com/intellij/gwt/model/GwtFacetTest.java
- com/intellij/gwt/model/GwtHtmlUtilTest.java
- com/intellij/gwt/model/GwtLanguageLevelPusherTest.java
- com/intellij/gwt/model/GwtLanguageLevelPusherWithDefaultClientDirectoryTest.java
- com/intellij/gwt/navigation/GwtAsyncServiceNavigationTest.java
- com/intellij/gwt/references/ClientBundleReferencesTest.java
- com/intellij/gwt/references/GwtCodeInsightTestCase.java
- com/intellij/gwt/references/GwtFixtureTestCase.java
- com/intellij/gwt/references/GwtModuleInHtmlReferencesTest.java
- com/intellij/gwt/references/GwtRenamePackageTest.java
- com/intellij/gwt/references/GwtSpringXmlHighlightingTest.java
- com/intellij/gwt/references/GwtTagNameReferenceTest.java
- com/intellij/gwt/references/GwtUiBinderFindUsagesTest.java
- com/intellij/gwt/references/GwtUiBinderHighlightingTest.java
- com/intellij/gwt/references/ImageResourceHighlightingTest.java
- com/intellij/gwt/references/UiBinderCssReferencesTest.java
- com/intellij/gwt/references/UiBinderReferencesTest.java
- com/intellij/gwt/references/UiRendererXmlFileCorrespondenceInUiBinderMappingServiceTest.java
- com/intellij/gwt/references/UiWithTagReferencesTest.java
- com/intellij/gwt/run/remoteUi/GwtErrorFilterTest.java
- com/intellij/gwt/uiBinder/GwtUiBinderCompletionTest.java
- com/intellij/gwt/uiBinder/GwtUiXmlCompletionTest.java
