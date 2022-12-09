package com.intellij.dmserver.libraries.obr;

import com.intellij.dmserver.libraries.ProgressListener;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public final class HttpRetriever {
  private HttpRetriever() {
  }

  public static String retrievePage(String url) throws IOException {
    return retrievePage(url, null);
  }

  public static String retrievePage(String url, @Nullable final ProgressListener progressListener) throws IOException {
    return HttpRequests.request(url).readString(createIndicator(progressListener));
  }

  public static File downloadFile(@NotNull String url, @NotNull final File targetFolder, @Nullable final ProgressListener progressListener) throws IOException {
    return HttpRequests.request(url).connect(new HttpRequests.RequestProcessor<>() {
      @Override
      public File process(@NotNull HttpRequests.Request request) throws IOException {
        String urlPath = request.getConnection().getURL().getPath();
        String targetPath = targetFolder.getAbsolutePath() + File.separatorChar + urlPath.substring(urlPath.lastIndexOf('/') + 1);
        return request.saveToFile(new File(targetPath), createIndicator(progressListener));
      }
    });
  }

  @Nullable
  private static AbstractProgressIndicatorBase createIndicator(@Nullable final ProgressListener progressListener) throws IOException {
    return progressListener == null ? null : new AbstractProgressIndicatorExBase() {
      @Override
      public void setFraction(double fraction) {
        progressListener.setProgressText((int)(100 * fraction + .5) + "%");
      }
    };
  }
}
