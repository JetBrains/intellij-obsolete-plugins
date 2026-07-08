module Puppet::Parser::Functions
  newfunction(:alert) do |args|
    desc <<EOT
<p>Log a message on the server at level alert.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:assert_type) do |args|
    desc <<EOT
<p>Returns the given value if it is of the given
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_data.html">data type</a>, or
otherwise either raises an error or executes an optional two-parameter
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a>.</p>

<p>The function takes two mandatory arguments, in this order:</p>

<ol>
  <li>The expected data type.</li>
  <li>A value to compare against the expected data type.</li>
</ol>

<p><strong>Example</strong>: Using <code>assert_type</code></p>

<pre><code class="language-puppet">$raw_username = 'Amy Berry'

# Assert that $raw_username is a non-empty string and assign it to $valid_username.
$valid_username = assert_type(String[1], $raw_username)

# $valid_username contains "Amy Berry".
# If $raw_username was an empty string or a different data type, the Puppet run would
# fail with an "Expected type does not match actual" error.
</code></pre>

<p>You can use an optional lambda to provide enhanced feedback. The lambda takes two
mandatory parameters, in this order:</p>

<ol>
  <li>The expected data type as described in the function’s first argument.</li>
  <li>The actual data type of the value.</li>
</ol>

<p><strong>Example</strong>: Using <code>assert_type</code> with a warning and default value</p>

<pre><code class="language-puppet">$raw_username = 'Amy Berry'

# Assert that $raw_username is a non-empty string and assign it to $valid_username.
# If it isn't, output a warning describing the problem and use a default value.
$valid_username = assert_type(String[1], $raw_username) |$expected, $actual| {
  warning( "The username should be '${expected}', not '${actual}'. Using 'anonymous'." )
  'anonymous'
}

# $valid_username contains "Amy Berry".
# If $raw_username was an empty string, the Puppet run would set $valid_username to
# "anonymous" and output a warning: "The username should be 'String[1, default]', not
# 'String[0, 0]'. Using 'anonymous'."
</code></pre>

<p>For more information about data types, see the
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_data.html">documentation</a>.</p>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:binary_file) do |args|
    desc <<EOT
<p>Loads a binary file from a module or file system and returns its contents as a Binary.</p>

<p>The argument to this function should be a <code>&lt;MODULE NAME&gt;/&lt;FILE&gt;</code>
reference, which will load <code>&lt;FILE&gt;</code> from a module’s <code>files</code>
directory. (For example, the reference <code>mysql/mysqltuner.pl</code> will load the
file <code>&lt;MODULES DIRECTORY&gt;/mysql/files/mysqltuner.pl</code>.)</p>

<p>This function also accepts an absolute file path that allows reading
binary file content from anywhere on disk.</p>

<p>An error is raised if the given file does not exists.</p>

<p>To search for the existence of files, use the <code>find_file()</code> function.</p>

<ul>
  <li>
    <p>since 4.8.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:break) do |args|
    desc <<EOT
<p>Breaks the innermost iteration as if it encountered an end of input.
This function does not return to the caller.</p>

<p>The signal produced to stop the iteration bubbles up through
the call stack until either terminating the innermost iteration or
raising an error if the end of the call stack is reached.</p>

<p>The break() function does not accept an argument.</p>

<p><strong>Example:</strong> Using <code>break</code></p>

<pre><code class="language-puppet">$data = [1,2,3]
notice $data.map |$x| { if $x == 3 { break() } $x*10 }
</code></pre>

<p>Would notice the value <code>[10, 20]</code></p>

<p><strong>Example:</strong> Using a nested <code>break</code></p>

<pre><code class="language-puppet">function break_if_even($x) {
  if $x % 2 == 0 { break() }
}
$data = [1,2,3]
notice $data.map |$x| { break_if_even($x); $x*10 }
</code></pre>
<p>Would notice the value <code>[10]</code></p>

<ul>
  <li>Also see functions <code>next</code> and <code>return</code></li>
  <li>
    <p>Since 4.8.0</p>
  </li>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:contain) do |args|
    desc <<EOT
<p>Contain one or more classes inside the current class. If any of
these classes are undeclared, they will be declared as if called with the
<code>include</code> function. Accepts a class name, an array of class names, or a
comma-separated list of class names.</p>

<p>A contained class will not be applied before the containing class is
begun, and will be finished before the containing class is finished.</p>

<p>You must use the class’s full name;
relative names are not allowed. In addition to names in string form,
you may also directly use Class and Resource Type values that are produced by
evaluating resource and relationship expressions.</p>

<p>The function returns an array of references to the classes that were contained thus
allowing the function call to <code>contain</code> to directly continue.</p>

<ul>
  <li>Since 4.0.0 support for Class and Resource Type values, absolute names</li>
  <li>
    <p>Since 4.7.0 an Array[Type[Class[n]]] is returned with all the contained classes</p>
  </li>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:create_resources) do |args|
    desc <<EOT
<p>Converts a hash into a set of resources and adds them to the catalog.</p>

<p>This function takes two mandatory arguments: a resource type, and a hash describing
a set of resources. The hash should be in the form <code>{title =&gt; {parameters} }</code>:</p>

<pre><code># A hash of user resources:
$myusers = {
  'nick' =&gt; { uid    =&gt; '1330',
              gid    =&gt; allstaff,
              groups =&gt; ['developers', 'operations', 'release'], },
  'dan'  =&gt; { uid    =&gt; '1308',
              gid    =&gt; allstaff,
              groups =&gt; ['developers', 'prosvc', 'release'], },
}

create_resources(user, $myusers)
</code></pre>

<p>A third, optional parameter may be given, also as a hash:</p>

<pre><code>$defaults = {
  'ensure'   =&gt; present,
  'provider' =&gt; 'ldap',
}

create_resources(user, $myusers, $defaults)
</code></pre>

<p>The values given on the third argument are added to the parameters of each resource
present in the set given on the second argument. If a parameter is present on both
the second and third arguments, the one on the second argument takes precedence.</p>

<p>This function can be used to create defined resources and classes, as well
as native resources.</p>

<p>Virtual and Exported resources may be created by prefixing the type name
with @ or @@ respectively.  For example, the $myusers hash may be exported
in the following manner:</p>

<pre><code>create_resources("@@user", $myusers)
</code></pre>

<p>The $myusers may be declared as virtual resources using:</p>

<pre><code>create_resources("@user", $myusers)
</code></pre>

<p>Note that <code>create_resources</code> will filter out parameter values that are <code>undef</code> so that normal
data binding and puppet default value expressions are considered (in that order) for the
final value of a parameter (just as when setting a parameter to <code>undef</code> in a puppet language
resource declaration).</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:crit) do |args|
    desc <<EOT
<p>Log a message on the server at level crit.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:debug) do |args|
    desc <<EOT
<p>Log a message on the server at level debug.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:defined) do |args|
    desc <<EOT
<p>Determines whether a given class or resource type is defined and returns a Boolean
value. You can also use <code>defined</code> to determine whether a specific resource is defined,
or whether a variable has a value (including <code>undef</code>, as opposed to the variable never
being declared or assigned).</p>

<p>This function takes at least one string argument, which can be a class name, type name,
resource reference, or variable reference of the form <code>'$name'</code>.</p>

<p>The <code>defined</code> function checks both native and defined types, including types
provided by modules. Types and classes are matched by their names. The function matches
resource declarations by using resource references.</p>

<p><strong>Examples</strong>: Different types of <code>defined</code> function matches</p>

<pre><code class="language-puppet"># Matching resource types
defined("file")
defined("customtype")

# Matching defines and classes
defined("foo")
defined("foo::bar")

# Matching variables
defined('$name')

# Matching declared resources
defined(File['/tmp/file'])
</code></pre>

<p>Puppet depends on the configuration’s evaluation order when checking whether a resource
is declared.</p>

<p><strong>Example</strong>: Importance of evaluation order when using <code>defined</code></p>

<pre><code class="language-puppet"># Assign values to $is_defined_before and $is_defined_after using identical `defined`
# functions.

$is_defined_before = defined(File['/tmp/file'])

file { "/tmp/file":
  ensure =&gt; present,
}

$is_defined_after = defined(File['/tmp/file'])

# $is_defined_before returns false, but $is_defined_after returns true.
</code></pre>

<p>This order requirement only refers to evaluation order. The order of resources in the
configuration graph (e.g. with <code>before</code> or <code>require</code>) does not affect the <code>defined</code>
function’s behavior.</p>

<blockquote>
  <p><strong>Warning:</strong> Avoid relying on the result of the <code>defined</code> function in modules, as you
might not be able to guarantee the evaluation order well enough to produce consistent
results. This can cause other code that relies on the function’s result to behave
inconsistently or fail.</p>
</blockquote>

<p>If you pass more than one argument to <code>defined</code>, the function returns <code>true</code> if <em>any</em>
of the arguments are defined. You can also match resources by type, allowing you to
match conditions of different levels of specificity, such as whether a specific resource
is of a specific data type.</p>

<p><strong>Example</strong>: Matching multiple resources and resources by different types with <code>defined</code></p>

<pre><code class="language-puppet">file { "/tmp/file1":
  ensure =&gt; file,
}

$tmp_file = file { "/tmp/file2":
  ensure =&gt; file,
}

# Each of these statements return `true` ...
defined(File['/tmp/file1'])
defined(File['/tmp/file1'],File['/tmp/file2'])
defined(File['/tmp/file1'],File['/tmp/file2'],File['/tmp/file3'])
# ... but this returns `false`.
defined(File['/tmp/file3'])

# Each of these statements returns `true` ...
defined(Type[Resource['file','/tmp/file2']])
defined(Resource['file','/tmp/file2'])
defined(File['/tmp/file2'])
defined('$tmp_file')
# ... but each of these returns `false`.
defined(Type[Resource['exec','/tmp/file2']])
defined(Resource['exec','/tmp/file2'])
defined(File['/tmp/file3'])
defined('$tmp_file2')
</code></pre>

<ul>
  <li>Since 2.7.0</li>
  <li>Since 3.6.0 variable reference and future parser types</li>
  <li>Since 3.8.1 type specific requests with future parser</li>
  <li>
    <p>Since 4.0.0 includes all future parser features</p>
  </li>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:dig) do |args|
    desc <<EOT
<p>Returns a value for a sequence of given keys/indexes into a structure.
This function is used to “dig into” a complex data structure by
using a sequence of keys / indexes to access a value from which
the next key/index is accessed recursively.</p>

<p>The first encountered <code>undef</code> value or key stops the “dig” and <code>undef</code> is returned.</p>

<p>An error is raised if an attempt is made to “dig” into
something other than an <code>undef</code> (which immediately returns <code>undef</code>), an <code>Array</code> or a <code>Hash</code>.</p>

<p><strong>Example:</strong> Using <code>dig</code></p>

<pre><code class="language-puppet">$data = {a =&gt; { b =&gt; [{x =&gt; 10, y =&gt; 20}, {x =&gt; 100, y =&gt; 200}]}}
notice $data.dig(a, b, 1, x)
</code></pre>

<p>Would notice the value 100.</p>

<ul>
  <li>
    <p>Since 4.5.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:digest) do |args|
    desc <<EOT
<p>Returns a hash value from a provided string using the digest_algorithm setting from the Puppet config file.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:each) do |args|
    desc <<EOT
<p>Runs a <a href="http://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a>
repeatedly using each value in a data structure, then returns the values unchanged.</p>

<p>This function takes two mandatory arguments, in this order:</p>

<ol>
  <li>An array or hash the function will iterate over.</li>
  <li>A lambda, which the function calls for each element in the first argument. It can
request one or two parameters.</li>
</ol>

<p><strong>Example</strong>: Using the <code>each</code> function</p>

<p><code>$data.each |$parameter| { &lt;PUPPET CODE BLOCK&gt; }</code></p>

<p>or</p>

<p><code>each($data) |$parameter| { &lt;PUPPET CODE BLOCK&gt; }</code></p>

<p>When the first argument (<code>$data</code> in the above example) is an array, Puppet passes each
value in turn to the lambda, then returns the original values.</p>

