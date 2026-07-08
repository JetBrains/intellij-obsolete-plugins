# <h2>trusted</h2>
# <p>A few special <strong>trusted facts</strong> appear in a <code>$trusted</code> hash. They can be accessed in manifests as <code>$trusted['fact_name']</code>. The variable name <code>$trusted</code> is reserved, so local scopes cannot re-use it.</p>
#   <p>Normal facts are self-reported by the node, and nothing guarantees their accuracy. Trusted facts are extracted from the node’s certificate, which can prove that the CA checked and approved them. This makes them useful for deciding whether a given node should receive sensitive data in its catalog.</p>
#   <p>The available keys in the <code>$trusted</code> hash are:</p>
#   <ul>
#   <li><code>authenticated</code> — an indication of whether the catalog request was authenticated, as well as how it was authenticated. The value will be one of:
# <ul>
#   <li><code>remote</code> for authenticated remote requests (as with agent/master Puppet configurations)</li>
#   <li><code>local</code> for all local requests (as with standalone Puppet apply nodes)</li>
#   <li><code>false</code> for unauthenticated remote requests (generally only possible if you’ve configured auth.conf to allow unauthenticated catalog requests)</li>
#   </ul>
#   </li>
#   <li><code>certname</code> — the node’s subject CN, as listed in its certificate. (When first requesting its certificate, the node requests a subject CN matching the value of its <code>certname</code> setting.)
# <ul>
#   <li>If <code>authenticated</code> is <code>remote</code>, this is the subject CN extracted from the node’s certificate.</li>
#   <li>If <code>authenticated</code> is <code>local</code>, this is read directly from the <code>certname</code> setting.</li>
#   <li>If <code>authenticated</code> is <code>false</code>, the value of this key will be an empty string.</li>
#   </ul>
#   </li>
#   <li><code>domain</code> — the node’s domain, as derived from its validated certificate name. The value can be empty if the certificate name doesn’t contain a fully qualified domain name.</li>
#   <li><code>extensions</code> — a hash containing any <a href="https://docs.puppet.com/puppet/latest/reference/ssl_attributes_extensions.html">custom extensions</a> present in the node’s certificate.
#     <ul>
#       <li>The keys of the hash will be the <a href="https://docs.puppet.com/puppet/latest/reference/ssl_attributes_extensions.html#recommended-oids-for-extensions">extension OIDs</a> — any OIDs in the ppRegCertExt range will appear using their short names, and other OIDs will appear as plain dotted numbers.</li>
#   <li>If no extensions are present or <code>authenticated</code> is <code>local</code> or <code>false</code>, this will be an empty hash.</li>
#   </ul>
#   </li>
#   <li><code>hostname</code> — the node’s hostname, as derived from its validated certificate name.</li>
#   </ul>

  $trusted = 'stub'

# <h2>facts</h2>
# <p>Facts also appear in a <code>$facts</code> hash. They can be accessed in manifests as <code>$facts['fact_name']</code>. The variable name <code>$facts</code> is reserved, so local scopes cannot re-use it. Structured facts show up as a nested structure inside the <code>$facts</code> namespace, and can be accessed using Puppet’s normal <a href="https://docs.puppet.com/puppet/latest/reference/lang_data_hash.html#accessing-values">hash access syntax</a>. Due to ambiguity with function invocation, the dot-separated access syntax that is available at the Facter command line is not available in manifests.</p>
# <p><strong>Benefits:</strong> More readable and maintainable code, by making facts visibly distinct from other variables. Eliminates possible confusion if you use a local variable whose name happens to match that of a common fact.</p>
# <p><strong>Drawbacks:</strong> Only works with Puppet 3.5 or later. Disabled by default in open source releases prior to Puppet 4.0.</p>

  $facts = 'stub'

# <h2>server_facts</h2>
# <p>The <code>$server_facts</code> variable provides a hash of server-side facts that cannot be overwritten by client side facts. This is important because it enables you to get trusted server facts that could otherwise be overwritten by client-side facts.</p>
# <p>For example, the Puppet master sets the global <code>$::environment</code> variable to contain the name of the node’s environment. However, if a node provides a fact with the name <code>environment</code>, that fact’s value overrides the server-set <code>environment</code> fact. The same happens with other server-set global variables, like <code>$::servername</code> and <code>$::serverip</code>. As a result, modules couldn’t reliably use these variables for whatever their intended purpose was.</p>
# <p>The <code>$server_facts</code> variable is opt-in. Its <code>trusted_server_facts</code> setting is set to false by default. If you set <code>trusted_server_facts</code> to <code>true</code>, the <code>$server_facts</code> variable will be populated, and will ensure that you get trusted server facts.</p>
# <p>In addition, a warning will be issued any time a node parameter is overwritten.</p>
# <h4 id="example">Example<a class="anchor-link" href="#example"><span class="icon-link"></span></a></h4>
# <p>The following is an example <code>$server_facts</code> hash.</p>
# <code class=" language-puppet"><span class="token punctuation">{</span>
# <span class="token attr-name">serverversion</span> <span class="token operator">=&gt;</span> <span class="token string"><span class="token double-quoted">"4.1.0"</span></span><span class="token punctuation">,</span>
# <span class="token attr-name">servername</span>    <span class="token operator">=&gt;</span> <span class="token string"><span class="token double-quoted">"v85ix8blah.delivery.puppetlabs.net"</span></span><span class="token punctuation">,</span>
# <span class="token attr-name">serverip</span>      <span class="token operator">=&gt;</span> <span class="token string"><span class="token double-quoted">"10.32.115.182"</span></span><span class="token punctuation">,</span>
# <span class="token attr-name">environment</span>   <span class="token operator">=&gt;</span> <span class="token string"><span class="token double-quoted">"production"</span></span><span class="token punctuation">,</span>
# <span class="token punctuation">}</span>
# </code>

  $server_facts = 'stub'

