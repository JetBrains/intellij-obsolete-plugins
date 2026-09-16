package com.intellij.bigdatatools.zeppelin.psi;

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile;
import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile;
import com.intellij.bigdatatools.zeppelin.integration.ZeppelinAutoImportPlacer;
import com.intellij.bigdatatools.zeppelin.utils.ScalaIntegrationUtil;
import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.LanguageImportStatements;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import kotlin.Unit;
import org.jetbrains.plugins.scala.editor.importOptimizer.ImportInfo;
import org.jetbrains.plugins.scala.editor.importOptimizer.ImportRangeInfo;
import org.jetbrains.plugins.scala.editor.importOptimizer.OptimizeImportSettings;
import org.jetbrains.plugins.scala.editor.importOptimizer.ScalaImportOptimizer;
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.usages.ImportUsed;
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl;
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory$;
import scala.Function1;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.collection.immutable.Set;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.ArrayBuffer$;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"ConditionCoveredByFurtherCondition", "ConstantValue"})
public class ZeppelinScalaPsiFile extends ScalaFileImpl {
  public ZeppelinScalaPsiFile(FileViewProvider viewProvider,
                              LanguageFileType getFileType) {
    super(viewProvider, getFileType);
  }

  @Override
  public PsiElement insertFirstImport(ScImportStmt importSt, PsiElement first) {
    if (first.getNode().getElementType() == ZeppelinTemplateTypes.OUTER && first.getNextSibling() != null) {
      first = first.getNextSibling();
      if (first instanceof PsiWhiteSpace && first.getNextSibling() != null) first = first.getNextSibling();
    }

    return super.insertFirstImport(importSt, first);
  }

  @Override
  public void addImportForPath(ImportPath path, PsiElement refsContainer) {
    ZeppelinAutoImportPlacer placer =
      ZeppelinAutoImportPlacer.Companion.getImportPlacer(refsContainer != null ? refsContainer : this);
    if (placer == null) {
      insertWithAnchor(getDefaultAnchor(), path);
      return;
    }

    placer.findAnchor(refsContainer != null? refsContainer : this, new kotlin.jvm.functions.Function1<>() {
      @Override
      public Unit invoke(PsiElement anchor) {
        insertWithAnchor(anchor == null ? getDefaultAnchor() : anchor, path);
        return null;
      }
    });
  }

  @Override
  public boolean isMultipleDeclarationsAllowed() {
    return true;
  }

  @Override
  public boolean isWorksheetFile() {
    VirtualFile vf = getVirtualFile();
    if (!(vf instanceof NotebookVirtualFile)) return super.isWorksheetFile();
    return !(((NotebookVirtualFile)vf).getOriginalFile() instanceof ZeppelinRemoteFile);
  }

  protected PsiElement getDefaultAnchor() {
    return getFirstChild();
  }

  private void insertWithAnchor(PsiElement anchor, ImportPath path) {
    Tuple2<ScImportStmt, scala.collection.immutable.Seq<ImportInfo>> infos = filterAndCreate(path, anchor);

    CommandProcessor.getInstance().executeCommand(anchor.getProject(), () -> ApplicationManager.getApplication().runWriteAction(
      () -> insertAutoImports(infos, anchor)), null, null);
  }