<p><strong>Example</strong>: Using the <code>each</code> function with an array and a one-parameter lambda</p>

<pre><code class="language-puppet"># For the array $data, run a lambda that creates a resource for each item.
$data = ["routers", "servers", "workstations"]
$data.each |$item| {
 notify { $item:
   message =&gt; $item
 }
}
# Puppet creates one resource for each of the three items in $data. Each resource is
# named after the item's value and uses the item's value in a parameter.
</code></pre>

<p>When the first argument is a hash, Puppet passes each key and value pair to the lambda
as an array in the form <code>[key, value]</code> and returns the original hash.</p>

<p><strong>Example</strong>: Using the <code>each</code> function with a hash and a one-parameter lambda</p>

<pre><code class="language-puppet"># For the hash $data, run a lambda using each item as a key-value array that creates a
# resource for each item.
$data = {"rtr" =&gt; "Router", "svr" =&gt; "Server", "wks" =&gt; "Workstation"}
$data.each |$items| {
 notify { $items[0]:
   message =&gt; $items[1]
 }
}
# Puppet creates one resource for each of the three items in $data, each named after the
# item's key and containing a parameter using the item's value.
</code></pre>

<p>When the first argument is an array and the lambda has two parameters, Puppet passes the
array’s indexes (enumerated from 0) in the first parameter and its values in the second
parameter.</p>

<p><strong>Example</strong>: Using the <code>each</code> function with an array and a two-parameter lambda</p>

<pre><code class="language-puppet"># For the array $data, run a lambda using each item's index and value that creates a
# resource for each item.
$data = ["routers", "servers", "workstations"]
$data.each |$index, $value| {
 notify { $value:
   message =&gt; $index
 }
}
# Puppet creates one resource for each of the three items in $data, each named after the
# item's value and containing a parameter using the item's index.
</code></pre>

<p>When the first argument is a hash, Puppet passes its keys to the first parameter and its
values to the second parameter.</p>

<p><strong>Example</strong>: Using the <code>each</code> function with a hash and a two-parameter lambda</p>

<pre><code class="language-puppet"># For the hash $data, run a lambda using each item's key and value to create a resource
# for each item.
$data = {"rtr" =&gt; "Router", "svr" =&gt; "Server", "wks" =&gt; "Workstation"}
$data.each |$key, $value| {
 notify { $key:
   message =&gt; $value
 }
}
# Puppet creates one resource for each of the three items in $data, each named after the
# item's key and containing a parameter using the item's value.
</code></pre>

<p>For an example that demonstrates how to create multiple <code>file</code> resources using <code>each</code>,
see the Puppet
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_iteration.html">iteration</a>
documentation.</p>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:emerg) do |args|
    desc <<EOT
<p>Log a message on the server at level emerg.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:epp) do |args|
    desc <<EOT
<p>Evaluates an Embedded Puppet (EPP) template file and returns the rendered text
result as a String.</p>

<p><code>epp('&lt;MODULE NAME&gt;/&lt;TEMPLATE FILE&gt;', &lt;PARAMETER HASH&gt;)</code></p>

<p>The first argument to this function should be a <code>&lt;MODULE NAME&gt;/&lt;TEMPLATE FILE&gt;</code>
reference, which loads <code>&lt;TEMPLATE FILE&gt;</code> from <code>&lt;MODULE NAME&gt;</code>’s <code>templates</code>
directory. In most cases, the last argument is optional; if used, it should be a
<a href="/puppet/latest/reference/lang_data_hash.html">hash</a> that contains parameters to
pass to the template.</p>

<ul>
  <li>See the <a href="/puppet/latest/reference/lang_template.html">template</a> documentation
for general template usage information.</li>
  <li>See the <a href="/puppet/latest/reference/lang_template_epp.html">EPP syntax</a>
documentation for examples of EPP.</li>
</ul>

<p>For example, to call the apache module’s <code>templates/vhost/_docroot.epp</code>
template and pass the <code>docroot</code> and <code>virtual_docroot</code> parameters, call the <code>epp</code>
function like this:</p>

<p><code>epp('apache/vhost/_docroot.epp', { 'docroot' =&gt; '/var/www/html',
'virtual_docroot' =&gt; '/var/www/example' })</code></p>

<p>Puppet produces a syntax error if you pass more parameters than are declared in
the template’s parameter tag. When passing parameters to a template that
contains a parameter tag, use the same names as the tag’s declared parameters.</p>

<p>Parameters are required only if they are declared in the called template’s
parameter tag without default values. Puppet produces an error if the <code>epp</code>
function fails to pass any required parameter.</p>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:err) do |args|
    desc <<EOT
<p>Log a message on the server at level err.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:fail) do |args|
    desc <<EOT
<p>Fail with a parse error.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:file) do |args|
    desc <<EOT
<p>Loads a file from a module and returns its contents as a string.</p>

<p>The argument to this function should be a <code>&lt;MODULE NAME&gt;/&lt;FILE&gt;</code>
reference, which will load <code>&lt;FILE&gt;</code> from a module’s <code>files</code>
directory. (For example, the reference <code>mysql/mysqltuner.pl</code> will load the
file <code>&lt;MODULES DIRECTORY&gt;/mysql/files/mysqltuner.pl</code>.)</p>

<p>This function can also accept:</p>

<ul>
  <li>An absolute path, which can load a file from anywhere on disk.</li>
  <li>
    <p>Multiple arguments, which will return the contents of the <strong>first</strong> file
found, skipping any files that don’t exist.</p>
  </li>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:filter) do |args|
    desc <<EOT
<p>Applies a <a href="http://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a>
to every value in a data structure and returns an array or hash containing any elements
for which the lambda evaluates to <code>true</code>.</p>

<p>This function takes two mandatory arguments, in this order:</p>

<ol>
  <li>An array or hash the function will iterate over.</li>
  <li>A lambda, which the function calls for each element in the first argument. It can
request one or two parameters.</li>
</ol>

<p><strong>Example</strong>: Using the <code>filter</code> function</p>

<p><code>$filtered_data = $data.filter |$parameter| { &lt;PUPPET CODE BLOCK&gt; }</code></p>

<p>or</p>

<p><code>$filtered_data = filter($data) |$parameter| { &lt;PUPPET CODE BLOCK&gt; }</code></p>

<p>When the first argument (<code>$data</code> in the above example) is an array, Puppet passes each
value in turn to the lambda and returns an array containing the results.</p>

<p><strong>Example</strong>: Using the <code>filter</code> function with an array and a one-parameter lambda</p>

<pre><code class="language-puppet"># For the array $data, return an array containing the values that end with "berry"
$data = ["orange", "blueberry", "raspberry"]
$filtered_data = $data.filter |$items| { $items =~ /berry$/ }
# $filtered_data = [blueberry, raspberry]
</code></pre>

<p>When the first argument is a hash, Puppet passes each key and value pair to the lambda
as an array in the form <code>[key, value]</code> and returns a hash containing the results.</p>

<p><strong>Example</strong>: Using the <code>filter</code> function with a hash and a one-parameter lambda</p>

<pre><code class="language-puppet"># For the hash $data, return a hash containing all values of keys that end with "berry"
$data = { "orange" =&gt; 0, "blueberry" =&gt; 1, "raspberry" =&gt; 2 }
$filtered_data = $data.filter |$items| { $items[0] =~ /berry$/ }
# $filtered_data = {blueberry =&gt; 1, raspberry =&gt; 2}

When the first argument is an array and the lambda has two parameters, Puppet passes the
array's indexes (enumerated from 0) in the first parameter and its values in the second
parameter.

**Example**: Using the `filter` function with an array and a two-parameter lambda

~~~ puppet
# For the array $data, return an array of all keys that both end with "berry" and have
# an even-numbered index
$data = ["orange", "blueberry", "raspberry"]
$filtered_data = $data.filter |$indexes, $values| { $indexes % 2 == 0 and $values =~ /berry$/ }
# $filtered_data = [raspberry]
</code></pre>

<p>When the first argument is a hash, Puppet passes its keys to the first parameter and its
values to the second parameter.</p>

<p><strong>Example</strong>: Using the <code>filter</code> function with a hash and a two-parameter lambda</p>

<pre><code class="language-puppet"># For the hash $data, return a hash of all keys that both end with "berry" and have
# values less than or equal to 1
$data = { "orange" =&gt; 0, "blueberry" =&gt; 1, "raspberry" =&gt; 2 }
$filtered_data = $data.filter |$keys, $values| { $keys =~ /berry$/ and $values &lt;= 1 }
# $filtered_data = {blueberry =&gt; 1}
</code></pre>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:find_file) do |args|
    desc <<EOT
<p>Finds an existing file from a module and returns its path.</p>

<p>The argument to this function should be a String as a <code>&lt;MODULE NAME&gt;/&lt;FILE&gt;</code>
reference, which will search for <code>&lt;FILE&gt;</code> relative to a module’s <code>files</code>
directory. (For example, the reference <code>mysql/mysqltuner.pl</code> will search for the
file <code>&lt;MODULES DIRECTORY&gt;/mysql/files/mysqltuner.pl</code>.)</p>

<p>This function can also accept:</p>

<ul>
  <li>An absolute String path, which will check for the existence of a file from anywhere on disk.</li>
  <li>Multiple String arguments, which will return the path of the <strong>first</strong> file
found, skipping non existing files.</li>
  <li>An array of string paths, which will return the path of the <strong>first</strong> file
found from the given paths in the array, skipping non existing files.</li>
</ul>

<p>The function returns <code>undef</code> if none of the given paths were found</p>

<ul>
  <li>
    <p>since 4.8.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:fqdn_rand) do |args|
    desc <<EOT
<p>Usage: <code>fqdn_rand(MAX, [SEED])</code>. MAX is required and must be a positive
integer; SEED is optional and may be any number or string.</p>

<p>Generates a random Integer number greater than or equal to 0 and less than MAX,
combining the <code>$fqdn</code> fact and the value of SEED for repeatable randomness.
(That is, each node will get a different random number from this function, but
a given node’s result will be the same every time unless its hostname changes.)</p>

<p>This function is usually used for spacing out runs of resource-intensive cron
tasks that run on many nodes, which could cause a thundering herd or degrade
other services if they all fire at once. Adding a SEED can be useful when you
have more than one such task and need several unrelated random numbers per
node. (For example, <code>fqdn_rand(30)</code>, <code>fqdn_rand(30, 'expensive job 1')</code>, and
<code>fqdn_rand(30, 'expensive job 2')</code> will produce totally different numbers.)</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:generate) do |args|
    desc <<EOT
<p>Calls an external command on the Puppet master and returns
the results of the command.  Any arguments are passed to the external command as
arguments.  If the generator does not exit with return code of 0,
the generator is considered to have failed and a parse error is
thrown.  Generators can only have file separators, alphanumerics, dashes,
and periods in them.  This function will attempt to protect you from
malicious generator calls (e.g., those with ‘..’ in them), but it can
never be entirely safe.  No subshell is used to execute
generators, so all shell metacharacters are passed directly to
the generator.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:hiera) do |args|
    desc <<EOT
<p>Performs a standard priority lookup of the hierarchy and returns the most specific value
for a given key. The returned value can be any type of data.</p>

<p>The function takes up to three arguments, in this order:</p>

<ol>
  <li>A string key that Hiera searches for in the hierarchy. <strong>Required</strong>.</li>
  <li>An optional default value to return if Hiera doesn’t find anything matching the key.
    <ul>
      <li>If this argument isn’t provided and this function results in a lookup failure, Puppet
 fails with a compilation error.</li>
    </ul>
  </li>
  <li>The optional name of an arbitrary
<a href="https://docs.puppetlabs.com/hiera/latest/hierarchy.html">hierarchy level</a> to insert at the
top of the hierarchy. This lets you temporarily modify the hierarchy for a single lookup.
    <ul>
      <li>If Hiera doesn’t find a matching key in the overriding hierarchy level, it continues
 searching the rest of the hierarchy.</li>
    </ul>
  </li>
</ol>

