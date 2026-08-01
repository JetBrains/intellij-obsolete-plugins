<!-- Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.-->
# GWT Plugin Knowledge Base

Navigation map for coding agents working on the IntelliJ **GWT (Google Web Toolkit)** plugin
(`com.intellij.gwt`). Use it to jump straight to the right entry point. Paths are repo-relative;
`src/...` = the main module, `jps-plugin/src/...` = the external build process.

## 1. Orientation

The plugin adds GWT support to IntelliJ IDEA Ultimate: `*.gwt.xml` module model, UiBinder
(`*.ui.xml`), JSNI, RPC async services, ClientBundle/CSS, i18n, run/dev-mode, and GWT compilation.

Two things to know before editing anything:

- **Two-process split.** The IDE side (`com.intellij.gwt.*`, this plugin jar) handles configuration,
  code-insight, UI, and launching. The **external JPS build process** (`org.jetbrains.jps.gwt.*`,
  packaged as `gwt-jps.jar`) actually runs `com.google.gwt.dev.Compiler` out-of-process. They share a
  serializable model in `org.jetbrains.jps.gwt.model.*` — see §10.
- **All feature wiring lives in `resources/META-INF/plugin.xml`.** To find who implements a feature,
  grep the descriptor for the extension point / class first. Optional integrations are in
  `resources/META-INF/gwt-maven-support.xml`, `gradle-support.xml`, `javascript-debugger.xml`.

## 2. Start-here hubs

Open these first depending on the area:

| Area | Hub file(s) |
|------|-------------|
| All wiring | `resources/META-INF/plugin.xml` |
| UiBinder | `src/com/intellij/gwt/uiBinder/UiBinderUtil.java`, `src/com/intellij/gwt/uiBinder/mapping/UiBinderMappingServiceImpl.java` |
| JSNI | `src/com/intellij/gwt/jsinject/GwtClassMemberReference.java`, `src/com/intellij/gwt/jsinject/parser/GwtParser.kt` |
| Run / dev mode | `src/com/intellij/gwt/run/GwtCommandLineState.java` |
| Compilation | `jps-plugin/src/org/jetbrains/jps/gwt/build/GwtBuilder.java` |
| Module & SDK model | `src/com/intellij/gwt/module/GwtModulesManager.java`, `src/com/intellij/gwt/sdk/GwtSdkManager.java` |

## 3. Project model & SDK

- **Facet** (how a module becomes "a GWT module"): `src/com/intellij/gwt/facet/GwtFacet.java`,
  `GwtFacetType.java` (nested `GwtFrameworkDetector` triggers on `*.gwt.xml`),
  `GwtFacetConfiguration.java` (the settings model; persisted via the JPS
  `GwtModuleExtensionProperties`). Almost all code-insight requires a `GwtFacet` on the module.
- **GWT module** (`*.gwt.xml`): DOM model `src/com/intellij/gwt/module/model/GwtModule.java`;
  central query API `src/com/intellij/gwt/module/GwtModulesManager.java` (impl `GwtModulesManagerImpl`);
  file indexes `src/com/intellij/gwt/module/index/GwtModuleRenameToIndex.java` and
  `GwtHtmlFileIndex.java`.
- **SDK**: `src/com/intellij/gwt/sdk/GwtSdkManager.java` (app service, caches SDKs),
  `src/com/intellij/gwt/sdk/GwtSdkType.java` (extension point for SDK layouts),
  `src/com/intellij/gwt/sdk/impl/GwtVersionImpl.java` + `GwtVersionDetector.java`. **The
  `GwtVersionImpl` capability flags gate features throughout the plugin — this is where you teach the
  plugin about a new GWT version.** JPS-side path layouts live in
  `jps-plugin/src/org/jetbrains/jps/gwt/model/impl/sdk/` (default / Maven / Gradle / Vaadin).

## 4. Language & code-insight

- **UiBinder tags / attributes / schema**: `src/com/intellij/gwt/uiBinder/GwtUiComponentDescriptor.java`
  (reflects widget classes into tag/attribute descriptors), `GwtCompositeXmlElementDescriptor.java`
  (merges reflection- and XSD-derived descriptors), `GwtUiXmlSchemaProvider.java` (bundled XSDs).
