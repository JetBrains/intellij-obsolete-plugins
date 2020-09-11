package com.intellij.cvsSupport2;

import org.junit.Test;
import org.netbeans.lib.cvsclient.admin.Entries;
import org.netbeans.lib.cvsclient.admin.InvalidEntryFormatException;

import java.io.IOException;
import java.io.StringReader;

/**
 * author: lesya
 */
public class EntriesReaderTest {
    @Test
    public void test() {
        doTest("");
        doTest("\n\n\n");
        doTest("   \n   \n");
        doTest("asdf\ndfgh\nhjkl");
        doTest("/D");
        doTest("/");
    }

    private static void doTest(String string) {
        try {
            new Entries().read(new StringReader(string));
        }
        catch (InvalidEntryFormatException ignored) { }
        catch (IOException e) {
            throw new RuntimeException("Unexpected exception for '" + string + "'", e);
        }
    }
}
