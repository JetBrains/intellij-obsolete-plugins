package com.intellij.tcserver.server.integration;

import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerUrlMapping;
import com.intellij.javaee.appServers.context.FacetContextProvider;
import com.intellij.javaee.web.WebFacetContextProvider;

import java.util.List;

public final class TcServerUrlMapping extends ApplicationServerUrlMapping {
  private static final TcServerUrlMapping INSTANCE = new TcServerUrlMapping();

  private TcServerUrlMapping() {
  }

  public static TcServerUrlMapping getInstance() {
    return INSTANCE;
  }

  @Override
  protected void collectFacetContextProviders(List<FacetContextProvider> facetContextProvider) {
    super.collectFacetContextProviders(facetContextProvider);
    facetContextProvider.add(new WebFacetContextProvider());
  }
}
