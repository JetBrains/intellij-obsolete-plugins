package com.intellij.guice;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiClass;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks the Guice intentions as the user sees them: availability, preview, and result in both files.
 * The intentions that annotate a class in another file get a plain {@code launchAction}: their preview
 * shows both files, so {@code checkPreviewAndLaunchAction} cannot compare it with the module text alone.
 */
public class GuiceIntentionsTest extends GuiceTestBase {

  // Standard 4-space indentation, so the formatter leaves the rewritten statement where it is.
  private static final String MODULE_HEADER = """
    import com.google.inject.AbstractModule;
    import com.google.inject.Scopes;
    import test.*;
    public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
    """;
  private static final String MODULE_FOOTER = """
        }
    }
    """;

  private void configureModule(String configureBody) {
    myFixture.configureByText("MyModule.java", MODULE_HEADER + "        " + configureBody + "\n" + MODULE_FOOTER);
  }

  private List<String> intentionTexts() {
    return myFixture.getAvailableIntentions().stream().map(IntentionAction::getText).toList();
  }

  public void testMoveBindingToClassAddsImplementedByAndRemovesTarget() {
    myFixture.addFileToProject("test/Foo.java", "package test;\npublic interface Foo {}\n");
    myFixture.addFileToProject("test/FooImpl.java", "package test;\npublic class FooImpl implements Foo {}\n");
    configureModule("bind(Foo.class).t<caret>o(FooImpl.class);");

    IntentionAction action = myFixture.findSingleIntention("Move binding class to class");
    assertThat(myFixture.getIntentionPreviewText(action)).contains("@ImplementedBy(FooImpl.class)");
    myFixture.launchAction(action);
    myFixture.checkResult(MODULE_HEADER + "        bind(Foo.class);\n" + MODULE_FOOTER);
    PsiClass foo = myFixture.findClass("test.Foo");
    assertThat(foo.hasAnnotation("com.google.inject.ImplementedBy")).isTrue();
    assertThat(foo.getAnnotation("com.google.inject.ImplementedBy").getText()).isEqualTo("@ImplementedBy(FooImpl.class)");
  }

  public void testMoveBindingToClassIsNotAvailableWhenBoundTypeIsNotWritable() {
    configureModule("bind(java.util.List.class).t<caret>o(java.util.ArrayList.class);");
    assertThat(intentionTexts()).doesNotContain("Move binding class to class");
    assertThat(intentionTexts()).doesNotContain("Move binding scope to class");
  }

  /**
   * N7: moving the target of a qualified key, of a multibinder element or of a {@code Key.get} link to an
   * {@code @ImplementedBy}/{@code @ProvidedBy} annotation would change what Guice injects.
   */
  public void testMoveBindingToClassIsNotOfferedWhenTheResultWouldChangeSemantics() {
    myFixture.addFileToProject("test/Foo.java", "package test;\npublic interface Foo {}\n");
    myFixture.addFileToProject("test/FooImpl.java", "package test;\npublic class FooImpl implements Foo {}\n");
    myFixture.addFileToProject("test/FooProvider.java", """
      package test;
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
      """);

    configureModule("bind(Foo.class).annotatedWith(com.google.inject.name.Names.named(\"x\")).t<caret>o(FooImpl.class);");
    assertThat(intentionTexts()).doesNotContain("Move binding class to class");

    configureModule("bind(Foo.class).annotatedWith(com.google.inject.name.Names.named(\"x\")).toProv<caret>ider(FooProvider.class);");
    assertThat(intentionTexts()).doesNotContain("Move provider binding to class");

    configureModule("bind(Foo.class).t<caret>o(com.google.inject.Key.get(FooImpl.class, com.google.inject.name.Names.named(\"q\")));");
    assertThat(intentionTexts()).doesNotContain("Move binding class to class");

    configureModule("com.google.inject.multibindings.Multibinder.newSetBinder(binder(), Foo.class).addBinding().t<caret>o(FooImpl.class);");
    assertThat(intentionTexts()).doesNotContain("Move binding class to class");

    // The plain linked binding is still offered.
    configureModule("bind(Foo.class).t<caret>o(FooImpl.class);");
    assertThat(intentionTexts()).contains("Move binding class to class");
  }

