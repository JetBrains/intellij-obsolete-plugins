package com.intellij.lang.puppet.psi.stubs;

import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PuppetSerializationUtil {
  public static void serializeList(@NotNull StubOutputStream dataStream, @Nullable List<String> list) throws IOException {
    if (list == null) {
      dataStream.writeInt(-1);
    }
    else {
      dataStream.writeInt(list.size());
      for (String name : list) {
        dataStream.writeName(name);
      }
    }
  }

  public static @Nullable List<String> deserializeList(@NotNull StubInputStream dataStream) throws IOException {
    int dataSize = dataStream.readInt();
    if (dataSize == -1) {
      return null;
    }
    List<String> list = new ArrayList<>(dataSize);
    for (int i = 0; i < dataSize; i++) {
      list.add(dataStream.readNameString());
    }
    return list;
  }
}