  private void insertAutoImports(Tuple2<ScImportStmt, scala.collection.immutable.Seq<ImportInfo>> infos, PsiElement anchor) {
    final PsiElement actualAnchor = anchor != null? anchor : getFirstChild();
    boolean needInsertFirst = true;
    PsiElement current = actualAnchor;

    ScalaImportOptimizer optimizer = findImportOptimizer();

    while (current != null && (current instanceof PsiWhiteSpace || current instanceof ScImportStmt ||
                               current instanceof PsiComment || current.getNode().getElementType() == ZeppelinTemplateTypes.OUTER)) {
      if (current instanceof ScImportStmt) {
        needInsertFirst = false;
        break;
      }

      current = current.getNextSibling();
    }

    scala.collection.immutable.Seq<Tuple2<ScImportStmt, scala.collection.immutable.Seq<ImportInfo>>> scalaInfos = createSeqFromJava(List.of(infos));
    OptimizeImportSettings settings = OptimizeImportSettings.apply(this);

    if (needInsertFirst) {
      PsiElement el = current != null && current.getPrevSibling() != null ? current.getPrevSibling() :
                      actualAnchor instanceof PsiWhiteSpace || actualAnchor.getNextSibling() == null ? actualAnchor : actualAnchor.getNextSibling();

      while (!(el.getParent() instanceof ScImportsHolder || el.getParent() instanceof PsiFile)) el = el.getParent();

      el.getParent().addBefore(ScalaPsiElementFactory$.MODULE$.createNewLine("\n", getProject()), el);
      PsiElement dummyImport = el.getParent().addBefore(ScalaPsiElementFactory$.MODULE$.createImportFromText("import dummy._", el.getParent(), null), el);
      PsiAnchor importAnchor = PsiAnchor.create(dummyImport);
      ImportRangeInfo rangeInfo =
        new ImportRangeInfo(importAnchor, importAnchor, scalaInfos, ScalaIntegrationUtil.createEmptyScalaSet(), false);

      optimizer.replaceWithNewImportInfos(rangeInfo, infos._2, settings, this);
    }
    else {
      Set<ImportRangeInfo> importRanges = optimizer.collectImportRanges(this, new SimpleScalaFunction1<>() {
        @Override
        public scala.collection.immutable.Seq<ImportInfo> apply(ScImportStmt v1) {
          return ImportInfo.createInfos(v1, createIsUsedFunction());
        }
      }, ScalaIntegrationUtil.createEmptyScalaSet());

      if (importRanges.isEmpty()) return;

      final PsiElement importRangeStart = current;

      Option<ImportRangeInfo> headOption = importRanges.find(new SimpleScalaFunction1<>() {
        @Override
        public Boolean apply(ImportRangeInfo v1) {
          return v1.firstPsi().retrieve() == importRangeStart;
        }
      });

      ImportRangeInfo head = headOption.isDefined() ? headOption.get() : null;
      if (head == null) return;

      ArrayBuffer<ImportInfo> buffer = ArrayBuffer$.MODULE$.empty();
      Iterator<Tuple2<ScImportStmt, scala.collection.immutable.Seq<ImportInfo>>> iter = head.importStmtWithInfos().iterator();

      while (iter.hasNext()) {
        buffer.addAll(iter.next()._2());
      }

      Seq<ImportInfo> resultInfos = ScalaImportOptimizer.insertImportInfos(infos._2(), buffer.toSeq(), head.firstPsi(), settings);
      optimizer.replaceWithNewImportInfos(head, resultInfos, settings, this);
    }
  }

  private ScalaImportOptimizer findImportOptimizer() {
    PsiFile mainFile = getViewProvider().getPsi(getViewProvider().getBaseLanguage());
    java.util.Set<ImportOptimizer> optimizers = LanguageImportStatements.INSTANCE.forFile(mainFile);
    if (optimizers.isEmpty()) return null;

    for (ImportOptimizer optimizer : optimizers) if (optimizer instanceof ScalaImportOptimizer) return (ScalaImportOptimizer) optimizer;
    return null;
  }

  private Tuple2<ScImportStmt, scala.collection.immutable.Seq<ImportInfo>> filterAndCreate(ImportPath path, PsiElement place) {
    ScImportStmt importStmt = ScalaPsiElementFactory$.MODULE$.createImportFromText("import " + path.importExpressionText(), place, null);
    Iterator<ImportInfo> infos = ImportInfo.createInfos(importStmt, createIsUsedFunction()).iterator();
    List<ImportInfo> result = new ArrayList<>();

    while (infos.hasNext()) {
      ImportInfo info = infos.next();

      ScStableCodeReference ref = ScalaPsiElementFactory$.MODULE$.createReferenceFromText(info.prefixQualifier().get(), this, place);
      if (ref.multiResolve(false).length > 0) result.add(info);
    }

    return new Tuple2<>(importStmt, createSeqFromJava(result));
  }

  private static <E> scala.collection.immutable.Seq<E> createSeqFromJava(List<E> objects) {
    return CollectionConverters.asScala(objects).toSeq();
  }

  private static Function1<ImportUsed, Object> createIsUsedFunction() {
    return new SimpleScalaFunction1<ImportUsed, Object>() {
      @Override
      public Boolean apply(ImportUsed v1) {
        return true;
      }
    };
  }

  private abstract static class SimpleScalaFunction1<A, B> implements Function1<A, B> {
    @Override
    public <A1> Function1<A1, B> compose(Function1<A1, A> g) {
      return null;
    }

    @Override
    public <A1> Function1<A, A1> andThen(Function1<B, A1> g) {
      return null;
    }
  }
}
