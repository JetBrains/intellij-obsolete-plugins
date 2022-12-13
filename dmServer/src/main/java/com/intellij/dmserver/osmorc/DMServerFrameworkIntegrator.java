package com.intellij.dmserver.osmorc;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.frameworkintegration.FrameworkRunner;
import org.osmorc.frameworkintegration.impl.AbstractFrameworkIntegrator;
import org.osmorc.run.OsgiRunConfiguration;
import org.osmorc.run.ui.SelectedBundle;

import java.util.List;

/**
 * @author michael.golubev
 */
public class DMServerFrameworkIntegrator extends AbstractFrameworkIntegrator {
  @NonNls
  public static final String FRAMEWORK_NAME = "dmServer";

  public DMServerFrameworkIntegrator() {
    super(ApplicationManager.getApplication().getService(DMServerFrameworkInstanceManager.class));
  }

  public static boolean isCompatibleInstance(FrameworkInstanceDefinition frameworkInstance) {
    return frameworkInstance != null && FRAMEWORK_NAME.equals(frameworkInstance.getFrameworkIntegratorName());
  }

  @NotNull
  @Override
  public DMServerFrameworkInstanceManager getFrameworkInstanceManager() {
    return (DMServerFrameworkInstanceManager)super.getFrameworkInstanceManager();
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return FRAMEWORK_NAME;
  }

  @Override
  @NotNull
  public FrameworkRunner createFrameworkRunner() {
    return new FrameworkRunner() {
      @Override
      public JavaParameters createJavaParameters(@NotNull OsgiRunConfiguration runConfiguration,
                                                 @NotNull List<SelectedBundle> bundles) throws ExecutionException {
        throw new ExecutionException(DmServerBundle.message("DMServerFrameworkIntegrator.dm.server.not.supported"));
      }

      @Override
      public void dispose() { }
    };
  }
}
