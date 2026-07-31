package com.intellij.gwt.run;

import com.intellij.compiler.CompilerIOUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class HostedModeWarDirectoryCache {
  private static final Logger LOG = Logger.getInstance(HostedModeWarDirectoryCache.class);
  private static final int VERSION = 0;
  private final File myCacheFile;
  private final Object2LongMap<String> myTimestamps=new Object2LongOpenHashMap<>();
  private final Map<String, String> myTargetPaths = new HashMap<>();
  private boolean myChanged;

  public HostedModeWarDirectoryCache(File cacheFile) {
    myCacheFile = cacheFile;
  }

  public Collection<String> getTargetPaths() {
    return myTargetPaths.values();
  }

  public Collection<String> getSourcePaths() {
    return myTargetPaths.keySet();
  }

  public void load() {
    myTimestamps.clear();
    myTargetPaths.clear();
    myChanged = false;
    if (!myCacheFile.exists()) {
      return;
    }

    try (DataInputStream input = new DataInputStream(new FileInputStream(myCacheFile))) {
      final int version = input.readInt();
      if (version != VERSION) return;

      int count = input.readInt();
      while (count-- > 0) {
        final String path = CompilerIOUtil.readString(input);
        final String targetPath = CompilerIOUtil.readString(input);
        long timestamp = input.readLong();
        myTimestamps.put(path, timestamp);
        myTargetPaths.put(path, targetPath);
      }
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  public void save() {
    if (!myChanged) {
      return;
    }
    FileUtil.createIfDoesntExist(myCacheFile);

    try (DataOutputStream output = new DataOutputStream(new FileOutputStream(myCacheFile))) {
      output.writeInt(VERSION);
      output.writeInt(myTimestamps.size());
      for (Object2LongMap.Entry<String> entry : myTimestamps.object2LongEntrySet()) {
        final String path = entry.getKey();
        CompilerIOUtil.writeString(path, output);
        CompilerIOUtil.writeString(myTargetPaths.get(path), output);
        output.writeLong(entry.getLongValue());
      }
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  public long getTimestamp(String path) {
    if (!myTimestamps.containsKey(path)) {
      return -1;
    }
    return myTimestamps.getLong(path);
  }

  public String getTargetPath(String path) {
    return myTargetPaths.get(path);
  }

  public void updateTimestamp(String path, long timestamp, String targetPath) {
    myChanged = true;
    myTimestamps.put(path, timestamp);
    myTargetPaths.put(path, targetPath);
  }

  public void remove(Set<String> paths) {
    if (!paths.isEmpty()) {
      myChanged = true;
      for (String path : paths) {
        myTimestamps.removeLong(path);
        myTargetPaths.remove(path);
      }
    }
  }
}
