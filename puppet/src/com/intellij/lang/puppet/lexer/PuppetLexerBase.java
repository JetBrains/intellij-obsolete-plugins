package com.intellij.lang.puppet.lexer;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.lexer.FlexLexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class PuppetLexerBase implements FlexLexer, PuppetTokenTypes {
  // following pattern must be the same as in Puppet.flex file
  public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-z][\\-a-zA-Z0-9_]*");

  private final NotNullLazyValue<PuppetLanguage.Version> myPuppetVersion;

  private static final TokenSet PRE_FQN_TOKENS = TokenSet.create(
    CLASS, DEFINE, FUNCTION
  );

  private final Project myProject;

  protected final Stack<Integer> stack = new Stack<>();

  protected final Deque<HeredocInfo> myHeredocQueue = new ArrayDeque<>(1);

  private boolean myIsLookLikeIncompleteHeredocOpenerLine = false;

  private final Map<String, IElementType> myKeywordsMap;

  public PuppetLexerBase() {
    this(null);
  }

  public PuppetLexerBase(Project project) {
    myProject = project;
    myKeywordsMap = PuppetLexerKeywords.getKeywordsMap(project);
    myPuppetVersion = NotNullLazyValue.atomicLazy(() -> {
      if (myProject == null) {
        return PuppetLanguage.Version.PUPPET_4;
      }
      return PuppetProjectConfiguration.getInstance(myProject).getLanguageVersion();
    });
  }

  protected static class HeredocInfo {
    final String myEndTag;
    final boolean myInterpolationAllowed;

    public HeredocInfo(String endTag, boolean isInterpolationAllowed) {
      myEndTag = endTag;
      myInterpolationAllowed = isInterpolationAllowed;
    }
  }

  /**
   * Push the actual state on top of the stack
   * and change into another state
   *
   * @param state The new state
   */
  protected void pushStateAndBegin(int state) {
    pushState();
    myYyBegin(state);
  }

  /**
   * Pushes current state to stack
   */
  protected void pushState() {
    stack.push(yystate());
  }

  /**
   * Pop the last state from the stack and change to it.
   * If the stack is empty, go to YYINITIAL
   */
  protected void popState() {
    if (!stack.empty()) {
      myYyBegin(stack.pop());
    }
    else {
      myYyBegin(_PuppetLexer.YYINITIAL);
    }
  }

  protected void myYyBegin(int state) {
    if (state == _PuppetLexer.YYINITIAL && !isSafeToInit()) {
      yybegin(_PuppetLexer.DEFAULT_NOT_INIT);
    }
    else if (state == _PuppetLexer.DEFAULT_NOT_INIT && isSafeToInit()) {
      yybegin(_PuppetLexer.YYINITIAL);
    }
    else {
      yybegin(state);
    }
  }

  private void refreshState() {
    myYyBegin(yystate());
  }

  protected void setLookLikeIncompleteHeredocOpenerLine(boolean newValue) {
    myIsLookLikeIncompleteHeredocOpenerLine = newValue;
    refreshState();
  }

  private boolean isSafeToInit() {
    return stack.empty() && myHeredocQueue.isEmpty() && !myIsLookLikeIncompleteHeredocOpenerLine;
  }

  public void clearOnReset() {
    stack.clear();
    myHeredocQueue.clear();
    myIsLookLikeIncompleteHeredocOpenerLine = false;
  }

  protected PuppetLanguage.Version getLanguageVersion() {
    return myPuppetVersion.getValue();
  }

  protected boolean isPuppet3() {
    return getLanguageVersion() == PuppetLanguage.Version.PUPPET_3;
  }

  protected IElementType proxyLiteralToken(IElementType tokenType) {
    myYyBegin(_PuppetLexer.AFTER_LITERAL);
    return tokenType;
  }

  protected IElementType getNameOrKeywordToken() {
    IElementType elementType = myKeywordsMap.get(yytext().toString());
    if (elementType == null) {
      return proxyLiteralToken(NAME);
    }

    if (PRE_FQN_TOKENS.contains(elementType)) {
      myYyBegin(_PuppetLexer.LEX_FQN);
    }
    else {
      myYyBegin(_PuppetLexer.AFTER_KEYWORD);
    }
    return elementType;
  }

  protected IElementType lexLBrack() {
    if (isPuppet3()) {
      return LBRACK;
    }
    return getTokenStart() == 0 || Character.isWhitespace(yycharat(-1)) ? LISTSTART : LBRACK;
  }

  /**
   * Method checks if current yytext is a close marker for currently processed here-doc
   *
   */
  protected boolean isCurrentHeredocCloser() {
    return yytext().toString().trim().endsWith(myHeredocQueue.peekFirst().myEndTag);
  }

  protected void pushback() {
    yypushback(yylength());
  }

  public abstract int yylength();

  public abstract void yypushback(int number);

  public abstract CharSequence yytext();

  @Override
  public abstract int getTokenStart();

  public abstract char yycharat(int pos);

  public abstract void setTokenEnd(int position);
}
