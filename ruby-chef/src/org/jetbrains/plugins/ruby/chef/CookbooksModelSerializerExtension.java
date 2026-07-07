package org.jetbrains.plugins.ruby.chef;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootDummyPropertiesSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer;
import org.jetbrains.plugins.ruby.chef.sourceRoot.CookbooksRootType;

import java.util.Collections;
import java.util.List;

public final class CookbooksModelSerializerExtension extends JpsModelSerializerExtension {
  @Override
  public @NotNull List<? extends JpsModuleSourceRootPropertiesSerializer<?>> getModuleSourceRootPropertiesSerializers() {
    return Collections.singletonList(new JpsModuleSourceRootDummyPropertiesSerializer(CookbooksRootType.COOKBOOKS, "cookbooks-root"));
  }
}
