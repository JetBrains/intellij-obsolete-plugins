package com.intellij.restClient;

import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Dennis.Ushakov
 */
public final class RestClientFileUtil {
  private static final Logger LOG = Logger.getInstance(RESTClient.class);
  private static final String REST_CLIENT_ROOT = "dummy://restClient";
  private static int ourRequestCounter = 0;

  public static FileType findFileType(String mimeType) {
    FileType fileType = PlainTextFileType.INSTANCE;
    if (mimeType != null && mimeType.endsWith("+json")) {
      mimeType = "application/json";
    }
    Collection<Language> languages = Language.findInstancesByMimeType(mimeType);
    if (isJson(languages)) {
      return JsonFileType.INSTANCE;
    }

    for (Language language : languages) {
      LanguageFileType langFileType = language.getAssociatedFileType();
      if (langFileType != null) {
        fileType = langFileType;
        break;
      }
    }
    return fileType;
  }

  private static boolean isJson(@NotNull Collection<Language> languages) {
    for (Language language : languages) {
      if (language == JsonLanguage.INSTANCE) {
        return true;
      }
    }
    return false;
  }

  public static VirtualFile createFile(final String response, final FileType fileType) {
    return WriteAction.compute(() -> {
      VirtualFile result = null;
      VirtualFile dummyRoot = getOrCreateDummyRoot();
      try {
        result = DummyFileSystem.getInstance().createChildFile(null, dummyRoot, generateFilename(fileType));
        VfsUtil.saveText(result, response);
      }
      catch (IOException e) {
        LOG.error(e);
      }
      return result;
    });
  }

  static void deleteFile(final VirtualFile file) {
    if (file == null) return;
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        DummyFileSystem.getInstance().deleteFile(null, file);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    });
  }

  private static String generateFilename(FileType fileType) {
    return "response" + (ourRequestCounter++) + "." + fileType.getDefaultExtension();
  }

  private static VirtualFile getOrCreateDummyRoot() {
    VirtualFile dummyRoot = VirtualFileManager.getInstance().refreshAndFindFileByUrl(REST_CLIENT_ROOT);
    if (dummyRoot == null) {
      dummyRoot = DummyFileSystem.getInstance().createRoot("restClient");
    }
    return dummyRoot;
  }
}