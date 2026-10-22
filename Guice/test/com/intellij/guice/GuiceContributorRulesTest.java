package com.intellij.guice;

import com.intellij.guice.model.GuiceEntryProducer;
import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.UntargetedBindDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.SetMultibindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.model.extensions.BinderShape;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.diagnostic.PluginException;

import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks how the contributor rules of the extension index apply to module classes:
 * which calls they match, and which parts of a class they scan.
 */
public class GuiceContributorRulesTest extends GuiceTestBase {

  /**
   * A method of an anonymous or local class resolves, but its class has no qualified name.
   * Such a call is a resolved call that matches no owner, not an unresolved call that the
   * {@code allowUnresolved("bind", ...)} fallback accepts. (The fallback stays for code that does not resolve.)
   */
  public void testFluentHelperOfLocalClassIsNotABinding() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          class Local {
            Local bind(Class<?> c) { return this; }
            Local to(Class<?> c) { return this; }
          }
          var helper = new Object() {
            public Local bind(Class<?> c) { return new Local(); }
          };
          new Local().bind(MyKey.class).to(MyServiceImpl.class);
          helper.bind(MyKey.class).to(MyServiceImpl.class);
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    PsiClass module = myFixture.findClass("MyModule");
    Set<BindDescriptor> descriptors = ReadAction.compute(() -> GuiceInjectorManager.getBindingDescriptors(module));
    assertThat(ReadAction.compute(() -> descriptors.stream().map(d -> d.getBindExpression().getText()).toList()))
      .containsExactly("bind(MyService.class).to(MyServiceImpl.class)");

    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject MyKey key;
      }
      """);
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  /**
   * Call rules apply to every declaration of a class: field initializers and initializer blocks too,
   * not only method bodies.
   */
  /**
   * E6: the sinks are add-only and the index takes their content only after a normal return. A handler,
   * a producer or a context producer that throws after it reported something contributes nothing, and the
   * results of the built-in contributors stay intact.
   */
  public void testThrowingContributorCallbacksContributeNothing() {
    // The index reports each failing contributor class once as a PluginException; that is expected here.
    LoggedErrorProcessor ignorePluginErrors = new LoggedErrorProcessor() {
      @Override
      public @NotNull Set<Action> processError(@NotNull String category, @NotNull String message,
                                               String @NotNull [] details, @Nullable Throwable t) {
        return t instanceof PluginException ? Action.NONE : super.processError(category, message, details, t);
      }
    };
    LoggedErrorProcessor.executeWith(ignorePluginErrors, this::runThrowingContributorCallbacksScenario);
  }

  private void runThrowingContributorCallbacksScenario() {
    var disposable = Disposer.newDisposable();
    try {
      GuiceBindingContributor.EP_NAME.getPoint().registerExtension(
        registrar -> {
          registrar.registerCallDescriptor(
            GuiceCallPattern.named("brokenBind").withOwnerClass("MyModule"),
            (call, sink) -> {
              sink.accept(new UntargetedBindDescriptor(call.getSourcePsi()));
              throw new RuntimeException("after the descriptor");
            });
          registrar.registerModuleCallEntry(
            GuiceCallPattern.named("brokenBind").withOwnerClass("MyModule"),
            (call, sink) -> {
              GuiceEntryProducer.addGetProviderEntry(call, sink);
              throw new RuntimeException("after the entry");
            });
          registrar.registerCallWithContext(
            GuiceCallPattern.named("brokenFlags").withOwnerClass("MyModule"),
            (call, context) -> {
              context.reportClassArgument(call.getValueArguments().getFirst(), (targetClass, sink) -> {
                for (var field : targetClass.getFields()) {
                  sink.accept(GuiceEntryProducer.createFieldBindingEntry(field, field.getType()));
                }
              });
              throw new RuntimeException("after the class entries");
            });
        },
        disposable);

      myFixture.addClass("public class Flags { public static String NAME = \"x\"; }");
      var file = myFixture.configureByText("MyModule.java", """
        import com.google.inject.AbstractModule;
        import com.google.inject.Inject;
        public class MyModule extends AbstractModule {
          @Inject MyKey injected;
          void brokenBind(Class<?> c) {}
          void brokenFlags(Class<?> c) {}
          @Override
          protected void configure() {
            bind(MyService.class).to(MyServiceImpl.class);
            brokenBind(MyKey.class);
            brokenFlags(Flags.class);
          }
        }
        """);

      Set<BindDescriptor> bindings = ReadAction.compute(() -> GuiceInjectorManager.getBindingsInFile(file));
      assertThat(bindings).hasSize(1);
      assertThat(bindings.iterator().next()).isInstanceOf(BindToDescriptor.class);

      var entries = ReadAction.compute(() -> GuiceEntryProducer.extractFileData(file).entries());
      var anchors = entries.stream()
        .map(e -> e.getGutterAnchorPointer().getElement())
        .map(e -> e == null ? "<null>" : e.getText())
        .toList();
      // The @Inject field, the bind() key and the to(MyServiceImpl) implementation reference; nothing from the broken calls.
      assertThat(anchors).containsNoneOf("brokenBind", "brokenFlags", "NAME");
      assertThat(entries.stream().noneMatch(e -> e.getPresentableText() != null && e.getPresentableText().contains("NAME"))).isTrue();
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  /**
   * M10: a binder with its own argument order and element call describes its shape through the API, and every
   * consumer follows it: the key types and the qualifier of the entries, and the element chain that turns a
   * {@code .to(Impl)} tail into an implementation reference instead of a binding of the value type.
   */
  public void testBinderShapeDrivesTypesQualifierAndElementCalls() {
    myFixture.addClass("""
      import com.google.inject.Binder;
      import com.google.inject.binder.LinkedBindingBuilder;
      import java.lang.annotation.Annotation;
      public final class RegistryBinder<K, V> {
        public static <K, V> RegistryBinder<K, V> create(Class<K> key, Class<V> value, Annotation qualifier, Binder binder) { return null; }
        public LinkedBindingBuilder<V> addEntry(K key) { return null; }
      }
      """);
    var disposable = Disposer.newDisposable();
    try {
      GuiceBindingContributor.EP_NAME.getPoint().registerExtension(
        registrar -> registrar.registerDualTypeBinder(
          GuiceCallPattern.named("create").withOwnerClass("RegistryBinder"),
          new BinderShape(
            List.of("RegistryBinder"),
            call -> new com.intellij.psi.PsiType[]{
              GuiceUtils.getBindingTypeFromExpression(call.getValueArguments().get(0)),
              GuiceUtils.getBindingTypeFromExpression(call.getValueArguments().get(1))},
            call -> call.getValueArguments().get(2),
            List.of("addEntry")),
          MapMultibindDescriptor::new),
        disposable);

      myFixture.addFileToProject("MyModule.java", """
        import com.google.inject.AbstractModule;
        import com.google.inject.name.Names;
        public class MyModule extends AbstractModule {
          @Override
          protected void configure() {
            RegistryBinder.create(String.class, MyService.class, Names.named("reg"), binder()).addEntry("a").to(MyServiceImpl.class);
          }
        }
        """);
      myFixture.configureByText("Client.java", """
        import com.google.inject.Inject;
        import com.google.inject.name.Named;
        import java.util.Map;
        public class Client {
          @Inject @Named("reg")
          Map<String, MyService> registry;
          @Inject
          Map<String, MyService> unqualifiedRegistry;
          @Inject
          MyService plain;
        }
        """);
      // The qualifier comes from the third argument, the key and value types from the first two, and the
      // .to(MyServiceImpl) tail of the element chain binds no MyService of its own.
      assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("registry");
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  public void testCallEntryRulesCoverFieldInitializersAndInitializerBlocks() {
    var disposable = Disposer.newDisposable();
    try {
      GuiceBindingContributor.EP_NAME.getPoint().registerExtension(
        registrar -> registrar.registerModuleCallEntry(
          GuiceCallPattern.named("provideLater").withOwnerClass("MyModule").argumentCount(1),
          GuiceEntryProducer::addGetProviderEntry),
        disposable);

      myFixture.addFileToProject("MyModule.java", """
        import com.google.inject.AbstractModule;
        import com.google.inject.Provides;
        public class MyModule extends AbstractModule {
          private final Object fromField = provideLater(MyService.class);
          {
            provideLater(MyKey.class);
          }
          Object provideLater(Class<?> c) { return null; }
          @Override
          protected void configure() {
            bind(MyService.class).to(MyServiceImpl.class);
            provideLater(MyService.class);
          }
          @Provides MyKey key() { return null; }
        }
        """);
      myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("MyModule.java"));
      assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("provideLater", "provideLater", "provideLater");
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  /**
   * {@code @ProvidesIntoSet int port()} contributes to {@code Set<Integer>}: Guice boxes a primitive element type.
   */
  public void testProvidesIntoSetOfPrimitiveBindsBoxedSet() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.ProvidesIntoSet;
      public class MyModule extends AbstractModule {
        @ProvidesIntoSet int port() { return 8080; }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.Set;
      public class Client {
        @Inject Set<Integer> ports;
      }
      """);
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@ProvidesIntoSet int port() { return 8080; }");
  }

  /**
   * A contributor whose factory does not follow the Guice argument shape {@code (binder, type)}
   * provides its own type extractor.
   */
  public void testSingleTypeBinderWithExplicitTypeExtractor() {
    myFixture.addClass("""
      package test;
      public class FooBinder<T> {
        public static <T> FooBinder<T> newFooBinder(Class<T> type, com.google.inject.Binder binder) { return null; }
        public com.google.inject.binder.LinkedBindingBuilder<T> addBinding() { return null; }
      }
      """);
    var disposable = Disposer.newDisposable();
    try {
      GuiceBindingContributor.EP_NAME.getPoint().registerExtension(
        registrar -> registrar.registerSingleTypeBinder(
          GuiceCallPattern.named("newFooBinder").withOwnerClass("test.FooBinder"),
          List.of("test.FooBinder"),
          call -> GuiceUtils.getBindingTypeFromExpression(call.getValueArguments().getFirst()),
          SetMultibindDescriptor::new),
        disposable);

      myFixture.addFileToProject("MyModule.java", """
        import com.google.inject.AbstractModule;
        import test.FooBinder;
        public class MyModule extends AbstractModule {
          @Override
          protected void configure() {
            FooBinder.newFooBinder(MyService.class, binder()).addBinding().to(MyServiceImpl.class);
          }
        }
        """);
      myFixture.configureByText("Client.java", """
        import com.google.inject.Inject;
        import java.util.Set;
        public class Client {
          @Inject Set<MyService> services;
        }
        """);
      assertThat(gutterTargets(TO_BINDINGS_TOOLTIP))
        .containsExactly("FooBinder.newFooBinder(MyService.class, binder()).addBinding().to(MyServiceImpl.class)");
    }
    finally {
      Disposer.dispose(disposable);
    }
  }
}