# <h2>environment</h2>
# <p>Several variables are set by the Puppet master. These are most useful when managing Puppet with Puppet. (For example, managing puppet.conf with a template.)</p>
# <p>These are <strong>not</strong> available in the <code>$facts</code> hash.</p>
# <p><code>$environment</code> (also available to <code>puppet apply</code>) — the agent node’s <a href="https://docs.puppet.com/puppet/latest/reference/environments.html">environment</a>. Note that nodes can accidentally or purposefully override this with a custom fact; the <code>$server_facts['environment']</code> variable always contains the correct environment, and can’t be overridden.</p>

  $environment = 'stub'

# <h2>servername</h2>
# <p>Several variables are set by the Puppet master. These are most useful when managing Puppet with Puppet. (For example, managing puppet.conf with a template.)</p>
# <p>These are <strong>not</strong> available in the <code>$facts</code> hash.</p>
# <p><code>$servername</code> — the Puppet master’s fully-qualified domain name. (Note that this information is gathered from the Puppet master by Facter, rather than read from the config files; even if the master’s certname is set to something other than its fully-qualified domain name, this variable will still contain the server’s fqdn.)</p>

  $servername = 'stub'

# <h2>serverip</h2>
# <p>Several variables are set by the Puppet master. These are most useful when managing Puppet with Puppet. (For example, managing puppet.conf with a template.)</p>
# <p>These are <strong>not</strong> available in the <code>$facts</code> hash.</p>
# <p><code>$serverip</code> — the Puppet master’s IP address.</p>

  $serverip  = 'stub'

# <h2>serverversion</h2>
# <p>Several variables are set by the Puppet master. These are most useful when managing Puppet with Puppet. (For example, managing puppet.conf with a template.)</p>
# <p>These are <strong>not</strong> available in the <code>$facts</code> hash.</p>
# <p><code>$serverversion</code> — the current version of Puppet on the Puppet master.</p>

  $serverversion = 'stub'

# <h2>module_name</h2>
# <p>These variables are set in every <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html">local scope</a> by the compiler during compilation. They are mostly useful when implementing complex <a href="https://docs.puppet.com/puppet/latest/reference/lang_defined_types.html">defined types</a>.</p>
# <p>These are <strong>not</strong> available in the <code>$facts</code> hash.</p>
# <p>These variables are always defined (by the standards of the <code>strict_variables</code> setting), but their value is <code>undef</code> whenever no other value is applicable.</p>
# <p><code>$module_name</code> — the name of the module that contains the current class or defined type.</p>

  $module_name  = 'stub'

# <h2>caller_module_name</h2>
# <p>These variables are set in every <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html">local scope</a> by the compiler during compilation. They are mostly useful when implementing complex <a href="https://docs.puppet.com/puppet/latest/reference/lang_defined_types.html">defined types</a>.</p>
# <p>These are <strong>not</strong> available in the <code>$facts</code> hash.</p>
# <p>These variables are always defined (by the standards of the <code>strict_variables</code> setting), but their value is <code>undef</code> whenever no other value is applicable.</p>
# <p><code>$caller_module_name</code> — the name of the module in which the <strong>specific instance</strong> of the surrounding defined type was declared. This is only useful when creating versatile defined types which will be re-used by several modules.</p>

  $caller_module_name = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $0 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $1 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $2 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $3 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $4 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $5 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $6 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $7 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $8 = 'stub'

# <p>Within <a href="https://docs.puppet.com/puppet/latest/reference/lang_conditional.html">conditional statements</a> and <a href="https://docs.puppet.com/puppet/latest/reference/lang_node_definitions.html">node definitions</a>, any captured substrings from parentheses in a regular expression will be available as numbered variables (<code>$1, $2</code>, etc.) inside the associated code section, and the entire match will be available as <code>$0</code>.</p>
# <p>These are not normal variables, and have some special behaviors:</p>
# <ul>
#   <li>The values of the numbered variables do not persist outside the code block associated with the pattern that set them.</li>
#   <li>You can’t manually assign values to a variable with only digits in its name; they can only be set by pattern matching.</li>
#   <li>In nested conditionals, each conditional has its own set of values for the set of numbered variables. At the end of an interior statement, the numbered variables are reset to their previous values for the remainder of the outside statement. (This causes conditional statements to act like <a href="https://docs.puppet.com/puppet/latest/reference/lang_scope.html#local-scopes">local scopes</a>, but only with regard to the numbered variables.)</li>
# </ul>

  $9 = 'stub'

