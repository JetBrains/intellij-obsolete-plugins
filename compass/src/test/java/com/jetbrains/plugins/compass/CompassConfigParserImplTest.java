package com.jetbrains.plugins.compass;

public class CompassConfigParserImplTest extends CompassBaseTestCase {

  private static final CompassConfigParserImpl PARSER = new CompassConfigParserImpl();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFixture.configureByFile(getTestName(true) + ".rb");
  }

  public void testParseAddImportCalls() {
    assertSameElements(parse().getImportPaths(), "/Users/chris/work/shared_sass");
  }

  public void testParseAddImportCallsRelative() {
    assertSameElements(parse().getImportPaths(), "/Users/chris/shared_sass", "/Users/chris/work/shared_sass");
  }

  public void testParseAdditionalImportPaths() {
    assertSameElements(parse().getImportPaths(), "/Users/chris/work/shared_sass_second", "/Users/chris/work/shared_sass_third");
  }

  public void testSkipNonArrayAdditionalImportPaths() {
    assertEmpty(parse().getImportPaths());
  }

  private CompassConfig parse() {
    return PARSER.parse(myFixture.getFile().getVirtualFile(), "/Users/chris/work", myFixture.getPsiManager());
  }

  @Override
  protected String getTestDataSubdir() {
    return "config";
  }
}
