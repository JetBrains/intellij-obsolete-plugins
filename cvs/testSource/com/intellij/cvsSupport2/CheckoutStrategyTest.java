package com.intellij.cvsSupport2;

import com.intellij.openapi.vcs.checkout.CheckoutStrategy;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * author: lesya
 */
public class CheckoutStrategyTest extends TestCase {
  public void test() {

    doTest(new File("c:/Work/Aurora/source/com"),
           new File("Aurora/source/com"),
           new File[]{new File("c:/Work/Aurora/source/com"),
                      new File("c:/Work/Aurora/source/com/Aurora/source/com"),
                      new File("c:/Work/Aurora/source/com/com")
           });

    doTest(new File("c:/temp/test"),
           new File("Aurora/source/com"),
           new File[]{
             new File("c:/temp/test/Aurora/source/com"),
             new File("c:/temp/test"),
             new File("c:/temp/test/com")});

    doTest(new File("c:/Work/Aurora"),
           new File("Aurora/source/com"),
           new File[]{new File("c:/Work/Aurora/Aurora/source/com"),
                      new File("c:/Work/Aurora/source/com"),
                      new File("c:/Work/Aurora"),
                      new File("c:/Work/Aurora/com"),
           }
    );

    doTest(new File("c:/Work/Aurora"),
           new File("Aurora"),
           new File[]{new File("c:/Work/Aurora"),
                      new File("c:/Work/Aurora/Aurora")});

    doTest(new File("c:/Work/test"),
           new File("Aurora"),
           new File[]{new File("c:/Work/test"),
                      new File("c:/Work/test/Aurora")});

    doTest(new File("c:/Work/test"),
           new File("Aurora/source/com"),
           new File[]{new File("c:/Work/test/aurora/source/com"),
                      new File("c:/Work/test"),
                      new File("c:/Work/test/com"),
           });


    doTest(new File("c:/Work/test"),
           new File("Aurora/source/com/file.txt"),
           new File[]{new File("c:/Work/test/aurora/source/com/file.txt"),
                      new File("c:/Work/test/file.txt")
           });

    doTest(new File("c:"),
           new File("Aurora/source/com/file.txt"),
           new File[]{new File("c:/Aurora/source/com/file.txt")
           });


    doTest(new File("c:"),
           new File("Aurora/source/com"),
           new File[]{new File("c:/Aurora/source/com"),
                      new File("c:/com")
           });

    doTest(new File("c:"),
           new File("Aurora"),
           new File[]{new File("c:/Aurora")
           });


  }

  private static void doTest(File selectedLocation, File cvsPath, File[] expected) {
    CheckoutStrategy[] allStrategies = CheckoutStrategy.createAllStrategies(selectedLocation,
                                                                            cvsPath,
                                                                            cvsPath.getName().endsWith(".txt"));

    Collection<File> results = new HashSet<>();
    for (CheckoutStrategy strategy : allStrategies) {
      File result = strategy.getResult();
      if (result != null) results.add(result);
    }

    assertEquals(ContainerUtil.set(expected), results);
  }
}
