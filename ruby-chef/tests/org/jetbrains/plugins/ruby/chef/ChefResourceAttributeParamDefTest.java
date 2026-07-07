package org.jetbrains.plugins.ruby.chef;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.PathUtil;
import org.jetbrains.plugins.ruby.chef.sourceRoot.CookbooksRootType;
import org.jetbrains.plugins.ruby.rails.RubyLightProjectDescriptorBase;
import org.jetbrains.plugins.ruby.ruby.testCases.DefaultRubyLightFixtureTestCase;

import java.util.List;

public class ChefResourceAttributeParamDefTest extends DefaultRubyLightFixtureTestCase {
  private static final String MY_COOKBOOK = "mycookbook";
  private static final String MY_COOKBOOK_RECIPES = MY_COOKBOOK + "/recipes";

  @Override
  protected @Nullable RubyLightProjectDescriptorBase createProjectDescriptor() {
    return new RubyLightProjectDescriptorBase() {
      {
        addGemToAttach(ChefUtil.CHEF_GEM);
      }
    };
  }

  @Override
  protected String getTestDataPath() {
    return PathUtil.getDataPath(ChefResourceAttributeParamDefTest.class);
  }

  public void testAResourceCompletionOutOfCookbook() {
    assertDoesntContain(myFixture.getCompletionVariants(MY_COOKBOOK_RECIPES + "/resourceCompletion.rb"),
                        "dpkg_package");
  }

  public void testCronDay() {
    List<String> numbers = new SmartList<>("\"*\"");
    for (int i = 0; i < 31; i++) {
      numbers.add("\"" + (i + 1) + "\"");
    }
    addCookbook();
    assertContainsElements(myFixture.getCompletionVariants(MY_COOKBOOK_RECIPES + "/cronDay.rb"), numbers);
  }

  public void testResourceNotifies() {
    addCookbook();
    assertContainsElements(myFixture.getCompletionVariants(MY_COOKBOOK_RECIPES + "/resourceNotifies.rb"),
                           ":create",
                           ":reload",
                           ":restart",
                           ":run");
  }

  public void testFileManageSymlinkSource() {
    addCookbook();
    assertContainsElements(myFixture.getCompletionVariants(MY_COOKBOOK_RECIPES + "/fileManageSymlinkSource.rb"),
                           "nil");
  }

  public void testCronHourNoWarning() {
    addCookbook();
    myFixture.testHighlighting(true, false, true, MY_COOKBOOK_RECIPES + "/cronHourNoWarning.rb");
  }

  public void testFileManageSymlinkSourceNoWarning() {
    addCookbook();
    myFixture.testHighlighting(true, false, true, MY_COOKBOOK_RECIPES + "/fileManageSymlinkSourceNoWarning.rb");
  }

  public void testResourceCompletion() {
    addCookbook();
    assertContainsElements(myFixture.getCompletionVariants(MY_COOKBOOK_RECIPES + "/resourceCompletion.rb"),
                           "dpkg_package");
  }

  public void testRemoteFileAttrCompletion() {
    addCookbook();
    assertContainsElements(myFixture.getCompletionVariants(MY_COOKBOOK_RECIPES + "/remoteFileAttrCompletion.rb"),
                           "checksum", // seems there are two different checksums in target file resources/file.rb
                           "checksum",
                           "content",
                           "defined_at");
  }

  private void addCookbook() {
    final Module module = getModule();
    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        final VirtualFile file = event.getFile();
        if (file.getName().equals(MY_COOKBOOK)) {
          PsiTestUtil.addSourceRoot(module, file, CookbooksRootType.COOKBOOKS);
          Disposer.register(myFixture.getTestRootDisposable(), () -> {
            if (file.isValid()) {
              PsiTestUtil.removeSourceRoot(module, file);
            }
          });
        }
      }

      @Override
      public void fileDeleted(final @NotNull VirtualFileEvent event) {
        final VirtualFile file = event.getFile();
        if (file.getName().equals(MY_COOKBOOK)) {
          PsiTestUtil.removeSourceRoot(module, file);
        }
      }
    }, myFixture.getTestRootDisposable());
  }
}