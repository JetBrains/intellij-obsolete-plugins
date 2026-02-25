// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gradle.tooling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a test assumption by Gradle version or version range.
 * <p>
 * Variants:
 * <ul>
 * <li> "7.0" -- start test with 7.0 Gradle version</li>
 * <li> "7.0+" -- start tests with Gradle versions since 7.0 (included)</li>
 * <li> "<7.0" -- start tests with Gradle versions until 7.0 (excluded)</li>
 * <li> "<=7.0" -- start tests with Gradle versions until 7.0 (included)</li>
 * <li> "!7.0" -- start tests with Gradle versions except 7.0</li>
 * <li> "4.0 <=> 7.0" -- start tests with Gradle versions in range [4.0, 7.0]</li>
 * <li> {"4.0+", <7.0} -- start tests with Gradle versions in range [4.0, 7.0)</li>
 * <li> {"4.0+", !7.0} -- start tests with Gradle versions since 4.0 (included) except 7.0</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TargetVersions {
    String[] value();

    boolean checkBaseVersions() default true;
}
