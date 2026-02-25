// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.taglib;

import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GspResolveTagsFromSameNamespaceTest extends Grails14TestCase {
  public void testResolveTagsFromSameNamespace() {
    addTaglib("""
                class T1TagLib {
                  static namespace = "e"
                
                  def foo = {attr, body ->
                  }
                
                  def bar = {attr, body ->
                  }
                
                }
                """);
    configureByTaglib("""
                        class T2TagLib {
                          static namespace = "e"
                        
                          def xxx = {attr, body ->
                            <caret>
                          }
                        }
                        """);

    checkCompletion("foo", "bar");
  }
}