<p>The <code>hiera</code> function does <strong>not</strong> find all matches throughout a hierarchy, instead
returining the first specific value starting at the top of the hierarchy. To search
throughout a hierarchy, use the <code>hiera_array</code> or <code>hiera_hash</code> functions.</p>

<p><strong>Example</strong>: Using <code>hiera</code></p>

<pre><code class="language-yaml"># Assuming hiera.yaml
# :hierarchy:
#   - web01.example.com
#   - common

# Assuming web01.example.com.yaml:
# users:
#   - "Amy Barry"
#   - "Carrie Douglas"

# Assuming common.yaml:
users:
  admins:
    - "Edith Franklin"
    - "Ginny Hamilton"
  regular:
    - "Iris Jackson"
    - "Kelly Lambert"
</code></pre>

<pre><code class="language-puppet"># Assuming we are not web01.example.com:

$users = hiera('users', undef)

# $users contains {admins  =&gt; ["Edith Franklin", "Ginny Hamilton"],
#                  regular =&gt; ["Iris Jackson", "Kelly Lambert"]}
</code></pre>

<p>You can optionally generate the default value with a
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a> that
takes one parameter.</p>

<p><strong>Example</strong>: Using <code>hiera</code> with a lambda</p>

<pre><code class="language-puppet"># Assuming the same Hiera data as the previous example:

$users = hiera('users') | $key | { "Key '${key}' not found" }

# $users contains {admins  =&gt; ["Edith Franklin", "Ginny Hamilton"],
#                  regular =&gt; ["Iris Jackson", "Kelly Lambert"]}
# If hiera couldn't match its key, it would return the lambda result,
# "Key 'users' not found".
</code></pre>

<p>The returned value’s data type depends on the types of the results. In the example
above, Hiera matches the ‘users’ key and returns it as a hash.</p>

<p>The <code>hiera</code> function is deprecated in favor of using <code>lookup</code> and will be removed in 6.0.0.
See  https://docs.puppet.com/puppet/4.10/reference/deprecated_language.html.
Replace the calls as follows:</p>

<table>
  <thead>
    <tr>
      <th>from</th>
      <th>to</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>hiera($key)</td>
      <td>lookup($key)</td>
    </tr>
    <tr>
      <td>hiera($key, $default)</td>
      <td>lookup($key, { ‘default_value’ =&gt; $default })</td>
    </tr>
    <tr>
      <td>hiera($key, $default, $level)</td>
      <td>override level not supported</td>
    </tr>
  </tbody>
</table>

<p>Note that calls using the ‘override level’ option are not directly supported by ‘lookup’ and the produced
result must be post processed to get exactly the same result, for example using simple hash/array <code>+</code> or
with calls to stdlib’s <code>deep_merge</code> function depending on kind of hiera call and setting of merge in hiera.yaml.</p>

<p>See
<a href="https://docs.puppetlabs.com/hiera/latest/puppet.html#hiera-lookup-functions">the documentation</a>
for more information about Hiera lookup functions.</p>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:hiera_array) do |args|
    desc <<EOT
<p>Finds all matches of a key throughout the hierarchy and returns them as a single flattened
array of unique values. If any of the matched values are arrays, they’re flattened and
included in the results. This is called an
<a href="https://docs.puppetlabs.com/hiera/latest/lookup_types.html#array-merge">array merge lookup</a>.</p>

<p>The <code>hiera_array</code> function takes up to three arguments, in this order:</p>

<ol>
  <li>A string key that Hiera searches for in the hierarchy. <strong>Required</strong>.</li>
  <li>An optional default value to return if Hiera doesn’t find anything matching the key.
    <ul>
      <li>If this argument isn’t provided and this function results in a lookup failure, Puppet
 fails with a compilation error.</li>
    </ul>
  </li>
  <li>The optional name of an arbitrary
<a href="https://docs.puppetlabs.com/hiera/latest/hierarchy.html">hierarchy level</a> to insert at the
top of the hierarchy. This lets you temporarily modify the hierarchy for a single lookup.
    <ul>
      <li>If Hiera doesn’t find a matching key in the overriding hierarchy level, it continues
 searching the rest of the hierarchy.</li>
    </ul>
  </li>
</ol>

<p><strong>Example</strong>: Using <code>hiera_array</code></p>

<pre><code class="language-yaml"># Assuming hiera.yaml
# :hierarchy:
#   - web01.example.com
#   - common

# Assuming common.yaml:
# users:
#   - 'cdouglas = regular'
#   - 'efranklin = regular'

# Assuming web01.example.com.yaml:
# users: 'abarry = admin'
</code></pre>

<pre><code class="language-puppet">$allusers = hiera_array('users', undef)

# $allusers contains ["cdouglas = regular", "efranklin = regular", "abarry = admin"].
</code></pre>

<p>You can optionally generate the default value with a
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a> that
takes one parameter.</p>

<p><strong>Example</strong>: Using <code>hiera_array</code> with a lambda</p>

<pre><code class="language-puppet"># Assuming the same Hiera data as the previous example:

$allusers = hiera_array('users') | $key | { "Key '${key}' not found" }

# $allusers contains ["cdouglas = regular", "efranklin = regular", "abarry = admin"].
# If hiera_array couldn't match its key, it would return the lambda result,
# "Key 'users' not found".
</code></pre>

<p><code>hiera_array</code> expects that all values returned will be strings or arrays. If any matched
value is a hash, Puppet raises a type mismatch error.</p>

<p><code>hiera_array</code> is deprecated in favor of using <code>lookup</code> and will be removed in 6.0.0.
See  https://docs.puppet.com/puppet/4.10/reference/deprecated_language.html.
Replace the calls as follows:</p>

<table>
  <thead>
    <tr>
      <th>from</th>
      <th>to</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>hiera_array($key)</td>
      <td>lookup($key, { ‘merge’ =&gt; ‘unique’ })</td>
    </tr>
    <tr>
      <td>hiera_array($key, $default)</td>
      <td>lookup($key, { ‘default_value’ =&gt; $default, ‘merge’ =&gt; ‘unique’ })</td>
    </tr>
    <tr>
      <td>hiera_array($key, $default, $level)</td>
      <td>override level not supported</td>
    </tr>
  </tbody>
</table>

<p>Note that calls using the ‘override level’ option are not directly supported by ‘lookup’ and the produced
result must be post processed to get exactly the same result, for example using simple hash/array <code>+</code> or
with calls to stdlib’s <code>deep_merge</code> function depending on kind of hiera call and setting of merge in hiera.yaml.</p>

<p>See
<a href="https://docs.puppetlabs.com/hiera/latest/puppet.html#hiera-lookup-functions">the documentation</a>
for more information about Hiera lookup functions.</p>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:hiera_hash) do |args|
    desc <<EOT
<p>Finds all matches of a key throughout the hierarchy and returns them in a merged hash.
If any of the matched hashes share keys, the final hash uses the value from the
highest priority match. This is called a
<a href="https://docs.puppetlabs.com/hiera/latest/lookup_types.html#hash-merge">hash merge lookup</a>.</p>

<p>The merge strategy is determined by Hiera’s
<a href="https://docs.puppetlabs.com/hiera/latest/configuring.html#mergebehavior"><code>:merge_behavior</code></a>
setting.</p>

<p>The <code>hiera_hash</code> function takes up to three arguments, in this order:</p>

<ol>
  <li>A string key that Hiera searches for in the hierarchy. <strong>Required</strong>.</li>
  <li>An optional default value to return if Hiera doesn’t find anything matching the key.
    <ul>
      <li>If this argument isn’t provided and this function results in a lookup failure, Puppet
 fails with a compilation error.</li>
    </ul>
  </li>
  <li>The optional name of an arbitrary
<a href="https://docs.puppetlabs.com/hiera/latest/hierarchy.html">hierarchy level</a> to insert at the
top of the hierarchy. This lets you temporarily modify the hierarchy for a single lookup.
    <ul>
      <li>If Hiera doesn’t find a matching key in the overriding hierarchy level, it continues
 searching the rest of the hierarchy.</li>
    </ul>
  </li>
</ol>

<p><strong>Example</strong>: Using <code>hiera_hash</code></p>

<pre><code class="language-yaml"># Assuming hiera.yaml
# :hierarchy:
#   - web01.example.com
#   - common

# Assuming common.yaml:
# users:
#   regular:
#     'cdouglas': 'Carrie Douglas'

# Assuming web01.example.com.yaml:
# users:
#   administrators:
#     'aberry': 'Amy Berry'
</code></pre>

<pre><code class="language-puppet"># Assuming we are not web01.example.com:

$allusers = hiera_hash('users', undef)

# $allusers contains {regular =&gt; {"cdouglas" =&gt; "Carrie Douglas"},
#                     administrators =&gt; {"aberry" =&gt; "Amy Berry"}}
</code></pre>

<p>You can optionally generate the default value with a
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a> that
takes one parameter.</p>

<p><strong>Example</strong>: Using <code>hiera_hash</code> with a lambda</p>

<pre><code class="language-puppet"># Assuming the same Hiera data as the previous example:

$allusers = hiera_hash('users') | $key | { "Key '${key}' not found" }

# $allusers contains {regular =&gt; {"cdouglas" =&gt; "Carrie Douglas"},
#                     administrators =&gt; {"aberry" =&gt; "Amy Berry"}}
# If hiera_hash couldn't match its key, it would return the lambda result,
# "Key 'users' not found".
</code></pre>

<p><code>hiera_hash</code> expects that all values returned will be hashes. If any of the values
found in the data sources are strings or arrays, Puppet raises a type mismatch error.</p>

<p><code>hiera_hash</code> is deprecated in favor of using <code>lookup</code> and will be removed in 6.0.0.
See  https://docs.puppet.com/puppet/4.10/reference/deprecated_language.html.
Replace the calls as follows:</p>

<table>
  <thead>
    <tr>
      <th>from</th>
      <th>to</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>hiera_hash($key)</td>
      <td>lookup($key, { ‘merge’ =&gt; ‘hash’ })</td>
    </tr>
    <tr>
      <td>hiera_hash($key, $default)</td>
      <td>lookup($key, { ‘default_value’ =&gt; $default, ‘merge’ =&gt; ‘hash’ })</td>
    </tr>
    <tr>
      <td>hiera_hash($key, $default, $level)</td>
      <td>override level not supported</td>
    </tr>
  </tbody>
</table>

<p>Note that calls using the ‘override level’ option are not directly supported by ‘lookup’ and the produced
result must be post processed to get exactly the same result, for example using simple hash/array <code>+</code> or
with calls to stdlib’s <code>deep_merge</code> function depending on kind of hiera call and setting of merge in hiera.yaml.</p>

<p>See
<a href="https://docs.puppetlabs.com/hiera/latest/puppet.html#hiera-lookup-functions">the documentation</a>
for more information about Hiera lookup functions.</p>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:hiera_include) do |args|
    desc <<EOT
<p>Assigns classes to a node using an
<a href="https://docs.puppetlabs.com/hiera/latest/lookup_types.html#array-merge">array merge lookup</a>
that retrieves the value for a user-specified key from Hiera’s data.</p>

<p>The <code>hiera_include</code> function requires:</p>

<ul>
  <li>A string key name to use for classes.</li>
  <li>A call to this function (i.e. <code>hiera_include('classes')</code>) in your environment’s
<code>sites.pp</code> manifest, outside of any node definitions and below any top-scope variables
that Hiera uses in lookups.</li>
  <li><code>classes</code> keys in the appropriate Hiera data sources, with an array for each
<code>classes</code> key and each value of the array containing the name of a class.</li>
</ul>

<p>The function takes up to three arguments, in this order:</p>

<ol>
  <li>A string key that Hiera searches for in the hierarchy. <strong>Required</strong>.</li>
  <li>An optional default value to return if Hiera doesn’t find anything matching the key.
    <ul>
      <li>If this argument isn’t provided and this function results in a lookup failure, Puppet
 fails with a compilation error.</li>
    </ul>
  </li>
  <li>The optional name of an arbitrary
