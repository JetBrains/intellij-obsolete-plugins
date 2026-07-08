package com.intellij.lang.puppet.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

public class PuppetDelegatingLightElement<T extends PuppetCompositePsiElement> extends LightElement implements PuppetCompositePsiElement {
  private final @NotNull T myOriginalDelegate;

  public PuppetDelegatingLightElement(@NotNull T delegate) {
    super(delegate.getManager(), delegate.getLanguage());
    myOriginalDelegate = delegate;
  }

  public @NotNull T getDelegate() {
    return myOriginalDelegate;
  }

  @Override
  public String toString() {
    return getDelegate().toString();
  }

  @Override
  public PsiReference[] getReferencesWithCache() {
    return getDelegate().getReferencesWithCache();
  }

  @Override
  public Object[] getReferencesCacheDependencies() {
    return getDelegate().getReferencesCacheDependencies();
  }

  @Override
  public boolean hasReferences() {
    return getDelegate().hasReferences();
  }

  @Override
  public void computeReferences(List<PsiReference> result) {
    getDelegate().computeReferences(result);
  }

  @Override
  public PsiElement getFirstChild() {
    return getDelegate().getFirstChild();
  }

  @Override
  public PsiElement getLastChild() {
    return getDelegate().getLastChild();
  }

  @Override
  public void acceptChildren(@NotNull PsiElementVisitor visitor) {
    getDelegate().acceptChildren(visitor);
  }

  @Override
  public PsiReference getReference() {
    return getDelegate().getReference();
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return getDelegate().getReferences();
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return getDelegate().findReferenceAt(offset);
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    return getDelegate().addRange(first, last);
  }

  @Override
  public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    return getDelegate().addRangeBefore(first, last, anchor);
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    return getDelegate().addRangeAfter(first, last, anchor);
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    getDelegate().deleteChildRange(first, last);
  }

  @Override
  public boolean textContains(char c) {
    return getDelegate().textContains(c);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {
    return getDelegate().processDeclarations(processor, state, lastParent, place);
  }

  @Override
  public PsiElement getContext() {
    return getDelegate().getContext();
  }

  @Override
  public PsiElement getOriginalElement() {
    return getDelegate().getOriginalElement();
  }

  @Override
  public @NotNull GlobalSearchScope getResolveScope() {
    return getDelegate().getResolveScope();
  }

  @Override
  public @NotNull SearchScope getUseScope() {
    return getDelegate().getUseScope();
  }

  @Override
  public void navigate(boolean requestFocus) {
    getDelegate().navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return getDelegate().canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return getDelegate().canNavigateToSource();
  }

  @Override
  public @NotNull Project getProject() {
    return getDelegate().getProject();
  }

  @Override
  public ItemPresentation getPresentation() {
    return getDelegate().getPresentation();
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return getDelegate().isEquivalentTo(another);
  }

  @Override
  public String getName() {
    return getDelegate().getName();
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    return getDelegate().getIcon(flags);
  }

  @Override
  public @NotNull Language getLanguage() {
    return getDelegate().getLanguage();
  }

  @Override
  public PsiManager getManager() {
    return getDelegate().getManager();
  }

  @Override
  public PsiElement getParent() {
    return getDelegate().getParent();
  }

  @Override
  public PsiElement @NotNull [] getChildren() {
    return getDelegate().getChildren();
  }

  @Override
  public PsiFile getContainingFile() {
    return getDelegate().getContainingFile();
  }

  @Override
  public TextRange getTextRange() {
    return getDelegate().getTextRange();
  }

  @Override
  public int getStartOffsetInParent() {
    return getDelegate().getStartOffsetInParent();
  }

  @Override
  public char @NotNull [] textToCharArray() {
    return getDelegate().textToCharArray();
  }

  @Override
  public boolean textMatches(@NotNull CharSequence text) {
    return getDelegate().textMatches(text);
  }

  @Override
  public boolean textMatches(@NotNull PsiElement element) {
    return getDelegate().textMatches(element);
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return getDelegate().findElementAt(offset);
  }

  @Override
  public int getTextOffset() {
    return getDelegate().getTextOffset();
  }

  @Override
  public boolean isWritable() {
    return getDelegate().isWritable();
  }

  @Override
  public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
    getDelegate().checkAdd(element);
  }

  @Override
  public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    return getDelegate().add(element);
  }

  @Override
  public PsiElement addBefore(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    return getDelegate().addBefore(element, anchor);
  }

  @Override
  public PsiElement addAfter(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    return getDelegate().addAfter(element, anchor);
  }

  @Override
  public void delete() throws IncorrectOperationException {
    getDelegate().delete();
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    getDelegate().checkDelete();
  }

  @Override
  public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
    return getDelegate().replace(newElement);
  }

  @Override
  public ASTNode getNode() {
    return getDelegate().getNode();
  }

  @Override
  public String getText() {
    return getDelegate().getText();
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    getDelegate().accept(visitor);
  }

  @Override
  public PsiElement copy() {
    return getDelegate().copy();
  }

  @Override
  public @NotNull PsiElement getNavigationElement() {
    return getDelegate().getNavigationElement();
  }

  @Override
  public PsiElement getPrevSibling() {
    return getDelegate().getPrevSibling();
  }

  @Override
  public PsiElement getNextSibling() {
    return getDelegate().getNextSibling();
  }

  @Override
  public int hashCode() {
    return getDelegate().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PuppetDelegatingLightElement<?> element = (PuppetDelegatingLightElement<?>)o;

    if (!getDelegate().equals(element.getDelegate())) return false;

    return true;
  }
}
