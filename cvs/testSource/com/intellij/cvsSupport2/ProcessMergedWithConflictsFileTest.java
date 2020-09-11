package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.actions.merge.CvsConflictsParser;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProcessMergedWithConflictsFileTest extends TestCase {
  public void test0() throws Exception {
    doTest("", "", "", "");
  }

  public void test1() throws Exception {
    doTest(
      "#include <stdlib.h>\n" +
      "#include <stdio.h>\n" +
      "\n" +
      "int main(int argc,\n" +
      "         char **argv)\n" +
      "{\n" +
      "    init_scanner();\n" +
      "    parse();\n" +
      "    if (argc != 1)\n" +
      "    {\n" +
      "        fprintf(stderr, \"tc: No args expected.\\n\");\n" +
      "        exit(1);\n" +
      "    }\n" +
      "    if (nerr == 0)\n" +
      "        gencode();\n" +
      "    else\n" +
      "        fprintf(stderr, \"No code generated.\\n\");\n" +
      "<<<<<<< driver.c\n" +
      "    exit(nerr == 0 ? EXIT_SUCCESS : EXIT_FAILURE);\n" +
      "=======\n" +
      "    exit(!!nerr);\n" +
      ">>>>>>> 1.6\n" +
      "}",

      "#include <stdlib.h>\n" +
      "#include <stdio.h>\n" +
      "\n" +
      "int main(int argc,\n" +
      "         char **argv)\n" +
      "{\n" +
      "    init_scanner();\n" +
      "    parse();\n" +
      "    if (argc != 1)\n" +
      "    {\n" +
      "        fprintf(stderr, \"tc: No args expected.\\n\");\n" +
      "        exit(1);\n" +
      "    }\n" +
      "    if (nerr == 0)\n" +
      "        gencode();\n" +
      "    else\n" +
      "        fprintf(stderr, \"No code generated.\\n\");\n" +
      "    exit(nerr == 0 ? EXIT_SUCCESS : EXIT_FAILURE);\n" +
      "}",

      "#include <stdlib.h>\n" +
      "#include <stdio.h>\n" +
      "\n" +
      "int main(int argc,\n" +
      "         char **argv)\n" +
      "{\n" +
      "    init_scanner();\n" +
      "    parse();\n" +
      "    if (argc != 1)\n" +
      "    {\n" +
      "        fprintf(stderr, \"tc: No args expected.\\n\");\n" +
      "        exit(1);\n" +
      "    }\n" +
      "    if (nerr == 0)\n" +
      "        gencode();\n" +
      "    else\n" +
      "        fprintf(stderr, \"No code generated.\\n\");\n" +
      "}",

      "#include <stdlib.h>\n" +
      "#include <stdio.h>\n" +
      "\n" +
      "int main(int argc,\n" +
      "         char **argv)\n" +
      "{\n" +
      "    init_scanner();\n" +
      "    parse();\n" +
      "    if (argc != 1)\n" +
      "    {\n" +
      "        fprintf(stderr, \"tc: No args expected.\\n\");\n" +
      "        exit(1);\n" +
      "    }\n" +
      "    if (nerr == 0)\n" +
      "        gencode();\n" +
      "    else\n" +
      "        fprintf(stderr, \"No code generated.\\n\");\n" +
      "    exit(!!nerr);\n" +
      "}"
    );
  }

  public void test2() throws Exception {
    doTest(
      "<%-- \n" +
      "<<<<<<< test2.jsp \n" +
      "<<<<<<< test2.jsp \n" +
      "change 5.5 \n" +
      "=======\n" +
      "<<<<<<< test2.jsp \n" +
      "<<<<<<< test2.jsp \n" +
      "change 6 \n" +
      "=======\n" +
      "change 3 \n" +
      ">>>>>>> 1.6.6.1 \n" +
      "=======\n" +
      "change 2 \n" +
      ">>>>>>> 1.6.4.1 \n" +
      ">>>>>>> 1.6.6.2 \n" +
      "=======\n" +
      "change 2 \n" +
      ">>>>>>> 1.6.4.1 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "<<<<<<< test2.jsp \n" +
      "change 5.5 \n" +
      "=======\n" +
      "<<<<<<< test2.jsp \n" +
      "<<<<<<< test2.jsp \n" +
      "change 6 \n" +
      "=======\n" +
      "change 3 \n" +
      ">>>>>>> 1.6.6.1 \n" +
      "=======\n" +
      "change 2 \n" +
      ">>>>>>> 1.6.4.1 \n" +
      ">>>>>>> 1.6.6.2 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "change 2 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> "
    );
  }

  public void test3() throws Exception {
    doTest(
      "<%-- \n" +
      "<<<<<<< test2.jsp \n" +
      "change 5.5 \n" +
      "=======\n" +
      "<<<<<<< test2.jsp \n" +
      "<<<<<<< test2.jsp \n" +
      "change 6 \n" +
      "=======\n" +
      "change 3 \n" +
      ">>>>>>> 1.6.6.1 \n" +
      "=======\n" +
      "change 2 \n" +
      ">>>>>>> 1.6.4.1 \n" +
      ">>>>>>> 1.6.6.2 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "change 5.5 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "<<<<<<< test2.jsp \n" +
      "<<<<<<< test2.jsp \n" +
      "change 6 \n" +
      "=======\n" +
      "change 3 \n" +
      ">>>>>>> 1.6.6.1 \n" +
      "=======\n" +
      "change 2 \n" +
      ">>>>>>> 1.6.4.1 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> "
    );
  }

  public void test4() throws Exception {
    doTest(
      "<%-- \n" +
      "<<<<<<< test2.jsp \n" +
      "<<<<<<< test2.jsp \n" +
      "change 6 \n" +
      "=======\n" +
      "change 3 \n" +
      ">>>>>>> 1.6.6.1 \n" +
      "=======\n" +
      "change 2 \n" +
      ">>>>>>> 1.6.4.1 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "<<<<<<< test2.jsp \n" +
      "change 6 \n" +
      "=======\n" +
      "change 3 \n" +
      ">>>>>>> 1.6.6.1 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> ",

      "<%-- \n" +
      "change 2 \n" +
      "  Created by IntelliJ IDEA. \n" +
      "  User: $User$ $Id: test2.jsp,v 1.7 2005/06/30 17:32:36 lesya Exp $ \n" +
      "  $Revision: 1.7 $ \n" +
      "  Date: Jun 29, 2005 \n" +
      "  Time: 8:32:37 PM \n" +
      "  To change this template use File | Settings | File Templates. \n" +
      "--%> \n" +
      "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %> \n" +
      "<html> \n" +
      "  <head><title>Simple jsp page</title></head> \n" +
      "  <body>Place your content here</body> \n" +
      "</html> "
    );
  }

  public void test5() throws Exception {
      doTest(
        "<<<<<<< test.txt\n" +
        "two\n" +
        "    // View Object Manipulation\n" +
        "    three=======\n" +
        "one>>>>>>> 1.1",
        "two\n" +
        "    // View Object Manipulation\n" +
        "    three",
        "",
        "one"
      );
  }

  private static void doTest(final String originalContent,
                             final String leftExpected,
                             final String centerExpected,
                             final String rightExpected)
    throws IOException {
    final CvsConflictsParser parser = CvsConflictsParser.createOn(new ByteArrayInputStream(originalContent.getBytes(StandardCharsets.UTF_8)));
    assertEquals(leftExpected, parser.getLeftVersion());
    assertEquals(centerExpected, parser.getCenterVersion());
    assertEquals(rightExpected, parser.getRightVersion());
  }
}
