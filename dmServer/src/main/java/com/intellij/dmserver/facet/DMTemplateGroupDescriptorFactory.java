package com.intellij.dmserver.facet;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.ide.highlighter.XmlFileType;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NonNls;

public class DMTemplateGroupDescriptorFactory implements FileTemplateGroupDescriptorFactory {
  @NonNls
  public static final String DM_SPRING_MODULE_CONTEXT_TEMPLATE = "module-context.xml";
  @NonNls
  public static final String DM_SPRING_OSGI_CONTEXT_TEMPLATE = "osgi-context.xml";

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    final FileTemplateDescriptor moduleContextXml =
      new FileTemplateDescriptor(DM_SPRING_MODULE_CONTEXT_TEMPLATE, XmlFileType.INSTANCE.getIcon());
    final FileTemplateDescriptor osgiContextXml = new FileTemplateDescriptor(DM_SPRING_OSGI_CONTEXT_TEMPLATE, XmlFileType.INSTANCE.getIcon());
    return new FileTemplateGroupDescriptor(DmServerBundle.message("DMTemplateGroupDescriptorFactory.template.group.name"),
                                           DmServerSupportIcons.DM, moduleContextXml, osgiContextXml);
  }
}