<a href="https://docs.puppetlabs.com/hiera/latest/hierarchy.html">hierarchy level</a> to insert at the
top of the hierarchy. This lets you temporarily modify the hierarchy for a single lookup.
    <ul>
      <li>If Hiera doesn’t find a matching key in the overriding hierarchy level, it continues
 searching the rest of the hierarchy.</li>
    </ul>
  </li>
</ol>

<p>The function uses an
<a href="https://docs.puppetlabs.com/hiera/latest/lookup_types.html#array-merge">array merge lookup</a>
to retrieve the <code>classes</code> array, so every node gets every class from the hierarchy.</p>

<p><strong>Example</strong>: Using <code>hiera_include</code></p>

<pre><code class="language-yaml"># Assuming hiera.yaml
# :hierarchy:
#   - web01.example.com
#   - common

# Assuming web01.example.com.yaml:
# classes:
#   - apache::mod::php

# Assuming common.yaml:
# classes:
#   - apache
</code></pre>

<pre><code class="language-puppet"># In site.pp, outside of any node definitions and below any top-scope variables:
hiera_include('classes', undef)

# Puppet assigns the apache and apache::mod::php classes to the web01.example.com node.
</code></pre>

<p>You can optionally generate the default value with a
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a> that
takes one parameter.</p>

<p><strong>Example</strong>: Using <code>hiera_include</code> with a lambda</p>

<pre><code class="language-puppet"># Assuming the same Hiera data as the previous example:

# In site.pp, outside of any node definitions and below any top-scope variables:
hiera_include('classes') | $key | {"Key '${key}' not found" }

# Puppet assigns the apache and apache::mod::php classes to the web01.example.com node.
# If hiera_include couldn't match its key, it would return the lambda result,
# "Key 'classes' not found".
</code></pre>

<p><code>hiera_include</code> is deprecated in favor of using a combination of <code>include</code>and <code>lookup</code> and will be
removed in 6.0.0. See  https://docs.puppet.com/puppet/4.10/reference/deprecated_language.html.
Replace the calls as follows:</p>

<table>
  <thead>
    <tr>
      <th>from</th>
      <th>to</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>hiera_include($key)</td>
      <td>include(lookup($key, { ‘merge’ =&gt; ‘unique’ }))</td>
    </tr>
    <tr>
      <td>hiera_include($key, $default)</td>
      <td>include(lookup($key, { ‘default_value’ =&gt; $default, ‘merge’ =&gt; ‘unique’ }))</td>
    </tr>
    <tr>
      <td>hiera_include($key, $default, $level)</td>
      <td>override level not supported</td>
    </tr>
  </tbody>
</table>

<p>Note that calls using the ‘override level’ option are not directly supported by ‘lookup’ and the produced
result must be post processed to get exactly the same result, for example using simple hash/array <code>+</code> or
with calls to stdlib’s <code>deep_merge</code> function depending on kind of hiera call and setting of merge in hiera.yaml.</p>

<p>See <a href="http://links.puppetlabs.com/hierainclude">the documentation</a> for more information
and a more detailed example of how <code>hiera_include</code> uses array merge lookups to classify
nodes.</p>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: statement</p>
  </li>
</ul>
EOT
  end

  newfunction(:include) do |args|
    desc <<EOT
<p>Declares one or more classes, causing the resources in them to be
evaluated and added to the catalog. Accepts a class name, an array of class
names, or a comma-separated list of class names.</p>

<p>The <code>include</code> function can be used multiple times on the same class and will
only declare a given class once. If a class declared with <code>include</code> has any
parameters, Puppet will automatically look up values for them in Hiera, using
<code>&lt;class name&gt;::&lt;parameter name&gt;</code> as the lookup key.</p>

<p>Contrast this behavior with resource-like class declarations
(<code>class {'name': parameter =&gt; 'value',}</code>), which must be used in only one place
per class and can directly set parameters. You should avoid using both <code>include</code>
and resource-like declarations with the same class.</p>

<p>The <code>include</code> function does not cause classes to be contained in the class
where they are declared. For that, see the <code>contain</code> function. It also
does not create a dependency relationship between the declared class and the
surrounding class; for that, see the <code>require</code> function.</p>

<p>You must use the class’s full name;
relative names are not allowed. In addition to names in string form,
you may also directly use Class and Resource Type values that are produced by
the future parser’s resource and relationship expressions.</p>

<ul>
  <li>Since &lt; 3.0.0</li>
  <li>Since 4.0.0 support for class and resource type values, absolute names</li>
  <li>
    <p>Since 4.7.0 returns an Array[Type[Class]] of all included classes</p>
  </li>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:info) do |args|
    desc <<EOT
<p>Log a message on the server at level info.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:inline_epp) do |args|
    desc <<EOT
<p>Evaluates an Embedded Puppet (EPP) template string and returns the rendered
text result as a String.</p>

<p><code>inline_epp('&lt;EPP TEMPLATE STRING&gt;', &lt;PARAMETER HASH&gt;)</code></p>

<p>The first argument to this function should be a string containing an EPP
template. In most cases, the last argument is optional; if used, it should be a
<a href="/puppet/latest/reference/lang_data_hash.html">hash</a> that contains parameters to
pass to the template.</p>

<ul>
  <li>See the <a href="/puppet/latest/reference/lang_template.html">template</a> documentation
for general template usage information.</li>
  <li>See the <a href="/puppet/latest/reference/lang_template_epp.html">EPP syntax</a>
documentation for examples of EPP.</li>
</ul>

<p>For example, to evaluate an inline EPP template and pass it the <code>docroot</code> and
<code>virtual_docroot</code> parameters, call the <code>inline_epp</code> function like this:</p>

<p><code>inline_epp('docroot: &lt;%= $docroot %&gt; Virtual docroot: &lt;%= $virtual_docroot %&gt;',
{ 'docroot' =&gt; '/var/www/html', 'virtual_docroot' =&gt; '/var/www/example' })</code></p>

<p>Puppet produces a syntax error if you pass more parameters than are declared in
the template’s parameter tag. When passing parameters to a template that
contains a parameter tag, use the same names as the tag’s declared parameters.</p>

<p>Parameters are required only if they are declared in the called template’s
parameter tag without default values. Puppet produces an error if the
<code>inline_epp</code> function fails to pass any required parameter.</p>

<p>An inline EPP template should be written as a single-quoted string or
<a href="/puppet/latest/reference/lang_data_string.html#heredocs">heredoc</a>.
A double-quoted string is subject to expression interpolation before the string
is parsed as an EPP template.</p>

<p>For example, to evaluate an inline EPP template using a heredoc, call the
<code>inline_epp</code> function like this:</p>

<pre><code class="language-puppet"># Outputs 'Hello given argument planet!'
inline_epp(@(END), { x =&gt; 'given argument' })
&lt;%- | $x, $y = planet | -%&gt;
Hello &lt;%= $x %&gt; &lt;%= $y %&gt;!
END
</code></pre>

<ul>
  <li>Since 3.5</li>
  <li>
    <p>Requires <a href="/puppet/3.8/reference/experiments_future.html">future parser</a> in Puppet 3.5 to 3.8</p>
  </li>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:inline_template) do |args|
    desc <<EOT
<p>Evaluate a template string and return its value.  See
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_template.html">the templating docs</a> for
more information. Note that if multiple template strings are specified, their
output is all concatenated and returned as the output of the function.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:lest) do |args|
    desc <<EOT
<p>Call a <a href="https://docs.puppet.com/puppet/latest/reference/lang_lambdas.html">lambda</a>
(which should accept no arguments) if the argument given to the function is <code>undef</code>.
Returns the result of calling the lambda if the argument is <code>undef</code>, otherwise the
given argument.</p>

<p>The <code>lest</code> function is useful in a chain of <code>then</code> calls, or in general
as a guard against <code>undef</code> values. The function can be used to call <code>fail</code>, or to
return a default value.</p>

<p>These two expressions are equivalent:</p>

<pre><code class="language-puppet">if $x == undef { do_things() }
lest($x) || { do_things() }
</code></pre>

<p><strong>Example:</strong> Using the <code>lest</code> function</p>

