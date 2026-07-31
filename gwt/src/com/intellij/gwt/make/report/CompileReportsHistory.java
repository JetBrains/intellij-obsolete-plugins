package com.intellij.gwt.make.report;

import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class CompileReportsHistory {
  private static final Logger LOG = Logger.getInstance(CompileReportsHistory.class);
  private final ProjectCompileReports myReports;
  private final Project myProject;

  public static CompileReportsHistory getInstance(@NotNull Project project) {
    return project.getService(CompileReportsHistory.class);
  }

  CompileReportsHistory(Project project) {
    myProject = project;
    myReports = loadReports(project);
  }

  private static ProjectCompileReports loadReports(Project project) {
    Path reportsFile = GwtCompilerPaths.getCompileReportInfo(project);
    try {
      if (Files.exists(reportsFile)) {
        return XmlSerializer.deserialize(JDOMUtil.load(reportsFile), ProjectCompileReports.class);
      }
    }
    catch (Exception e) {
      LOG.info(e);
    }
    return new ProjectCompileReports();
  }

  public synchronized @Nullable GwtModuleCompileReport getCompileReport(String gwtModuleName) {
    return myReports.myReportGenerationTimes.get(gwtModuleName);
  }

  public synchronized void updateReport(String gwtModuleName, long generationTime, String path) {
    myReports.myReportGenerationTimes.put(gwtModuleName, new GwtModuleCompileReport(generationTime, path));
    save();
  }

  private void save() {
    Element root = XmlSerializer.serialize(myReports);
    try {
      JDOMUtil.write(root, GwtCompilerPaths.getCompileReportInfo(myProject), System.lineSeparator());
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  public static class ProjectCompileReports {
    @Tag("generated-reports")
    @MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false,
                   entryTagName = "gwt-module", keyAttributeName = "name")
    public Map<String, GwtModuleCompileReport> myReportGenerationTimes = new HashMap<>();
  }

  @Tag("compile-report")
  public static class GwtModuleCompileReport {
    private long myGenerationTime;
    private String myPath;

    public GwtModuleCompileReport() {
    }

    public GwtModuleCompileReport(long generationTime, String path) {
      myGenerationTime = generationTime;
      myPath = path;
    }

    @Tag("generation-time")
    public long getGenerationTime() {
      return myGenerationTime;
    }

    public void setGenerationTime(long generationTime) {
      myGenerationTime = generationTime;
    }

    @Tag("path")
    public String getPath() {
      return myPath;
    }

    public void setPath(String path) {
      myPath = path;
    }
  }
}
