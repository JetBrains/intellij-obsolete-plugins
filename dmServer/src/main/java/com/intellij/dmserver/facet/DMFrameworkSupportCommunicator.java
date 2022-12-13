package com.intellij.dmserver.facet;

import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportCommunicator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;

import java.util.List;

public class DMFrameworkSupportCommunicator extends FrameworkSupportCommunicator {

  @Override
  public void onFrameworkSupportAdded(Module module,
                                      ModifiableRootModel rootModel,
                                      List<FrameworkSupportConfigurable> selectedFrameworks,
                                      FrameworkSupportModel model) {
    for (FrameworkSupportConfigurable framework : selectedFrameworks) {
      if (framework instanceof DMServerSupportConfigurable) {
        ((DMServerSupportConfigurable)framework).finishAddSupport(module, rootModel);
      }
    }
  }
}
