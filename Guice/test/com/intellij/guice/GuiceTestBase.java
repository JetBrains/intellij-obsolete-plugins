package com.intellij.guice;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public abstract class GuiceTestBase extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(
        com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES,
        new com.intellij.guice.model.GuiceVfsListener(getProject())
    );
    // Annotations
    myFixture.addClass("""
      package com.google.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
      public @interface Inject {}
      """);
    myFixture.addClass("""
      package com.google.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.METHOD)
      public @interface Provides {}
      """);
    myFixture.addClass("""
      package javax.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
      public @interface Inject {}
      """);
    myFixture.addClass("""
      package jakarta.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
      public @interface Inject {}
      """);
    
    // Core binder mocks
    myFixture.addClass("""
      package com.google.inject;
      public interface Module {}
      """);
    myFixture.addClass("""
      package com.google.inject;
      public interface Binder {
        <T> com.google.inject.binder.LinkedBindingBuilder<T> bind(Class<T> clazz);
      }
      """);
    myFixture.addClass("""
      package com.google.inject.binder;
      public interface LinkedBindingBuilder<T> {
        void to(Class<? extends T> implementation);
        void toInstance(T instance);
        void toProvider(Class<? extends javax.inject.Provider<? extends T>> provider);
        void toConstructor(java.lang.reflect.Constructor<? extends T> constructor);
      }
      """);
    myFixture.addClass("""
      package com.google.inject;
      public abstract class AbstractModule implements Module {
        protected void configure() {}
        protected <T> com.google.inject.binder.LinkedBindingBuilder<T> bind(Class<T> clazz) { return null; }
        protected Binder binder() { return null; }
      }
      """);
    
    // Multibinder and MapBinder/OptionalBinder
    myFixture.addClass("""
      package com.google.inject.multibindings;
      import com.google.inject.Binder;
      public class Multibinder<T> {
        public static <T> Multibinder<T> newSetBinder(Binder binder, Class<T> type) { return null; }
        public com.google.inject.binder.LinkedBindingBuilder<T> addBinding() { return null; }
      }
      """);
    myFixture.addClass("""
      package com.google.inject.multibindings;
      import com.google.inject.Binder;
      public class OptionalBinder<T> {
        public static <T> OptionalBinder<T> newOptionalBinder(Binder binder, Class<T> type) { return null; }
        public com.google.inject.binder.LinkedBindingBuilder<T> setBinding() { return null; }
        public com.google.inject.binder.LinkedBindingBuilder<T> setDefault() { return null; }
      }
      """);
    myFixture.addClass("""
      package com.google.inject.multibindings;
      import com.google.inject.Binder;
      public class MapBinder<K, V> {
        public static <K, V> MapBinder<K, V> newMapBinder(Binder binder, Class<K> keyType, Class<V> valueType) { return null; }
        public com.google.inject.binder.LinkedBindingBuilder<V> addBinding(K key) { return null; }
      }
      """);
    myFixture.addClass("""
      package javax.inject;
      public interface Provider<T> {
        T get();
      }
      """);

    // Add dummy classes for binding targets
    myFixture.addClass("public interface MyKey {}");
    myFixture.addClass("public interface MyService {}");
    myFixture.addClass("public class MyServiceImpl implements MyService { public MyServiceImpl() {} }");
    myFixture.addClass("""
      public class MyServiceProvider implements javax.inject.Provider<MyService> {
        public MyService get() { return null; }
      }
      """);
  }

  protected com.intellij.guice.model.GuiceLiveIndex getIndex() {
    com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    final com.intellij.guice.model.GuiceLiveIndex[] liveIndexHolder = new com.intellij.guice.model.GuiceLiveIndex[1];
    com.intellij.openapi.progress.ProgressManager.getInstance().runProcess(() -> {
      com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {
        liveIndexHolder[0] = com.intellij.guice.model.GuiceProjectModel.getInstance(getProject()).getIndex(myFixture.getModule());
      });
    }, new com.intellij.openapi.progress.EmptyProgressIndicator());
    return liveIndexHolder[0];
  }
}
