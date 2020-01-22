package com.jetbrains.plugins.compass;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public abstract class CompassBaseTestCase extends BasePlatformTestCase {
    @Override
    protected String getTestDataPath() {
        return getBasePath() + getTestDataSubdir();
    }

    protected String getBasePath() {
        return "src/test/testData/";
    }

    protected String getTestDataSubdir() {
        return "";
    }
}
