package com.jetbrains.plugins.compass;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelper;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelperImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.vfs.VfsUtilCore.pathToUrl;

public abstract class CompassTestCase extends CompassBaseTestCase {
    protected CompassSettings myCompassSettings;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        SassRubyIntegrationHelper.setTestingHelper(new SassRubyIntegrationHelperImpl(), getTestRootDisposable());
        final String compassPath = getTestDataPath() + "/test/testData/gems/compass-0.13.alpha.2";
        myCompassSettings = CompassSettings.getInstance(myFixture.getModule());
        myCompassSettings.setCompassSupportEnabled(true);
        myCompassSettings.setCompassExecutableFilePath(compassPath + "/bin/compass");
        SassRubyIntegrationHelper.getInstance().getCompassExtension().startActivity(myFixture.getModule());
        UIUtil.dispatchAllInvocationEvents();
    }

    @Override
    public void tearDown() throws Exception {
        try {
            SassRubyIntegrationHelper.getInstance().getCompassExtension().stopActivity(myFixture.getModule());
            CompassSettings compassSettings = CompassSettings.getInstance(myFixture.getModule());
            compassSettings.setCompassExecutableFilePath(null);
            compassSettings.setCompassConfigPath(null);
            compassSettings.setCompassSupportEnabled(false);
            compassSettings.setImportPaths(new ArrayList<>());
            CompassUtil.removeCompassLibraryIfNeeded(myFixture.getModule());
        } catch (Throwable e) {
            addSuppressedException(e);
        } finally {
            UIUtil.dispatchAllInvocationEvents();
            super.tearDown();
        }
    }

    @NotNull
    protected VirtualFile addCompassIncludePath() throws IOException {
        final File extraImportPath = FileUtil.createTempDirectory("sassTest", "");
        List<String> paths = new ArrayList<>(myCompassSettings.getImportPaths());
        paths.add(extraImportPath.getAbsolutePath());
        myCompassSettings.setImportPaths(paths);
        CompassUtil.updateCompassLibraries(myCompassSettings);
        UIUtil.dispatchAllInvocationEvents();
        final VirtualFile result = VfsUtil.findFileByIoFile(extraImportPath, true);
        assertNotNull(result);
        return result;
    }

    @Nullable
    protected VirtualFile getCompassRoot() {
        final VirtualFile compassExe = VirtualFileManager.getInstance().refreshAndFindFileByUrl(pathToUrl(myCompassSettings.getCompassExecutableFilePath()));
        if (compassExe != null) {
            final VirtualFile compassExeCanonicalFile = compassExe.getCanonicalFile();
            if (compassExeCanonicalFile != null) {
                return compassExeCanonicalFile.findFileByRelativePath("../../");
            }
        }
        throw new RuntimeException("Can't find compass root");
    }

    @NotNull
    protected VirtualFile getCompassStylesheetsRoot() {
        final VirtualFile compassExe = VirtualFileManager.getInstance().refreshAndFindFileByUrl(pathToUrl(myCompassSettings.getCompassExecutableFilePath()));
        if (compassExe != null) {
            final VirtualFile compassExeCanonicalFile = compassExe.getCanonicalFile();
            if (compassExeCanonicalFile != null) {
                final VirtualFile compassStylesheetRoot = compassExeCanonicalFile.findFileByRelativePath(CompassUtil.COMPASS_STYLESHEETS_RELATIVE_PATH);
                if (compassStylesheetRoot != null) {
                    return compassStylesheetRoot;
                }
            }
        }
        throw new RuntimeException("Can't find compass stylesheet root");
    }
}
