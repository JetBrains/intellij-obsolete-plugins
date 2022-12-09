package com.intellij.dmserver.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.lang.manifest.ManifestFileType;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.HeaderValue;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.jetbrains.lang.manifest.psi.Section;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * @author michael.golubev
 */
public final class ManifestUtils {
  @NonNls public static final String IMPORT_BUNDLE_HEADER = "Import-Bundle";
  @NonNls public static final String IMPORT_LIBRARY_HEADER = "Import-Library";
  @NonNls public final static String VERSION_RANGE_ATTRIBUTE_NAME = "version";
  @NonNls public final static String VERSION_ATTRIBUTE_NAME = "version";
  @NonNls public static final String WEB_CONTEXT_PATH_HEADER = "Web-ContextPath";

  private static final ManifestUtils ourInstance = new ManifestUtils();

  public static ManifestUtils getInstance() {
    return ourInstance;
  }

  private ManifestUtils() { }

  @Nullable
  public String getHeaderValue(@NotNull PsiFile manifestFile, @NotNull String headerName) {
    Header header = findHeader(manifestFile, headerName);
    return header != null ? getHeaderValue(header) : null;
  }

  @Nullable
  public String getHeaderValue(@NotNull Header header) {
    HeaderValue value = header.getHeaderValue();
    return value != null ? value.getUnwrappedText() : null;
  }

  @Nullable
  public Header findHeader(@NotNull PsiFile manifestFile, @NotNull String headerName) {
    List<Header> headers = findHeaders(manifestFile, headerName);
    return headers.size() == 0 ? null : headers.get(0);
  }

  @NotNull
  public List<Header> findHeaders(@NotNull PsiFile file, @NotNull final String headerName) {
    if (!(file instanceof ManifestFile)) {
      return Collections.emptyList();
    }

    return ContainerUtil.filter(((ManifestFile)file).getHeaders(), header -> headerName.equalsIgnoreCase(header.getName()));
  }

  public Header createHeader(@NotNull Project project, @NotNull String headerName, @NotNull String headerValue) {
    return createHeader(project, createHeaderText(headerName, headerValue));
  }

  public String createHeaderText(@NotNull String headerName, @NotNull String headerValue) {
    return MessageFormat.format("{0}: {1}\n", headerName, headerValue);
  }

  public Header createHeader(@NotNull Project project, @NotNull CharSequence headerText) {
    return createDummyFile(project, headerText).getHeaders().get(0);
  }

  private static ManifestFile createDummyFile(@NotNull Project project, @NonNls @NotNull CharSequence content) {
    return (ManifestFile)PsiFileFactory.getInstance(project).createFileFromText("DUMMY.MF", ManifestFileType.INSTANCE, content);
  }

  private static Section findSection(@NotNull PsiFile file) {
    return file instanceof ManifestFile ? ((ManifestFile)file).getMainSection() : null;
  }

  public void addHeader(@NotNull PsiFile manifestFile, @NotNull Header header) {
    Section section = findSection(manifestFile);
    if (section == null) {
      Section dummySection = findSection(createDummyFile(manifestFile.getManager().getProject(), "Header: value"));
      dummySection.deleteChildRange(dummySection.getFirstChild(), dummySection.getLastChild());
      manifestFile.add(dummySection);
      section = findSection(manifestFile);
    }
    section.add(header);
  }
}
