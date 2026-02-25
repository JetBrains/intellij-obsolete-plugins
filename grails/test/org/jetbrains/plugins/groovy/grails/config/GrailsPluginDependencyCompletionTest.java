// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.config;

import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GrailsPluginDependencyCompletionTest extends HddGrailsTestCase {
  public void testCompletionPluginName1() throws Exception {
    GrailsTestUtil.createBuildConfig(myFixture, ".", Map.of("grails.work.dir", getTestRootPath("/testdata/grails/pluginList")), Collections.emptyMap(), """
      grails.project.dependency.resolution = {
          plugins {
              compile ":<caret>"
          }
      }
      """);
    updateApplications();
    myFixture.testCompletionVariants("grails-app/conf/BuildConfig.groovy", "hibernate", "tomcat", "webflow");
  }

  public void testCompletionPluginName2() throws Exception {
    LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
    map.put("grails.work.dir", getTestRootPath("/testdata/grails/pluginList"));
    GrailsTestUtil.createBuildConfig(myFixture, ".", map, new LinkedHashMap<>(), """
      
      grails.project.dependency.resolution = {
          plugins {
              build ""\":<caret>""\"
          }
      }
      """);
    updateApplications();
    myFixture.testCompletionVariants("grails-app/conf/BuildConfig.groovy", "hibernate", "tomcat", "webflow");
  }

  public void testCompletionPluginName3() throws Exception {
    GrailsTestUtil.createBuildConfig(myFixture, ".",
                                     Map.of("grails.work.dir", getTestRootPath("/testdata/grails/pluginList")),
                                     Collections.emptyMap(), """
                                       grails.project.dependency.resolution = {
                                           plugins {
                                               compile 'Q:<caret>'
                                           }
                                       }
                                       """);
    updateApplications();
    myFixture.testCompletionVariants("grails-app/conf/BuildConfig.groovy", "hibernate", "tomcat", "webflow");
  }

  public void testReplace() throws Exception {
    GrailsTestUtil.createBuildConfig(myFixture, ".",
                                     Map.of("grails.work.dir", getTestRootPath("/testdata/grails/pluginList")),
                                     Collections.emptyMap(), """
                                       grails.project.dependency.resolution = {
                                           plugins {
                                               compile 'Q:<caret>hibernate:1.0'
                                           }
                                       }
                                       """);
    updateApplications();
    myFixture.configureByFile("grails-app/conf/BuildConfig.groovy");

    myFixture.completeBasic();
    myFixture.type("t\t");

    String suffix = "grails.work.dir='" + getTestRootPath("/testdata/grails/pluginList") + "'\n";
    myFixture.checkResult("""
                            grails.project.dependency.resolution = {
                                plugins {
                                    compile 'Q:tomcat:1.0'
                                }
                            }
                            
                            """ + suffix);
  }

  public void testCompletionPluginVersion() throws Exception {
    GrailsTestUtil.createBuildConfig(myFixture, ".",
                                     Map.of("grails.work.dir", getTestRootPath("/testdata/grails/pluginList")),
                                     Collections.emptyMap(), """
                                       grails.project.dependency.resolution = {
                                           plugins {
                                               compile ":hibernate:<caret>"
                                           }
                                       }
                                       """);
    updateApplications();
    myFixture.testCompletionVariants("grails-app/conf/BuildConfig.groovy", "1.1", "1.1.1", "1.1.1-SNAPSHOT");
  }

  public void testCompletionGString() throws Exception {
    GrailsTestUtil.createBuildConfig(myFixture, ".", Map.of("grails.work.dir", getTestRootPath("/testdata/grails/pluginList")), Collections.emptyMap(), """
      grails.project.dependency.resolution = {
          plugins {
              compile ":<caret>:$grailsVersion"
          }
      }
      """);
    updateApplications();
    myFixture.testCompletionVariants("grails-app/conf/BuildConfig.groovy", "hibernate", "tomcat", "webflow");
  }
}