<pre><code class="language-puppet">$data = {a =&gt; [ b, c ] }
notice $data.dig(a, b, c)
 .then |$x| { $x * 2 }
 .lest || { fail("no value for $data[a][b][c]" }
</code></pre>

<p>Would fail the operation because $data[a][b][c] results in <code>undef</code>
(there is no <code>b</code> key in <code>a</code>).</p>

<p>In contrast - this example:</p>

<pre><code class="language-puppet">$data = {a =&gt; { b =&gt; { c =&gt; 10 } } }
notice $data.dig(a, b, c)
 .then |$x| { $x * 2 }
 .lest || { fail("no value for $data[a][b][c]" }
</code></pre>

<p>Would notice the value <code>20</code></p>

<ul>
  <li>
    <p>Since 4.5.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:lookup) do |args|
    desc <<EOT
<p>Uses the Puppet lookup system to retrieve a value for a given key. By default,
this returns the first value found (and fails compilation if no values are
available), but you can configure it to merge multiple values into one, fail
gracefully, and more.</p>

<p>When looking up a key, Puppet will search up to three tiers of data, in the
following order:</p>

<ol>
  <li>Hiera.</li>
  <li>The current environment’s data provider.</li>
  <li>The indicated module’s data provider, if the key is of the form
<code>&lt;MODULE NAME&gt;::&lt;SOMETHING&gt;</code>.</li>
</ol>

<h4 id="arguments">Arguments</h4>

<p>You must provide the name of a key to look up, and can optionally provide other
arguments. You can combine these arguments in the following ways:</p>

<ul>
  <li><code>lookup( &lt;NAME&gt;, [&lt;VALUE TYPE&gt;], [&lt;MERGE BEHAVIOR&gt;], [&lt;DEFAULT VALUE&gt;] )</code></li>
  <li><code>lookup( [&lt;NAME&gt;], &lt;OPTIONS HASH&gt; )</code></li>
  <li><code>lookup( as above ) |$key| { # lambda returns a default value }</code></li>
</ul>

<p>Arguments in <code>[square brackets]</code> are optional.</p>

<p>The arguments accepted by <code>lookup</code> are as follows:</p>

<ol>
  <li><code>&lt;NAME&gt;</code> (string or array) — The name of the key to look up.
    <ul>
      <li>This can also be an array of keys. If Puppet doesn’t find anything for the
 first key, it will try again with the subsequent ones, only resorting to a
 default value if none of them succeed.</li>
    </ul>
  </li>
  <li><code>&lt;VALUE TYPE&gt;</code> (data type) — A
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_data_type.html">data type</a>
that must match the retrieved value; if not, the lookup (and catalog
compilation) will fail. Defaults to <code>Data</code> (accepts any normal value).</li>
  <li><code>&lt;MERGE BEHAVIOR&gt;</code> (string or hash; see <strong>“Merge Behaviors”</strong> below) —
Whether (and how) to combine multiple values. If present, this overrides any
merge behavior specified in the data sources. Defaults to no value; Puppet will
use merge behavior from the data sources if present, and will otherwise do a
first-found lookup.</li>
  <li><code>&lt;DEFAULT VALUE&gt;</code> (any normal value) — If present, <code>lookup</code> returns this
when it can’t find a normal value. Default values are never merged with found
values. Like a normal value, the default must match the value type. Defaults to
no value; if Puppet can’t find a normal value, the lookup (and compilation) will
fail.</li>
  <li><code>&lt;OPTIONS HASH&gt;</code> (hash) — Alternate way to set the arguments above, plus
some less-common extra options. If you pass an options hash, you can’t combine
it with any regular arguments (except <code>&lt;NAME&gt;</code>). An options hash can have the
following keys:
    <ul>
      <li><code>'name'</code> — Same as <code>&lt;NAME&gt;</code> (argument 1). You can pass this as an
 argument or in the hash, but not both.</li>
      <li><code>'value_type'</code> — Same as <code>&lt;VALUE TYPE&gt;</code> (argument 2).</li>
      <li><code>'merge'</code> — Same as <code>&lt;MERGE BEHAVIOR&gt;</code> (argument 3).</li>
      <li><code>'default_value'</code> — Same as <code>&lt;DEFAULT VALUE&gt;</code> (argument 4).</li>
      <li><code>'default_values_hash'</code> (hash) — A hash of lookup keys and default
 values. If Puppet can’t find a normal value, it will check this hash for the
 requested key before giving up. You can combine this with <code>default_value</code> or
 a lambda, which will be used if the key isn’t present in this hash. Defaults
 to an empty hash.</li>
      <li><code>'override'</code> (hash) — A hash of lookup keys and override values. Puppet
 will check for the requested key in the overrides hash <em>first;</em> if found, it
 returns that value as the <em>final</em> value, ignoring merge behavior. Defaults
 to an empty hash.</li>
    </ul>
  </li>
</ol>

<p>Finally, <code>lookup</code> can take a lambda, which must accept a single parameter.
This is yet another way to set a default value for the lookup; if no results are
found, Puppet will pass the requested key to the lambda and use its result as
the default value.</p>

<h4 id="merge-behaviors">Merge Behaviors</h4>

<p>Puppet lookup uses a hierarchy of data sources, and a given key might have
values in multiple sources. By default, Puppet returns the first value it finds,
but it can also continue searching and merge all the values together.</p>

<blockquote>
  <p><strong>Note:</strong> Data sources can use the special <code>lookup_options</code> metadata key to
request a specific merge behavior for a key. The <code>lookup</code> function will use that
requested behavior unless you explicitly specify one.</p>
</blockquote>

<p>The valid merge behaviors are:</p>

<ul>
  <li><code>'first'</code> — Returns the first value found, with no merging. Puppet lookup’s
default behavior.</li>
  <li><code>'unique'</code> (called “array merge” in classic Hiera) — Combines any number of
arrays and scalar values to return a merged, flattened array with all duplicate
values removed. The lookup will fail if any hash values are found.</li>
  <li><code>'hash'</code> — Combines the keys and values of any number of hashes to return a
merged hash. If the same key exists in multiple source hashes, Puppet will use
the value from the highest-priority data source; it won’t recursively merge the
values.</li>
  <li><code>'deep'</code> — Combines the keys and values of any number of hashes to return a
merged hash. If the same key exists in multiple source hashes, Puppet will
recursively merge hash or array values (with duplicate values removed from
arrays). For conflicting scalar values, the highest-priority value will win.</li>
  <li><code>{'strategy' =&gt; 'first|unique|hash'}</code> — Same as the string versions of these
merge behaviors.</li>
  <li><code>{'strategy' =&gt; 'deep', &lt;DEEP OPTION&gt; =&gt; &lt;VALUE&gt;, ...}</code> — Same as <code>'deep'</code>,
but can adjust the merge with additional options. The available options are:
    <ul>
      <li><code>'knockout_prefix'</code> (string or undef) — A string prefix to indicate a
  value should be <em>removed</em> from the final result. Defaults to <code>undef</code>, which
  disables this feature.</li>
      <li><code>'sort_merged_arrays'</code> (boolean) — Whether to sort all arrays that are
  merged together. Defaults to <code>false</code>.</li>
      <li><code>'merge_hash_arrays'</code> (boolean) — Whether to merge hashes within arrays.
  Defaults to <code>false</code>.</li>
    </ul>
  </li>
</ul>

<h4 id="examples">Examples</h4>

<p>Look up a key and return the first value found:</p>

<pre><code>lookup('ntp::service_name')
</code></pre>

<p>Do a unique merge lookup of class names, then add all of those classes to the
catalog (like <code>hiera_include</code>):</p>

<pre><code>lookup('classes', Array[String], 'unique').include
</code></pre>

<p>Do a deep hash merge lookup of user data, but let higher priority sources
remove values by prefixing them with <code>--</code>:</p>

<pre><code>lookup( { 'name'  =&gt; 'users',
          'merge' =&gt; {
            'strategy'        =&gt; 'deep',
            'knockout_prefix' =&gt; '--',
          },
})
</code></pre>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:map) do |args|
    desc <<EOT
<p>Applies a <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a>
to every value in a data structure and returns an array containing the results.</p>

<p>This function takes two mandatory arguments, in this order:</p>

<ol>
  <li>An array or hash the function will iterate over.</li>
  <li>A lambda, which the function calls for each element in the first argument. It can
request one or two parameters.</li>
</ol>

<p><strong>Example</strong>: Using the <code>map</code> function</p>

<p><code>$transformed_data = $data.map |$parameter| { &lt;PUPPET CODE BLOCK&gt; }</code></p>

<p>or</p>

<p><code>$transformed_data = map($data) |$parameter| { &lt;PUPPET CODE BLOCK&gt; }</code></p>

<p>When the first argument (<code>$data</code> in the above example) is an array, Puppet passes each
value in turn to the lambda.</p>

<p><strong>Example</strong>: Using the <code>map</code> function with an array and a one-parameter lambda</p>

<pre><code class="language-puppet"># For the array $data, return an array containing each value multiplied by 10
$data = [1,2,3]
$transformed_data = $data.map |$items| { $items * 10 }
# $transformed_data contains [10,20,30]
</code></pre>

<p>When the first argument is a hash, Puppet passes each key and value pair to the lambda
as an array in the form <code>[key, value]</code>.</p>

<p><strong>Example</strong>: Using the <code>map</code> function with a hash and a one-parameter lambda</p>

<pre><code class="language-puppet"># For the hash $data, return an array containing the keys
$data = {'a'=&gt;1,'b'=&gt;2,'c'=&gt;3}
$transformed_data = $data.map |$items| { $items[0] }
# $transformed_data contains ['a','b','c']
</code></pre>

<p>When the first argument is an array and the lambda has two parameters, Puppet passes the
array’s indexes (enumerated from 0) in the first parameter and its values in the second
parameter.</p>

<p><strong>Example</strong>: Using the <code>map</code> function with an array and a two-parameter lambda</p>

<pre><code class="language-puppet"># For the array $data, return an array containing the indexes
$data = [1,2,3]
$transformed_data = $data.map |$index,$value| { $index }
# $transformed_data contains [0,1,2]
</code></pre>

<p>When the first argument is a hash, Puppet passes its keys to the first parameter and its
values to the second parameter.</p>

<p><strong>Example</strong>: Using the <code>map</code> function with a hash and a two-parameter lambda</p>

<pre><code class="language-puppet"># For the hash $data, return an array containing each value
$data = {'a'=&gt;1,'b'=&gt;2,'c'=&gt;3}
$transformed_data = $data.map |$key,$value| { $value }
# $transformed_data contains [1,2,3]
</code></pre>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:match) do |args|
    desc <<EOT
<p>Matches a regular expression against a string and returns an array containing the match
and any matched capturing groups.</p>

<p>The first argument is a string or array of strings. The second argument is either a
regular expression, regular expression represented as a string, or Regex or Pattern
data type that the function matches against the first argument.</p>

<p>The returned array contains the entire match at index 0, and each captured group at
subsequent index values. If the value or expression being matched is an array, the
function returns an array with mapped match results.</p>

<p>If the function doesn’t find a match, it returns ‘undef’.</p>

<p><strong>Example</strong>: Matching a regular expression in a string</p>

<pre><code class="language-ruby">$matches = "abc123".match(/[a-z]+[1-9]+/)
# $matches contains [abc123]
</code></pre>

<p><strong>Example</strong>: Matching a regular expressions with grouping captures in a string</p>

<pre><code class="language-ruby">$matches = "abc123".match(/([a-z]+)([1-9]+)/)
# $matches contains [abc123, abc, 123]
</code></pre>

<p><strong>Example</strong>: Matching a regular expression with grouping captures in an array of strings</p>

<pre><code class="language-ruby">$matches = ["abc123","def456"].match(/([a-z]+)([1-9]+)/)
# $matches contains [[abc123, abc, 123], [def456, def, 456]]
</code></pre>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: statement</p>
  </li>
</ul>
EOT
  end

  newfunction(:md5) do |args|
    desc <<EOT
<p>Returns a MD5 hash value from a provided string.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:new) do |args|
    desc <<EOT
<p>Creates a new instance/object of a given data type.</p>

<p>This function makes it possible to create new instances of
concrete data types. If a block is given it is called with the
just created instance as an argument.</p>

<p>Calling this function is equivalent to directly
calling the data type:</p>

<p><strong>Example:</strong> <code>new</code> and calling type directly are equivalent</p>

<pre><code class="language-puppet">$a = Integer.new("42")
$b = Integer("42")
</code></pre>

<p>These would both convert the string <code>"42"</code> to the decimal value <code>42</code>.</p>

<p><strong>Example:</strong> arguments by position or by name</p>

<pre><code class="language-puppet">$a = Integer.new("42", 8)
$b = Integer({from =&gt; "42", radix =&gt; 8})
</code></pre>

<p>This would convert the octal (radix 8) number <code>"42"</code> in string form
to the decimal value <code>34</code>.</p>

<p>The new function supports two ways of giving the arguments:</p>

<ul>
  <li>by name (using a hash with property to value mapping)</li>
  <li>by position (as regular arguments)</li>
</ul>

<p>Note that it is not possible to create new instances of
some abstract data types (for example <code>Variant</code>). The data type <code>Optional[T]</code> is an
exception as it will create an instance of <code>T</code> or <code>undef</code> if the
value to convert is <code>undef</code>.</p>

<p>The arguments that can be given is determined by the data type.</p>

<blockquote>
  <p>An assertion is always made that the produced value complies with the given type constraints.</p>
</blockquote>

<p><strong>Example:</strong> data type constraints are checked</p>

<pre><code class="language-puppet">Integer[0].new("-100")
</code></pre>

<p>Would fail with an assertion error (since value is less than 0).</p>

<p>The following sections show the arguments and conversion rules
per data type built into the Puppet Type System.</p>
EOT
  end

  newfunction(:next) do |args|
    desc <<EOT
<p>Immediately returns the given optional value from a block (lambda), function, class body or user defined type body.
If a value is not given, an <code>undef</code> value is returned. This function does not return to the immediate caller.</p>

<p>The signal produced to return a value bubbles up through
the call stack until reaching a code block (lambda), function, class definition or
definition of a user defined type at which point the value given to the function will
be produced as the result of that body of code. An error is raised
if the signal to return a value reaches the end of the call stack.</p>

<p><strong>Example:</strong> Using <code>next</code> in <code>each</code></p>

<pre><code class="language-puppet">$data = [1,2,3]
$data.each |$x| { if $x == 2 { next() } notice $x }
</code></pre>

<p>Would notice the values <code>1</code> and <code>3</code></p>

<p><strong>Example:</strong> Using <code>next</code> to produce a value</p>

<p>If logic consists of deeply nested conditionals it may be complicated to get out of the innermost conditional.
A call to <code>next</code> can then simplify the logic. This example however, only shows the principle.</p>
<pre><code class="language-puppet">$data = [1,2,3]
notice $data.map |$x| { if $x == 2 { next($x*100) }; $x*10 }
</code></pre>
<p>Would notice the value <code>[10, 200, 30]</code></p>

<ul>
  <li>Also see functions <code>return</code> and <code>break</code></li>
  <li>
    <p>Since 4.8.0</p>
  </li>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:notice) do |args|
    desc <<EOT
<p>Log a message on the server at level notice.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:realize) do |args|
    desc <<EOT
<p>Make a virtual object real.  This is useful
when you want to know the name of the virtual object and don’t want to
bother with a full collection.  It is slightly faster than a collection,
and, of course, is a bit shorter.  You must pass the object using a
reference; e.g.: <code>realize User[luke]</code>.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:reduce) do |args|
    desc <<EOT
<p>Applies a <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a>
to every value in a data structure from the first argument, carrying over the returned
value of each iteration, and returns the result of the lambda’s final iteration. This
lets you create a new value or data structure by combining values from the first
argument’s data structure.</p>

<p>This function takes two mandatory arguments, in this order:</p>

<ol>
  <li>An array or hash the function will iterate over.</li>
  <li>A lambda, which the function calls for each element in the first argument. It takes