- **UiBinder ↔ Java references** (`ui:field`, `@UiHandler`, style/`@src`):
  `src/com/intellij/gwt/uiBinder/references/GwtUiReferenceContributor.java`.
- **JSNI**: `src/com/intellij/gwt/jsinject/JsInjector.java` (injects JS into `/*-{ ... }-*/` native
  method bodies), `src/com/intellij/gwt/jsinject/parser/GwtParser.kt` (the `@class::member` syntax),
  `JSGwtReferenceExpressionImpl.java` + `GwtClassMemberReference.java` (resolve JS→Java members,
  completion, rename).
- **General references** (GWT module names, HTML element ids, CSS classes, RPC service paths, i18n
  keys, generated JS): `src/com/intellij/gwt/references/GwtReferenceContributor.java`.
- **Inspections**: `src/com/intellij/gwt/inspections/` — bases `BaseGwtInspection.java` and
  `AbstractClientCodeReferencesInspection.java`; all ~32 are registered in `plugin.xml` and gated on a
  `GwtFacet`.
- **Create/generate actions & intentions**: `src/com/intellij/gwt/actions/` (action group `GWT`:
  new module / entry point / remote service / UiBinder / etc.); UiBinder intentions
  `src/com/intellij/gwt/uiBinder/CreateUiFieldIntention.java`, `CreateUiHandlerIntention.java`.

## 5. ClientBundle, CSS, RPC, i18n

- **ClientBundle / CssResource + the GWT-CSS dialect**:
  `src/com/intellij/gwt/clientBundle/ClientBundleUtil.java` (Java ↔ `.css` ↔ `ui:style` bridge),
  JAM model in `src/com/intellij/gwt/clientBundle/jam/`, dialect
  `src/com/intellij/gwt/clientBundle/css/language/GwtCssParser.java` (adds `@def`/`@if`/`@sprite`/…
  at-rules) activated by `.../css/language/GwtCssLanguageSubstitutor.java`.
- **RPC async/sync services**: `src/com/intellij/gwt/rpc/RemoteServiceUtil.java` (the
  `RemoteService`/`…Async` convention + method correspondence + code-gen) drives
  `src/com/intellij/gwt/inspections/GwtInconsistentAsyncInterfaceInspection.java`.
- **i18n (`Constants`/`Messages`)**: `src/com/intellij/gwt/i18n/GwtI18nUtil.java`,
  `src/com/intellij/gwt/i18n/GwtI18nManager.java`.

## 6. Run / dev mode

- **Config**: `src/com/intellij/gwt/run/GwtRunConfiguration.java` (settings in the nested
  `GwtRunConfigurationState`), `GwtRunConfigurationType.java`, editor + producer in the same package.
- **Launch flow**: `src/com/intellij/gwt/run/GwtCommandLineState.java` — builds the classpath,
  super-dev-mode jars (incl. bundled `lib/superdevmode-launcher-legacy.jar`), and the exploded WAR dir
  via `HostedModeWarDirectoryGenerator.java`. Pluggable dev servers: extension point
  `src/com/intellij/gwt/run/GwtDevModeServerProvider.java`.
- **Remote UI** (protobuf socket back from the running dev-mode process, port 7901):
  `src/com/intellij/gwt/run/remoteUi/RemoteUiConnection.java` / `RemoteUiView.java`; wire format in
  `remoteUi/remotemessage.proto` (regenerate with `remoteUi/gen_proto.sh` / `.bat` — the generated
  Java is checked in under `remoteUi/generated-source/`).

## 7. Compilation

- **IDE side**: `src/com/intellij/gwt/make/GwtModulesIndexBuildTask.java` (writes `gwt-modules.xml`
  before build), `src/com/intellij/gwt/build/GwtBuildTargetScopeProvider.java` (which facets to
  build), `src/com/intellij/gwt/make/GwtBuilderMessageHandler.java` (receives builder messages),
  packaging elements `src/com/intellij/gwt/packaging/GwtCompilerOutputElement.java`.
- **External JPS process**: `jps-plugin/src/org/jetbrains/jps/gwt/build/GwtBuilder.java` (launches
  `com.google.gwt.dev.Compiler`), `GwtBuildTargetType.java`, `GwtExternalCompilerProcessHandler.java`
  (parses compiler output into messages); the IDE↔process module-index bridge is
  `jps-plugin/src/org/jetbrains/jps/gwt/index/impl/JpsGwtModulesCache.java`.

