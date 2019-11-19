package com.jetbrains.plugins.compass.ruby;

import com.jetbrains.plugins.compass.CompassConfig;
import org.junit.Test;

import java.io.File;

public class CompassConfigParserRMTest extends SassExtensionsBaseTest {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureByFile(getTestName(true) + ".rb");
    }

    @Test
    public void testParseAddImportCalls() {
        assertSameElements(parse().getImportPaths(), "/Users/chris/work/shared_sass");
    }

    @Test
    public void testParseAddImportCallsRelative() {
        assertSameElements(parse().getImportPaths(), "/Users/chris/shared_sass", "/Users/chris/work/shared_sass");
    }

    @Test
    public void testParseAdditionalImportPaths() {
        assertSameElements(parse().getImportPaths(), "/Users/chris/work/shared_sass_second", "/Users/chris/work/shared_sass_third");
    }

    @Test
    public void testSkipNonArrayAdditionalImportPaths() {
        assertEmpty(parse().getImportPaths());
    }

    private CompassConfig parse() {
        return new CompassConfigParserRM().parse(myFixture.getFile().getVirtualFile(), "/Users/chris/work", myFixture.getPsiManager());
    }

    @Override
    protected String getTestDataPath() {
        return new File("/Users/zolotov/dev/intellij-obsolete-plugins/compass/src/test/testData/config").getAbsolutePath();
    }
}