two mandatory parameters:
    <ol>
      <li>A memo value that is overwritten after each iteration with the iteration’s result.</li>
      <li>A second value that is overwritten after each iteration with the next value in the
 function’s first argument.</li>
    </ol>
  </li>
</ol>

<p><strong>Example</strong>: Using the <code>reduce</code> function</p>

<p><code>$data.reduce |$memo, $value| { ... }</code></p>

<p>or</p>

<p><code>reduce($data) |$memo, $value| { ... }</code></p>

<p>You can also pass an optional “start memo” value as an argument, such as <code>start</code> below:</p>

<p><code>$data.reduce(start) |$memo, $value| { ... }</code></p>

<p>or</p>

<p><code>reduce($data, start) |$memo, $value| { ... }</code></p>

<p>When the first argument (<code>$data</code> in the above example) is an array, Puppet passes each
of the data structure’s values in turn to the lambda’s parameters. When the first
argument is a hash, Puppet converts each of the hash’s values to an array in the form
<code>[key, value]</code>.</p>

<p>If you pass a start memo value, Puppet executes the lambda with the provided memo value
and the data structure’s first value. Otherwise, Puppet passes the structure’s first two
values to the lambda.</p>

<p>Puppet calls the lambda for each of the data structure’s remaining values. For each
call, it passes the result of the previous call as the first parameter ($memo in the
above examples) and the next value from the data structure as the second parameter
($value).</p>

<p>If the structure has one value, Puppet returns the value and does not call the lambda.</p>

<p><strong>Example</strong>: Using the <code>reduce</code> function</p>

<pre><code class="language-puppet"># Reduce the array $data, returning the sum of all values in the array.
$data = [1, 2, 3]
$sum = $data.reduce |$memo, $value| { $memo + $value }
# $sum contains 6

# Reduce the array $data, returning the sum of a start memo value and all values in the
# array.
$data = [1, 2, 3]
$sum = $data.reduce(4) |$memo, $value| { $memo + $value }
# $sum contains 10

# Reduce the hash $data, returning the sum of all values and concatenated string of all
# keys.
$data = {a =&gt; 1, b =&gt; 2, c =&gt; 3}
$combine = $data.reduce |$memo, $value| {
  $string = "${memo[0]}${value[0]}"
  $number = $memo[1] + $value[1]
  [$string, $number]
}
# $combine contains [abc, 6]
</code></pre>

<p><strong>Example</strong>: Using the <code>reduce</code> function with a start memo and two-parameter lambda</p>

<pre><code class="language-puppet"># Reduce the array $data, returning the sum of all values in the array and starting
# with $memo set to an arbitrary value instead of $data's first value.
$data = [1, 2, 3]
$sum = $data.reduce(4) |$memo, $value| { $memo + $value }
# At the start of the lambda's first iteration, $memo contains 4 and $value contains 1.
# After all iterations, $sum contains 10.

# Reduce the hash $data, returning the sum of all values and concatenated string of
# all keys, and starting with $memo set to an arbitrary array instead of $data's first
# key-value pair.
$data = {a =&gt; 1, b =&gt; 2, c =&gt; 3}
$combine = $data.reduce( [d, 4] ) |$memo, $value| {
  $string = "${memo[0]}${value[0]}"
  $number = $memo[1] + $value[1]
  [$string, $number]
}
# At the start of the lambda's first iteration, $memo contains [d, 4] and $value
# contains [a, 1].
# $combine contains [dabc, 10]
</code></pre>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:regsubst) do |args|
    desc <<EOT
<p>Perform regexp replacement on a string or array of strings.</p>

<ul>
  <li><em>Parameters</em> (in order):
    <ul>
      <li><em>target</em>  The string or array of strings to operate on.  If an array, the replacement will be performed on each of the elements in the array, and the return value will be an array.</li>
      <li><em>regexp</em>  The regular expression matching the target string.  If you want it anchored at the start and or end of the string, you must do that with ^ and $ yourself.</li>
      <li><em>replacement</em>  Replacement string. Can contain backreferences to what was matched using \0 (whole match), \1 (first set of parentheses), and so on.</li>
      <li><em>flags</em>  Optional. String of single letter flags for how the regexp is interpreted:
        <ul>
          <li><em>E</em>         Extended regexps</li>
          <li><em>I</em>         Ignore case in regexps</li>
          <li><em>M</em>         Multiline regexps</li>
          <li><em>G</em>         Global replacement; all occurrences of the regexp in each target string will be replaced.  Without this, only the first occurrence will be replaced.</li>
        </ul>
      </li>
      <li><em>encoding</em>  Optional.  How to handle multibyte characters.  A single-character string with the following values:
        <ul>
          <li><em>N</em>         None</li>
          <li><em>E</em>         EUC</li>
          <li><em>S</em>         SJIS</li>
          <li><em>U</em>         UTF-8</li>
        </ul>
      </li>
    </ul>
  </li>
  <li><em>Examples</em></li>
</ul>

<p>Get the third octet from the node’s IP address:</p>

<pre><code>$i3 = regsubst($ipaddress,'^(\d+)\.(\d+)\.(\d+)\.(\d+)$','\3')
</code></pre>

<p>Put angle brackets around each octet in the node’s IP address:</p>

<pre><code>$x = regsubst($ipaddress, '([0-9]+)', '&lt;\1&gt;', 'G')
</code></pre>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:require) do |args|
    desc <<EOT
<p>Evaluate one or more classes,  adding the required class as a dependency.</p>

<p>The relationship metaparameters work well for specifying relationships
between individual resources, but they can be clumsy for specifying
relationships between classes.  This function is a superset of the
‘include’ function, adding a class relationship so that the requiring
class depends on the required class.</p>

<p>Warning: using require in place of include can lead to unwanted dependency cycles.</p>

<p>For instance the following manifest, with ‘require’ instead of ‘include’ would produce a nasty dependence cycle, because notify imposes a before between File[/foo] and Service[foo]:</p>

<pre><code>class myservice {
  service { foo: ensure =&gt; running }
}

class otherstuff {
  include myservice
  file { '/foo': notify =&gt; Service[foo] }
}
</code></pre>

<p>Note that this function only works with clients 0.25 and later, and it will
fail if used with earlier clients.</p>

<p>You must use the class’s full name;
relative names are not allowed. In addition to names in string form,
you may also directly use Class and Resource Type values that are produced when evaluating
resource and relationship expressions.</p>

<ul>
  <li>Since 4.0.0 Class and Resource types, absolute names</li>
  <li>
    <p>Since 4.7.0 Returns an Array[Type[Class]] with references to the required classes</p>
  </li>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:return) do |args|
    desc <<EOT
<p>Immediately returns the given optional value from a function, class body or user defined type body.
If a value is not given, an <code>undef</code> value is returned. This function does not return to the immediate caller.
If called from within a lambda the return will return from the function evaluating the lambda.</p>

<p>The signal produced to return a value bubbles up through
the call stack until reaching a function, class definition or
definition of a user defined type at which point the value given to the function will
be produced as the result of that body of code. An error is raised
if the signal to return a value reaches the end of the call stack.</p>

<p><strong>Example:</strong> Using <code>return</code></p>

<pre><code class="language-puppet">function example($x) {
  # handle trivial cases first for better readability of
  # what follows
  if $x == undef or $x == [] or $x == '' {
    return false
  }
  # complex logic to determine if value is true
  true
}
notice example([]) # would notice false
notice example(42) # would notice true
</code></pre>

<p><strong>Example:</strong> Using <code>return</code> in a class</p>

<pre><code class="language-puppet">class example($x) {
  # handle trivial cases first for better readability of
  # what follows
  if $x == undef or $x == [] or $x == '' {
    # Do some default configuration of this class
    notice 'foo'
    return()
  }
  # complex logic configuring the class if something more interesting
  # was given in $x
  notice 'bar'
}
</code></pre>

<p>When used like this:</p>

<pre><code class="language-puppet">class { example: x =&gt; [] }
</code></pre>

<p>The code would notice <code>'foo'</code>, but not <code>'bar'</code>.</p>

<p>When used like this:</p>

<pre><code class="language-puppet">class { example: x =&gt; [some_value] }
</code></pre>

<p>The code would notice <code>'bar'</code> but not <code>'foo'</code></p>

<p>Note that the returned value is ignored if used in a class or user defined type.</p>

<ul>
  <li>Also see functions <code>return</code> and <code>break</code></li>
  <li>
    <p>Since 4.8.0</p>
  </li>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:reverse_each) do |args|
    desc <<EOT
<p>Reverses the order of the elements of something that is iterable and optionally runs a
<a href="http://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a> for each
element.</p>

<p>This function takes one to two arguments:</p>

<ol>
  <li>An <code>Iterable</code> that the function will iterate over.</li>
  <li>An optional lambda, which the function calls for each element in the first argument. It must
request one parameter.</li>
</ol>

<p><strong>Example:</strong> Using the <code>reverse_each</code> function</p>

<pre><code class="language-puppet">$data.reverse_each |$parameter| { &lt;PUPPET CODE BLOCK&gt; }
</code></pre>

<p>or</p>

<pre><code class="language-puppet">$reverse_data = $data.reverse_each
</code></pre>

<p>or</p>

<pre><code class="language-puppet">reverse_each($data) |$parameter| { &lt;PUPPET CODE BLOCK&gt; }
</code></pre>

<p>or</p>

<pre><code class="language-puppet">$reverse_data = reverse_each($data)
</code></pre>

<p>When no second argument is present, Puppet returns an <code>Iterable</code> that represents the reverse
order of its first argument. This allows methods on <code>Iterable</code> to be chained.</p>

<p>When a lamdba is given as the second argument, Puppet iterates the first argument in reverse
order and passes each value in turn to the lambda, then returns <code>undef</code>.</p>

<p><strong>Example:</strong> Using the <code>reverse_each</code> function with an array and a one-parameter lambda</p>

<pre><code class="language-puppet"># Puppet will log a notice for each of the three items
# in $data in reverse order.
$data = [1,2,3]
$data.reverse_each |$item| { notice($item) }
</code></pre>

<p>When no second argument is present, Puppet returns a new <code>Iterable</code> which allows it to
be directly chained into another function that takes an <code>Iterable</code> as an argument.</p>

<p><strong>Example:</strong> Using the <code>reverse_each</code> function chained with a <code>map</code> function.</p>

<pre><code class="language-puppet"># For the array $data, return an array containing each
# value multiplied by 10 in reverse order
$data = [1,2,3]
$transformed_data = $data.reverse_each.map |$item| { $item * 10 }
# $transformed_data is set to [30,20,10]
</code></pre>

<p><strong>Example:</strong> Using <code>reverse_each</code> function chained with a <code>map</code> in alternative syntax</p>

<pre><code class="language-puppet"># For the array $data, return an array containing each
# value multiplied by 10 in reverse order
$data = [1,2,3]
$transformed_data = map(reverse_each($data)) |$item| { $item * 10 }
# $transformed_data is set to [30,20,10]
</code></pre>

<ul>
  <li>
    <p>Since 4.4.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:scanf) do |args|
    desc <<EOT
<p>Scans a string and returns an array of one or more converted values based on the given format string.
See the documentation of Ruby’s String#scanf method for details about the supported formats (which
are similar but not identical to the formats used in Puppet’s <code>sprintf</code> function.)</p>

<p>This function takes two mandatory arguments: the first is the string to convert, and the second is
the format string. The result of the scan is an array, with each successfully scanned and transformed value.
The scanning stops if a scan is unsuccessful, and the scanned result up to that point is returned. If there
was no successful scan, the result is an empty array.</p>

<pre><code class="language-puppet">"42".scanf("%i")
</code></pre>

<p>You can also optionally pass a lambda to scanf, to do additional validation or processing.</p>

<pre><code class="language-puppet">"42".scanf("%i") |$x| {
  unless $x[0] =~ Integer {
    fail "Expected a well formed integer value, got '$x[0]'"
  }
  $x[0]
}
</code></pre>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:sha1) do |args|
    desc <<EOT
