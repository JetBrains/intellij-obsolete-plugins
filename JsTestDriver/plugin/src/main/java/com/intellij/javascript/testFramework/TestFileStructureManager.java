package com.intellij.javascript.testFramework;

import com.google.jstestdriver.idea.assertFramework.jstd.JstdTestFileStructureBuilder;
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder;
import com.intellij.javascript.testFramework.qunit.QUnitFileStructureBuilder;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestFileStructureManager {

  private static final Logger LOG = Logger.getInstance(TestFileStructureManager.class);

  private static final Key<CachedValue<TestFileStructurePack>> TEST_FILE_STRUCTURE_REGISTRY_KEY = Key.create(
    TestFileStructurePack.class.getName()
  );

  private TestFileStructureManager() {}

  @Nullable
  public static TestFileStructurePack fetchTestFileStructurePackByJsFile(final JSFile jsFile) {
    CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(jsFile.getProject());
    return cachedValuesManager.getCachedValue(
      jsFile,
      TEST_FILE_STRUCTURE_REGISTRY_KEY,
      () -> {
        TestFileStructurePack pack = createTestFileStructurePack(jsFile);
        return CachedValueProvider.Result.create(pack, jsFile);
      },
      false
    );
  }

  @NotNull
  private static TestFileStructurePack createTestFileStructurePack(@NotNull JSFile jsFile) {
    long startTimeNano = System.nanoTime();
    List<AbstractTestFileStructureBuilder<?>> builders = getBuilders();
    List<AbstractTestFileStructure> fileStructures = new ArrayList<>();
    for (AbstractTestFileStructureBuilder<?> builder : builders) {
      AbstractTestFileStructure testFileStructure = builder.fetchCachedTestFileStructure(jsFile);
      fileStructures.add(testFileStructure);
    }
    long durationNano = System.nanoTime() - startTimeNano;
    if (durationNano > 50 * 1000000) {
      // more than 50 ms
      String message = String.format("JsTestDriver: Creating TestFileStructurePack for %s took %.2f ms",
                                     jsFile.getName(),
                                     durationNano / 1000000.0);
      LOG.info(message);
    }
    return new TestFileStructurePack(fileStructures);
  }

  @NotNull
  private static List<AbstractTestFileStructureBuilder<?>> getBuilders() {
    return Arrays.asList(
        new JasmineFileStructureBuilder(),
        new QUnitFileStructureBuilder(),
        JstdTestFileStructureBuilder.getInstance()
    );
  }
}