  /**
   * M12: the intentions edit Java PSI; a Kotlin class (a light class) is not a target they can annotate.
   */
  public void testMoveIntentionsAreNotOfferedWhenTheTargetClassIsKotlin() {
    myFixture.addFileToProject("test/Foo.java", "package test;\npublic interface Foo {}\n");
    myFixture.addFileToProject("test/KtImpl.kt", "package test\nclass KtImpl : Foo\n");
    myFixture.addFileToProject("test/KtApi.kt", "package test\ninterface KtApi\n");
    myFixture.addFileToProject("test/KtApiImpl.java", "package test;\npublic class KtApiImpl implements KtApi {}\n");

    configureModule("bind(Foo.class).to(KtImpl.class).i<caret>n(Scopes.SINGLETON);");
    assertThat(intentionTexts()).doesNotContain("Move binding scope to class");

    configureModule("bind(KtApi.class).t<caret>o(KtApiImpl.class);");
    assertThat(intentionTexts()).doesNotContain("Move binding class to class");
  }

  public void testMoveBindingScopeToClassForLinkedBinding() {
    myFixture.addFileToProject("test/Foo.java", "package test;\npublic interface Foo {}\n");
    myFixture.addFileToProject("test/FooImpl.java", "package test;\npublic class FooImpl implements Foo {}\n");
    configureModule("bind(Foo.class).to(FooImpl.class).i<caret>n(Scopes.SINGLETON);");

    IntentionAction action = myFixture.findSingleIntention("Move binding scope to class");
    assertThat(myFixture.getIntentionPreviewText(action)).contains("@Singleton\npublic class FooImpl");
    myFixture.launchAction(action);
    myFixture.checkResult(MODULE_HEADER + "        bind(Foo.class).to(FooImpl.class);\n" + MODULE_FOOTER);
    // The scope goes to the class that Guice constructs: the implementation.
    assertThat(myFixture.findClass("test.FooImpl").hasAnnotation("com.google.inject.Singleton")).isTrue();
    assertThat(myFixture.findClass("test.Foo").hasAnnotation("com.google.inject.Singleton")).isFalse();
  }

  public void testMoveBindingScopeToClassForUntargetedBinding() {
    myFixture.addFileToProject("test/Foo.java", "package test;\npublic class Foo {}\n");
    configureModule("bind(Foo.class).i<caret>n(Scopes.SINGLETON);");

    IntentionAction action = myFixture.findSingleIntention("Move binding scope to class");
    assertThat(myFixture.getIntentionPreviewText(action)).contains("@Singleton\npublic class Foo");
    myFixture.launchAction(action);
    myFixture.checkResult(MODULE_HEADER + "        bind(Foo.class);\n" + MODULE_FOOTER);
    assertThat(myFixture.findClass("test.Foo").hasAnnotation("com.google.inject.Singleton")).isTrue();
  }

  public void testMoveProviderBindingToClass() {
    myFixture.addFileToProject("test/Foo.java", "package test;\npublic interface Foo {}\n");
    myFixture.addFileToProject("test/FooProvider.java", """
      package test;
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
      """);
    configureModule("bind(Foo.class).toProv<caret>ider(FooProvider.class);");

    IntentionAction action = myFixture.findSingleIntention("Move provider binding to class");
    assertThat(myFixture.getIntentionPreviewText(action)).contains("@ProvidedBy(FooProvider.class)");
    myFixture.launchAction(action);
    myFixture.checkResult(MODULE_HEADER + "        bind(Foo.class);\n" + MODULE_FOOTER);
    assertThat(myFixture.findClass("test.Foo").getAnnotation("com.google.inject.ProvidedBy").getText())
      .isEqualTo("@ProvidedBy(FooProvider.class)");
  }

  public void testToggleInjectionRequiredKeepsGuiceInjectAndSkipsJavaxInject() {
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inj<caret>ect MyService service;
      }
      """);
    IntentionAction action = myFixture.findSingleIntention("Make injection optional");
    myFixture.checkPreviewAndLaunchAction(action);
    myFixture.checkResult("""
      import com.google.inject.Inject;
      public class Client {
        @Inject(optional = true)
        MyService service;
      }
      """);
    myFixture.launchAction(myFixture.findSingleIntention("Make injection mandatory"));
    myFixture.checkResult("""
      import com.google.inject.Inject;
      public class Client {
        @Inject
        MyService service;
      }
      """);

    // javax.inject.Inject has no 'optional' attribute: the intention must not offer to rewrite it.
    myFixture.configureByText("JavaxClient.java", """
      import javax.inject.Inject;
      public class JavaxClient {
        @Inj<caret>ect MyService service;
      }
      """);
    assertThat(intentionTexts()).containsNoneOf("Make injection optional", "Make injection mandatory", "Toggle 'optional'");
  }
}