<p>Returns a SHA1 hash value from a provided string.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:shellquote) do |args|
    desc <<EOT
<p>Quote and concatenate arguments for use in Bourne shell.</p>

<p>Each argument is quoted separately, and then all are concatenated
with spaces.  If an argument is an array, the elements of that
array is interpolated within the rest of the arguments; this makes
it possible to have an array of arguments and pass that array to
shellquote instead of having to specify each argument
individually in the call.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:slice) do |args|
    desc <<EOT
<p>This function takes two mandatory arguments: the first should be an array or hash, and the second specifies
the number of elements to include in each slice.</p>

<p>When the first argument is a hash, each key value pair is counted as one. For example, a slice size of 2 will produce
an array of two arrays with key, and value.</p>

<pre><code>$a.slice(2) |$entry|          { notice "first ${$entry[0]}, second ${$entry[1]}" }
$a.slice(2) |$first, $second| { notice "first ${first}, second ${second}" }
</code></pre>

<p>The function produces a concatenated result of the slices.</p>

<pre><code>slice([1,2,3,4,5,6], 2) # produces [[1,2], [3,4], [5,6]]
slice(Integer[1,6], 2)  # produces [[1,2], [3,4], [5,6]]
slice(4,2)              # produces [[0,1], [2,3]]
slice('hello',2)        # produces [[h, e], [l, l], [o]]
</code></pre>

<p>You can also optionally pass a lambda to slice.</p>

<pre><code>$a.slice($n) |$x| { ... }
slice($a) |$x| { ... }
</code></pre>

<p>The lambda should have either one parameter (receiving an array with the slice), or the same number
of parameters as specified by the slice size (each parameter receiving its part of the slice).
If there are fewer remaining elements than the slice size for the last slice, it will contain the remaining
elements. If the lambda has multiple parameters, excess parameters are set to undef for an array, or
to empty arrays for a hash.</p>

<pre><code>$a.slice(2) |$first, $second| { ... }
</code></pre>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:split) do |args|
    desc <<EOT
<p>Split a string variable into an array using the specified split regexp.</p>

<p><em>Example:</em></p>

<pre><code>$string     = 'v1.v2:v3.v4'
$array_var1 = split($string, ':')
$array_var2 = split($string, '[.]')
$array_var3 = split($string, Regexp['[.:]'])
</code></pre>

<p><code>$array_var1</code> now holds the result <code>['v1.v2', 'v3.v4']</code>,
while <code>$array_var2</code> holds <code>['v1', 'v2:v3', 'v4']</code>, and
<code>$array_var3</code> holds <code>['v1', 'v2', 'v3', 'v4']</code>.</p>

<p>Note that in the second example, we split on a literal string that contains
a regexp meta-character (.), which must be escaped.  A simple
way to do that for a single character is to enclose it in square
brackets; a backslash will also escape a single character.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:sprintf) do |args|
    desc <<EOT
<p>Perform printf-style formatting of text.</p>

<p>The first parameter is format string describing how the rest of the parameters should be formatted.  See the documentation for the <code>Kernel::sprintf</code> function in Ruby for all the details.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:step) do |args|
    desc <<EOT
<p>Provides stepping with given interval over elements in an iterable and optionally runs a
<a href="http://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a> for each
element.</p>

<p>This function takes two to three arguments:</p>

<ol>
  <li>An ‘Iterable’ that the function will iterate over.</li>
  <li>An <code>Integer</code> step factor. This must be a positive integer.</li>
  <li>An optional lambda, which the function calls for each element in the interval. It must
request one parameter.</li>
</ol>

<p><strong>Example:</strong> Using the <code>step</code> function</p>

<pre><code class="language-puppet">$data.step(&lt;n&gt;) |$parameter| { &lt;PUPPET CODE BLOCK&gt; }
</code></pre>

<p>or</p>

<pre><code class="language-puppet">$stepped_data = $data.step(&lt;n&gt;)
</code></pre>

<p>or</p>
<pre><code class="language-puppet">step($data, &lt;n&gt;) |$parameter| { &lt;PUPPET CODE BLOCK&gt; }
</code></pre>

<p>or</p>

<pre><code class="language-puppet">$stepped_data = step($data, &lt;n&gt;)
</code></pre>

<p>When no block is given, Puppet returns an <code>Iterable</code> that yields the first element and every nth successor
element, from its first argument. This allows functions on iterables to be chained.
When a block is given, Puppet iterates and calls the block with the first element and then with
every nth successor element. It then returns <code>undef</code>.</p>

<p><strong>Example:</strong> Using the <code>step</code> function with an array, a step factor, and a one-parameter block</p>

<pre><code class="language-puppet"># For the array $data, call a block with the first element and then with each 3rd successor element
$data = [1,2,3,4,5,6,7,8]
$data.step(3) |$item| {
 notice($item)
}
# Puppet notices the values '1', '4', '7'.
</code></pre>

<p>When no block is given, Puppet returns a new <code>Iterable</code> which allows it to be directly chained into
another function that takes an <code>Iterable</code> as an argument.</p>

<p><strong>Example:</strong> Using the <code>step</code> function chained with a <code>map</code> function.</p>

<pre><code class="language-puppet"># For the array $data, return an array, set to the first element and each 5th successor element, in reverse
# order multiplied by 10
$data = Integer[0,20]
$transformed_data = $data.step(5).map |$item| { $item * 10 }
$transformed_data contains [0,50,100,150,200]
</code></pre>

<p><strong>Example:</strong> The same example using <code>step</code> function chained with a <code>map</code> in alternative syntax</p>

<pre><code class="language-puppet"># For the array $data, return an array, set to the first and each 5th
# successor, in reverse order, multiplied by 10
$data = Integer[0,20]
$transformed_data = map(step($data, 5)) |$item| { $item * 10 }
$transformed_data contains [0,50,100,150,200]
</code></pre>

<ul>
  <li>
    <p>Since 4.4.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:strftime) do |args|
    desc <<EOT
<p>Formats timestamp or timespan according to the directives in the given format string. The directives begins with a percent (%) character.
Any text not listed as a directive will be passed through to the output string.</p>

<p>A third optional timezone argument can be provided. The first argument will then be formatted to represent a local time in that
timezone. The timezone can be any timezone that is recognized when using the ‘%z’ or ‘%Z’ formats, or the word ‘current’, in which
case the current timezone of the evaluating process will be used. The timezone argument is case insensitive.</p>

<p>The default timezone, when no argument is provided, or when using the keyword <code>default</code>, is ‘UTC’.</p>

<p>The directive consists of a percent (%) character, zero or more flags, optional minimum field width and
a conversion specifier as follows:</p>
<pre><code>%[Flags][Width]Conversion
</code></pre>

<h3 id="flags-that-controls-padding">Flags that controls padding</h3>

<table>
  <thead>
    <tr>
      <th>Flag</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>-</td>
      <td>Don’t pad numerical output</td>
    </tr>
    <tr>
      <td>_</td>
      <td>Use spaces for padding</td>
    </tr>
    <tr>
      <td>0</td>
      <td>Use zeros for padding</td>
    </tr>
  </tbody>
</table>

<h3 id="timestamp-specific-flags"><code>Timestamp</code> specific flags</h3>

<table>
  <thead>
    <tr>
      <th>Flag</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>#</td>
      <td>Change case</td>
    </tr>
    <tr>
      <td>^</td>
      <td>Use uppercase</td>
    </tr>
    <tr>
      <td>:</td>
      <td>Use colons for %z</td>
    </tr>
  </tbody>
</table>

<h3 id="format-directives-applicable-to-timestamp-names-and-padding-can-be-altered-using-flags">Format directives applicable to <code>Timestamp</code> (names and padding can be altered using flags):</h3>

<p><strong>Date (Year, Month, Day):</strong></p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Y</td>
      <td>Year with century, zero-padded to at least 4 digits</td>
    </tr>
    <tr>
      <td>C</td>
      <td>year / 100 (rounded down such as 20 in 2009)</td>
    </tr>
    <tr>
      <td>y</td>
      <td>year % 100 (00..99)</td>
    </tr>
    <tr>
      <td>m</td>
      <td>Month of the year, zero-padded (01..12)</td>
    </tr>
    <tr>
      <td>B</td>
      <td>The full month name (“January”)</td>
    </tr>
    <tr>
      <td>b</td>
      <td>The abbreviated month name (“Jan”)</td>
    </tr>
    <tr>
      <td>h</td>
      <td>Equivalent to %b</td>
    </tr>
    <tr>
      <td>d</td>
      <td>Day of the month, zero-padded (01..31)</td>
    </tr>
    <tr>
      <td>e</td>
      <td>Day of the month, blank-padded ( 1..31)</td>
    </tr>
    <tr>
      <td>j</td>
      <td>Day of the year (001..366)</td>
    </tr>
  </tbody>
</table>

<p><strong>Time (Hour, Minute, Second, Subsecond):</strong></p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>H</td>
      <td>Hour of the day, 24-hour clock, zero-padded (00..23)</td>
    </tr>
    <tr>
      <td>k</td>
      <td>Hour of the day, 24-hour clock, blank-padded ( 0..23)</td>
    </tr>
    <tr>
      <td>I</td>
      <td>Hour of the day, 12-hour clock, zero-padded (01..12)</td>
    </tr>
    <tr>
      <td>l</td>
      <td>Hour of the day, 12-hour clock, blank-padded ( 1..12)</td>
    </tr>
    <tr>
      <td>P</td>
      <td>Meridian indicator, lowercase (“am” or “pm”)</td>
    </tr>
    <tr>
      <td>p</td>
      <td>Meridian indicator, uppercase (“AM” or “PM”)</td>
    </tr>
    <tr>
      <td>M</td>
      <td>Minute of the hour (00..59)</td>
    </tr>
    <tr>
      <td>S</td>
      <td>Second of the minute (00..60)</td>
    </tr>
    <tr>
      <td>L</td>
      <td>Millisecond of the second (000..999). Digits under millisecond are truncated to not produce 1000</td>
    </tr>
    <tr>
      <td>N</td>
      <td>Fractional seconds digits, default is 9 digits (nanosecond). Digits under a specified width are truncated to avoid carry up</td>
    </tr>
  </tbody>
</table>

<p><strong>Time (Hour, Minute, Second, Subsecond):</strong></p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>z</td>
      <td>Time zone as hour and minute offset from UTC (e.g. +0900)</td>
    </tr>
    <tr>
      <td>:z</td>
      <td>hour and minute offset from UTC with a colon (e.g. +09:00)</td>
    </tr>
    <tr>
      <td>::z</td>
      <td>hour, minute and second offset from UTC (e.g. +09:00:00)</td>
    </tr>
    <tr>
      <td>Z</td>
      <td>Abbreviated time zone name or similar information.  (OS dependent)</td>
    </tr>
  </tbody>
</table>

<p><strong>Weekday:</strong></p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>A</td>
      <td>The full weekday name (“Sunday”)</td>
    </tr>
    <tr>
      <td>a</td>
      <td>The abbreviated name (“Sun”)</td>
    </tr>
    <tr>
      <td>u</td>
      <td>Day of the week (Monday is 1, 1..7)</td>
    </tr>
    <tr>
      <td>w</td>
      <td>Day of the week (Sunday is 0, 0..6)</td>
    </tr>
  </tbody>
</table>

<p><strong>ISO 8601 week-based year and week number:</strong></p>

<p>The first week of YYYY starts with a Monday and includes YYYY-01-04.
The days in the year before the first week are in the last week of
the previous year.</p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>G</td>
      <td>The week-based year</td>
    </tr>
    <tr>
      <td>g</td>
      <td>The last 2 digits of the week-based year (00..99)</td>
    </tr>
    <tr>
      <td>V</td>
      <td>Week number of the week-based year (01..53)</td>
    </tr>
  </tbody>
</table>

<p><strong>Week number:</strong></p>

<p>The first week of YYYY that starts with a Sunday or Monday (according to %U
or %W). The days in the year before the first week are in week 0.</p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>U</td>
      <td>Week number of the year. The week starts with Sunday. (00..53)</td>
    </tr>
    <tr>
      <td>W</td>
      <td>Week number of the year. The week starts with Monday. (00..53)</td>
    </tr>
  </tbody>
