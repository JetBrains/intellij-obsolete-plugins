package com.intellij.jboss.bpmn.jpdl.providers;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JpdlBeansTemplatesFactory implements FileTemplateGroupDescriptorFactory {
  public static final String PROCESS_4_4_JPDL_XML = "process.4.4.jpdl.xml";

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    final Icon icon = JbossJbpmIcons.Jboss;
    final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("jPDL", icon);
    final FileTemplateDescriptor descriptor = new FileTemplateDescriptor(PROCESS_4_4_JPDL_XML, icon) {
      @Override
      public @NotNull String getDisplayName() {
        return JpdlBundle.message("jpdl.4.4.template.title");
      }
    };
    group.addTemplate(descriptor);
    return group;
  }
}
