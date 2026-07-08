package com.intellij.lang.puppet.ide.refactoring;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.Language;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer;
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer;
import com.intellij.util.containers.Interner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class PuppetMemberInplaceRenamer extends MemberInplaceRenamer {
  private static final Logger LOG = Logger.getInstance(PuppetMemberInplaceRenamer.class);
  private final List<String> myOldNames = new ArrayList<>();

  public PuppetMemberInplaceRenamer(@NotNull PsiNamedElement elementToRename,
                                    PsiElement substituted,
                                    Editor editor) {
    super(elementToRename, substituted, editor);
  }

  public PuppetMemberInplaceRenamer(@NotNull PsiNamedElement elementToRename,
                                    PsiElement substituted,
                                    Editor editor, String initialName, String oldName) {
    super(elementToRename, substituted, editor, initialName, oldName);
  }

  @Override
  protected @NotNull TextRange getRangeToRename(@NotNull PsiElement element) {
    ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(element);
    return manipulator == null ? super.getRangeToRename(element) : manipulator.getRangeInElement(element);
  }

  // fixme this is a copypaste because of private com.intellij.refactoring.rename.inplace.MemberInplaceRenamer.appendAdditionalElement(java.util.Collection<com.intellij.openapi.util.Pair<com.intellij.psi.PsiElement,com.intellij.openapi.util.TextRange>>, com.intellij.psi.PsiNamedElement, com.intellij.psi.PsiElement)
  @Override
  protected boolean appendAdditionalElement(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages) {
    // fixme this is a DIRTY hack to avoid calling super method, which adds additional usage with full range
    boolean showChooser = stringUsages.isEmpty() || StartMarkAction.canStart(myEditor) != null;
    PsiNamedElement variable = getVariable();
    if (variable != null) {
      final PsiElement substituted = getSubstituted();
      if (substituted != null) {
        appendAdditionalElement(stringUsages, variable, substituted);
        RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(substituted);
        final HashMap<PsiElement, String> allRenames = new HashMap<>();
        PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        processor.prepareRenaming(substituted, "", allRenames, new LocalSearchScope(currentFile));
        for (PsiElement element : allRenames.keySet()) {
          appendAdditionalElement(stringUsages, variable, element);
        }
      }
    }
    return showChooser;
  }

  private void appendAdditionalElement(Collection<Pair<PsiElement, TextRange>> stringUsages,
                                       PsiNamedElement variable,
                                       PsiElement element) {
    if (element != variable && element instanceof PsiNameIdentifierOwner &&
        !notSameFile(null, element.getContainingFile())) {
      final PsiElement identifier = ((PsiNameIdentifierOwner)element).getNameIdentifier();
      if (identifier != null) {
        stringUsages.add(Pair.create(identifier, getRangeToRename(identifier)));
      }
    }
  }

  @Override
  protected @NotNull VariableInplaceRenamer createInplaceRenamerToRestart(PsiNamedElement variable, Editor editor, String initialName) {
    return new PuppetMemberInplaceRenamer(variable, getSubstituted(), editor, initialName, myOldName);
  }

  @Override
  protected boolean buildTemplateAndStart(@NotNull Collection<PsiReference> refs,
                                          @NotNull Collection<Pair<PsiElement, TextRange>> stringUsages,
                                          @NotNull PsiElement scope,
                                          @NotNull PsiFile containingFile) {
    Editor topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(myEditor);
    String documentText = topLevelEditor.getDocument().getText();

    boolean result = super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);
    if (result) {
      final TemplateState templateState = TemplateManagerImpl.getTemplateState(topLevelEditor);
      assert templateState != null;
      myOldNames.clear();
      Interner<String> stringInterner = Interner.createStringInterner();

      int count = templateState.getSegmentsCount();

      int documentOffset = 0;
      int templateOffset = 0;
      int initialNameSize = myInitialName.length();
      int oldNameSize = myOldName.length();

      for (int i = 0; i < count; i++) {
        TextRange templateSegmentRange = templateState.getSegmentRange(i);
        int segmentOffset = templateSegmentRange.getStartOffset() - templateOffset;
        documentOffset += segmentOffset;
        TextRange documentRange = TextRange.from(documentOffset, oldNameSize);
        myOldNames.add(stringInterner.intern(documentRange.substring(documentText)));
        documentOffset += oldNameSize;
        templateOffset += segmentOffset + initialNameSize;
      }
    }
    return result;
  }


  @Override
  protected boolean isIdentifier(String newName, Language language) {
    var variableNameElement = getVariable();
    return variableNameElement != null &&
           super.isIdentifier(newName, language) &&
           RenameUtil.isValidName(myProject, variableNameElement, newName);
  }


  @Override
  protected @Nullable PsiNamedElement getVariable() {
    PsiNamedElement variable = super.getVariable();
    if (variable != null) {
      return variable;
    }

    if (myElementToRename instanceof PuppetDelegatingLightNamedElement) {
      final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
      if (psiFile != null) {
        PsiElement psiLeaf = psiFile.getViewProvider().findElementAt(myRenameOffset.getStartOffset(), PuppetLanguage.INSTANCE);
        if (psiLeaf != null) {
          PsiElement namedElement = TargetElementUtil.getInstance().getNamedElement(psiLeaf, 0);
          if (namedElement instanceof PuppetDelegatingLightNamedElement) {
            return (PsiNamedElement)namedElement;
          }
        }
      }
    }

    return null;
  }

  @Override
  protected void revertState() {
    CommandProcessor.getInstance().executeCommand(myProject, () -> {
      final Editor topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(myEditor);
      ApplicationManager.getApplication().runWriteAction(() -> {
        final TemplateState state = TemplateManagerImpl.getTemplateState(topLevelEditor);
        LOG.assertTrue(state != null);
        final int segmentsCount = state.getSegmentsCount();
        final Document document = topLevelEditor.getDocument();
        for (int i = 0; i < segmentsCount; i++) {
          final TextRange segmentRange = state.getSegmentRange(i);
          document.replaceString(segmentRange.getStartOffset(), segmentRange.getEndOffset(), myOldNames.get(i));
        }
        myOldNames.clear();
      });
      if (!myProject.isDisposed() && myProject.isOpen()) {
        PsiDocumentManager.getInstance(myProject).commitDocument(topLevelEditor.getDocument());
      }
    }, getCommandName(), null);
  }

  @Override
  protected void revertStateOnFinish() {
    super.revertStateOnFinish();
    final Editor editor = InjectedLanguageEditorUtil.getTopLevelEditor(myEditor);
    if (editor == FileEditorManager.getInstance(myProject).getSelectedTextEditor() && editor instanceof EditorImpl) {
      ((EditorImpl)editor).startDumb();
    }
    revertState();
  }
}