</table>

<p><strong>Seconds since the Epoch:</strong></p>

<table>
  <tbody>
    <tr>
      <td>Format</td>
      <td>Meaning</td>
    </tr>
    <tr>
      <td>s</td>
      <td>Number of seconds since 1970-01-01 00:00:00 UTC.</td>
    </tr>
  </tbody>
</table>

<p><strong>Literal string:</strong></p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>n</td>
      <td>Newline character (</td>
    </tr>
    <tr>
      <td>)</td>
      <td> </td>
    </tr>
    <tr>
      <td>t</td>
      <td>Tab character (	)</td>
    </tr>
    <tr>
      <td>%</td>
      <td>Literal “%” character</td>
    </tr>
  </tbody>
</table>

<p><strong>Combination:</strong></p>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>c</td>
      <td>date and time (%a %b %e %T %Y)</td>
    </tr>
    <tr>
      <td>D</td>
      <td>Date (%m/%d/%y)</td>
    </tr>
    <tr>
      <td>F</td>
      <td>The ISO 8601 date format (%Y-%m-%d)</td>
    </tr>
    <tr>
      <td>v</td>
      <td>VMS date (%e-%^b-%4Y)</td>
    </tr>
    <tr>
      <td>x</td>
      <td>Same as %D</td>
    </tr>
    <tr>
      <td>X</td>
      <td>Same as %T</td>
    </tr>
    <tr>
      <td>r</td>
      <td>12-hour time (%I:%M:%S %p)</td>
    </tr>
    <tr>
      <td>R</td>
      <td>24-hour time (%H:%M)</td>
    </tr>
    <tr>
      <td>T</td>
      <td>24-hour time (%H:%M:%S)</td>
    </tr>
  </tbody>
</table>

<p><strong>Example</strong>: Using <code>strftime</code> with a <code>Timestamp</code>:</p>

<pre><code class="language-puppet">$timestamp = Timestamp('2016-08-24T12:13:14')

# Notice the timestamp using a format that notices the ISO 8601 date format
notice($timestamp.strftime('%F')) # outputs '2016-08-24'

# Notice the timestamp using a format that notices weekday, month, day, time (as UTC), and year
notice($timestamp.strftime('%c')) # outputs 'Wed Aug 24 12:13:14 2016'

# Notice the timestamp using a specific timezone
notice($timestamp.strftime('%F %T %z', 'PST')) # outputs '2016-08-24 04:13:14 -0800'

# Notice the timestamp using timezone that is current for the evaluating process
notice($timestamp.strftime('%F %T', 'current')) # outputs the timestamp using the timezone for the current process
</code></pre>

<h3 id="format-directives-applicable-to-timespan">Format directives applicable to <code>Timespan</code>:</h3>

<table>
  <thead>
    <tr>
      <th>Format</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>D</td>
      <td>Number of Days</td>
    </tr>
    <tr>
      <td>H</td>
      <td>Hour of the day, 24-hour clock</td>
    </tr>
    <tr>
      <td>M</td>
      <td>Minute of the hour (00..59)</td>
    </tr>
    <tr>
      <td>S</td>
      <td>Second of the minute (00..59)</td>
    </tr>
    <tr>
      <td>L</td>
      <td>Millisecond of the second (000..999). Digits under millisecond are truncated to not produce 1000.</td>
    </tr>
    <tr>
      <td>N</td>
      <td>Fractional seconds digits, default is 9 digits (nanosecond). Digits under a specified length are truncated to avoid carry up</td>
    </tr>
  </tbody>
</table>

<p>The format directive that represents the highest magnitude in the format will be allowed to overflow.
I.e. if no “%D” is used but a “%H” is present, then the hours will be more than 23 in case the
timespan reflects more than a day.</p>

<p><strong>Example</strong>: Using <code>strftime</code> with a Timespan and a format</p>

<pre><code class="language-puppet">$duration = Timespan({ hours =&gt; 3, minutes =&gt; 20, seconds =&gt; 30 })

# Notice the duration using a format that outputs &lt;hours&gt;:&lt;minutes&gt;:&lt;seconds&gt;
notice($duration.strftime('%H:%M:%S')) # outputs '03:20:30'

# Notice the duration using a format that outputs &lt;minutes&gt;:&lt;seconds&gt;
notice($duration.strftime('%M:%S')) # outputs '200:30'
</code></pre>

<ul>
  <li>
    <p>Since 4.8.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:tag) do |args|
    desc <<EOT
<p>Add the specified tags to the containing class
or definition.  All contained objects will then acquire that tag, also.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:tagged) do |args|
    desc <<EOT
<p>A boolean function that
tells you whether the current container is tagged with the specified tags.
The tags are ANDed, so that all of the specified tags must be included for
the function to return true.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:template) do |args|
    desc <<EOT
<p>Loads an ERB template from a module, evaluates it, and returns the resulting
value as a string.</p>

<p>The argument to this function should be a <code>&lt;MODULE NAME&gt;/&lt;TEMPLATE FILE&gt;</code>
reference, which will load <code>&lt;TEMPLATE FILE&gt;</code> from a module’s <code>templates</code>
directory. (For example, the reference <code>apache/vhost.conf.erb</code> will load the
file <code>&lt;MODULES DIRECTORY&gt;/apache/templates/vhost.conf.erb</code>.)</p>

<p>This function can also accept:</p>

<ul>
  <li>An absolute path, which can load a template file from anywhere on disk.</li>
  <li>
    <p>Multiple arguments, which will evaluate all of the specified templates and
return their outputs concatenated into a single string.</p>
  </li>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:then) do |args|
    desc <<EOT
<p>Call a <a href="https://docs.puppet.com/puppet/latest/reference/lang_lambdas.html">lambda</a>
with the given argument unless the argument is undef. Return <code>undef</code> if argument is
<code>undef</code>, and otherwise the result of giving the argument to the lambda.</p>

<p>This is useful to process a sequence of operations where an intermediate
result may be <code>undef</code> (which makes the entire sequence <code>undef</code>).
The <code>then</code> function is especially useful with the function <code>dig</code> which
performs in a similar way “digging out” a value in a complex structure.</p>

<p><strong>Example:</strong> Using <code>dig</code> and <code>then</code></p>

<pre><code class="language-puppet">$data = {a =&gt; { b =&gt; [{x =&gt; 10, y =&gt; 20}, {x =&gt; 100, y =&gt; 200}]}}
notice $data.dig(a, b, 1, x).then |$x| { $x * 2 }
</code></pre>

<p>Would notice the value 200</p>

<p>Contrast this with:</p>

<pre><code class="language-puppet">$data = {a =&gt; { b =&gt; [{x =&gt; 10, y =&gt; 20}, {ex =&gt; 100, why =&gt; 200}]}}
notice $data.dig(a, b, 1, x).then |$x| { $x * 2 }
</code></pre>

<p>Which would notice <code>undef</code> since the last lookup of ‘x’ results in <code>undef</code> which
is returned (without calling the lambda given to the <code>then</code> function).</p>

<p>As a result there is no need for conditional logic or a temporary (non local)
variable as the result is now either the wanted value (<code>x</code>) multiplied
by 2 or <code>undef</code>.</p>

<p>Calls to <code>then</code> can be chained. In the next example, a structure is using an offset based on
using 1 as the index to the first element (instead of 0 which is used in the language).
We are not sure if user input actually contains an index at all, or if it is
outside the range of available names.args.</p>

<p><strong>Example:</strong> Chaining calls to the <code>then</code> function</p>

<pre><code class="language-puppet"># Names to choose from
$names = ['Ringo', 'Paul', 'George', 'John']

# Structure where 'beatle 2' is wanted (but where the number refers
# to 'Paul' because input comes from a source using 1 for the first
# element).

$data = ['singer', { beatle =&gt; 2 }]
$picked = assert_type(String,
  # the data we are interested in is the second in the array,
  # a hash, where we want the value of the key 'beatle'
  $data.dig(1, 'beatle')
    # and we want the index in $names before the given index
    .then |$x| { $names[$x-1] }
    # so we can construct a string with that beatle's name
    .then |$x| { "Picked Beatle '${x}'" }
)
</code></pre>

<p>Would notice “Picked Beatle ‘Paul’”, and would raise an error if the result
was not a String.</p>

<ul>
  <li>
    <p>Since 4.5.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:type) do |args|
    desc <<EOT
<p>Returns the data type of a given value with a given degree of generality.</p>

<pre><code class="language-puppet">type InferenceFidelity = Enum[generalized, reduced, detailed]

function type(Any $value, InferenceFidelity $fidelity = 'detailed') # returns Type
</code></pre>

<p><strong>Example:</strong> Using <code>type</code></p>

<p><code>puppet
 notice type(42) =~ Type[Integer]
</code></p>

<p>Would notice <code>true</code>.</p>

<p>By default, the best possible inference is made where all details are retained.
 This is good when the type is used for further type calculations but is overwhelmingly
 rich in information if it is used in a error message.</p>

<p>The optional argument <code>$fidelity</code> may be given as (from lowest to highest fidelity):</p>

<ul>
  <li><code>generalized</code> - reduces to common type and drops size constraints</li>
  <li><code>reduced</code> - reduces to common type in collections</li>
  <li><code>detailed</code> - (default) all details about inferred types is retained</li>
</ul>

<p><strong>Example:</strong> Using <code>type()</code> with different inference fidelity:</p>

<p><code>puppet
 notice type([3.14, 42], 'generalized')
 notice type([3.14, 42], 'reduced'')
 notice type([3.14, 42], 'detailed')
 notice type([3.14, 42])
</code></p>

<p>Would notice the four values:</p>

<ol>
  <li>‘Array[Numeric]’</li>
  <li>‘Array[Numeric, 2, 2]’</li>
  <li>‘Tuple[Float[3.14], Integer[42,42]]]’</li>
  <li>‘Tuple[Float[3.14], Integer[42,42]]]’</li>
</ol>

<ul>
  <li>
    <p>Since 4.4.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

  newfunction(:versioncmp) do |args|
    desc <<EOT
<p>Compares two version numbers.</p>

<p>Prototype:</p>

<pre><code>$result = versioncmp(a, b)
</code></pre>

<p>Where a and b are arbitrary version strings.</p>

<p>This function returns:</p>

<ul>
  <li><code>1</code> if version a is greater than version b</li>
  <li><code>0</code> if the versions are equal</li>
  <li><code>-1</code> if version a is less than version b</li>
</ul>

<p>Example:</p>

<pre><code>if versioncmp('2.6-1', '2.4.5') &gt; 0 {
    notice('2.6-1 is &gt; than 2.4.5')
}
</code></pre>

<p>This function uses the same version comparison algorithm used by Puppet’s
<code>package</code> type.</p>

<ul>
  <li><em>Type</em>: rvalue</li>
</ul>
EOT
  end

  newfunction(:warning) do |args|
    desc <<EOT
<p>Log a message on the server at level warning.</p>

<ul>
  <li><em>Type</em>: statement</li>
</ul>
EOT
  end

  newfunction(:with) do |args|
    desc <<EOT
<p>Call a <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html">lambda</a>
with the given arguments and return the result. Since a lambda’s scope is
<a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_lambdas.html#lambda-scope">local</a>
to the lambda, you can use the <code>with</code> function to create private blocks of code within a
class using variables whose values cannot be accessed outside of the lambda.</p>

<p><strong>Example</strong>: Using <code>with</code></p>

<pre><code class="language-puppet"># Concatenate three strings into a single string formatted as a list.
$fruit = with("apples", "oranges", "bananas") |$x, $y, $z| {
  "${x}, ${y}, and ${z}"
}
$check_var = $x
# $fruit contains "apples, oranges, and bananas"
# $check_var is undefined, as the value of $x is local to the lambda.
</code></pre>

<ul>
  <li>
    <p>Since 4.0.0</p>
  </li>
  <li>
    <p><em>Type</em>: rvalue</p>
  </li>
</ul>
EOT
  end

end
