// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.debug;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.debugger.impl.DebuggerUtilsAsync;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;

import java.util.*;
import java.util.regex.Pattern;

public class GspPositionManager implements PositionManager {

  private final DebugProcess myDebugProcess;

  private final Map<String, String> className2gspName = Collections.synchronizedMap(new HashMap<>());

  public static final Pattern ESCAPED_CHAR = Pattern.compile("[^\\w]");
  private static final Set<FileType> ourFileTypes = Collections.singleton(GspFileType.GSP_FILE_TYPE);

  public GspPositionManager(DebugProcess debugProcess) {
    myDebugProcess = debugProcess;
  }

  private @Nullable String getGspClassName(PsiFile file) {
    if (!(file instanceof GspFile)) return null;

    String path

    = ReadAction.compute(()->{
      VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
      if (virtualFile == null) return null;
      return virtualFile.getPath();
    });

    String className = ESCAPED_CHAR.matcher(path.startsWith("/") ? path.substring(1) : path).replaceAll("_");

    className2gspName.put(className, path);

    return className;
  }

  @Override
  public SourcePosition getSourcePosition(Location location) throws NoDataException {
    if (location == null) throw NoDataException.INSTANCE;

    final ReferenceType refType = location.declaringType();
    if (refType == null) throw NoDataException.INSTANCE;

    String className = refType.name();
    int dollar = className.indexOf('$');
    String qname = dollar == -1 ? className : className.substring(0, dollar);

    String path = className2gspName.get(qname);
    if (path == null) throw NoDataException.INSTANCE;

    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(path);
    if (virtualFile == null) throw NoDataException.INSTANCE;

    PsiFile psiFile = PsiManager.getInstance(myDebugProcess.getProject()).findFile(virtualFile);

    checkGspFile(psiFile);

    return SourcePosition.createFromLine(psiFile, location.lineNumber() - 1);
  }

  private static void collectNestedTypes(List<ReferenceType> res, ReferenceType type, int line) {
    if (!type.isPrepared()) return;

    try {
      if (!DebuggerUtilsAsync.locationsOfLineSync(type, line).isEmpty()) {
        res.add(type);
      }
    }
    catch (AbsentInformationException ignored) {
    }

    Map<String, ReferenceType> nestedTypeMap = new HashMap<>();

    for (ReferenceType referenceType : type.nestedTypes()) {
      nestedTypeMap.put(referenceType.name(), referenceType);
    }

    for (ReferenceType referenceType : nestedTypeMap.values()) {
      collectNestedTypes(res, referenceType, line);
    }
  }

  @Override
  public @NotNull List<ReferenceType> getAllClasses(@NotNull SourcePosition classPosition) throws NoDataException {
    PsiFile file = classPosition.getFile();

    checkGspFile(file);

    String className = getGspClassName(file);

    if (className == null) throw NoDataException.INSTANCE;

    List<ReferenceType> res = new ArrayList<>();

    List<ReferenceType> referenceTypes = myDebugProcess.getVirtualMachineProxy().classesByName(className);
    if (!referenceTypes.isEmpty()) {
      collectNestedTypes(res, referenceTypes.get(referenceTypes.size() - 1), classPosition.getLine() + 1);
    }

    return res;
  }

  private static void checkGspFile(PsiFile file) throws NoDataException {
    if (!(file instanceof GspFile)) {
      throw NoDataException.INSTANCE;
    }
  }

  @Override
  public @NotNull List<Location> locationsOfLine(@NotNull ReferenceType type, @NotNull SourcePosition position) throws NoDataException {
    checkGspFile(position.getFile());

    String className = type.name();
    int dollar = className.indexOf('$');
    String qname = dollar == -1 ? className : className.substring(0, dollar);

    if (!className2gspName.containsKey(qname)) throw NoDataException.INSTANCE;

    try {
      List<Location> locations = DebuggerUtilsAsync.locationsOfLineSync(type, position.getLine() + 1);
      if (!locations.isEmpty()) {
        return locations;
      }
    }
    catch (AbsentInformationException e) {
      // Ignore - throw NoDataException.INSTANCE;
    }

    throw NoDataException.INSTANCE;
  }

  @Override
  public ClassPrepareRequest createPrepareRequest(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position) throws NoDataException {
    String className = getGspClassName(position.getFile());

    if (className == null) throw NoDataException.INSTANCE;

    return myDebugProcess.getRequestsManager().createClassPrepareRequest(requestor, className + '*');
  }

  @Override
  public @NotNull Set<? extends FileType> getAcceptedFileTypes() {
    return ourFileTypes;
  }
 }