## 8. Build-system integration

- **Maven** (wired in `resources/META-INF/gwt-maven-support.xml`):
  `src/com/intellij/gwt/maven/GwtFacetImporter.java` (base) + Codehaus / Totsp / Ltgt importers.
- **Gradle** (wired in `resources/META-INF/gradle-support.xml`):
  `src/com/intellij/gwt/gradle/GwtDependencyDataService.java` (creates facet on import) and
  `GwtGradleExecutionEnvironmentProvider.java` (runs `gwtDev`/`gwtSuperDev` Gradle tasks).
- **Spring** content module: `spring/src/com/intellij/gwt/spring/SpringControllerServletCondition.java`
  feeds the `gwt.unregisteredServletFilter` EP (suppresses false "service not registered" reports for
  Spring-MVC controllers).
- **Dev** content module: `dev/src/com/intellij/gwt/dev/psiViewer/GwtUiXmlPsiViewerExtension.java`
  (plugin-developer PSI Viewer aid).

## 9. Extension points

Declared in `resources/META-INF/plugin.xml`, all `dynamic`:

| EP | Interface | Purpose |
|----|-----------|---------|
| `com.intellij.gwt.sdkType` | `sdk/GwtSdkType` | pluggable SDK layouts (Maven/Gradle/Vaadin) |
| `com.intellij.gwt.devModeServerProvider` | `run/GwtDevModeServerProvider` | pluggable dev-mode/code-server launch backends |
| `com.intellij.gwt.unregisteredServletFilter` | `com.intellij.openapi.util.Condition` | exempt classes from the "servlet not registered" inspection |
| `com.intellij.gwt.compileOutputRelativePathSuggester` | `packaging/GwtCompileOutputRelativePathSuggester` | where GWT output lands inside a WAR artifact |

## 10. Key conventions

- **IDE ↔ JPS shared model**: settings, SDK paths, and constants live in
  `jps-plugin/src/org/jetbrains/jps/gwt/model/` so both the IDE and the compiler process agree. Change
  both sides in lockstep; facet settings are persisted through these JPS classes.
- **Facet-gated**: most inspections/code-insight bail out unless the file's module has a `GwtFacet`
  (see `BaseGwtInspection.shouldCheck`).
- **Two "module" concepts**: the IntelliJ `Module` (facet host) vs. the GWT `GwtModule` (a `*.gwt.xml`
  DOM). `GwtModulesManager` and `GwtModule` bridge them — don't conflate them.

## 11. Build & module layout

- **Gradle build** (IntelliJ Platform Gradle Plugin 2.x). The root `build.gradle.kts` builds the plugin;
  `settings.gradle.kts` adds subprojects `jps-plugin` (the external build process, packaged as
  `gwt-jps.jar`), `runtime` (small runtime jar), and the plugin **content modules** `spring` and `dev`.
  Platform version and language level live in `gradle.properties`. Build with `./gradlew buildPlugin`;
  `verifyPluginProjectConfiguration` validates the descriptor. Tests run from `test/` (JetBrains-only
  test bases are quarantined under `test-quarantined/` and excluded from compilation).
- **Gradle integration needs *both* Gradle plugin dependencies** — they are distinct, not redundant:
  `bundledPlugin("com.intellij.gradle")` is the base "Gradle" plugin (provides
  `org.jetbrains.plugins.gradle.util.*`, e.g. `GradleConstants`), while
  `bundledPlugin("org.jetbrains.plugins.gradle")` is the separate "Gradle for Java" plugin that owns the
  `intellij.gradle.java` module referenced by `gradle-support.xml`.
- **`.gitignore` gotcha.** The root `**/build` rule ignores Gradle *output* dirs, but the plugin also has
  *source* packages named `build`: `com.intellij.gwt.build` (IDE side, `GwtBuildTargetScopeProvider`) and
  `org.jetbrains.jps.gwt.build` (JPS side, incl. `GwtBuilder`). These stay in VCS only because of the
  `!**/src/**/build/` negations in the root `.gitignore` — keep those, and if you add a file under any
  `.../src/.../build/` package, confirm `git status` actually shows it.
