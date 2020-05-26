foo();
<error descr="JSLint: Undeclared 'bar'.">bar</error>();
<error descr="JSLint: Unexpected 'eval'.">eval</error>("it");
<error descr="JSLint: This function needs a 'use strict' pragma.">function</error> qqq() {
  var <error descr="JSLint: Unused 'f'.">f</error>;
}
