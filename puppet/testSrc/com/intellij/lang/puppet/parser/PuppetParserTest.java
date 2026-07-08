package com.intellij.lang.puppet.parser;

import com.intellij.lang.ParserDefinition;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.psi.PuppetParserDefinition;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ParsingTestCase;
import com.intellij.testFramework.TestDataFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public abstract class PuppetParserTest extends ParsingTestCase {

  protected String myOutputSuffix = "";

  public PuppetParserTest(@NotNull ParserDefinition parserDefinition) {
    super("parser", PuppetFileType.DEFAULT_EXTENSION, true, parserDefinition);
  }

  protected abstract @NotNull String getSuffix();

  @Override
  protected String getTestDataPath() {
    return PuppetTestUtil.getTestDataPath();
  }

  protected void levelDependentTest() {
    myOutputSuffix = getSuffix();
    doTest(true);
  }

  protected final void defaultTest() {
    myOutputSuffix = "";
    doTest(true);
  }

  @Override
  protected @NotNull String loadFile(@NotNull @NonNls @TestDataFile String name) throws IOException {
    return FileUtil.loadFile(new File(myFullDataPath, name.replace("." + myFileExt, ".code")), CharsetToolkit.UTF8, true).trim();
  }

  @Override
  protected void checkResult(@NotNull @NonNls @TestDataFile String targetDataName, @NotNull PsiFile file) throws IOException {
    super.checkResult(targetDataName + myOutputSuffix, file);
  }

  public void testRuby19710() {levelDependentTest();}

  public void testVersionDependentKeywords() {
    levelDependentTest();
  }

  public void testDeclarationVsDefault() {
    levelDependentTest();
  }

  public void testDefaultExpression() {
    levelDependentTest();
  }

  public void testParallelAssignment() {
    levelDependentTest();
  }

  public void testExpressions() {
    levelDependentTest();
  }

  public void testRuby18639() {levelDependentTest();}

  public void testElsifAfterElse() {levelDependentTest();}

  public void testRuby18493() {
    defaultTest();
  }

  public void testRuby18502() {
    levelDependentTest();
  }

  public void testCaseRecovery() {
    levelDependentTest();
  }

  public void testRuby18503() {
    defaultTest();
  }

  public void testSimpleClass() {
    defaultTest();
  }

  public void testCaseResourceLikeCondition() {
    defaultTest();
  }

  public void testSimpleResource() {
    defaultTest();
  }

  public void testSimpleClassWithBody() {
    defaultTest();
  }

  public void testResourceWithBody() {
    defaultTest();
  }

  public void testResourceOverride() {
    defaultTest();
  }

  public void testClassLikeResource() {
    levelDependentTest();
  }

  public void testResourceNoParams() {
    defaultTest();
  }

  public void testRuby18641() {
    defaultTest();
  }

  public void testRuby18642() {
    defaultTest();
  }

  public void testRuby18647() {
    defaultTest();
  }

  public void testRuby18645() {
    defaultTest();
  }

  public void testFullyQualifiedClassName() {
    defaultTest();
  }

  public void testResourceInstances() {
    defaultTest();
  }

  public void testStringsWithSigils() {
    defaultTest();
  }

  public void testCollections() {
    defaultTest();
  }

  public void testTrailingCommaInArray() {
    levelDependentTest();
  }

  public void testProducesAndConsumes() {
    levelDependentTest();
  }

  public void testAssignments() {
    levelDependentTest();
  }

  public void testSiteCompound() {
    levelDependentTest();
  }

  public void testApplicationDefinition() {
    levelDependentTest();
  }

  public void testExpressionStatements() {
    levelDependentTest();
  }

  public void testAnonymousBlocks() {
    levelDependentTest();
  }

  public void testSemicolons() {
    levelDependentTest();
  }

  public void testUnlessWithElse() {
    levelDependentTest();
  }

  public void testMatchWithString() {
    levelDependentTest();
  }

  public void testInfixFunctionCalls() {
    levelDependentTest();
  }

  public void testInterpolation() {
    levelDependentTest();
  }

  public void testHeredocUnclosed() {
    levelDependentTest();
  }

  public void testheredocEmpty() {
    levelDependentTest();
  }

  public void testHeredocExpressions() {
    levelDependentTest();
  }

  public void testAssignmentOfRegexp() {
    levelDependentTest();
  }

  public void testDifferentRHSWithMatchOperator() {
    levelDependentTest();
  }

  public void testHashParamSet() {
    levelDependentTest();
  }

  public void testUnarySplatOperator() {
    levelDependentTest();
  }

  public void testFunctionDefinition() {
    levelDependentTest();
  }

  public void testRelationship() {
    defaultTest();
  }

  public void testRuby18036() {
    levelDependentTest();
  }

  public void testRuby17410() {
    levelDependentTest();
  }

  public void testRuby18646() {
    levelDependentTest();
  }

  public void testRuby17191() {
    levelDependentTest();
  }

  public void testSplatInCase() {levelDependentTest();}

  public void testRuby17102() {
    levelDependentTest(); // it's level-dependent because there are ASSIGNMENT and EXPRESSION_ASSIGNMENT psi elements  in different versions
  }

  public void testRuby17495() {
    levelDependentTest();
  }

  public void testRuby18129() {
    levelDependentTest();
  }

  public void testRuby18643() {
    levelDependentTest();
  }

  public void testMultilineCommentInString() {
    levelDependentTest();
  }

  public static class Puppet3ParserTest extends PuppetParserTest {

    public Puppet3ParserTest() {
      super(new PuppetParserDefinition());
    }

    @Override
    protected void setUp() throws Exception {
      super.setUp();

      project.registerService(PuppetProjectConfiguration.class);
      PuppetProjectConfiguration.getInstance(project).setLanguageVersion(PuppetLanguage.Version.PUPPET_3);
    }

    @Override
    protected @NotNull String getSuffix() {
      return ".pp3";
    }
  }

  public static class Puppet4ParserTest extends PuppetParserTest {

    public Puppet4ParserTest() {
      super(new PuppetParserDefinition());
    }

    @Override
    protected void setUp() throws Exception {
      super.setUp();

      project.registerService(PuppetProjectConfiguration.class);
      PuppetProjectConfiguration.getInstance(project).setLanguageVersion(PuppetLanguage.Version.PUPPET_4);
    }

    @Override
    protected void tearDown() throws Exception {
      try {
        PuppetProjectConfiguration.getInstance(project).setLanguageVersion(PuppetLanguage.Version.PUPPET_3);
      }
      catch (Throwable e) {
        addSuppressedException(e);
      }
      finally {
        super.tearDown();
      }
    }

    @Override
    protected @NotNull String getSuffix() {
      return ".pp4";
    }

    public void testTypes() {
      levelDependentTest();
    }

    public void testTypedArguments() {
      levelDependentTest();
    }

    public void testPerExpressionDefault() {
      levelDependentTest();
    }
  }
}
