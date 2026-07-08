package com.intellij.lang.puppet.ide.navigation.plugins;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PuppetExtFunctionInfo {
  public static final DataExternalizer<PuppetExtFunctionInfo> EXTERNALIZER = new MyExternalizer();

  private final int myOffsetInFile;
  private final @NotNull List<PuppetExtParamInfo> myParamInfos;

  public PuppetExtFunctionInfo(int offsetInFile,
                               @NotNull List<PuppetExtParamInfo> paramInfos) {
    myOffsetInFile = offsetInFile;
    myParamInfos = paramInfos;
  }

  public int getOffsetInFile() {
    return myOffsetInFile;
  }

  public @NotNull List<PuppetExtParamInfo> getParamInfos() {
    return myParamInfos;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PuppetExtFunctionInfo info = (PuppetExtFunctionInfo)o;

    if (myOffsetInFile != info.myOffsetInFile) return false;
    if (!myParamInfos.equals(info.myParamInfos)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myOffsetInFile;
    result = 31 * result + myParamInfos.hashCode();
    return result;
  }

  public static class PuppetExtParamInfo {

    private final int myOffsetInFile;
    private final @NotNull String myParamName;
    private final @Nullable String myDescription;

    public PuppetExtParamInfo(int offsetInFile, @NotNull String paramName, @Nullable String description) {
      myOffsetInFile = offsetInFile;
      myParamName = paramName;
      myDescription = description;
    }

    public int getOffsetInFile() {
      return myOffsetInFile;
    }

    public @NotNull String getParamName() {
      return myParamName;
    }

    public @Nullable String getDescription() {
      return myDescription;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PuppetExtParamInfo info = (PuppetExtParamInfo)o;

      if (myOffsetInFile != info.myOffsetInFile) return false;
      if (!myParamName.equals(info.myParamName)) return false;
      if (!Objects.equals(myDescription, info.myDescription)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myOffsetInFile;
      result = 31 * result + myParamName.hashCode();
      result = 31 * result + (myDescription != null ? myDescription.hashCode() : 0);
      return result;
    }
  }

  private static class MyExternalizer implements DataExternalizer<PuppetExtFunctionInfo> {

    @Override
    public void save(@NotNull DataOutput out, PuppetExtFunctionInfo value) throws IOException {
      EnumeratorIntegerDescriptor.INSTANCE.save(out, value.myOffsetInFile);
      out.writeInt(value.myParamInfos.size());
      for (PuppetExtParamInfo info : value.myParamInfos) {
        out.writeInt(info.myOffsetInFile);
        out.writeUTF(info.myParamName);
        out.writeUTF(StringUtil.notNullize(info.myDescription));
      }
    }

    @Override
    public PuppetExtFunctionInfo read(@NotNull DataInput in) throws IOException {
      final Integer offset = EnumeratorIntegerDescriptor.INSTANCE.read(in);
      int numberOfParams = in.readInt();
      List<PuppetExtParamInfo> paramInfos = numberOfParams == 0 ? Collections.emptyList()
                                                                : new ArrayList<>(numberOfParams);
      for (int i = 0; i < numberOfParams; ++i) {
        final int fileOffset = in.readInt();
        final String paramName = in.readUTF();
        final String desc = StringUtil.nullize(in.readUTF());

        paramInfos.add(new PuppetExtParamInfo(fileOffset, paramName, desc));
      }

      return new PuppetExtFunctionInfo(offset, paramInfos);
    }
  }
}
