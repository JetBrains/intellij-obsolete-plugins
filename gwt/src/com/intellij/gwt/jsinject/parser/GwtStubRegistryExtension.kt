package com.intellij.gwt.jsinject.parser

import com.intellij.gwt.jsinject.parser.GwtParserDefinition.GWT_FILE
import com.intellij.lang.javascript.stubs.serializers.JSFileStubSerializer
import com.intellij.psi.stubs.StubRegistry
import com.intellij.psi.stubs.StubRegistryExtension

class GwtStubRegistryExtension : StubRegistryExtension {
  override fun register(registry: StubRegistry) {
    GWT_FILE.let {
      registry.registerStubSerializer(it, JSFileStubSerializer(it))
    }
  }
}