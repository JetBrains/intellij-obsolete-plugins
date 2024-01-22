package com.intellij.jboss.bpmn.jbpm.providers;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ProvidersCoordinator {
  final private Map<Class<?>, Object> providers = new HashMap<>();

  public <T> T getProvider(Class<? extends T> clazz) {
    T provider = (T)providers.get(clazz);
    if (provider == null) {
      provider = createProvider(clazz);
      assert provider != null : "Could not create instance of type " + clazz.getName();
      providers.put(clazz, provider);
    }
    return provider;
  }

  @Nullable
  public <T> T createProvider(Class<? extends T> providerClass) {
    try {
      return providerClass.newInstance();
    }
    catch (InstantiationException | IllegalAccessException e) {
      return null;
    }
  }

  public static ProvidersCoordinator getInstance() {
    return SingletonHolder.HOLDER_INSTANCE;
  }

  private static final class SingletonHolder {
    public static final ProvidersCoordinator HOLDER_INSTANCE = new ProvidersCoordinator();
  }
}
