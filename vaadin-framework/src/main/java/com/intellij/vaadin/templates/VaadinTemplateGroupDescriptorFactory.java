package com.intellij.vaadin.templates;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.vaadin.VaadinBundle;
import com.intellij.vaadin.framework.VaadinVersion;
import com.intellij.vaadin.framework.VaadinVersionUtil;
import com.intellij.vaadin.VaadinIcons;

import java.util.ArrayList;
import java.util.List;

final class VaadinTemplateGroupDescriptorFactory implements FileTemplateGroupDescriptorFactory {
  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    List<FileTemplateDescriptor> groups = new ArrayList<>();
    for (VaadinVersion version : VaadinVersionUtil.getAllVersions()) {
      List<FileTemplateDescriptor> descriptors = new ArrayList<>();
      for (String templateName : version.getTemplateNames().getAllTemplates()) {
        descriptors.add(new FileTemplateDescriptor(templateName));
      }
      groups.add(new FileTemplateGroupDescriptor(version.getVersionName(), VaadinIcons.VaadinIcon,
                                                 descriptors.toArray(new FileTemplateDescriptor[0])));
    }
    return new FileTemplateGroupDescriptor(VaadinBundle.VAADIN, VaadinIcons.VaadinIcon, groups.toArray(new FileTemplateDescriptor[0]));
  }
}
