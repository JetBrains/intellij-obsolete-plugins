package com.jetbrains.plugins.compass.ruby;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class CompassCompletionTest extends SassExtensionsBaseTest {
    @Test
    public void testCompassFunctions() {
        myFixture.testCompletionVariants(getTestFileName(), "linear_end_position", "linear_gradient", "linear_svg", "linear_svg_gradient");
    }

    @Test
    public void testCompassNestedFunctions() {
        myFixture.testCompletionVariants(getTestFileName(), "generated_image_url", "image_url", "image_url");
    }

    @Test
    public void testCompassSassFunctions() {
        myFixture.testCompletionVariants(getTestFileName(), "get-column-fluid-grid", "get-column-gradient");
    }

    @Test
    public void testCustomCompassFunctions() {
        myFixture.testCompletionVariants(getTestFileName(), "-o", "-owg");
    }


    @NotNull
    @Override
    protected String getTestDataRelativePath() {
        return "completion";
    }
}
