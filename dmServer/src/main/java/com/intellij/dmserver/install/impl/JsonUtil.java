package com.intellij.dmserver.install.impl;

import com.intellij.openapi.vfs.VirtualFile;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class JsonUtil {

  private static String convertStreamToString(InputStream is) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      return reader.lines().map(line -> line + "\n").collect(Collectors.joining());
    }
  }


  public static JSONObject loadConfig(VirtualFile file) throws IOException {
    try (InputStream is = file.getInputStream()) {
      return new JSONObject(convertStreamToString(is));
    }
    catch (IOException | JSONException e) {
      throw new IOException(".config file has unknown format", e); // TODO: not the correct msg
    }
  }

  public static void saveConfig(VirtualFile file, JSONObject config) throws IOException {
    try (OutputStreamWriter osw = new OutputStreamWriter(file.getOutputStream(JsonUtil.class), StandardCharsets.UTF_8)) {
      config.write(osw);
    }
    catch (IOException | JSONException e) {
      throw new IOException(".config file has unknown format", e); // TODO: not the correct msg
    }
  }
}
