package com.intellij.lang.puppet.autocompletion;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.TestFrameworkUtil;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.util.ArrayUtil;
import junit.framework.TestSuite;

import java.nio.charset.StandardCharsets;

public class PuppetCompletionTest extends PuppetCompletionTestCase {
  @Override
  protected String getBasePath() {
    return "autocompletion";
  }

  public void testRuby18112() {
    doTestWithTopAndFunctions(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES);
  }

  public void testBareCapitalizedName() {
    doTest("Boolean", "Selboolean");
  }

  public void testRuby18676() {
    doTest();
  }

  public void testRuby16876() {
    doTest("greeting");
  }

  public void testSelectorValue() {
    doTest(ALL_DATA_TYPES);
  }

  public void testTypeTypeParam() {
    doTest(mergeArrays(ALL_DATA_TYPES, ArrayUtil.mergeArrays(ALL_CAPITALIZED_RESOURCE_TYPES, ALL_LOWERCASED_RESOURCE_TYPES)));
  }

  public void testRuby18650() {doTest(LOWERCASE_LIB_CLASSES);}

  public void testRuby18650text() {doTest(LOWERCASE_LIB_CLASSES);}

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testClassParamType() {
    doTest(ALL_DATA_TYPES);
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testFunctionParamType() {
    doTest(ALL_DATA_TYPES);
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testLambdaParamType() {
    doTest(ALL_DATA_TYPES);
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testResourceTypeParamType() {
    doTest(ALL_DATA_TYPES);
  }

  public void testClassTypeParameter() { doTest(mergeArrays(ALL_LIB_CLASSES, new String[]{"myclass", "Myclass"}));}

  public void testSimpleVariable() {
    doTest("myQVariable", "myQAnotherVariable", "decoy::myQamama");
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testFunctionReturnType() {doTest(mergeArrays(ALL_BUILT_IN_RESOURCE_TYPES, ALL_PUPPET_DEFINED_LIB_RESOURCE_TYPES));}

  public void testResourceFirstParam() {
    doTest(mergeArrays(ALL_BUILT_IN_RESOURCE_TYPES, ALL_PUPPET_DEFINED_LIB_RESOURCE_TYPES));
  }

  public void testInheritance() {
    doTest(mergeArrays(ALL_LIB_CLASSES, new String[]{"mybaseclass", "mychildclass", "Mybaseclass", "Mychildclass"}));
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testPuppetInFileFunction() {
    String filename = "puppetFunctionDefinition";
    myFixture.copyFileToProject(filename + ".code", "manifests/" + filename + "." + PuppetFileType.DEFAULT_EXTENSION);
    doTestWithTopAndFunctions(
      ArrayUtil
        .mergeArrays(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES, "bla::func", "::func", "func", "::bla::func", "someexternalfunction"));
  }

  public void testTypeInCaseCondition() {doTest(ALL_DATA_TYPES);}

  public void testTypeInSelectorCondition() {doTest(ALL_DATA_TYPES);}

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testProducesFirst() {doTest(ALL_BUILT_IN_AND_LIB_RESOURCE_TYPES);}

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testConsumesFirst() {doTest(ALL_BUILT_IN_AND_LIB_RESOURCE_TYPES);}

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testProducesLast() {doTest(ALL_BUILT_IN_AND_LIB_RESOURCE_TYPES);}

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testConsumesLast() {doTest(ALL_BUILT_IN_AND_LIB_RESOURCE_TYPES);}

  public void testQualifiedVariable() {
    doTest("decoy::myamama", "decoy::myAnother");
  }

  public void testScopedVariable() {
    doTest(ArrayUtil.mergeArrays(LIB_FACTS, "myamama", "myVariable", "myAnotherVariable"));
  }

  public void testTopScopeVariable() {
    doTest(ArrayUtil.mergeArrays(LIB_FACTS, "myVariable", "myAnotherVariable", "decoy::myamama"));
  }

  public void testInterpolated1() {
    doTest(ArrayUtil.mergeArrays(LIB_VARS_WITHOUT_NAMES, "variable", "variaba"));
  }

  public void testRuby18703() {
    doTest(); // fixme functions should be here i guess
  }

  public void testInterpolated2() {
    doTest(ArrayUtil.mergeArrays(LIB_VARS_WITHOUT_NAMES, "variable", "variaba"));
  }

  public void testSimpleTopLevel1() {
    doTestWithTopAndFunctions(
      ArrayUtil.mergeArrays(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES, "myres", "Myres"));
  }

  public void testSimpleTopLevel2() {
    doTestWithTopAndFunctions(
      ArrayUtil.mergeArrays(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES, "myres", "Myres"));
  }

  public void testAfterIf() {
    doTestWithTopAndFunctions(
      ArrayUtil.mergeArrays(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES, "elsif...", "else..."));
  }

  public void testAfterUnless() {
    doTestWithTopAndFunctions(ArrayUtil.mergeArrays(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES, "else..."));
  }

  public void testSimpleTopLevel3() {
    doTestWithTopAndFunctions(ArrayUtil.mergeArrays(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES, "myres", "Myres"));
  }

  public void testSimpleTopLevel4() {
    doTestWithTopAndFunctions(ArrayUtil.mergeArrays(LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES, "myres", "Myres"));
  }

  public void testSimpleTopLevel5() {
    doTest(mergeArrays(CAPITALIZED_PUPPET_DEFINED_LIB_RESOURCE_TYPES, LIVE_TEMPLATE_LOOKUP_ELEMENTS,
                       ArrayUtil.mergeArrays(LOWERCASED_PUPPET_DEFINED_LIB_RESOURCE_TYPES, "myres", "Myres")));
  }

  public void testQualifiedResource() {
    doTest("holder::resource", "holder::anotherresource");
  }

  public void testResourceWithNameParam() {
    doTestWithMetaparams("param1", "param2");
  }

  public void testResourceDefaultParam() {
    doTestWithMetaparams("param1", "param2");
  }

  public void testResourceReferenceParam() {
    doTestWithMetaparams("param1", "param2");
  }

  public void testLibClassCompletion() {
    doTest("testlib", "testlib::resource::subclass");
  }

  public void testLibPackageCompletion() {
    myFixture.testCompletion("libPackageCompletion.pp", "libPackageCompletion.txt");
  }

  public void testLibClassVariable() { doTest(LIB_VARS); }

  public void testLibResourceParams() {
    doTestWithMetaparams("param1", "param2");
  }

  public void testLibFacts() {
    doTest(LIB_FACTS);
  }

  public void testLibRubyTypeParams() {
    doTest(ArrayUtil.mergeArrays(METAPARAMETERS, "after", "ensure", "line", "ma_super_param", "match", "multiple", "name", "path")
    );
  }

  public void testResourceLikeClass1() {
    doTest("mysuperclass", "testlib", "testlib::resource::subclass");
  }

  public void testResourceLikeClassQualified() {
    doTest("testlib::resource::subclass");
  }

  public void testResourceRefClassQualified() {
    doTest("testlib::resource::subclass");
  }

  public void testResourceLikeClassParams() {
    doTest("myParam1", "myParam2");
  }

  public void testResourceLikeClassQualifiedParams() {
    doTest("innerParam1", "innerParam2");
  }

  @OnVersion({PuppetLanguage.Version.PUPPET_4})
  public void testAnonymousBlockParams() {
    doTestWithFacts("pi", "pj");
  }

  public void testLibFactsCompletionDynamic() {
    doTestCompletionVariants(getTestFileName(), LIB_FACTS);

    final TempDirTestFixture tempFixture = myFixture.getTempDirFixture();
    String testFactsFile = "/lib/templib/lib/facts.d/new.txt";
    try {
      final VirtualFile file = tempFixture.createFile(testFactsFile, "myfactNew=newnew");
      assertNotNull(file);

      doTestCompletionVariants(getTestFileName(), ArrayUtil.mergeArrays(LIB_FACTS, "myfactNew"));

      HeavyPlatformTestCase.setBinaryContent(file, "myfactNew=newnew\nmyfactNew2=new".getBytes(StandardCharsets.UTF_8));
      doTestCompletionVariants(getTestFileName(), ArrayUtil.mergeArrays(LIB_FACTS, "myfactNew", "myfactNew2"));
      HeavyPlatformTestCase.setBinaryContent(file, "myfactNew=newnew".getBytes(StandardCharsets.UTF_8));
      doTestCompletionVariants(getTestFileName(), ArrayUtil.mergeArrays(LIB_FACTS, "myfactNew"));
      VfsTestUtil.deleteFile(file);
      doTestCompletionVariants(getTestFileName(), LIB_FACTS);
    }
    catch (Exception e) {
      VfsTestUtil.deleteFile(tempFixture.getFile(testFactsFile));
    }
  }


  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetCompletionTest.class));
  }
}
