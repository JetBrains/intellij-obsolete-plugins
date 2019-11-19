package com.jetbrains.plugins.compass;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CompassBaseTestCase extends BasePlatformTestCase {
    @Override
    protected String getBasePath() {
        return "test/testData/" + getTestDataSubdir();
    }

    protected String getTestDataSubdir() {
        return "";
    }
}
