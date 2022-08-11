package com.intellij.vaadin.framework;

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.ultimate.PluginVerifier;
import com.intellij.vaadin.VaadinBundle;
import com.intellij.vaadin.VaadinIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class VaadinFrameworkType extends FrameworkTypeEx {
  public static final String ID = "vaadin";

  public VaadinFrameworkType() {
    super(ID);
  }

  static {
    PluginVerifier.verifyUltimatePlugin();
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleProvider createProvider() {
    return new VaadinFrameworkSupportProvider(this);
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return VaadinBundle.VAADIN;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return VaadinIcons.VaadinIcon;
  }

  @Nullable
  @Override
  public String getUnderlyingFrameworkTypeId() {
    return FacetBasedFrameworkSupportProvider.getProviderId(WebFacet.ID);
  }
}
