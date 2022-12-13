package com.intellij.seam.structure;

import com.intellij.ide.DeleteProvider;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.javaee.module.view.nodes.JavaeeNodeDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeamModuleLibrariesNodeDescriptor extends JavaeeNodeDescriptor<Module> {
  private final List<? extends SeamJamComponent> myCompiledElements;
  private final List<? extends XmlFile> myModelFiles;

  public SeamModuleLibrariesNodeDescriptor(final List<? extends SeamJamComponent> compiledElements,
                                           final List<? extends XmlFile> modelFiles,
                                           final Project project,
                                           final NodeDescriptor parentDescriptor,
                                           final Object parameters,
                                           final Module element) {
    super(project, parentDescriptor, parameters, element);

    myCompiledElements = compiledElements;
    myModelFiles = modelFiles;
  }

  @Override
  protected String getNewNodeText() {
    return SeamBundle.message("seam.j2ee.structure.module.libraries");
  }

  @Override
  protected DeleteProvider getDeleteProvider() {
    return super.getDeleteProvider();
  }

  @Override
  public JavaeeNodeDescriptor @NotNull [] getChildren() {
    List<JavaeeNodeDescriptor> nodes = new ArrayList<>();
    Map<VirtualFile, List<SeamJamComponent>> libs = new HashMap<>();
    Map<VirtualFile, List<XmlFile>> modelLibs = new HashMap<>();

    List<SeamJamComponent> unknownLib = new ArrayList<>();

    for (SeamJamComponent compiledElement : myCompiledElements) {
      VirtualFile forJar = getJarFile(compiledElement.getPsiElement().getContainingFile().getVirtualFile());
      if (forJar != null) {
        if (libs.get(forJar) == null) libs.put(forJar, new ArrayList<>());
        libs.get(forJar).add(compiledElement);
      }
      else {
        unknownLib.add(compiledElement);
      }
    }

    for (XmlFile modelFile : myModelFiles) {
      VirtualFile forJar = getJarFile(modelFile.getVirtualFile());
      if (forJar != null) {
        if (modelLibs.get(forJar) == null) modelLibs.put(forJar, new ArrayList<>());
        modelLibs.get(forJar).add(modelFile);
      }
    }

    for (VirtualFile file : libs.keySet()) {
      nodes.add(new ModuleLibNodeDescriptor(getProject(), this, getParameters(), file, libs.get(file), modelLibs.get(file)));
    }
    for (SeamJamComponent seamComponent : unknownLib) {
      nodes.add(new SeamComponentNodeDescriptor(seamComponent, this, getParameters()));
    }

    return nodes.toArray(JavaeeNodeDescriptor.EMPTY_ARRAY);
  }

  private VirtualFile getJarFile(final VirtualFile file) {
    VirtualFile forJar = JarFileSystem.getInstance().getVirtualFileForJar(file);
    return forJar;
  }

  @Override
  protected Icon getNewIcon() {
    return PlatformIcons.LIBRARY_ICON;
  }

  private static final class ModuleLibNodeDescriptor extends JavaeeNodeDescriptor<VirtualFile> {
    private final List<? extends SeamJamComponent> mySeamComponents;
    private final List<? extends XmlFile> myXmlFiles;

    private ModuleLibNodeDescriptor(final Project project,
                                    final NodeDescriptor parentDescriptor,
                                    final Object parameters,
                                    final VirtualFile element,
                                    @Nullable final List<? extends SeamJamComponent> seamComponents,
                                    @Nullable final List<? extends XmlFile> xmlFiles) {
      super(project, parentDescriptor, parameters, element);
      mySeamComponents = seamComponents;
      myXmlFiles = xmlFiles;
    }

    @Override
    protected String getNewNodeText() {
      return getJamElement().getName();
    }

    @Override
    public JavaeeNodeDescriptor @NotNull [] getChildren() {
      List<JavaeeNodeDescriptor> nodes = new ArrayList<>();
      if (myXmlFiles != null) {
        for (XmlFile xmlFile : myXmlFiles) {
          nodes.add(new SeamDomModelNodeDescriptor(getProject(), this, getParameters(), xmlFile, JarFileSystem.getInstance().getJarRootForLocalFile(
            getJamElement())));
        }
      }
      if (mySeamComponents != null) {
        for (SeamJamComponent seamComponent : mySeamComponents) {
          nodes.add(new SeamComponentNodeDescriptor(seamComponent, this, getParameters()));
        }
      }
      return nodes.toArray(JavaeeNodeDescriptor.EMPTY_ARRAY);
    }

    @Override
    protected Icon getNewIcon() {
      return getJamElement().getFileType().getIcon();
    }
  }

  @Override
  public int getWeight() {
    return 10000;
  }
}
