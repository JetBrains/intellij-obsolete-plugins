# <h2>clientcert</h2>
# <p>Puppet agent and Puppet apply both add several extra pieces of info to their facts before requesting or compiling a catalog. Like other facts, these are available as either top-scope variables or elements in the <code>$facts</code> hash.</p>
# <p><code>$clientcert</code> — the value of the node’s <a href="https://docs.puppet.com/puppet/latest/reference/configuration.html#certname"><code>certname</code> setting</a>. (This is self-reported; for the verified certificate name, use <code>$trusted['certname']</code>.)</p>

$clientcert = 'stub'

# <h2>clientversion</h2>
# <p>Puppet agent and Puppet apply both add several extra pieces of info to their facts before requesting or compiling a catalog. Like other facts, these are available as either top-scope variables or elements in the <code>$facts</code> hash.</p>
# <p><code>$clientversion</code> — the current version of Puppet agent.</p>

$clientversion = 'stub'

# <h2>clientnoop</h2>
# <p>Puppet agent and Puppet apply both add several extra pieces of info to their facts before requesting or compiling a catalog. Like other facts, these are available as either top-scope variables or elements in the <code>$facts</code> hash.</p>
# <p><code>$clientnoop</code> — the value of the node’s <a href="https://docs.puppet.com/puppet/latest/reference/configuration.html#noop"><code>noop</code> setting</a> (true or false) at the time of the run.</p>

$clientnoop = 'stub'

# <h2>agent_specified_environment</h2>
# <p>Puppet agent and Puppet apply both add several extra pieces of info to their facts before requesting or compiling a catalog. Like other facts, these are available as either top-scope variables or elements in the <code>$facts</code> hash.</p>
# <p><code>$agent_specified_environment</code> — the value of the node’s <a href="https://docs.puppet.com/puppet/latest/reference/configuration.html#environment"><code>environment</code> setting</a>. If the Puppet master’s node classifier specified an environment for the node, <code>$agent_specified_environment</code> and <code>$environment</code> can have different values.</p>
# <p>If no value was set for the <code>environment</code> setting (in puppet.conf or with <code>--environment</code>), the value of <code>$agent_specified_environment</code> will be <code>undef</code>. (That is, it won’t default to <code>production</code> like the setting does.)</p>

$agent_specified_environment = 'stub'

# <h2>aio_agent_version</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the version of the puppet-agent package that installed facter.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: use the compile-time enabled version definition.</li>
# </ul>

$aio_agent_version = 'stub'

# <h2>augeas</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return information about augeas.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>version</code> (string) — The version of augparse.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: query augparse for augeas metadata.</li>
# </ul>

$augeas = 'stub'

# <h2>cloud</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Information about the cloud instance of the node. This is currently only populated on Linux nodes running in Microsoft Azure.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>provider</code> (string) — The cloud provider for the node.</li>
# </ul>

$cloud = 'stub'

# <h2>disks</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the disk (block) devices attached to the system.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>&lt;devicename&gt;</code> (map) — Represents a disk or block device.
#     <ul>
#       <li><code>model</code> (string) — The model of the disk or block device.</li>
#       <li><code>product</code> (string) — The product name of the disk or block device.</li>
#       <li><code>size</code> (string) — The display size of the disk or block device (e.g. “1 GiB”).</li>
#       <li><code>size_bytes</code> (integer) — The size of the disk or block device, in bytes.</li>
#       <li><code>vendor</code> (string) — The vendor of the disk or block device.</li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>AIX: query the ODM for all disk devices</li>
#   <li>Linux: parse the contents of <code>/sys/block/&lt;device&gt;/</code>.</li>
#   <li>Solaris: use the <code>kstat</code> function to query disk information.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$disks = 'stub'

# <h2>dmi</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system management information.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>bios</code> (map) — The system BIOS information.
#     <ul>
#       <li><code>release_date</code> (string) — The release date of the system BIOS.</li>
#       <li><code>vendor</code> (string) — The vendor of the system BIOS.</li>
#       <li><code>version</code> (string) — The version of the system BIOS.</li>
#     </ul>
#   </li>
#   <li><code>board</code> (map) — The system board information.
#     <ul>
#       <li><code>asset_tag</code> (string) — The asset tag of the system board.</li>
#       <li><code>manufacturer</code> (string) — The manufacturer of the system board.</li>
#       <li><code>product</code> (string) — The product name of the system board.</li>
#       <li><code>serial_number</code> (string) — The serial number of the system board.</li>
#     </ul>
#   </li>
#   <li><code>chassis</code> (map) — The system chassis information.
#     <ul>
#       <li><code>asset_tag</code> (string) — The asset tag of the system chassis.</li>
#       <li><code>type</code> (string) — The type of the system chassis.</li>
#     </ul>
#   </li>
#   <li><code>manufacturer</code> (string) — The system manufacturer.</li>
#   <li><code>product</code> (map) — The system product information.
#     <ul>
#       <li><code>name</code> (string) — The product name of the system.</li>
#       <li><code>serial_number</code> (string) — The product serial number of the system.</li>
#       <li><code>uuid</code> (string) — The product unique identifier of the system.</li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/</code> to retrieve system management information.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve system management information.</li>
#   <li>Solaris: use the <code>smbios</code>, <code>prtconf</code>, and <code>uname</code> utilities to retrieve system management information.</li>
#   <li>Windows: use WMI to retrieve system management information.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$dmi = 'stub'

# <h2>ec2_metadata</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Amazon Elastic Compute Cloud (EC2) instance metadata.
# Please see the <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html">EC2 instance metadata documentation</a> for the contents of this fact.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>EC2: query the EC2 metadata endpoint and parse the response.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>All platforms: <code>libfacter</code> must be built with <code>libcurl</code> support.</li>
# </ul>

$ec2_metadata = 'stub'

# <h2>ec2_userdata</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Amazon Elastic Compute Cloud (EC2) instance user data.
# Please see the <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html">EC2 instance user data documentation</a> for the contents of this fact.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>EC2: query the EC2 user data endpoint and parse the response.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>All platforms: <code>libfacter</code> must be built with <code>libcurl</code> support.</li>
# </ul>

$ec2_userdata = 'stub'

# <h2>env_windows_installdir</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the path of the directory in which Puppet was installed.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Windows: This fact is specific to the Windows MSI generated environment, and is</li>
#   <li>set using the <code>environment.bat</code> script that configures the runtime environment</li>
#   <li>for all Puppet executables. Please see <a href="https://github.com/puppetlabs/puppet_for_the_win/commit/0cc32c1a09550c13d725b200d3c0cc17d93ec262">the original commit in the puppet_for_the_win repo</a> for more information.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>This fact is specific to Windows, and will not resolve on any other platform.</li>
# </ul>

$env_windows_installdir = 'stub'

# <h2>facterversion</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the version of facter.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: use the built-in version of libfacter.</li>
# </ul>

$facterversion = 'stub'

# <h2>filesystems</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the usable file systems for block or disk devices.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>AIX: parse the contents of <code>/etc/vfs</code> to retrieve the usable file systems.</li>
#   <li>Linux: parse the contents of <code>/proc/filesystems</code> to retrieve the usable file systems.</li>
#   <li>Mac OSX: use the <code>getfsstat</code> function to retrieve the usable file systems.</li>
#   <li>Solaris: use the <code>sysdef</code> utility to retrieve the usable file systems.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: The proc file system must be mounted.</li>
#   <li>Mac OSX: The usable file systems is limited to the file system of mounted devices.</li>
# </ul>

$filesystems = 'stub'

# <h2>gce</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Google Compute Engine (GCE) metadata.
# Please see the <a href="https://cloud.google.com/compute/docs/metadata">GCE metadata documentation</a> for the contents of this fact.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>GCE: query the GCE metadata endpoint and parse the response.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>All platforms: <code>libfacter</code> must be built with <code>libcurl</code> support.</li>
# </ul>

$gce = 'stub'

# <h2>identity</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the identity information of the user running facter.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>gid</code> (integer) — The group identifier of the user running facter.</li>
#   <li><code>group</code> (string) — The group name of the user running facter.</li>
#   <li><code>uid</code> (integer) — The user identifier of the user running facter.</li>
#   <li><code>user</code> (string) — The user name of the user running facter.</li>
#   <li><code>privileged</code> (boolean) — True if facter is running as a privileged process or false if not.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>getegid</code>, <code>getpwuid_r</code>, <code>geteuid</code>, and <code>getgrgid_r</code> functions to retrieve the identity information; use the result of the <code>geteuid() == 0</code> test as the value of the privileged element</li>
#   <li>Windows: use the <code>GetUserNameExW</code> function to retrieve the identity information; use the <code>GetTokenInformation</code> to get the current process token elevation status and use it as the value of the privileged element on versions of Windows supporting the token elevation, on older versions of Windows use the <code>CheckTokenMembership</code> to test whether the well known local Administrators group SID is enabled in the current thread impersonation token and use the test result as the value of the privileged element</li>
# </ul>

$identity = 'stub'

# <h2>is_virtual</h2>
# <p><strong>Type:</strong> boolean</p>
# <p><strong>Purpose:</strong></p>
# <p>Return whether or not the host is a virtual machine.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use procfs or utilities such as <code>vmware</code> and <code>virt-what</code> to retrieve virtual machine status.</li>
#   <li>Mac OSX: use the system profiler to retrieve virtual machine status.</li>
#   <li>Solaris: use the <code>zonename</code> utility to retrieve virtual machine status.</li>
#   <li>Windows: use WMI to retrieve virtual machine status.</li>
# </ul>

$is_virtual = 'stub'

# <h2>kernel</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the kernel’s name.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>uname</code> function to retrieve the kernel name.</li>
#   <li>Windows: use the value of <code>windows</code> for all Windows versions.</li>
# </ul>

$kernel = 'stub'

# <h2>kernelmajversion</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the kernel’s major version.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>uname</code> function to retrieve the kernel’s major version.</li>
#   <li>Windows: use the file version of <code>kernel32.dll</code> to retrieve the kernel’s major version.</li>
# </ul>

$kernelmajversion = 'stub'

# <h2>kernelrelease</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the kernel’s release.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>uname</code> function to retrieve the kernel’s release.</li>
#   <li>Windows: use the file version of <code>kernel32.dll</code> to retrieve the kernel’s release.</li>
# </ul>

$kernelrelease = 'stub'

# <h2>kernelversion</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the kernel’s version.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>uname</code> function to retrieve the kernel’s version.</li>
#   <li>Windows: use the file version of <code>kernel32.dll</code> to retrieve the kernel’s version.</li>
# </ul>

$kernelversion = 'stub'

# <h2>ldom</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return Solaris LDom information from the <code>virtinfo</code> utility.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>virtinfo</code> utility to retrieve LDom information.</li>
# </ul>

$ldom = 'stub'

# <h2>load_averages</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the load average over the last 1, 5 and 15 minutes.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>1m</code> (double) — The system load average over the last minute.</li>
#   <li><code>5m</code> (double) — The system load average over the last 5 minutes.</li>
#   <li><code>15m</code> (double) — The system load average over the last 15 minutes.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use <code>getloadavg</code> function to retrieve the system load averages.</li>
# </ul>

$load_averages = 'stub'

# <h2>memory</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system memory information.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>swap</code> (map) — Represents information about swap memory.
#     <ul>
#       <li><code>available</code> (string) — The display size of the available amount of swap memory (e.g. “1 GiB”).</li>
#       <li><code>available_bytes</code> (integer) — The size of the available amount of swap memory, in bytes.</li>
#       <li><code>capacity</code> (string) — The capacity percentage (0% is empty, 100% is full).</li>
#       <li><code>encrypted</code> (boolean) — True if the swap is encrypted or false if not.</li>
#       <li><code>total</code> (string) — The display size of the total amount of swap memory (e.g. “1 GiB”).</li>
#       <li><code>total_bytes</code> (integer) — The size of the total amount of swap memory, in bytes.</li>
#       <li><code>used</code> (string) — The display size of the used amount of swap memory (e.g. “1 GiB”).</li>
#       <li><code>used_bytes</code> (integer) — The size of the used amount of swap memory, in bytes.</li>
#     </ul>
#   </li>
#   <li><code>system</code> (map) — Represents information about system memory.
#     <ul>
#       <li><code>available</code> (string) — The display size of the available amount of system memory (e.g. “1 GiB”).</li>
#       <li><code>available_bytes</code> (integer) — The size of the available amount of system memory, in bytes.</li>
#       <li><code>capacity</code> (string) — The capacity percentage (0% is empty, 100% is full).</li>
#       <li><code>total</code> (string) — The display size of the total amount of system memory (e.g. “1 GiB”).</li>
#       <li><code>total_bytes</code> (integer) — The size of the total amount of system memory, in bytes.</li>
#       <li><code>used</code> (string) — The display size of the used amount of system memory (e.g. “1 GiB”).</li>
#       <li><code>used_bytes</code> (integer) — The size of the used amount of system memory, in bytes.</li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the system memory information.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the system memory information.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the system memory information.</li>
#   <li>Windows: use the <code>GetPerformanceInfo</code> function to retrieve the system memory information.</li>
# </ul>

$memory = 'stub'

# <h2>mountpoints</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the current mount points of the system.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>&lt;mountpoint&gt;</code> (map) — Represents a mount point.
#     <ul>
#       <li><code>available</code> (string) — The display size of the available space (e.g. “1 GiB”).</li>
#       <li><code>available_bytes</code> (integer) — The size of the available space, in bytes.</li>
#       <li><code>capacity</code> (string) — The capacity percentage (0% is empty, 100% is full).</li>
#       <li><code>device</code> (string) — The name of the mounted device.</li>
#       <li><code>filesystem</code> (string) — The file system of the mounted device.</li>
#       <li><code>options</code> (array) — The mount options.</li>
#       <li><code>size</code> (string) — The display size of the total space (e.g. “1 GiB”).</li>
#       <li><code>size_bytes</code> (integer) — The size of the total space, in bytes.</li>
#       <li><code>used</code> (string) — The display size of the used space (e.g. “1 GiB”).</li>
#       <li><code>used_bytes</code> (integer) — The size of the used space, in bytes.</li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>AIX: use the <code>mntctl</code> function to retrieve the mount points.</li>
#   <li>Linux: use the <code>setmntent</code> function to retrieve the mount points.</li>
#   <li>Mac OSX: use the <code>getfsstat</code> function to retrieve the mount points.</li>
#   <li>Solaris: parse the contents of <code>/etc/mnttab</code> to retrieve the mount points.</li>
# </ul>

$mountpoints = 'stub'

# <h2>networking</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the networking information for the system.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>dhcp</code> (ip) — The address of the DHCP server for the default interface.</li>
#   <li><code>domain</code> (string) — The domain name of the system.</li>
#   <li><code>fqdn</code> (string) — The fully-qualified domain name of the system.</li>
#   <li><code>hostname</code> (string) — The host name of the system.</li>
#   <li><code>interfaces</code> (map) — The network interfaces of the system.
#     <ul>
#       <li><code>&lt;interface&gt;</code> (map) — Represents a network interface.
#         <ul>
#           <li><code>bindings</code> (array) — The array of IPv4 address bindings for the interface.</li>
#           <li><code>bindings6</code> (array) — The array of IPv6 address bindings for the interface.</li>
#           <li><code>dhcp</code> (ip) — The DHCP server for the network interface.</li>
#           <li><code>ip</code> (ip) — The IPv4 address for the network interface.</li>
#           <li><code>ip6</code> (ip6) — The IPv6 address for the network interface.</li>
#           <li><code>mac</code> (mac) — The MAC address for the network interface.</li>
#           <li><code>mtu</code> (integer) — The Maximum Transmission Unit (MTU) for the network interface.</li>
#           <li><code>netmask</code> (ip) — The IPv4 netmask for the network interface.</li>
#           <li><code>netmask6</code> (ip6) — The IPv6 netmask for the network interface.</li>
#           <li><code>network</code> (ip) — The IPv4 network for the network interface.</li>
#           <li><code>network6</code> (ip6) — The IPv6 network for the network interface.</li>
#         </ul>
#       </li>
#     </ul>
#   </li>
#   <li><code>ip</code> (ip) — The IPv4 address of the default network interface.</li>
#   <li><code>ip6</code> (ip6) — The IPv6 address of the default network interface.</li>
#   <li><code>mac</code> (mac) — The MAC address of the default network interface.</li>
#   <li><code>mtu</code> (integer) — The Maximum Transmission Unit (MTU) of the default network interface.</li>
#   <li><code>netmask</code> (ip) — The IPv4 netmask of the default network interface.</li>
#   <li><code>netmask6</code> (ip6) — The IPv6 netmask of the default network interface.</li>
#   <li><code>network</code> (ip) — The IPv4 network of the default network interface.</li>
#   <li><code>network6</code> (ip6) — The IPv6 network of the default network interface.</li>
#   <li><code>primary</code> (string) — The name of the primary interface.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interfaces.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interfaces.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interfaces.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interfaces.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Windows Server 2003: the <code>GetAdaptersInfo</code> function is used for DHCP and netmask lookup. This function does not support IPv6 netmasks.</li>
# </ul>

$networking = 'stub'

# <h2>os</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return information about the host operating system.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>architecture</code> (string) — The operating system’s hardware architecture.</li>
#   <li><code>distro</code> (map) — Represents information about a Linux distribution.
#     <ul>
#       <li><code>codename</code> (string) — The code name of the Linux distribution.</li>
#       <li><code>description</code> (string) — The description of the Linux distribution.</li>
#       <li><code>id</code> (string) — The identifier of the Linux distribution.</li>
#       <li><code>release</code> (map) — Represents information about a Linux distribution release.
#         <ul>
#           <li><code>full</code> (string) — The full release of the Linux distribution.</li>
#           <li><code>major</code> (string) — The major release of the Linux distribution.</li>
#           <li><code>minor</code> (string) — The minor release of the Linux distribution.</li>
#         </ul>
#       </li>
#       <li><code>specification</code> (string) — The Linux Standard Base (LSB) release specification.</li>
#     </ul>
#   </li>
#   <li><code>family</code> (string) — The operating system family.</li>
#   <li><code>hardware</code> (string) — The operating system’s hardware model.</li>
#   <li><code>macosx</code> (map) — Represents information about Mac OSX.
#     <ul>
#       <li><code>build</code> (string) — The Mac OSX build version.</li>
#       <li><code>product</code> (string) — The Mac OSX product name.</li>
#       <li><code>version</code> (map) — Represents information about the Mac OSX version.
#         <ul>
#           <li><code>full</code> (string) — The full Mac OSX version number.</li>
#           <li><code>major</code> (string) — The major Mac OSX version number.</li>
#           <li><code>minor</code> (string) — The minor Mac OSX version number.</li>
#         </ul>
#       </li>
#     </ul>
#   </li>
#   <li><code>name</code> (string) — The operating system’s name.</li>
#   <li><code>release</code> (map) — Represents the operating system’s release.
#     <ul>
#       <li><code>full</code> (string) — The full operating system release.</li>
#       <li><code>major</code> (string) — The major release of the operating system.</li>
#       <li><code>minor</code> (string) — The minor release of the operating system.</li>
#     </ul>
#   </li>
#   <li><code>selinux</code> (map) — Represents information about Security-Enhanced Linux (SELinux).
#     <ul>
#       <li><code>config_mode</code> (string) — The configured SELinux mode.</li>
#       <li><code>config_policy</code> (string) — The configured SELinux policy.</li>
#       <li><code>current_mode</code> (string) — The current SELinux mode.</li>
#       <li><code>enabled</code> (boolean) — True if SELinux is enabled or false if not.</li>
#       <li><code>enforced</code> (boolean) — True if SELinux policy is enforced or false if not.</li>
#       <li><code>policy_version</code> (string) — The version of the SELinux policy.</li>
#     </ul>
#   </li>
#   <li><code>windows</code> (map) — Represents information about Windows.
#     <ul>
#       <li><code>system32</code> (string) — The path to the System32 directory.</li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility and parse the contents of release files in <code>/etc</code> to retrieve the OS information.</li>
#   <li>OSX: use the <code>sw_vers</code> utility to retrieve the OS information.</li>
#   <li>Solaris: parse the contents of <code>/etc/release</code> to retrieve the OS information.</li>
#   <li>Windows: use WMI to retrieve the OS information.</li>
# </ul>

$os = 'stub'

# <h2>partitions</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the disk partitions of the system.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>&lt;partition&gt;</code> (map) — Represents a disk partition.
#     <ul>
#       <li><code>filesystem</code> (string) — The file system of the partition.</li>
#       <li><code>label</code> (string) — The label of the partition.</li>
#       <li><code>mount</code> (string) — The mount point of the partition (if mounted).</li>
#       <li><code>partlabel</code> (string) — The label of a GPT partition.</li>
#       <li><code>partuuid</code> (string) — The unique identifier of a GPT partition.</li>
#       <li><code>size</code> (string) — The display size of the partition (e.g. “1 GiB”).</li>
#       <li><code>size_bytes</code> (integer) — The size of the partition, in bytes.</li>
#       <li><code>uuid</code> (string) — The unique identifier of a partition.</li>
#       <li><code>backing_file</code> (string) — The path to the file backing the partition.</li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>AIX: use the ODM to retrieve list of logical volumes; use <code>lvm_querylv</code> function to get details</li>
#   <li>Linux: use <code>libblkid</code> to retrieve the disk partitions.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: <code>libfacter</code> must be built with <code>libblkid</code> support.</li>
# </ul>

$partitions = 'stub'

# <h2>path</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the PATH environment variable.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: retrieve the value of the PATH environment variable.</li>
# </ul>

$path = 'stub'

# <h2>processors</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return information about the system’s processors.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>count</code> (integer) — The count of logical processors.</li>
#   <li><code>isa</code> (string) — The processor instruction set architecture.</li>
#   <li><code>models</code> (array) — The processor model strings (one for each logical processor).</li>
#   <li><code>physicalcount</code> (integer) — The count of physical processors.</li>
#   <li><code>speed</code> (string) — The speed of the processors (e.g. “2.0 GHz”).</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents <code>/sys/devices/system/cpu/</code> and <code>/proc/cpuinfo</code> to retrieve the processor information.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the processor information.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the processor information.</li>
#   <li>Windows: use WMI to retrieve the processor information.</li>
# </ul>

$processors = 'stub'

# <h2>ruby</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return information about the Ruby loaded by facter.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>platform</code> (string) — The platform Ruby was built for.</li>
#   <li><code>sitedir</code> (string) — The path to Ruby’s site library directory.</li>
#   <li><code>version</code> (string) — The version of Ruby.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: Use <code>RbConfig</code>, <code>RUBY_PLATFORM</code>, and <code>RUBY_VERSION</code> to retrieve information about Ruby.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>All platforms: facter must be able to locate <code>libruby</code>.</li>
# </ul>

$ruby = 'stub'

# <h2>solaris_zones</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return information about Solaris zones.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>current</code> (string) — The name of the current Solaris zone.</li>
#   <li><code>zones</code> (map) — Represents the Solaris zones.
#     <ul>
#       <li><code>&lt;zonename&gt;</code> (map) — Represents a Solaris zone.
#         <ul>
#           <li><code>brand</code> (string) — The brand of the Solaris zone.</li>
#           <li><code>id</code> (string) — The id of the Solaris zone.</li>
#           <li><code>ip_type</code> (string) — The IP type of the Solaris zone.</li>
#           <li><code>path</code> (string) — The path of the Solaris zone.</li>
#           <li><code>status</code> (string) — The status of the Solaris zone.</li>
#           <li><code>uuid</code> (string) — The unique identifier of the Solaris zone.</li>
#         </ul>
#       </li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>zoneadm</code> and <code>zonename</code> utilities to retrieve information about the Solaris zones.</li>
# </ul>

$solaris_zones = 'stub'

# <h2>ssh</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return SSH public keys and fingerprints.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>dsa</code> (map) — Represents the public key and fingerprints for the DSA algorithm.
#     <ul>
#       <li><code>fingerprints</code> (map) — Represents fingerprint information.
#         <ul>
#           <li><code>sha1</code> (string) — The SHA1 fingerprint of the public key.</li>
#           <li><code>sha256</code> (string) — The SHA256 fingerprint of the public key.</li>
#         </ul>
#       </li>
#       <li><code>key</code> (string) — The DSA public key.</li>
#     </ul>
#   </li>
#   <li><code>ecdsa</code> (map) — Represents the public key and fingerprints for the ECDSA algorithm.
#     <ul>
#       <li><code>fingerprints</code> (map) — Represents fingerprint information.
#         <ul>
#           <li><code>sha1</code> (string) — The SHA1 fingerprint of the public key.</li>
#           <li><code>sha256</code> (string) — The SHA256 fingerprint of the public key.</li>
#         </ul>
#       </li>
#       <li><code>key</code> (string) — The ECDSA public key.</li>
#     </ul>
#   </li>
#   <li><code>ed25519</code> (map) — Represents the public key and fingerprints for the Ed25519 algorithm.
#     <ul>
#       <li><code>fingerprints</code> (map) — Represents fingerprint information.
#         <ul>
#           <li><code>sha1</code> (string) — The SHA1 fingerprint of the public key.</li>
#           <li><code>sha256</code> (string) — The SHA256 fingerprint of the public key.</li>
#         </ul>
#       </li>
#       <li><code>key</code> (string) — The Ed25519 public key.</li>
#     </ul>
#   </li>
#   <li><code>rsa</code> (map) — Represents the public key and fingerprints for the RSA algorithm.
#     <ul>
#       <li><code>fingerprints</code> (map) — Represents fingerprint information.
#         <ul>
#           <li><code>sha1</code> (string) — The SHA1 fingerprint of the public key.</li>
#           <li><code>sha256</code> (string) — The SHA256 fingerprint of the public key.</li>
#         </ul>
#       </li>
#       <li><code>key</code> (string) — The RSA public key.</li>
#     </ul>
#   </li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: parse SSH public key files and derive fingerprints.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>POSIX platforms: facter must be built with OpenSSL support.</li>
# </ul>

$ssh = 'stub'

# <h2>system_profiler</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return information from the Mac OSX system profiler.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>boot_mode</code> (string) — The boot mode.</li>
#   <li><code>boot_rom_version</code> (string) — The boot ROM version.</li>
#   <li><code>boot_volume</code> (string) — The boot volume.</li>
#   <li><code>computer_name</code> (string) — The name of the computer.</li>
#   <li><code>cores</code> (string) — The total number of processor cores.</li>
#   <li><code>hardware_uuid</code> (string) — The hardware unique identifier.</li>
#   <li><code>kernel_version</code> (string) — The version of the kernel.</li>
#   <li><code>l2_cache_per_core</code> (string) — The size of the processor per-core L2 cache.</li>
#   <li><code>l3_cache</code> (string) — The size of the processor L3 cache.</li>
#   <li><code>memory</code> (string) — The size of the system memory.</li>
#   <li><code>model_identifier</code> (string) — The identifier of the computer model.</li>
#   <li><code>model_name</code> (string) — The name of the computer model.</li>
#   <li><code>processor_name</code> (string) — The model name of the processor.</li>
#   <li><code>processor_speed</code> (string) — The speed of the processor.</li>
#   <li><code>processors</code> (string) — The total number of processors.</li>
#   <li><code>secure_virtual_memory</code> (string) — Whether or not secure virtual memory is enabled.</li>
#   <li><code>serial_number</code> (string) — The serial number of the computer.</li>
#   <li><code>smc_version</code> (string) — The System Management Controller (SMC) version.</li>
#   <li><code>system_version</code> (string) — The operating system version.</li>
#   <li><code>uptime</code> (string) — The uptime of the system.</li>
#   <li><code>username</code> (string) — The name of the user running facter.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Mac OSX: use the <code>system_profiler</code> utility to retrieve system profiler information.</li>
# </ul>

$system_profiler = 'stub'

# <h2>system_uptime</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system uptime information.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>days</code> (integer) — The number of complete days the system has been up.</li>
#   <li><code>hours</code> (integer) — The number of complete hours the system has been up.</li>
#   <li><code>seconds</code> (integer) — The number of total seconds the system has been up.</li>
#   <li><code>uptime</code> (string) — The full uptime string.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>sysinfo</code> function to retrieve the system uptime.</li>
#   <li>POSIX platforms: use the <code>uptime</code> utility to retrieve the system uptime.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the system uptime.</li>
#   <li>Windows: use WMI to retrieve the system uptime.</li>
# </ul>

$system_uptime = 'stub'

# <h2>timezone</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system timezone.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>localtime_r</code> function to retrieve the system timezone.</li>
#   <li>Windows: use the <code>localtime_s</code> function to retrieve the system timezone.</li>
# </ul>

$timezone = 'stub'

# <h2>virtual</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the hypervisor name for virtual machines or “physical” for physical machines.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use procfs or utilities such as <code>vmware</code> and <code>virt-what</code> to retrieve virtual machine name.</li>
#   <li>Mac OSX: use the system profiler to retrieve virtual machine name.</li>
#   <li>Solaris: use the <code>zonename</code> utility to retrieve virtual machine name.</li>
#   <li>Windows: use WMI to retrieve virtual machine name.</li>
# </ul>

$virtual = 'stub'

# <h2>xen</h2>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return metadata for the Xen hypervisor.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>domains</code> (array) — list of strings identifying active Xen domains.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use <code>/usr/lib/xen-common/bin/xen-toolstack</code> to locate xen admin commands if available, otherwise fallback to <code>/usr/sbin/xl</code> or <code>/usr/sbin/xm</code>. Use the found command to execute the <code>list</code> query.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>POSIX platforms: confined to Xen privileged virtual machines.</li>
# </ul>

$xen = 'stub'

# <h2>zfs_featurenumbers</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the comma-delimited feature numbers for ZFS.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>zfs</code> utility to retrieve the feature numbers for ZFS</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Solaris: the <code>zfs</code> utility must be present.</li>
# </ul>

$zfs_featurenumbers = 'stub'

# <h2>zfs_version</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the version for ZFS.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>zfs</code> utility to retrieve the version for ZFS</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Solaris: the <code>zfs</code> utility must be present.</li>
# </ul>

$zfs_version = 'stub'

# <h2>zpool_featurenumbers</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the comma-delimited feature numbers for ZFS storage pools.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>zpool</code> utility to retrieve the feature numbers for ZFS storage pools</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Solaris: the <code>zpool</code> utility must be present.</li>
# </ul>

$zpool_featurenumbers = 'stub'

# <h2>zpool_version</h2>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the version for ZFS storage pools.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>zpool</code> utility to retrieve the version for ZFS storage pools</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Solaris: the <code>zpool</code> utility must be present.</li>
# </ul>

$zpool_version = 'stub'

# <h2>architecture</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the operating system’s hardware architecture.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>uname</code> function to retrieve the OS hardware architecture.</li>
#   <li>Windows: use the <code>GetNativeSystemInfo</code> function to retrieve the OS hardware architecture.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Debian, Gentoo, kFreeBSD, and Ubuntu use “amd64” for “x86_64” and Gentoo uses “x86” for “i386”.</li>
# </ul>

$architecture = 'stub'

# <h2>augeasversion</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the version of augeas.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: query augparse for the augeas version.</li>
# </ul>

$augeasversion = 'stub'

# <h2>blockdevices</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return a comma-separated list of block devices.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/block/&lt;device&gt;/</code>.</li>
#   <li>Solaris: use the <code>kstat</code> function to query disk information.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$blockdevices = 'stub'

# <h2>bios_release_date</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the release date of the system BIOS.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/bios_date</code> to retrieve the system BIOS release date.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system BIOS release date.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$bios_release_date = 'stub'

# <h2>bios_vendor</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the vendor of the system BIOS.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/bios_vendor</code> to retrieve the system BIOS vendor.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system BIOS vendor.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$bios_vendor = 'stub'

# <h2>bios_version</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the version of the system BIOS.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/bios_version</code> to retrieve the system BIOS version.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system BIOS version.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$bios_version = 'stub'

# <h2>boardassettag</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system board asset tag.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/board_asset_tag</code> to retrieve the system board asset tag.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$boardassettag = 'stub'

# <h2>boardmanufacturer</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system board manufacturer.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/board_vendor</code> to retrieve the system board manufacturer.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$boardmanufacturer = 'stub'

# <h2>boardproductname</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system board product name.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/board_name</code> to retrieve the system board product name.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$boardproductname = 'stub'

# <h2>boardserialnumber</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system board serial number.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/board_serial</code> to retrieve the system board serial number.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$boardserialnumber = 'stub'

# <h2>chassisassettag</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system chassis asset tag.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/chassis_asset_tag</code> to retrieve the system chassis asset tag.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system chassis asset tag.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$chassisassettag = 'stub'

# <h2>chassistype</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system chassis type.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/chassis_type</code> to retrieve the system chassis type.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system chassis type.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$chassistype = 'stub'

# <h2>dhcp_servers</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> map</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the DHCP servers for the system.</p>
# <p><strong>Elements:</strong></p>
# <ul>
#   <li><code>&lt;interface&gt;</code> (ip) — The DHCP server for the interface.</li>
#   <li><code>system</code> (ip) — The DHCP server for the default interface.</li>
# </ul>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse <code>dhclient</code> lease files or use the <code>dhcpcd</code> utility to retrieve the DHCP servers.</li>
#   <li>Mac OSX: use the <code>ipconfig</code> utility to retrieve the DHCP servers.</li>
#   <li>Solaris: use the <code>dhcpinfo</code> utility to retrieve the DHCP servers.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> (Windows Server 2003: <code>GetAdaptersInfo</code>) function to retrieve the DHCP servers.</li>
# </ul>

$dhcp_servers = 'stub'

# <h2>domain</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the network domain of the system.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>getaddrinfo</code> function to retrieve the network domain.</li>
#   <li>Windows: query the registry to retrieve the network domain; falls back to the primary interface’s domain if not set in the registry.</li>
# </ul>

$domain = 'stub'

# <h2>fqdn</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the fully qualified domain name (FQDN) of the system.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>getaddrinfo</code> function to retrieve the FQDN or use host and domain names.</li>
#   <li>Windows: use the host and domain names to build the FQDN.</li>
# </ul>

$fqdn = 'stub'

# <h2>gid</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the group identifier (GID) of the user running facter.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>getegid</code> fuction to retrieve the group identifier.</li>
# </ul>

$gid = 'stub'

# <h2>hardwareisa</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the hardware instruction set architecture (ISA).</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use <code>uname</code> to retrieve the hardware ISA.</li>
#   <li>Windows: use WMI to retrieve the hardware ISA.</li>
# </ul>

$hardwareisa = 'stub'

# <h2>hardwaremodel</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the operating system’s hardware model.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>uname</code> function to retrieve the OS hardware model.</li>
#   <li>Windows: use the <code>GetNativeSystemInfo</code> function to retrieve the OS hardware model.</li>
# </ul>

$hardwaremodel = 'stub'

# <h2>hostname</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the host name of the system.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>gethostname</code> function to retrieve the host name</li>
#   <li>Windows: use the <code>GetComputerNameExW</code> function to retrieve the host name.</li>
# </ul>

$hostname = 'stub'

# <h2>id</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the user identifier (UID) of the user running facter.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: use the <code>geteuid</code> fuction to retrieve the user identifier.</li>
# </ul>

$id = 'stub'

# <h2>interfaces</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the comma-separated list of network interface names.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface names.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface names.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface names.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interface names.</li>
# </ul>

$interfaces = 'stub'

# <h2>ipaddress</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> ip</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the IPv4 address for the default network interface.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface address.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface address.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface address.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interface address.</li>
# </ul>

$ipaddress = 'stub'

# <h2>ipaddress6</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> ip6</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the IPv6 address for the default network interface.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface address.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface address.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface address.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interface address.</li>
# </ul>

$ipaddress6 = 'stub'

# <h2>lsbdistcodename</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Linux Standard Base (LSB) distribution code name.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility to retrieve the LSB distribution code name.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Requires that the <code>lsb_release</code> utility be installed.</li>
# </ul>

$lsbdistcodename = 'stub'

# <h2>lsbdistdescription</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Linux Standard Base (LSB) distribution description.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility to retrieve the LSB distribution description.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Requires that the <code>lsb_release</code> utility be installed.</li>
# </ul>

$lsbdistdescription = 'stub'

# <h2>lsbdistid</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Linux Standard Base (LSB) distribution identifier.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility to retrieve the LSB distribution identifier.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Requires that the <code>lsb_release</code> utility be installed.</li>
# </ul>

$lsbdistid = 'stub'

# <h2>lsbdistrelease</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Linux Standard Base (LSB) distribution release.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility to retrieve the LSB distribution release.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Requires that the <code>lsb_release</code> utility be installed.</li>
# </ul>

$lsbdistrelease = 'stub'

# <h2>lsbmajdistrelease</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Linux Standard Base (LSB) major distribution release.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility to retrieve the LSB major distribution release.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Requires that the <code>lsb_release</code> utility be installed.</li>
# </ul>

$lsbmajdistrelease = 'stub'

# <h2>lsbminordistrelease</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Linux Standard Base (LSB) minor distribution release.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility to retrieve the LSB minor distribution release.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Requires that the <code>lsb_release</code> utility be installed.</li>
# </ul>

$lsbminordistrelease = 'stub'

# <h2>lsbrelease</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Linux Standard Base (LSB) release.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>lsb_release</code> utility to retrieve the LSB release.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: Requires that the <code>lsb_release</code> utility be installed.</li>
# </ul>

$lsbrelease = 'stub'

# <h2>macaddress</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> mac</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the MAC address for the default network interface.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface address.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface address.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface address.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interface address.</li>
# </ul>

$macaddress = 'stub'

# <h2>macosx_buildversion</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Mac OSX build version.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Mac OSX: use the <code>sw_vers</code> utility to retrieve the Mac OSX build version.</li>
# </ul>

$macosx_buildversion = 'stub'

# <h2>macosx_productname</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Mac OSX product name.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Mac OSX: use the <code>sw_vers</code> utility to retrieve the Mac OSX product name.</li>
# </ul>

$macosx_productname = 'stub'

# <h2>macosx_productversion</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Mac OSX product version.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Mac OSX: use the <code>sw_vers</code> utility to retrieve the Mac OSX product version.</li>
# </ul>

$macosx_productversion = 'stub'

# <h2>macosx_productversion_major</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Mac OSX product major version.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Mac OSX: use the <code>sw_vers</code> utility to retrieve the Mac OSX product major version.</li>
# </ul>

$macosx_productversion_major = 'stub'

# <h2>macosx_productversion_minor</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Mac OSX product minor version.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Mac OSX: use the <code>sw_vers</code> utility to retrieve the Mac OSX product minor version.</li>
# </ul>

$macosx_productversion_minor = 'stub'

# <h2>manufacturer</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system manufacturer.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/sys_vendor</code> to retrieve the system manufacturer.</li>
#   <li>Solaris: use the <code>prtconf</code> utility to retrieve the system manufacturer.</li>
#   <li>Windows: use WMI to retrieve the system manufacturer.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$manufacturer = 'stub'

# <h2>memoryfree</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the display size of the free system memory (e.g. “1 GiB”).</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the free system memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the free system memory.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the free system memory.</li>
#   <li>Windows: use the <code>GetPerformanceInfo</code> function to retrieve the free system memory.</li>
# </ul>

$memoryfree = 'stub'

# <h2>memoryfree_mb</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> double</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the size of the free system memory, in mebibytes.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the free system memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the free system memory.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the free system memory.</li>
#   <li>Windows: use the <code>GetPerformanceInfo</code> function to retrieve the free system memory.</li>
# </ul>

$memoryfree_mb = 'stub'

# <h2>memorysize</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the display size of the total system memory (e.g. “1 GiB”).</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the total system memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the total system memory.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the total system memory.</li>
#   <li>Windows: use the <code>GetPerformanceInfo</code> function to retrieve the total system memory.</li>
# </ul>

$memorysize = 'stub'

# <h2>memorysize_mb</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> double</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the size of the total system memory, in mebibytes.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the total system memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the total system memory.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the total system memory.</li>
#   <li>Windows: use the <code>GetPerformanceInfo</code> function to retrieve the total system memory.</li>
# </ul>

$memorysize_mb = 'stub'

# <h2>netmask</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> ip</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the IPv4 netmask for the default network interface.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface netmask.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface netmask.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface netmask.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> (Windows Server 2003: <code>GetAdaptersInfo</code>) function to retrieve the network interface netmask.</li>
# </ul>

$netmask = 'stub'

# <h2>netmask6</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> ip6</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the IPv6 netmask for the default network interface.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface netmask.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface netmask.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface netmask.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interface netmask.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Windows Server 2003: IPv6 netmasks are not supported.</li>
# </ul>

$netmask6 = 'stub'

# <h2>network</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> ip</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the IPv4 network for the default network interface.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface network.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface network.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface network.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interface network.</li>
# </ul>

$network = 'stub'

# <h2>network6</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> ip6</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the IPv6 network for the default network interface.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>getifaddrs</code> function to retrieve the network interface network.</li>
#   <li>Mac OSX: use the <code>getifaddrs</code> function to retrieve the network interface network.</li>
#   <li>Solaris: use the <code>ioctl</code> function to retrieve the network interface network.</li>
#   <li>Windows: use the <code>GetAdaptersAddresses</code> function to retrieve the network interface network.</li>
# </ul>

$network6 = 'stub'

# <h2>operatingsystem</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the name of the operating system.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: default to the kernel name.</li>
#   <li>Linux: use various release files in <code>/etc</code> to retrieve the OS name.</li>
# </ul>

$operatingsystem = 'stub'

# <h2>operatingsystemmajrelease</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the major release of the operating system.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: default to the major version of the kernel release.</li>
#   <li>Linux: parse the contents of release files in <code>/etc</code> to retrieve the OS major release.</li>
#   <li>Solaris: parse the contents of <code>/etc/release</code> to retrieve the OS major release.</li>
#   <li>Windows: use WMI to retrieve the OS major release.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: for Ubuntu, the major release is X.Y (e.g. “10.4”).</li>
# </ul>

$operatingsystemmajrelease = 'stub'

# <h2>operatingsystemrelease</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the release of the operating system.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: default to the kernel release.</li>
#   <li>Linux: parse the contents of release files in <code>/etc</code> to retrieve the OS release.</li>
#   <li>Solaris: parse the contents of <code>/etc/release</code> to retrieve the OS release.</li>
#   <li>Windows: use WMI to retrieve the OS release.</li>
# </ul>

$operatingsystemrelease = 'stub'

# <h2>osfamily</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the family of the operating system.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: default to the kernel name.</li>
#   <li>Linux: map various Linux distributions to their base distribution (e.g. Ubuntu is a “Debian” distro).</li>
#   <li>Solaris: map various Solaris-based operating systems to the “Solaris” family.</li>
#   <li>Windows: use “windows” as the family name.</li>
# </ul>

$osfamily = 'stub'

# <h2>physicalprocessorcount</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> integer</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the count of physical processors.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents <code>/sys/devices/system/cpu/</code> and <code>/proc/cpuinfo</code> to retrieve the count of physical processors.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the count of physical processors.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the count of physical processors.</li>
#   <li>Windows: use WMI to retrieve the count of physical processors.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$physicalprocessorcount = 'stub'

# <h2>processorcount</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> integer</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the count of logical processors.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents <code>/sys/devices/system/cpu/</code> and <code>/proc/cpuinfo</code> to retrieve the count of logical processors.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the count of logical processors.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the count of logical processors.</li>
#   <li>Windows: use WMI to retrieve the count of logical processors.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$processorcount = 'stub'

# <h2>productname</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system product name.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/product_name</code> to retrieve the system product name.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the system product name.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system product name.</li>
#   <li>Windows: use WMI to retrieve the system product name.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$productname = 'stub'

# <h2>rubyplatform</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the platform Ruby was built for.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: use <code>RUBY_PLATFORM</code> from the Ruby loaded by facter.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>All platforms: facter must be able to locate <code>libruby</code>.</li>
# </ul>

$rubyplatform = 'stub'

# <h2>rubysitedir</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the path to Ruby’s site library directory.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: use <code>RbConfig</code> from the Ruby loaded by facter.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>All platforms: facter must be able to locate <code>libruby</code>.</li>
# </ul>

$rubysitedir = 'stub'

# <h2>rubyversion</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the version of Ruby.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>All platforms: use <code>RUBY_VERSION</code> from the Ruby loaded by facter.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>All platforms: facter must be able to locate <code>libruby</code>.</li>
# </ul>

$rubyversion = 'stub'

# <h2>selinux</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> boolean</p>
# <p><strong>Purpose:</strong></p>
# <p>Return whether Security-Enhanced Linux (SELinux) is enabled.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/self/mounts</code> to determine if SELinux is enabled.</li>
# </ul>

$selinux = 'stub'

# <h2>selinux_config_mode</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the configured Security-Enhanced Linux (SELinux) mode.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/etc/selinux/config</code> to retrieve the configured SELinux mode.</li>
# </ul>

$selinux_config_mode = 'stub'

# <h2>selinux_config_policy</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the configured Security-Enhanced Linux (SELinux) policy.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/etc/selinux/config</code> to retrieve the configured SELinux policy.</li>
# </ul>

$selinux_config_policy = 'stub'

# <h2>selinux_current_mode</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the current Security-Enhanced Linux (SELinux) mode.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>&lt;mountpoint&gt;/enforce</code> to retrieve the current SELinux mode.</li>
# </ul>

$selinux_current_mode = 'stub'

# <h2>selinux_enforced</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> boolean</p>
# <p><strong>Purpose:</strong></p>
# <p>Return whether Security-Enhanced Linux (SELinux) is enforced.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>&lt;mountpoint&gt;/enforce</code> to retrieve the current SELinux mode.</li>
# </ul>

$selinux_enforced = 'stub'

# <h2>selinux_policyversion</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the Security-Enhanced Linux (SELinux) policy version.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>&lt;mountpoint&gt;/policyvers</code> to retrieve the SELinux policy version.</li>
# </ul>

$selinux_policyversion = 'stub'

# <h2>serialnumber</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system product serial number.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/product_name</code> to retrieve the system product serial number.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system product serial number.</li>
#   <li>Windows: use WMI to retrieve the system product serial number.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$serialnumber = 'stub'

# <h2>swapencrypted</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> boolean</p>
# <p><strong>Purpose:</strong></p>
# <p>Return whether or not the swap is encrypted.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve swap encryption status.</li>
# </ul>

$swapencrypted = 'stub'

# <h2>swapfree</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the display size of the free swap memory (e.g. “1 GiB”).</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the free swap memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the free swap memory.</li>
#   <li>Solaris: use the <code>swapctl</code> function to retrieve the free swap memory.</li>
# </ul>

$swapfree = 'stub'

# <h2>swapfree_mb</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> double</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the size of the free swap memory, in mebibytes.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the free swap memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the free swap memory.</li>
#   <li>Solaris: use the <code>swapctl</code> function to retrieve the free swap memory.</li>
# </ul>

$swapfree_mb = 'stub'

# <h2>swapsize</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the display size of the total swap memory (e.g. “1 GiB”).</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the total swap memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the total swap memory.</li>
#   <li>Solaris: use the <code>swapctl</code> function to retrieve the total swap memory.</li>
# </ul>

$swapsize = 'stub'

# <h2>swapsize_mb</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> double</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the size of the total swap memory, in mebibytes.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/proc/meminfo</code> to retrieve the total swap memory.</li>
#   <li>Mac OSX: use the <code>sysctl</code> function to retrieve the total swap memory.</li>
#   <li>Solaris: use the <code>swapctl</code> function to retrieve the total swap memory.</li>
# </ul>

$swapsize_mb = 'stub'

# <h2>system32</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the path to the System32 directory on Windows.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Windows: use the <code>SHGetFolderPath</code> function to retrieve the path to the System32 directory.</li>
# </ul>

$system32 = 'stub'

# <h2>uptime</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system uptime.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>sysinfo</code> function to retrieve the system uptime.</li>
#   <li>POSIX platforms: use the <code>uptime</code> utility to retrieve the system uptime.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the system uptime.</li>
#   <li>Windows: use WMI to retrieve the system uptime.</li>
# </ul>

$uptime = 'stub'

# <h2>uptime_days</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> integer</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system uptime days.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>sysinfo</code> function to retrieve the system uptime days.</li>
#   <li>POSIX platforms: use the <code>uptime</code> utility to retrieve the system uptime days.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the system uptime days.</li>
#   <li>Windows: use WMI to retrieve the system uptime days.</li>
# </ul>

$uptime_days = 'stub'

# <h2>uptime_hours</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> integer</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system uptime hours.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>sysinfo</code> function to retrieve the system uptime hours.</li>
#   <li>POSIX platforms: use the <code>uptime</code> utility to retrieve the system uptime hours.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the system uptime hours.</li>
#   <li>Windows: use WMI to retrieve the system uptime hours.</li>
# </ul>

$uptime_hours = 'stub'

# <h2>uptime_seconds</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> integer</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system uptime seconds.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: use the <code>sysinfo</code> function to retrieve the system uptime seconds.</li>
#   <li>POSIX platforms: use the <code>uptime</code> utility to retrieve the system uptime seconds.</li>
#   <li>Solaris: use the <code>kstat</code> function to retrieve the system uptime seconds.</li>
#   <li>Windows: use WMI to retrieve the system uptime seconds.</li>
# </ul>

$uptime_seconds = 'stub'

# <h2>uuid</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the system product unique identifier.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Linux: parse the contents of <code>/sys/class/dmi/id/product_uuid</code> to retrieve the system product unique identifier.</li>
#   <li>Solaris: use the <code>smbios</code> utility to retrieve the system product unique identifier.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Linux: kernel 2.6+ is required due to the reliance on sysfs.</li>
# </ul>

$uuid = 'stub'

# <h2>xendomains</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return a list of comma-separated active Xen domain names.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>POSIX platforms: see the <code>xen</code> structured fact.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>POSIX platforms: confined to Xen privileged virtual machines.</li>
# </ul>

$xendomains = 'stub'

# <h2>zonename</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> string</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the name of the current Solaris zone.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>zonename</code> utility to retrieve the current zone name.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Solaris: the <code>zonename</code> utility must be present.</li>
# </ul>

$zonename = 'stub'

# <h2>zones</h2>
# <p>This legacy fact is hidden by default in Facter’s command-line output.</p>
# <p><strong>Type:</strong> integer</p>
# <p><strong>Purpose:</strong></p>
# <p>Return the count of Solaris zones.</p>
# <p><strong>Resolution:</strong></p>
# <ul>
#   <li>Solaris: use the <code>zoneadm</code> utility to retrieve the count of Solaris zones.</li>
# </ul>
# <p><strong>Caveats:</strong></p>
# <ul>
#   <li>Solaris: the <code>zoneadm</code> utility must be present.</li>
# </ul>
#         </div>
#         <blockquote><p style="font-size: 1.3em; text-align: center;"><a href="#content">↑ Back to top</a></p></blockquote>
#       </div>
#       <div class="sidebar col-xs-12 col-md-3 col-md-pull-9 col-lg-4 col-lg-pull-8">
#         <nav class="main" id="document-nav">

$zones = 'stub'




define stub__metaparams__ (
  # <h2>name</h2>
  # <p><code>$name</code> defaults to the value of $title, but users can optionally specify a different value when they declare an instance.
  # This is only useful for mimicking the behavior of a resource with a namevar, which is usually unnecessary. If you are
  # wondering whether to use <code>$name</code> or <code>$title</code>, use <code>$title</code>.</p>
  # <p>Unlike the other parameters, the values of <code>$title</code> and <code>$name</code> are already available inside the
  # parameter list. This means you can use <code>$title</code> as the default value (or part of the default value) for another attribute:</p>

  $name,

  # <h2>title</h2>
  # <p><code>$title</code> is always set to the title of the instance. Since it is guaranteed to be unique for each instance, it is useful
  # when making sure that contained resources are unique.</p>
  # <p>Unlike the other parameters, the values of <code>$title</code> and <code>$name</code> are already available inside the
  # parameter list. This means you can use <code>$title</code> as the default value (or part of the default value) for another attribute:</p>

  $title,

  # <h2>alias</h2>
  # <p>Creates an alias for the resource.  Puppet uses this internally when you
  # provide a symbolic title and an explicit namevar value:</p>
  # <pre><code>file { 'sshdconfig':
  #   path =&gt; $operatingsystem ? {
  #     solaris =&gt; '/usr/local/etc/ssh/sshd_config',
  #     default =&gt; '/etc/ssh/sshd_config',
  #   },
  #   source =&gt; '...'
  # }
  # service { 'sshd':
  #   subscribe =&gt; File['sshdconfig'],
  # }
  # </code></pre>
  # <p>When you use this feature, the parser sets <code>sshdconfig</code> as the title,
  # and the library sets that as an alias for the file so the dependency
  # lookup in <code>Service['sshd']</code> works.  You can use this metaparameter yourself,
  # but note that aliases generally only work for creating relationships; anything
  # else that refers to an existing resource (such as amending or overriding
  # resource attributes in an inherited class) must use the resource’s exact
  # title. For example, the following code will not work:</p>
  # <pre><code>file { '/etc/ssh/sshd_config':
  #   owner =&gt; root,
  #   group =&gt; root,
  #   alias =&gt; 'sshdconfig',
  # }
  # File['sshdconfig'] {
  #   mode =&gt; '0644',
  # }
  # </code></pre>
  # <p>There’s no way here for the Puppet parser to know that these two stanzas
  # should be affecting the same file.</p>

  $alias,

  # <h2>audit</h2>
  # <p>(This metaparameter is deprecated and will be ignored in a future release.)</p>
  # <p>Marks a subset of this resource’s unmanaged attributes for auditing. Accepts an
  # attribute name, an array of attribute names, or <code>all</code>.</p>
  # <p>Auditing a resource attribute has two effects: First, whenever a catalog
  # is applied with puppet apply or puppet agent, Puppet will check whether
  # that attribute of the resource has been modified, comparing its current
  # value to the previous run; any change will be logged alongside any actions
  # performed by Puppet while applying the catalog.</p>
  # <p>Secondly, marking a resource attribute for auditing will include that
  # attribute in inspection reports generated by puppet inspect; see the
  # puppet inspect documentation for more details.</p>
  # <p>Managed attributes for a resource can also be audited, but note that
  # changes made by Puppet will be logged as additional modifications. (I.e.
  # if a user manually edits a file whose contents are audited and managed,
  # puppet agent’s next two runs will both log an audit notice: the first run
  # will log the user’s edit and then revert the file to the desired state,
  # and the second run will log the edit made by Puppet.)</p>

  $audit,

  # <h2>before</h2>
  # <p>One or more resources that depend on this resource, expressed as
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_data_resource_reference.html">resource references</a>.
  # Multiple resources can be specified as an array of references. When this
  # attribute is present:</p>
  # <ul>
  #   <li>This resource will be applied <em>before</em> the dependent resource(s).</li>
  # </ul>
  # <p>This is one of the four relationship metaparameters, along with
  # <code>require</code>, <code>notify</code>, and <code>subscribe</code>. For more context, including the
  # alternate chaining arrow (<code>-&gt;</code> and <code>~&gt;</code>) syntax, see
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_relationships.html">the language page on relationships</a>.</p>

  $before,

  # <h2>consume</h2>
  # <p>Consume a capability resource.</p>
  # <p>The value of this parameter must be a reference to a capability resource,
  # or an array of such references. Each capability resource referenced here
  # must have been exported by another resource in the same environment.</p>
  # <p>The referenced capability resource(s) will be looked up, added to the
  # current node catalog, and processed following the underlying consumes
  # clause.</p>
  # <p>It is an error if this metaparameter references resources whose type is not
  # a capability type, or of there is no consumes clause for the type of the
  # current resource and the capability resource mentioned in this parameter.</p>
  # <p>For example:</p>
  # <p>define web(..) { .. }
  # Web consumes Sql { .. }
  # web { server:
  #   consume =&gt; Sql[my_db]
  # }</p>

  $consume,

  # <h2>export</h2>
  # <p>Export a capability resource.</p>
  # <p>The value of this parameter must be a reference to a capability resource,
  # or an array of such references. Each capability resource referenced here
  # will be instantiated in the node catalog and exported to consumers of this
  # resource. The title of the capability resource will be the title given in
  # the reference, and all other attributes of the resource will be filled
  # according to the corresponding produces statement.</p>
  # <p>It is an error if this metaparameter references resources whose type is not
  # a capability type, or of there is no produces clause for the type of the
  # current resource and the capability resource mentioned in this parameter.</p>
  # <p>For example:</p>
  # <p>define web(..) { .. }
  # Web produces Http { .. }
  # web { server:
  #   export =&gt; Http[main_server]
  # }</p>

  $export,

  # <h2>loglevel</h2>
  # <p>Sets the level that information will be logged.
  # The log levels have the biggest impact when logs are sent to
  # syslog (which is currently the default).</p>
  # <p>The order of the log levels, in decreasing priority, is:</p>
  # <ul>
  #   <li><code>crit</code></li>
  #   <li><code>emerg</code></li>
  #   <li><code>alert</code></li>
  #   <li><code>err</code></li>
  #   <li><code>warning</code></li>
  #   <li><code>notice</code></li>
  #   <li><code>info</code> / <code>verbose</code></li>
  #   <li><code>debug</code></li>
  # </ul>
  # <p>Valid values are <code>debug</code>, <code>info</code>, <code>notice</code>, <code>warning</code>, <code>err</code>, <code>alert</code>, <code>emerg</code>, <code>crit</code>, <code>verbose</code>.</p>

  $loglevel,

  # <h2>noop</h2>
  # <p>Whether to apply this resource in noop mode.</p>
  # <p>When applying a resource in noop mode, Puppet will check whether it is in sync,
  # like it does when running normally. However, if a resource attribute is not in
  # the desired state (as declared in the catalog), Puppet will take no
  # action, and will instead report the changes it <em>would</em> have made. These
  # simulated changes will appear in the report sent to the puppet master, or
  # be shown on the console if running puppet agent or puppet apply in the
  # foreground. The simulated changes will not send refresh events to any
  # subscribing or notified resources, although Puppet will log that a refresh
  # event <em>would</em> have been sent.</p>
  # <p><strong>Important note:</strong>
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/configuration.html#noop">The <code>noop</code> setting</a>
  # allows you to globally enable or disable noop mode, but it will <em>not</em> override
  # the <code>noop</code> metaparameter on individual resources. That is, the value of the
  # global <code>noop</code> setting will <em>only</em> affect resources that do not have an explicit
  # value set for their <code>noop</code> attribute.</p>
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $noop,

  # <h2>notify</h2>
  # <p>One or more resources that depend on this resource, expressed as
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_data_resource_reference.html">resource references</a>.
  # Multiple resources can be specified as an array of references. When this
  # attribute is present:</p>
  # <ul>
  #   <li>This resource will be applied <em>before</em> the notified resource(s).</li>
  #   <li>If Puppet makes changes to this resource, it will cause all of the
  # notified resources to <em>refresh.</em> (Refresh behavior varies by resource
  # type: services will restart, mounts will unmount and re-mount, etc. Not
  # all types can refresh.)</li>
  # </ul>
  # <p>This is one of the four relationship metaparameters, along with
  # <code>before</code>, <code>require</code>, and <code>subscribe</code>. For more context, including the
  # alternate chaining arrow (<code>-&gt;</code> and <code>~&gt;</code>) syntax, see
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_relationships.html">the language page on relationships</a>.</p>

  $notify,

  # <h2>require</h2>
  # <p>One or more resources that this resource depends on, expressed as
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_data_resource_reference.html">resource references</a>.
  # Multiple resources can be specified as an array of references. When this
  # attribute is present:</p>
  # <ul>
  #   <li>The required resource(s) will be applied <strong>before</strong> this resource.</li>
  # </ul>
  # <p>This is one of the four relationship metaparameters, along with
  # <code>before</code>, <code>notify</code>, and <code>subscribe</code>. For more context, including the
  # alternate chaining arrow (<code>-&gt;</code> and <code>~&gt;</code>) syntax, see
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_relationships.html">the language page on relationships</a>.</p>

  $require,

  # <h2>schedule</h2>
  # <p>A schedule to govern when Puppet is allowed to manage this resource.
  # The value of this metaparameter must be the <code>name</code> of a <code>schedule</code>
  # resource. This means you must declare a schedule resource, then
  # refer to it by name; see
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/type.html#schedule">the docs for the <code>schedule</code> type</a>
  # for more info.</p>
  # <pre><code>schedule { 'everyday':
  #   period =&gt; daily,
  #   range  =&gt; "2-4"
  # }
  # exec { "/usr/bin/apt-get update":
  #   schedule =&gt; 'everyday'
  # }
  # </code></pre>
  # <p>Note that you can declare the schedule resource anywhere in your
  # manifests, as long as it ends up in the final compiled catalog.</p>

  $schedule,

  # <h2>stage</h2>
  # <p>Which run stage this class should reside in.</p>
  # <p><strong>Note: This metaparameter can only be used on classes,</strong> and only when
  # declaring them with the resource-like syntax. It cannot be used on normal
  # resources or on classes declared with <code>include</code>.</p>
  # <p>By default, all classes are declared in the <code>main</code> stage. To assign a class
  # to a different stage, you must:</p>
  # <ul>
  #   <li>Declare the new stage as a <a href="https://docs.puppetlabs.com/puppet/latest/reference/type.html#stage"><code>stage</code> resource</a>.</li>
  #   <li>Declare an order relationship between the new stage and the <code>main</code> stage.</li>
  #   <li>Use the resource-like syntax to declare the class, and set the <code>stage</code>
  # metaparameter to the name of the desired stage.</li>
  # </ul>
  # <p>For example:</p>
  # <pre><code>stage { 'pre':
  #   before =&gt; Stage['main'],
  # }
  # class { 'apt-updates':
  #   stage =&gt; 'pre',
  # }
  # </code></pre>

  $stage,

  # <h2>subscribe</h2>
  # <p>One or more resources that this resource depends on, expressed as
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_data_resource_reference.html">resource references</a>.
  # Multiple resources can be specified as an array of references. When this
  # attribute is present:</p>
  # <ul>
  #   <li>The subscribed resource(s) will be applied <em>before</em> this resource.</li>
  #   <li>If Puppet makes changes to any of the subscribed resources, it will cause
  # this resource to <em>refresh.</em> (Refresh behavior varies by resource
  # type: services will restart, mounts will unmount and re-mount, etc. Not
  # all types can refresh.)</li>
  # </ul>
  # <p>This is one of the four relationship metaparameters, along with
  # <code>before</code>, <code>require</code>, and <code>notify</code>. For more context, including the
  # alternate chaining arrow (<code>-&gt;</code> and <code>~&gt;</code>) syntax, see
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/lang_relationships.html">the language page on relationships</a>.</p>

  $subscribe,

  # <h2>tag</h2>
  # <p>Add the specified tags to the associated resource.  While all resources
  # are automatically tagged with as much information as possible
  # (e.g., each class and definition containing the resource), it can
  # be useful to add your own tags to a given resource.</p>
  # <p>Multiple tags can be specified as an array:</p>
  # <pre><code>file {'/etc/hosts':
  #   ensure =&gt; file,
  #   source =&gt; 'puppet:///modules/site/hosts',
  #   mode   =&gt; '0644',
  #   tag    =&gt; ['bootstrap', 'minimumrun', 'mediumrun'],
  # }
  # </code></pre>
  # <p>Tags are useful for things like applying a subset of a host’s configuration
  # with <a href="/puppet/latest/reference/configuration.html#tags">the <code>tags</code> setting</a>
  # (e.g. <code>puppet agent --test --tags bootstrap</code>).</p>

  $tag,

){}

# <h2>augeas</h2>
# <p>Apply a change or an array of changes to the filesystem
# using the augeas tool.</p>
#
# <p>Requires:</p>
#
# <ul>
#   <li><a href="http://www.augeas.net">Augeas</a></li>
#   <li>The ruby-augeas bindings</li>
# </ul>
#
# <p>Sample usage with a string:</p>
#
# <pre><code>augeas{"test1" :
#   context =&gt; "/files/etc/sysconfig/firstboot",
#   changes =&gt; "set RUN_FIRSTBOOT YES",
#   onlyif  =&gt; "match other_value size &gt; 0",
# }
# </code></pre>
#
# <p>Sample usage with an array and custom lenses:</p>
#
# <pre><code>augeas{"jboss_conf":
#   context   =&gt; "/files",
#   changes   =&gt; [
#       "set etc/jbossas/jbossas.conf/JBOSS_IP $ipaddress",
#       "set etc/jbossas/jbossas.conf/JAVA_HOME /usr",
#     ],
#   load_path =&gt; "$/usr/share/jbossas/lenses",
# }
# </code></pre>
# <h3>Providers</h3>
# <h4 id="augeas-provider-augeas">augeas</h4>
#
# <ul>
#   <li>Supported features: <code>execute_changes</code>, <code>need_to_run?</code>, <code>parse_commands</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>execute_changes</code> — Actually make the changes</li>
#   <li><code>need_to_run?</code> — If the command should run</li>
#   <li><code>parse_commands</code> — Parse the command string</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>execute changes</th>
#       <th>need to run?</th>
#       <th>parse commands</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>augeas</td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#     </tr>
#   </tbody>
# </table>
define augeas(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this task. Used for uniqueness.</p>

  $name,

  # <h2>changes</h2>
  # <p>The changes which should be applied to the filesystem. This
  # can be a command or an array of commands. The following commands are supported:</p>
  #
  # <ul>
  #   <li><code>set &lt;PATH&gt; &lt;VALUE&gt;</code> — Sets the value <code>VALUE</code> at location <code>PATH</code></li>
  #   <li><code>setm &lt;PATH&gt; &lt;SUB&gt; &lt;VALUE&gt;</code> — Sets multiple nodes (matching <code>SUB</code> relative to <code>PATH</code>) to <code>VALUE</code></li>
  #   <li><code>rm &lt;PATH&gt;</code> — Removes the node at location <code>PATH</code></li>
  #   <li><code>remove &lt;PATH&gt;</code> — Synonym for <code>rm</code></li>
  #   <li><code>clear &lt;PATH&gt;</code> — Sets the node at <code>PATH</code> to <code>NULL</code>, creating it if needed</li>
  #   <li><code>clearm &lt;PATH&gt; &lt;SUB&gt;</code> — Sets multiple nodes (matching <code>SUB</code> relative to <code>PATH</code>) to <code>NULL</code></li>
  #   <li><code>touch &lt;PATH&gt;</code> — Creates <code>PATH</code> with the value <code>NULL</code> if it does not exist</li>
  #   <li><code>ins &lt;LABEL&gt; (before|after) &lt;PATH&gt;</code> — Inserts an empty node <code>LABEL</code> either before or after <code>PATH</code>.</li>
  #   <li><code>insert &lt;LABEL&gt; &lt;WHERE&gt; &lt;PATH&gt;</code> — Synonym for <code>ins</code></li>
  #   <li><code>mv &lt;PATH&gt; &lt;OTHER PATH&gt;</code> — Moves a node at <code>PATH</code> to the new location <code>OTHER PATH</code></li>
  #   <li><code>move &lt;PATH&gt; &lt;OTHER PATH&gt;</code> — Synonym for <code>mv</code></li>
  #   <li><code>rename &lt;PATH&gt; &lt;LABEL&gt;</code> — Rename a node at <code>PATH</code> to a new <code>LABEL</code></li>
  #   <li><code>defvar &lt;NAME&gt; &lt;PATH&gt;</code> — Sets Augeas variable <code>$NAME</code> to <code>PATH</code></li>
  #   <li><code>defnode &lt;NAME&gt; &lt;PATH&gt; &lt;VALUE&gt;</code> — Sets Augeas variable <code>$NAME</code> to <code>PATH</code>, creating it with <code>VALUE</code> if needed</li>
  # </ul>
  #
  # <p>If the <code>context</code> parameter is set, that value is prepended to any relative <code>PATH</code>s.</p>

  $changes,

  # <h2>context</h2>
  # <p>Optional context path. This value is prepended to the paths of all
  # changes if the path is relative. If the <code>incl</code> parameter is set,
  # defaults to <code>/files + incl</code>; otherwise, defaults to the empty string.</p>

  $context,

  # <h2>force</h2>
  # <p>Optional command to force the augeas type to execute even if it thinks changes
  # will not be made. This does not override the <code>onlyif</code> parameter.</p>

  $force,

  # <h2>incl</h2>
  # <p>Load only a specific file, e.g. <code>/etc/hosts</code>. This can greatly speed
  # up the execution the resource. When this parameter is set, you must also
  # set the <code>lens</code> parameter to indicate which lens to use.</p>

  $incl,

  # <h2>lens</h2>
  # <p>Use a specific lens, e.g. <code>Hosts.lns</code>. When this parameter is set, you
  # must also set the <code>incl</code> parameter to indicate which file to load.
  # The Augeas documentation includes <a href="http://augeas.net/stock_lenses.html">a list of available lenses</a>.</p>

  $lens,

  # <h2>load_path</h2>
  # <p>Optional colon-separated list or array of directories; these directories are searched for schema definitions. The agent’s <code>$libdir/augeas/lenses</code> path will always be added to support pluginsync.</p>

  $load_path,

  # <h2>onlyif</h2>
  # <p>Optional augeas command and comparisons to control the execution of this type.</p>
  #
  # <p>Note: <code>values</code> is not an actual augeas API command. It calls <code>match</code> to retrieve an array of paths
  #        in <MATCH_PATH> and then `get` to retrieve the values from each of the returned paths.</MATCH_PATH></p>
  #
  # <p>Supported onlyif syntax:</p>
  #
  # <ul>
  #   <li><code>get &lt;AUGEAS_PATH&gt; &lt;COMPARATOR&gt; &lt;STRING&gt;</code></li>
  #   <li><code>values &lt;MATCH_PATH&gt; include &lt;STRING&gt;</code></li>
  #   <li><code>values &lt;MATCH_PATH&gt; not_include &lt;STRING&gt;</code></li>
  #   <li><code>values &lt;MATCH_PATH&gt; == &lt;AN_ARRAY&gt;</code></li>
  #   <li><code>values &lt;MATCH_PATH&gt; != &lt;AN_ARRAY&gt;</code></li>
  #   <li><code>match &lt;MATCH_PATH&gt; size &lt;COMPARATOR&gt; &lt;INT&gt;</code></li>
  #   <li><code>match &lt;MATCH_PATH&gt; include &lt;STRING&gt;</code></li>
  #   <li><code>match &lt;MATCH_PATH&gt; not_include &lt;STRING&gt;</code></li>
  #   <li><code>match &lt;MATCH_PATH&gt; == &lt;AN_ARRAY&gt;</code></li>
  #   <li><code>match &lt;MATCH_PATH&gt; != &lt;AN_ARRAY&gt;</code></li>
  # </ul>
  #
  # <p>where:</p>
  #
  # <ul>
  #   <li><code>AUGEAS_PATH</code> is a valid path scoped by the context</li>
  #   <li><code>MATCH_PATH</code> is a valid match syntax scoped by the context</li>
  #   <li><code>COMPARATOR</code> is one of <code>&gt;, &gt;=, !=, ==, &lt;=,</code> or <code>&lt;</code></li>
  #   <li><code>STRING</code> is a string</li>
  #   <li><code>INT</code> is a number</li>
  #   <li><code>AN_ARRAY</code> is in the form <code>['a string', 'another']</code></li>
  # </ul>

  $onlyif,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>augeas</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#augeas-provider-augeas"><code>augeas</code></a></li>
  # </ul>

  $provider,

  # <h2>returns</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The expected return code from the augeas command. Should not be set.</p>

  $returns,

  # <h2>root</h2>
  # <p>A file system path; all files loaded by Augeas are loaded underneath <code>root</code>.</p>

  $root,

  # <h2>show_diff</h2>
  # <p>Whether to display differences when the file changes, defaulting to
  # true.  This parameter is useful for files that may contain passwords or
  # other secret data, which might otherwise be included in Puppet reports or
  # other insecure outputs.  If the global <code>show_diff</code> setting
  # is false, then no diffs will be shown even if this parameter is true.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $show_diff,

  # <h2>type_check</h2>
  # <p>Whether augeas should perform typechecking. Defaults to false.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $type_check,

){}

# <h2>computer</h2>
# <p>Computer object management using DirectoryService
# on OS X.</p>
#
# <p>Note that these are distinctly different kinds of objects to ‘hosts’,
# as they require a MAC address and can have all sorts of policy attached to
# them.</p>
#
# <p>This provider only manages Computer objects in the local directory service
# domain, not in remote directories.</p>
#
# <p>If you wish to manage <code>/etc/hosts</code> file on Mac OS X, then simply use the host
# type as per other platforms.</p>
#
# <p>This type primarily exists to create localhost Computer objects that MCX
# policy can then be attached to.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the plist file representing a
# Computer object (located at <code>/var/db/dslocal/nodes/Default/computers/{name}.plist</code>),
# the Computer resource will autorequire it.</p>
# <h3>Providers</h3>
# <h4 id="computer-provider-directoryservice">directoryservice</h4>
#
# <p>Computer object management using DirectoryService on OS X.
# Note that these are distinctly different kinds of objects to ‘hosts’,
# as they require a MAC address and can have all sorts of policy attached to
# them.</p>
#
# <p>This provider only manages Computer objects in the local directory service
# domain, not in remote directories.</p>
#
# <p>If you wish to manage /etc/hosts on Mac OS X, then simply use the host
# type as per other platforms.</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>darwin</code>.</li>
# </ul>
define computer(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The authoritative ‘short’ name of the computer record.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Control the existences of this computer record. Set this attribute to
  # <code>present</code> to ensure the computer record exists.  Set it to <code>absent</code>
  # to delete any computer records with this name</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>en_address</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The MAC address of the primary network interface. Must match en0.</p>

  $en_address,

  # <h2>ip_address</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The IP Address of the Computer object.</p>

  $ip_address,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>computer</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#computer-provider-directoryservice"><code>directoryservice</code></a></li>
  # </ul>

  $provider,

  # <h2>realname</h2>
  # <p>The ‘long’ name of the computer record.</p>

  $realname,

){}

# <h2>cron</h2>
# <p>Installs and manages cron jobs.  Every cron resource created by Puppet
# requires a command and at least one periodic attribute (hour, minute,
# month, monthday, weekday, or special).  While the name of the cron job is
# not part of the actual job, the name is stored in a comment beginning with
# <code># Puppet Name: </code>. These comments are used to match crontab entries created
# by Puppet with cron resources.</p>
#
# <p>If an existing crontab entry happens to match the scheduling and command of a
# cron resource that has never been synched, Puppet will defer to the existing
# crontab entry and will not create a new entry tagged with the <code># Puppet Name: </code>
# comment.</p>
#
# <p>Example:</p>
#
# <pre><code>cron { 'logrotate':
#   command =&gt; '/usr/sbin/logrotate',
#   user    =&gt; 'root',
#   hour    =&gt; 2,
#   minute  =&gt; 0,
# }
# </code></pre>
#
# <p>Note that all periodic attributes can be specified as an array of values:</p>
#
# <pre><code>cron { 'logrotate':
#   command =&gt; '/usr/sbin/logrotate',
#   user    =&gt; 'root',
#   hour    =&gt; [2, 4],
# }
# </code></pre>
#
# <p>…or using ranges or the step syntax <code>*/2</code> (although there’s no guarantee
# that your <code>cron</code> daemon supports these):</p>
#
# <pre><code>cron { 'logrotate':
#   command =&gt; '/usr/sbin/logrotate',
#   user    =&gt; 'root',
#   hour    =&gt; ['2-4'],
#   minute  =&gt; '*/10',
# }
# </code></pre>
#
# <p>An important note: <em>the Cron type will not reset parameters that are
# removed from a manifest</em>. For example, removing a <code>minute =&gt; 10</code> parameter
# will not reset the minute component of the associated cronjob to <code>*</code>.
# These changes must be expressed by setting the parameter to
# <code>minute =&gt; absent</code> because Puppet only manages parameters that are out of
# sync with manifest entries.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the user account specified by the
# <code>user</code> property of a cron resource, then the cron resource will autorequire
# that user.</p>
# <h3>Providers</h3>
# <h4 id="cron-provider-crontab">crontab</h4>
#
# <ul>
#   <li>Required binaries: <code>crontab</code>.</li>
# </ul>
define cron(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The symbolic name of the cron job.  This name
  # is used for human reference only and is generated automatically
  # for cron jobs found on the system.  This generally won’t
  # matter, as Puppet will do its best to match existing cron jobs
  # against specified jobs (and Puppet adds a comment to cron jobs it adds),
  # but it is at least possible that converting from unmanaged jobs to
  # managed jobs might require manual intervention.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>command</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The command to execute in the cron job.  The environment
  # provided to the command varies by local system rules, and it is
  # best to always provide a fully qualified command.  The user’s
  # profile is not sourced when the command is run, so if the
  # user’s environment is desired it should be sourced manually.</p>
  #
  # <p>All cron parameters support <code>absent</code> as a value; this will
  # remove any existing values for that field.</p>

  $command,

  # <h2>environment</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Any environment settings associated with this cron job.  They
  # will be stored between the header and the job in the crontab.  There
  # can be no guarantees that other, earlier settings will not also
  # affect a given cron job.</p>
  #
  # <p>Also, Puppet cannot automatically determine whether an existing,
  # unmanaged environment setting is associated with a given cron
  # job.  If you already have cron jobs with environment settings,
  # then Puppet will keep those settings in the same place in the file,
  # but will not associate them with a specific job.</p>
  #
  # <p>Settings should be specified exactly as they should appear in
  # the crontab, e.g., <code>PATH=/bin:/usr/bin:/usr/sbin</code>.</p>

  $environment,

  # <h2>hour</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The hour at which to run the cron job. Optional;
  # if specified, must be between 0 and 23, inclusive.</p>

  $hour,

  # <h2>minute</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The minute at which to run the cron job.
  # Optional; if specified, must be between 0 and 59, inclusive.</p>

  $minute,

  # <h2>month</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The month of the year.  Optional; if specified
  # must be between 1 and 12 or the month name (e.g., December).</p>

  $month,

  # <h2>monthday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The day of the month on which to run the
  # command.  Optional; if specified, must be between 1 and 31.</p>

  $monthday,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>cron</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#cron-provider-crontab"><code>crontab</code></a></li>
  # </ul>

  $provider,

  # <h2>special</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A special value such as ‘reboot’ or ‘annually’.
  # Only available on supported systems such as Vixie Cron.
  # Overrides more specific time of day/week settings.
  # Set to ‘absent’ to make puppet revert to a plain numeric schedule.</p>

  $special,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The name of the crontab file in which the cron job should be stored.</p>
  #
  # <p>This property defaults to the value of the <code>user</code> property if set, the
  # user running Puppet or <code>root</code>.</p>
  #
  # <p>For the default crontab provider, this property is functionally
  # equivalent to the <code>user</code> property and should be avoided. In particular,
  # setting both <code>user</code> and <code>target</code> to different values will result in
  # undefined behavior.</p>

  $target,

  # <h2>user</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user who owns the cron job.  This user must
  # be allowed to run cron jobs, which is not currently checked by
  # Puppet.</p>
  #
  # <p>This property defaults to the user running Puppet or <code>root</code>.</p>
  #
  # <p>The default crontab provider executes the system <code>crontab</code> using
  # the user account specified by this property.</p>

  $user,

  # <h2>weekday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The weekday on which to run the command.
  # Optional; if specified, must be between 0 and 7, inclusive, with
  # 0 (or 7) being Sunday, or must be the name of the day (e.g., Tuesday).</p>

  $weekday,

){}

# <h2>exec</h2>
# <p>Executes external commands.</p>
#
# <p>Any command in an <code>exec</code> resource <strong>must</strong> be able to run multiple times
# without causing harm — that is, it must be <em>idempotent</em>. There are three
# main ways for an exec to be idempotent:</p>
#
# <ul>
#   <li>The command itself is already idempotent. (For example, <code>apt-get update</code>.)</li>
#   <li>The exec has an <code>onlyif</code>, <code>unless</code>, or <code>creates</code> attribute, which prevents
# Puppet from running the command unless some condition is met.</li>
#   <li>The exec has <code>refreshonly =&gt; true</code>, which only allows Puppet to run the
# command when some other resource is changed. (See the notes on refreshing
# below.)</li>
# </ul>
#
# <p>A caution: There’s a widespread tendency to use collections of execs to
# manage resources that aren’t covered by an existing resource type. This
# works fine for simple tasks, but once your exec pile gets complex enough
# that you really have to think to understand what’s happening, you should
# consider developing a custom resource type instead, as it will be much
# more predictable and maintainable.</p>
#
# <p><strong>Refresh:</strong> <code>exec</code> resources can respond to refresh events (via
# <code>notify</code>, <code>subscribe</code>, or the <code>~&gt;</code> arrow). The refresh behavior of execs
# is non-standard, and can be affected by the <code>refresh</code> and
# <code>refreshonly</code> attributes:</p>
#
# <ul>
#   <li>If <code>refreshonly</code> is set to true, the exec will <em>only</em> run when it receives an
# event. This is the most reliable way to use refresh with execs.</li>
#   <li>If the exec already would have run and receives an event, it will run its
# command <strong>up to two times.</strong> (If an <code>onlyif</code>, <code>unless</code>, or <code>creates</code> condition
# is no longer met after the first run, the second run will not occur.)</li>
#   <li>If the exec already would have run, has a <code>refresh</code> command, and receives an
# event, it will run its normal command, then run its <code>refresh</code> command
# (as long as any <code>onlyif</code>, <code>unless</code>, or <code>creates</code> conditions are still met
# after the normal command finishes).</li>
#   <li>If the exec would <strong>not</strong> have run (due to an <code>onlyif</code>, <code>unless</code>, or <code>creates</code>
# attribute) and receives an event, it still will not run.</li>
#   <li>If the exec has <code>noop =&gt; true</code>, would otherwise have run, and receives
# an event from a non-noop resource, it will run once (or run its <code>refresh</code>
# command instead, if it has one).</li>
# </ul>
#
# <p>In short: If there’s a possibility of your exec receiving refresh events,
# it becomes doubly important to make sure the run conditions are restricted.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing an exec’s cwd or the executable
# file used in an exec’s command, the exec resource will autorequire those
# files. If Puppet is managing the user that an exec should run as, the
# exec resource will autorequire that user.</p>
# <h3>Providers</h3>
# <h4 id="exec-provider-posix">posix</h4>
#
# <p>Executes external binaries directly, without passing through a shell or
# performing any interpolation. This is a safer and more predictable way
# to execute most commands, but prevents the use of globbing and shell
# built-ins (including control logic like “for” and “if” statements).</p>
#
# <ul>
#   <li>Default for <code>feature</code> == <code>posix</code>.</li>
# </ul>
#
# <h4 id="exec-provider-shell">shell</h4>
#
# <p>Passes the provided command through <code>/bin/sh</code>; only available on
# POSIX systems. This allows the use of shell globbing and built-ins, and
# does not require that the path to a command be fully-qualified. Although
# this can be more convenient than the <code>posix</code> provider, it also means that
# you need to be more careful with escaping; as ever, with great power comes
# etc. etc.</p>
#
# <p>This provider closely resembles the behavior of the <code>exec</code> type
# in Puppet 0.25.x.</p>
#
# <h4 id="exec-provider-windows">windows</h4>
#
# <p>Execute external binaries on Windows systems. As with the <code>posix</code>
# provider, this provider directly calls the command with the arguments
# given, without passing it through a shell or performing any interpolation.
# To use shell built-ins — that is, to emulate the <code>shell</code> provider on
# Windows — a command must explicitly invoke the shell:</p>
#
# <pre><code>exec {'echo foo':
#   command =&gt; 'cmd.exe /c echo "foo"',
# }
# </code></pre>
#
# <p>If no extension is specified for a command, Windows will use the <code>PATHEXT</code>
# environment variable to locate the executable.</p>
#
# <p><strong>Note on PowerShell scripts:</strong> PowerShell’s default <code>restricted</code>
# execution policy doesn’t allow it to run saved scripts. To run PowerShell
# scripts, specify the <code>remotesigned</code> execution policy as part of the
# command:</p>
#
# <pre><code>exec { 'test':
#   path    =&gt; 'C:/Windows/System32/WindowsPowerShell/v1.0',
#   command =&gt; 'powershell -executionpolicy remotesigned -file C:/test.ps1',
# }
# </code></pre>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>windows</code>.</li>
# </ul>
define exec(
  # <h2>command</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The actual command to execute.  Must either be fully qualified
  # or a search path for the command must be provided.  If the command
  # succeeds, any output produced will be logged at the instance’s
  # normal log level (usually <code>notice</code>), but if the command fails
  # (meaning its return code does not match the specified code) then
  # any output is logged at the <code>err</code> log level.</p>

  $command,

  # <h2>creates</h2>
  # <p>A file to look for before running the command. The command will
  # only run if the file <strong>doesn’t exist.</strong></p>
  #
  # <p>This parameter doesn’t cause Puppet to create a file; it is only
  # useful if <strong>the command itself</strong> creates a file.</p>
  #
  # <pre><code>exec { 'tar -xf /Volumes/nfs02/important.tar':
  #   cwd     =&gt; '/var/tmp',
  #   creates =&gt; '/var/tmp/myfile',
  #   path    =&gt; ['/usr/bin', '/usr/sbin',],
  # }
  # </code></pre>
  #
  # <p>In this example, <code>myfile</code> is assumed to be a file inside
  # <code>important.tar</code>. If it is ever deleted, the exec will bring it
  # back by re-extracting the tarball. If <code>important.tar</code> does <strong>not</strong>
  # actually contain <code>myfile</code>, the exec will keep running every time
  # Puppet runs.</p>

  $creates,

  # <h2>cwd</h2>
  # <p>The directory from which to run the command.  If
  # this directory does not exist, the command will fail.</p>

  $cwd,

  # <h2>environment</h2>
  # <p>Any additional environment variables you want to set for a
  # command.  Note that if you use this to set PATH, it will override
  # the <code>path</code> attribute.  Multiple environment variables should be
  # specified as an array.</p>

  $environment,

  # <h2>group</h2>
  # <p>The group to run the command as.  This seems to work quite
  # haphazardly on different platforms – it is a platform issue
  # not a Ruby or Puppet one, since the same variety exists when
  # running commands as different users in the shell.</p>

  $group,

  # <h2>logoutput</h2>
  # <p>Whether to log command output in addition to logging the
  # exit code.  Defaults to <code>on_failure</code>, which only logs the output
  # when the command has an exit code that does not match any value
  # specified by the <code>returns</code> attribute. As with any resource type,
  # the log level can be controlled with the <code>loglevel</code> metaparameter.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>on_failure</code>.</p>

  $logoutput,

  # <h2>onlyif</h2>
  # <p>If this parameter is set, then this <code>exec</code> will only run if
  # the command has an exit code of 0.  For example:</p>
  #
  # <pre><code>exec { 'logrotate':
  #   path   =&gt; '/usr/bin:/usr/sbin:/bin',
  #   onlyif =&gt; 'test `du /var/log/messages | cut -f1` -gt 100000',
  # }
  # </code></pre>
  #
  # <p>This would run <code>logrotate</code> only if that test returned true.</p>
  #
  # <p>Note that this command follows the same rules as the main command,
  # such as which user and group it’s run as.
  # This also means it must be fully qualified if the path is not set.</p>
  #
  # <p>It also uses the same provider as the main command, so any behavior
  # that differs by provider will match.</p>
  #
  # <p>Also note that onlyif can take an array as its value, e.g.:</p>
  #
  # <pre><code>onlyif =&gt; ['test -f /tmp/file1', 'test -f /tmp/file2'],
  # </code></pre>
  #
  # <p>This will only run the exec if <em>all</em> conditions in the array return true.</p>

  $onlyif,

  # <h2>path</h2>
  # <p>The search path used for command execution.
  # Commands must be fully qualified if no path is specified.  Paths
  # can be specified as an array or as a ‘:’ separated list.</p>

  $path,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>exec</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#exec-provider-posix"><code>posix</code></a></li>
  #   <li><a href="#exec-provider-shell"><code>shell</code></a></li>
  #   <li><a href="#exec-provider-windows"><code>windows</code></a></li>
  # </ul>

  $provider,

  # <h2>refresh</h2>
  # <p>How to refresh this command.  By default, the exec is just
  # called again when it receives an event from another resource,
  # but this parameter allows you to define a different command
  # for refreshing.</p>

  $refresh,

  # <h2>refreshonly</h2>
  # <p>The command should only be run as a
  # refresh mechanism for when a dependent object is changed.  It only
  # makes sense to use this option when this command depends on some
  # other object; it is useful for triggering an action:</p>
  #
  # <pre><code># Pull down the main aliases file
  # file { '/etc/aliases':
  #   source =&gt; 'puppet://server/module/aliases',
  # }
  #
  # # Rebuild the database, but only when the file changes
  # exec { newaliases:
  #   path        =&gt; ['/usr/bin', '/usr/sbin'],
  #   subscribe   =&gt; File['/etc/aliases'],
  #   refreshonly =&gt; true,
  # }
  # </code></pre>
  #
  # <p>Note that only <code>subscribe</code> and <code>notify</code> can trigger actions, not <code>require</code>,
  # so it only makes sense to use <code>refreshonly</code> with <code>subscribe</code> or <code>notify</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $refreshonly,

  # <h2>returns</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The expected exit code(s).  An error will be returned if the
  # executed command has some other exit code.  Defaults to 0. Can be
  # specified as an array of acceptable exit codes or a single value.</p>
  #
  # <p>On POSIX systems, exit codes are always integers between 0 and 255.</p>
  #
  # <p>On Windows, <strong>most</strong> exit codes should be integers between 0
  # and 2147483647.</p>
  #
  # <p>Larger exit codes on Windows can behave inconsistently across different
  # tools. The Win32 APIs define exit codes as 32-bit unsigned integers, but
  # both the cmd.exe shell and the .NET runtime cast them to signed
  # integers. This means some tools will report negative numbers for exit
  # codes above 2147483647. (For example, cmd.exe reports 4294967295 as -1.)
  # Since Puppet uses the plain Win32 APIs, it will report the very large
  # number instead of the negative number, which might not be what you
  # expect if you got the exit code from a cmd.exe session.</p>
  #
  # <p>Microsoft recommends against using negative/very large exit codes, and
  # you should avoid them when possible. To convert a negative exit code to
  # the positive one Puppet will use, add it to 4294967296.</p>

  $returns,

  # <h2>timeout</h2>
  # <p>The maximum time the command should take.  If the command takes
  # longer than the timeout, the command is considered to have failed
  # and will be stopped. The timeout is specified in seconds. The default
  # timeout is 300 seconds and you can set it to 0 to disable the timeout.</p>

  $timeout,

  # <h2>tries</h2>
  # <p>The number of times execution of the command should be tried.
  # Defaults to ‘1’. This many attempts will be made to execute
  # the command until an acceptable return code is returned.
  # Note that the timeout parameter applies to each try rather than
  # to the complete set of tries.</p>

  $tries,

  # <h2>try_sleep</h2>
  # <p>The time to sleep in seconds between ‘tries’.</p>

  $try_sleep,

  # <h2>umask</h2>
  # <p>Sets the umask to be used while executing this command</p>

  $umask,

  # <h2>unless</h2>
  # <p>If this parameter is set, then this <code>exec</code> will run unless
  # the command has an exit code of 0.  For example:</p>
  #
  # <pre><code>exec { '/bin/echo root &gt;&gt; /usr/lib/cron/cron.allow':
  #   path   =&gt; '/usr/bin:/usr/sbin:/bin',
  #   unless =&gt; 'grep root /usr/lib/cron/cron.allow 2&gt;/dev/null',
  # }
  # </code></pre>
  #
  # <p>This would add <code>root</code> to the cron.allow file (on Solaris) unless
  # <code>grep</code> determines it’s already there.</p>
  #
  # <p>Note that this command follows the same rules as the main command,
  # such as which user and group it’s run as.
  # This also means it must be fully qualified if the path is not set.
  # It also uses the same provider as the main command, so any behavior
  # that differs by provider will match.</p>
  #
  # <p>Also note that unless can take an array as its value, e.g.:</p>
  #
  # <pre><code>unless =&gt; ['test -f /tmp/file1', 'test -f /tmp/file2'],
  # </code></pre>
  #
  # <p>This will only run the exec if <em>all</em> conditions in the array return false.</p>

  $unless,

  # <h2>user</h2>
  # <p>The user to run the command as.  Note that if you
  # use this then any error output is not currently captured.  This
  # is because of a bug within Ruby.  If you are using Puppet to
  # create this user, the exec will automatically require the user,
  # as long as it is specified by name.</p>
  #
  # <p>Please note that the $HOME environment variable is not automatically set
  # when using this attribute.</p>

  $user,

){}

# <h2>file</h2>
# <p>Manages files, including their content, ownership, and permissions.</p>
#
# <p>The <code>file</code> type can manage normal files, directories, and symlinks; the
# type should be specified in the <code>ensure</code> attribute.</p>
#
# <p>File contents can be managed directly with the <code>content</code> attribute, or
# downloaded from a remote source using the <code>source</code> attribute; the latter
# can also be used to recursively serve directories (when the <code>recurse</code>
# attribute is set to <code>true</code> or <code>local</code>). On Windows, note that file
# contents are managed in binary mode; Puppet never automatically translates
# line endings.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the user or group that owns a
# file, the file resource will autorequire them. If Puppet is managing any
# parent directories of a file, the file resource will autorequire them.</p>
# <h3>Providers</h3>
# <h4 id="file-provider-posix">posix</h4>
#
# <p>Uses POSIX functionality to manage file ownership and permissions.</p>
#
# <ul>
#   <li>Supported features: <code>manages_symlinks</code>.</li>
# </ul>
#
# <h4 id="file-provider-windows">windows</h4>
#
# <p>Uses Microsoft Windows functionality to manage file ownership and permissions.</p>
#
# <ul>
#   <li>Supported features: <code>manages_symlinks</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>manages_symlinks</code> — The provider can manage symbolic links.</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>manages symlinks</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>posix</td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>windows</td>
#       <td><em>X</em> </td>
#     </tr>
#   </tbody>
# </table>
define file(
  # <h2>path</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The path to the file to manage.  Must be fully qualified.</p>
  #
  # <p>On Windows, the path should include the drive letter and should use <code>/</code> as
  # the separator character (rather than <code>\\</code>).</p>

  $path,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether the file should exist, and if so what kind of file it should be.
  # Possible values are <code>present</code>, <code>absent</code>, <code>file</code>, <code>directory</code>, and <code>link</code>.</p>
  #
  # <ul>
  #   <li><code>present</code> accepts any form of file existence, and creates a
  # normal file if the file is missing. (The file will have no content
  # unless the <code>content</code> or <code>source</code> attribute is used.)</li>
  #   <li><code>absent</code> ensures the file doesn’t exist, and deletes it if necessary.</li>
  #   <li><code>file</code> ensures it’s a normal file, and enables use of the <code>content</code> or
  # <code>source</code> attribute.</li>
  #   <li><code>directory</code> ensures it’s a directory, and enables use of the <code>source</code>,
  # <code>recurse</code>, <code>recurselimit</code>, <code>ignore</code>, and <code>purge</code> attributes.</li>
  #   <li><code>link</code> ensures the file is a symlink, and <strong>requires</strong> that you also
  # set the <code>target</code> attribute. Symlinks are supported on all Posix
  # systems and on Windows Vista / 2008 and higher. On Windows, managing
  # symlinks requires Puppet agent’s user account to have the “Create
  # Symbolic Links” privilege; this can be configured in the “User Rights
  # Assignment” section in the Windows policy editor. By default, Puppet
  # agent runs as the Administrator account, which has this privilege.</li>
  # </ul>
  #
  # <p>Puppet avoids destroying directories unless the <code>force</code> attribute is set
  # to <code>true</code>. This means that if a file is currently a directory, setting
  # <code>ensure</code> to anything but <code>directory</code> or <code>present</code> will cause Puppet to
  # skip managing the resource and log either a notice or an error.</p>
  #
  # <p>There is one other non-standard value for <code>ensure</code>. If you specify the
  # path to another file as the ensure value, it is equivalent to specifying
  # <code>link</code> and using that path as the <code>target</code>:</p>
  #
  # <pre><code># Equivalent resources:
  #
  # file { '/etc/inetd.conf':
  #   ensure =&gt; '/etc/inet/inetd.conf',
  # }
  #
  # file { '/etc/inetd.conf':
  #   ensure =&gt; link,
  #   target =&gt; '/etc/inet/inetd.conf',
  # }
  # </code></pre>
  #
  # <p>However, we recommend using <code>link</code> and <code>target</code> explicitly, since this
  # behavior can be harder to read and is
  # <a href="https://docs.puppetlabs.com/puppet/4.3/reference/deprecated_language.html">deprecated</a>
  # as of Puppet 4.3.0.</p>
  #
  # <p>Valid values are <code>absent</code> (also called <code>false</code>), <code>file</code>, <code>present</code>, <code>directory</code>, <code>link</code>. Values can match <code>/./</code>.</p>

  $ensure,

  # <h2>backup</h2>
  # <p>Whether (and how) file content should be backed up before being replaced.
  # This attribute works best as a resource default in the site manifest
  # (<code>File { backup =&gt; main }</code>), so it can affect all file resources.</p>
  #
  # <ul>
  #   <li>If set to <code>false</code>, file content won’t be backed up.</li>
  #   <li>If set to a string beginning with <code>.</code> (e.g., <code>.puppet-bak</code>), Puppet will
  # use copy the file in the same directory with that value as the extension
  # of the backup. (A value of <code>true</code> is a synonym for <code>.puppet-bak</code>.)</li>
  #   <li>If set to any other string, Puppet will try to back up to a filebucket
  # with that title. See the <code>filebucket</code> resource type for more details.
  # (This is the preferred method for backup, since it can be centralized
  # and queried.)</li>
  # </ul>
  #
  # <p>Default value: <code>puppet</code>, which backs up to a filebucket of the same name.
  # (Puppet automatically creates a <strong>local</strong> filebucket named <code>puppet</code> if one
  # doesn’t already exist.)</p>
  #
  # <p>Backing up to a local filebucket isn’t particularly useful. If you want
  # to make organized use of backups, you will generally want to use the
  # puppet master server’s filebucket service. This requires declaring a
  # filebucket resource and a resource default for the <code>backup</code> attribute
  # in site.pp:</p>
  #
  # <pre><code># /etc/puppetlabs/puppet/manifests/site.pp
  # filebucket { 'main':
  #   path   =&gt; false,                # This is required for remote filebuckets.
  #   server =&gt; 'puppet.example.com', # Optional; defaults to the configured puppet master.
  # }
  #
  # File { backup =&gt; main, }
  # </code></pre>
  #
  # <p>If you are using multiple puppet master servers, you will want to
  # centralize the contents of the filebucket. Either configure your load
  # balancer to direct all filebucket traffic to a single master, or use
  # something like an out-of-band rsync task to synchronize the content on all
  # masters.</p>

  $backup,

  # <h2>checksum</h2>
  # <p>The checksum type to use when determining whether to replace a file’s contents.</p>
  #
  # <p>The default checksum type is md5.</p>
  #
  # <p>Valid values are <code>md5</code>, <code>md5lite</code>, <code>sha256</code>, <code>sha256lite</code>, <code>mtime</code>, <code>ctime</code>, <code>none</code>.</p>

  $checksum,

  # <h2>checksum_value</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The checksum of the source contents. Only md5 and sha256 are supported when
  # specifying this parameter. If this parameter is set, source_permissions will be
  # assumed to be false, and ownership and permissions will not be read from source.</p>

  $checksum_value,

  # <h2>content</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The desired contents of a file, as a string. This attribute is mutually
  # exclusive with <code>source</code> and <code>target</code>.</p>
  #
  # <p>Newlines and tabs can be specified in double-quoted strings using
  # standard escaped syntax — \n for a newline, and \t for a tab.</p>
  #
  # <p>With very small files, you can construct content strings directly in
  # the manifest…</p>
  #
  # <pre><code>define resolve(nameserver1, nameserver2, domain, search) {
  #     $str = "search ${search}
  #         domain ${domain}
  #         nameserver ${nameserver1}
  #         nameserver ${nameserver2}
  #         "
  #
  #     file { '/etc/resolv.conf':
  #       content =&gt; $str,
  #     }
  # }
  # </code></pre>
  #
  # <p>…but for larger files, this attribute is more useful when combined with the
  # <a href="https://docs.puppetlabs.com/puppet/latest/reference/function.html#template">template</a>
  # or <a href="https://docs.puppetlabs.com/puppet/latest/reference/function.html#file">file</a>
  # function.</p>

  $content,

  # <h2>ctime</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A read-only state to check the file ctime. On most modern *nix-like
  # systems, this is the time of the most recent change to the owner, group,
  # permissions, or content of the file.</p>

  $ctime,

  # <h2>force</h2>
  # <p>Perform the file operation even if it will destroy one or more directories.
  # You must use <code>force</code> in order to:</p>
  #
  # <ul>
  #   <li><code>purge</code> subdirectories</li>
  #   <li>Replace directories with files or links</li>
  #   <li>Remove a directory when <code>ensure =&gt; absent</code></li>
  # </ul>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $force,

  # <h2>group</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Which group should own the file.  Argument can be either a group
  # name or a group ID.</p>
  #
  # <p>On Windows, a user (such as “Administrator”) can be set as a file’s group
  # and a group (such as “Administrators”) can be set as a file’s owner;
  # however, a file’s owner and group shouldn’t be the same. (If the owner
  # is also the group, files with modes like <code>0640</code> will cause log churn, as
  # they will always appear out of sync.)</p>

  $group,

  # <h2>ignore</h2>
  # <p>A parameter which omits action on files matching
  # specified patterns during recursion.  Uses Ruby’s builtin globbing
  # engine, so shell metacharacters are fully supported, e.g. <code>[a-z]*</code>.
  # Matches that would descend into the directory structure are ignored,
  # e.g., <code>*/*</code>.</p>

  $ignore,

  # <h2>links</h2>
  # <p>How to handle links during file actions.  During file copying,
  # <code>follow</code> will copy the target file instead of the link and <code>manage</code>
  # will copy the link itself. When not copying, <code>manage</code> will manage
  # the link, and <code>follow</code> will manage the file to which the link points.</p>
  #
  # <p>Valid values are <code>follow</code>, <code>manage</code>.</p>

  $links,

  # <h2>mode</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The desired permissions mode for the file, in symbolic or numeric
  # notation. This value <strong>must</strong> be specified as a string; do not use
  # un-quoted numbers to represent file modes.</p>
  #
  # <p>The <code>file</code> type uses traditional Unix permission schemes and translates
  # them to equivalent permissions for systems which represent permissions
  # differently, including Windows. For detailed ACL controls on Windows,
  # you can leave <code>mode</code> unmanaged and use
  # <a href="https://forge.puppetlabs.com/puppetlabs/acl">the puppetlabs/acl module.</a></p>
  #
  # <p>Numeric modes should use the standard octal notation of
  # <code>&lt;SETUID/SETGID/STICKY&gt;&lt;OWNER&gt;&lt;GROUP&gt;&lt;OTHER&gt;</code> (e.g. ‘0644’).</p>
  #
  # <ul>
  #   <li>Each of the “owner,” “group,” and “other” digits should be a sum of the
  # permissions for that class of users, where read = 4, write = 2, and
  # execute/search = 1.</li>
  #   <li>The setuid/setgid/sticky digit is also a sum, where setuid = 4, setgid = 2,
  # and sticky = 1.</li>
  #   <li>The setuid/setgid/sticky digit is optional. If it is absent, Puppet will
  # clear any existing setuid/setgid/sticky permissions. (So to make your intent
  # clear, you should use at least four digits for numeric modes.)</li>
  #   <li>When specifying numeric permissions for directories, Puppet sets the search
  # permission wherever the read permission is set.</li>
  # </ul>
  #
  # <p>Symbolic modes should be represented as a string of comma-separated
  # permission clauses, in the form <code>&lt;WHO&gt;&lt;OP&gt;&lt;PERM&gt;</code>:</p>
  #
  # <ul>
  #   <li>“Who” should be u (user), g (group), o (other), and/or a (all)</li>
  #   <li>“Op” should be = (set exact permissions), + (add select permissions),
  # or - (remove select permissions)</li>
  #   <li>“Perm” should be one or more of:
  #     <ul>
  #       <li>r (read)</li>
  #       <li>w (write)</li>
  #       <li>x (execute/search)</li>
  #       <li>t (sticky)</li>
  #       <li>s (setuid/setgid)</li>
  #       <li>X (execute/search if directory or if any one user can execute)</li>
  #       <li>u (user’s current permissions)</li>
  #       <li>g (group’s current permissions)</li>
  #       <li>o (other’s current permissions)</li>
  #     </ul>
  #   </li>
  # </ul>
  #
  # <p>Thus, mode <code>0664</code> could be represented symbolically as either <code>a=r,ug+w</code>
  # or <code>ug=rw,o=r</code>.  However, symbolic modes are more expressive than numeric
  # modes: a mode only affects the specified bits, so <code>mode =&gt; 'ug+w'</code> will
  # set the user and group write bits, without affecting any other bits.</p>
  #
  # <p>See the manual page for GNU or BSD <code>chmod</code> for more details
  # on numeric and symbolic modes.</p>
  #
  # <p>On Windows, permissions are translated as follows:</p>
  #
  # <ul>
  #   <li>Owner and group names are mapped to Windows SIDs</li>
  #   <li>The “other” class of users maps to the “Everyone” SID</li>
  #   <li>The read/write/execute permissions map to the <code>FILE_GENERIC_READ</code>,
  # <code>FILE_GENERIC_WRITE</code>, and <code>FILE_GENERIC_EXECUTE</code> access rights; a
  # file’s owner always has the <code>FULL_CONTROL</code> right</li>
  #   <li>“Other” users can’t have any permissions a file’s group lacks,
  # and its group can’t have any permissions its owner lacks; that is, 0644
  # is an acceptable mode, but 0464 is not.</li>
  # </ul>

  $mode,

  # <h2>mtime</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A read-only state to check the file mtime. On *nix-like systems, this
  # is the time of the most recent change to the content of the file.</p>

  $mtime,

  # <h2>owner</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user to whom the file should belong.  Argument can be a user name or a
  # user ID.</p>
  #
  # <p>On Windows, a group (such as “Administrators”) can be set as a file’s owner
  # and a user (such as “Administrator”) can be set as a file’s group; however,
  # a file’s owner and group shouldn’t be the same. (If the owner is also
  # the group, files with modes like <code>0640</code> will cause log churn, as they
  # will always appear out of sync.)</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>file</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#file-provider-posix"><code>posix</code></a></li>
  #   <li><a href="#file-provider-windows"><code>windows</code></a></li>
  # </ul>

  $provider,

  # <h2>purge</h2>
  # <p>Whether unmanaged files should be purged. This option only makes
  # sense when <code>ensure =&gt; directory</code> and <code>recurse =&gt; true</code>.</p>
  #
  # <ul>
  #   <li>When recursively duplicating an entire directory with the <code>source</code>
  # attribute, <code>purge =&gt; true</code> will automatically purge any files
  # that are not in the source directory.</li>
  #   <li>When managing files in a directory as individual resources,
  # setting <code>purge =&gt; true</code> will purge any files that aren’t being
  # specifically managed.</li>
  # </ul>
  #
  # <p>If you have a filebucket configured, the purged files will be uploaded,
  # but if you do not, this will destroy data.</p>
  #
  # <p>Unless <code>force =&gt; true</code> is set, purging will <strong>not</strong> delete directories,
  # although it will delete the files they contain.</p>
  #
  # <p>If <code>recurselimit</code> is set and you aren’t using <code>force =&gt; true</code>, purging
  # will obey the recursion limit; files in any subdirectories deeper than the
  # limit will be treated as unmanaged and left alone.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $purge,

  # <h2>recurse</h2>
  # <p>Whether to recursively manage the <em>contents</em> of a directory. This attribute
  # is only used when <code>ensure =&gt; directory</code> is set. The allowed values are:</p>
  #
  # <ul>
  #   <li><code>false</code> — The default behavior. The contents of the directory will not be
  # automatically managed.</li>
  #   <li>
  #     <p><code>remote</code> — If the <code>source</code> attribute is set, Puppet will automatically
  # manage the contents of the source directory (or directories), ensuring
  # that equivalent files and directories exist on the target system and
  # that their contents match.</p>
  #
  #     <p>Using <code>remote</code> will disable the <code>purge</code> attribute, but results in faster
  # catalog application than <code>recurse =&gt; true</code>.</p>
  #
  #     <p>The <code>source</code> attribute is mandatory when <code>recurse =&gt; remote</code>.</p>
  #   </li>
  #   <li>
  #     <p><code>true</code> — If the <code>source</code> attribute is set, this behaves similarly to
  # <code>recurse =&gt; remote</code>, automatically managing files from the source directory.</p>
  #
  #     <p>This also enables the <code>purge</code> attribute, which can delete unmanaged
  # files from a directory. See the description of <code>purge</code> for more details.</p>
  #
  #     <p>The <code>source</code> attribute is not mandatory when using <code>recurse =&gt; true</code>, so you
  # can enable purging in directories where all files are managed individually.</p>
  #   </li>
  # </ul>
  #
  # <p>By default, setting recurse to <code>remote</code> or <code>true</code> will manage <em>all</em>
  # subdirectories. You can use the <code>recurselimit</code> attribute to limit the
  # recursion depth.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>remote</code>.</p>

  $recurse,

  # <h2>recurselimit</h2>
  # <p>How far Puppet should descend into subdirectories, when using
  # <code>ensure =&gt; directory</code> and either <code>recurse =&gt; true</code> or <code>recurse =&gt; remote</code>.
  # The recursion limit affects which files will be copied from the <code>source</code>
  # directory, as well as which files can be purged when <code>purge =&gt; true</code>.</p>
  #
  # <p>Setting <code>recurselimit =&gt; 0</code> is the same as setting <code>recurse =&gt; false</code> —
  # Puppet will manage the directory, but all of its contents will be treated
  # as unmanaged.</p>
  #
  # <p>Setting <code>recurselimit =&gt; 1</code> will manage files and directories that are
  # directly inside the directory, but will not manage the contents of any
  # subdirectories.</p>
  #
  # <p>Setting <code>recurselimit =&gt; 2</code> will manage the direct contents of the
  # directory, as well as the contents of the <em>first</em> level of subdirectories.</p>
  #
  # <p>And so on — 3 will manage the contents of the second level of
  # subdirectories, etc.</p>
  #
  # <p>Values can match <code>/^[0-9]+$/</code>.</p>

  $recurselimit,

  # <h2>replace</h2>
  # <p>Whether to replace a file or symlink that already exists on the local system but
  # whose content doesn’t match what the <code>source</code> or <code>content</code> attribute
  # specifies.  Setting this to false allows file resources to initialize files
  # without overwriting future changes.  Note that this only affects content;
  # Puppet will still manage ownership and permissions. Defaults to <code>true</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $replace,

  # <h2>selinux_ignore_defaults</h2>
  # <p>If this is set then Puppet will not ask SELinux (via matchpathcon) to
  # supply defaults for the SELinux attributes (seluser, selrole,
  # seltype, and selrange). In general, you should leave this set at its
  # default and only set it to true when you need Puppet to not try to fix
  # SELinux labels automatically.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $selinux_ignore_defaults,

  # <h2>selrange</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>What the SELinux range component of the context of the file should be.
  # Any valid SELinux range component is accepted.  For example <code>s0</code> or
  # <code>SystemHigh</code>.  If not specified it defaults to the value returned by
  # matchpathcon for the file, if any exists.  Only valid on systems with
  # SELinux support enabled and that have support for MCS (Multi-Category
  # Security).</p>

  $selrange,

  # <h2>selrole</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>What the SELinux role component of the context of the file should be.
  # Any valid SELinux role component is accepted.  For example <code>role_r</code>.
  # If not specified it defaults to the value returned by matchpathcon for
  # the file, if any exists.  Only valid on systems with SELinux support
  # enabled.</p>

  $selrole,

  # <h2>seltype</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>What the SELinux type component of the context of the file should be.
  # Any valid SELinux type component is accepted.  For example <code>tmp_t</code>.
  # If not specified it defaults to the value returned by matchpathcon for
  # the file, if any exists.  Only valid on systems with SELinux support
  # enabled.</p>

  $seltype,

  # <h2>seluser</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>What the SELinux user component of the context of the file should be.
  # Any valid SELinux user component is accepted.  For example <code>user_u</code>.
  # If not specified it defaults to the value returned by matchpathcon for
  # the file, if any exists.  Only valid on systems with SELinux support
  # enabled.</p>

  $seluser,

  # <h2>show_diff</h2>
  # <p>Whether to display differences when the file changes, defaulting to
  # true.  This parameter is useful for files that may contain passwords or
  # other secret data, which might otherwise be included in Puppet reports or
  # other insecure outputs.  If the global <code>show_diff</code> setting
  # is false, then no diffs will be shown even if this parameter is true.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $show_diff,

  # <h2>source</h2>
  # <p>A source file, which will be copied into place on the local system. This
  # attribute is mutually exclusive with <code>content</code> and <code>target</code>. Allowed
  # values are:</p>
  #
  # <ul>
  #   <li><code>puppet:</code> URIs, which point to files in modules or Puppet file server
  # mount points.</li>
  #   <li>Fully qualified paths to locally available files (including files on NFS
  # shares or Windows mapped drives).</li>
  #   <li><code>file:</code> URIs, which behave the same as local file paths.</li>
  #   <li><code>http:</code> URIs, which point to files served by common web servers</li>
  # </ul>
  #
  # <p>The normal form of a <code>puppet:</code> URI is:</p>
  #
  # <p><code>puppet:///modules/&lt;MODULE NAME&gt;/&lt;FILE PATH&gt;</code></p>
  #
  # <p>This will fetch a file from a module on the Puppet master (or from a
  # local module when using Puppet apply). Given a <code>modulepath</code> of
  # <code>/etc/puppetlabs/code/modules</code>, the example above would resolve to
  # <code>/etc/puppetlabs/code/modules/&lt;MODULE NAME&gt;/files/&lt;FILE PATH&gt;</code>.</p>
  #
  # <p>Unlike <code>content</code>, the <code>source</code> attribute can be used to recursively copy
  # directories if the <code>recurse</code> attribute is set to <code>true</code> or <code>remote</code>. If
  # a source directory contains symlinks, use the <code>links</code> attribute to
  # specify whether to recreate links or follow them.</p>
  #
  # <p><em>HTTP</em> URIs cannot be used to recursively synchronize whole directory
  # trees. It is also not possible to use <code>source_permissions</code> values other
  # than <code>ignore</code>. That’s because HTTP servers do not transfer any metadata
  # that translates to ownership or permission details.</p>
  #
  # <p>Multiple <code>source</code> values can be specified as an array, and Puppet will
  # use the first source that exists. This can be used to serve different
  # files to different system types:</p>
  #
  # <pre><code>file { '/etc/nfs.conf':
  #   source =&gt; [
  #     "puppet:///modules/nfs/conf.${host}",
  #     "puppet:///modules/nfs/conf.${operatingsystem}",
  #     'puppet:///modules/nfs/conf'
  #   ]
  # }
  # </code></pre>
  #
  # <p>Alternately, when serving directories recursively, multiple sources can
  # be combined by setting the <code>sourceselect</code> attribute to <code>all</code>.</p>

  $source,

  # <h2>source_permissions</h2>
  # <p>Whether (and how) Puppet should copy owner, group, and mode permissions from
  # the <code>source</code> to <code>file</code> resources when the permissions are not explicitly
  # specified. (In all cases, explicit permissions will take precedence.)
  # Valid values are <code>use</code>, <code>use_when_creating</code>, and <code>ignore</code>:</p>
  #
  # <ul>
  #   <li><code>ignore</code> (the default) will never apply the owner, group, or mode from
  # the <code>source</code> when managing a file. When creating new files without explicit
  # permissions, the permissions they receive will depend on platform-specific
  # behavior. On POSIX, Puppet will use the umask of the user it is running as.
  # On Windows, Puppet will use the default DACL associated with the user it is
  # running as.</li>
  #   <li><code>use</code> will cause Puppet to apply the owner, group,
  # and mode from the <code>source</code> to any files it is managing.</li>
  #   <li><code>use_when_creating</code> will only apply the owner, group, and mode from the
  # <code>source</code> when creating a file; existing files will not have their permissions
  # overwritten.</li>
  # </ul>
  #
  # <p>Valid values are <code>use</code>, <code>use_when_creating</code>, <code>ignore</code>.</p>

  $source_permissions,

  # <h2>sourceselect</h2>
  # <p>Whether to copy all valid sources, or just the first one.  This parameter
  # only affects recursive directory copies; by default, the first valid
  # source is the only one used, but if this parameter is set to <code>all</code>, then
  # all valid sources will have all of their contents copied to the local
  # system. If a given file exists in more than one source, the version from
  # the earliest source in the list will be used.</p>
  #
  # <p>Valid values are <code>first</code>, <code>all</code>.</p>

  $sourceselect,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target for creating a link.  Currently, symlinks are the
  # only type supported. This attribute is mutually exclusive with <code>source</code>
  # and <code>content</code>.</p>
  #
  # <p>Symlink targets can be relative, as well as absolute:</p>
  #
  # <pre><code># (Useful on Solaris)
  # file { '/etc/inetd.conf':
  #   ensure =&gt; link,
  #   target =&gt; 'inet/inetd.conf',
  # }
  # </code></pre>
  #
  # <p>Directories of symlinks can be served recursively by instead using the
  # <code>source</code> attribute, setting <code>ensure</code> to <code>directory</code>, and setting the
  # <code>links</code> attribute to <code>manage</code>.</p>
  #
  # <p>Valid values are <code>notlink</code>. Values can match <code>/./</code>.</p>

  $target,

  # <h2>type</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A read-only state to check the file type.</p>

  $type,

  # <h2>validate_cmd</h2>
  # <p>A command for validating the file’s syntax before replacing it. If
  # Puppet would need to rewrite a file due to new <code>source</code> or <code>content</code>, it
  # will check the new content’s validity first. If validation fails, the file
  # resource will fail.</p>
  #
  # <p>This command must have a fully qualified path, and should contain a
  # percent (<code>%</code>) token where it would expect an input file. It must exit <code>0</code>
  # if the syntax is correct, and non-zero otherwise. The command will be
  # run on the target system while applying the catalog, not on the puppet master.</p>
  #
  # <p>Example:</p>
  #
  # <pre><code>file { '/etc/apache2/apache2.conf':
  #   content      =&gt; 'example',
  #   validate_cmd =&gt; '/usr/sbin/apache2 -t -f %',
  # }
  # </code></pre>
  #
  # <p>This would replace apache2.conf only if the test returned true.</p>
  #
  # <p>Note that if a validation command requires a <code>%</code> as part of its text,
  # you can specify a different placeholder token with the
  # <code>validate_replacement</code> attribute.</p>

  $validate_cmd,

  # <h2>validate_replacement</h2>
  # <p>The replacement string in a <code>validate_cmd</code> that will be replaced
  # with an input file name. Defaults to: <code>%</code></p>

  $validate_replacement,

){}

# <h2>filebucket</h2>
# <p>A repository for storing and retrieving file content by MD5 checksum. Can
# be local to each agent node, or centralized on a puppet master server. All
# puppet masters provide a filebucket service that agent nodes can access
# via HTTP, but you must declare a filebucket resource before any agents
# will do so.</p>
#
# <p>Filebuckets are used for the following features:</p>
#
# <ul>
#   <li><strong>Content backups.</strong> If the <code>file</code> type’s <code>backup</code> attribute is set to
# the name of a filebucket, Puppet will back up the <em>old</em> content whenever
# it rewrites a file; see the documentation for the <code>file</code> type for more
# details. These backups can be used for manual recovery of content, but
# are more commonly used to display changes and differences in a tool like
# Puppet Dashboard.</li>
#   <li><strong>Content distribution.</strong> The optional static compiler populates the
# puppet master’s filebucket with the <em>desired</em> content for each file,
# then instructs the agent to retrieve the content for a specific
# checksum. For more details,
# <a href="https://docs.puppetlabs.com/puppet/latest/reference/indirection.html#catalog">see the <code>static_compiler</code> section in the catalog indirection docs</a>.</li>
# </ul>
#
# <p>To use a central filebucket for backups, you will usually want to declare
# a filebucket resource and a resource default for the <code>backup</code> attribute
# in site.pp:</p>
#
# <pre><code># /etc/puppetlabs/puppet/manifests/site.pp
# filebucket { 'main':
#   path   =&gt; false,                # This is required for remote filebuckets.
#   server =&gt; 'puppet.example.com', # Optional; defaults to the configured puppet master.
# }
#
# File { backup =&gt; main, }
# </code></pre>
#
# <p>Puppet master servers automatically provide the filebucket service, so
# this will work in a default configuration. If you have a heavily
# restricted <code>auth.conf</code> file, you may need to allow access to the
# <code>file_bucket_file</code> endpoint.</p>
define filebucket(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the filebucket.</p>

  $name,

  # <h2>path</h2>
  # <p>The path to the <em>local</em> filebucket; defaults to the value of the
  # <code>clientbucketdir</code> setting.  To use a remote filebucket, you <em>must</em> set
  # this attribute to <code>false</code>.</p>

  $path,

  # <h2>port</h2>
  # <p>The port on which the remote server is listening. Defaults to the
  # value of the <code>masterport</code> setting, which is usually 8140.</p>

  $port,

  # <h2>server</h2>
  # <p>The server providing the remote filebucket service. Defaults to the
  # value of the <code>server</code> setting (that is, the currently configured
  # puppet master server).</p>
  #
  # <p>This setting is <em>only</em> consulted if the <code>path</code> attribute is set to <code>false</code>.</p>

  $server,

){}

# <h2>group</h2>
# <p>Manage groups. On most platforms this can only create groups.
# Group membership must be managed on individual users.</p>
#
# <p>On some platforms such as OS X, group membership is managed as an
# attribute of the group, not the user record. Providers must have
# the feature ‘manages_members’ to manage the ‘members’ property of
# a group record.</p>
# <h3>Providers</h3>
# <h4 id="group-provider-aix">aix</h4>
#
# <p>Group management for AIX.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/chgroup</code>, <code>/usr/bin/mkgroup</code>, <code>/usr/sbin/lsgroup</code>, <code>/usr/sbin/rmgroup</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>aix</code>.</li>
#   <li>Supported features: <code>manages_aix_lam</code>, <code>manages_members</code>.</li>
# </ul>
#
# <h4 id="group-provider-directoryservice">directoryservice</h4>
#
# <p>Group management using DirectoryService on OS X.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/dscl</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>darwin</code>.</li>
#   <li>Supported features: <code>manages_members</code>.</li>
# </ul>
#
# <h4 id="group-provider-groupadd">groupadd</h4>
#
# <p>Group management via <code>groupadd</code> and its ilk. The default for most platforms.</p>
#
# <ul>
#   <li>Required binaries: <code>groupadd</code>, <code>groupdel</code>, <code>groupmod</code>, <code>lgroupadd</code>.</li>
#   <li>Supported features: <code>system_groups</code>.</li>
# </ul>
#
# <h4 id="group-provider-ldap">ldap</h4>
#
# <p>Group management via LDAP.</p>
#
# <p>This provider requires that you have valid values for all of the
# LDAP-related settings in <code>puppet.conf</code>, including <code>ldapbase</code>.  You will
# almost definitely need settings for <code>ldapuser</code> and <code>ldappassword</code> in order
# for your clients to write to LDAP.</p>
#
# <p>Note that this provider will automatically generate a GID for you if you do
# not specify one, but it is a potentially expensive operation, as it
# iterates across all existing groups to pick the appropriate next one.</p>
#
# <h4 id="group-provider-pw">pw</h4>
#
# <p>Group management via <code>pw</code> on FreeBSD and DragonFly BSD.</p>
#
# <ul>
#   <li>Required binaries: <code>pw</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>freebsd, dragonfly</code>.</li>
#   <li>Supported features: <code>manages_members</code>.</li>
# </ul>
#
# <h4 id="group-provider-windows_adsi">windows_adsi</h4>
#
# <p>Local group management for Windows. Group members can be both users and groups.
# Additionally, local groups can contain domain users.</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>windows</code>.</li>
#   <li>Supported features: <code>manages_members</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>libuser</code> — Allows local groups to be managed on systems that also use some other remote NSS method of managing accounts.</li>
#   <li><code>manages_aix_lam</code> — The provider can manage AIX Loadable Authentication Module (LAM) system.</li>
#   <li><code>manages_members</code> — For directories where membership is an attribute of groups not users.</li>
#   <li><code>system_groups</code> — The provider allows you to create system groups with lower GIDs.</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>libuser</th>
#       <th>manages aix lam</th>
#       <th>manages members</th>
#       <th>system groups</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>aix</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>directoryservice</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>groupadd</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>ldap</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pw</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>windows_adsi</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#   </tbody>
# </table>
define group(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The group name. While naming limitations vary by operating system,
  # it is advisable to restrict names to the lowest common denominator,
  # which is a maximum of 8 characters beginning with a letter.</p>
  #
  # <p>Note that Puppet considers group names to be case-sensitive, regardless
  # of the platform’s own rules; be sure to always use the same case when
  # referring to a given group.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Create or remove the group.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>allowdupe</h2>
  # <p>Whether to allow duplicate GIDs. Defaults to <code>false</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $allowdupe,

  # <h2>attribute_membership</h2>
  # <p>AIX only. Configures the behavior of the <code>attributes</code> parameter.</p>
  #
  # <ul>
  #   <li><code>minimum</code> (default) — The provided list of attributes is partial, and Puppet
  # <strong>ignores</strong> any attributes that aren’t listed there.</li>
  #   <li><code>inclusive</code> — The provided list of attributes is comprehensive, and
  # Puppet <strong>purges</strong> any attributes that aren’t listed there.</li>
  # </ul>
  #
  # <p>Valid values are <code>inclusive</code>, <code>minimum</code>.</p>

  $attribute_membership,

  # <h2>attributes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Specify group AIX attributes, as an array of <code>'key=value'</code> strings. This
  # parameter’s behavior can be configured with <code>attribute_membership</code>.</p>
  #
  # <p>Requires features manages_aix_lam.</p>

  $attributes,

  # <h2>auth_membership</h2>
  # <p>Configures the behavior of the <code>members</code> parameter.</p>
  #
  # <ul>
  #   <li><code>false</code> (default) — The provided list of group members is partial,
  # and Puppet <strong>ignores</strong> any members that aren’t listed there.</li>
  #   <li><code>true</code> — The provided list of of group members is comprehensive, and
  # Puppet <strong>purges</strong> any members that aren’t listed there.</li>
  # </ul>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $auth_membership,

  # <h2>forcelocal</h2>
  # <p>Forces the management of local accounts when accounts are also
  # being managed by some other NSS</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>
  #
  # <p>Requires features libuser.</p>

  $forcelocal,

  # <h2>gid</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The group ID.  Must be specified numerically.  If no group ID is
  # specified when creating a new group, then one will be chosen
  # automatically according to local system standards. This will likely
  # result in the same group having different GIDs on different systems,
  # which is not recommended.</p>
  #
  # <p>On Windows, this property is read-only and will return the group’s security
  # identifier (SID).</p>

  $gid,

  # <h2>ia_load_module</h2>
  # <p>The name of the I&amp;A module to use to manage this user</p>
  #
  # <p>Requires features manages_aix_lam.</p>

  $ia_load_module,

  # <h2>members</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The members of the group. For platforms or directory services where group
  # membership is stored in the group objects, not the users. This parameter’s
  # behavior can be configured with <code>auth_membership</code>.</p>
  #
  # <p>Requires features manages_members.</p>

  $members,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>group</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#group-provider-aix"><code>aix</code></a></li>
  #   <li><a href="#group-provider-directoryservice"><code>directoryservice</code></a></li>
  #   <li><a href="#group-provider-groupadd"><code>groupadd</code></a></li>
  #   <li><a href="#group-provider-ldap"><code>ldap</code></a></li>
  #   <li><a href="#group-provider-pw"><code>pw</code></a></li>
  #   <li><a href="#group-provider-windows_adsi"><code>windows_adsi</code></a></li>
  # </ul>

  $provider,

  # <h2>system</h2>
  # <p>Whether the group is a system group with lower GID.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $system,

){}

# <h2>host</h2>
# <p>Installs and manages host entries.  For most systems, these
# entries will just be in <code>/etc/hosts</code>, but some systems (notably OS X)
# will have different solutions.</p>
# <h3>Providers</h3>
# <h4 id="host-provider-parsed">parsed</h4>
define host(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The host name.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>comment</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A comment that will be attached to the line with a # character.</p>

  $comment,

  # <h2>host_aliases</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Any aliases the host might have.  Multiple values must be
  # specified as an array.</p>

  $host_aliases,

  # <h2>ip</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The host’s IP address, IPv4 or IPv6.</p>

  $ip,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>host</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#host-provider-parsed"><code>parsed</code></a></li>
  # </ul>

  $provider,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The file in which to store service information.  Only used by
  # those providers that write to disk. On most systems this defaults to <code>/etc/hosts</code>.</p>

  $target,

){}

# <h2>interface</h2>
# <p>This represents a router or switch interface. It is possible to manage
# interface mode (access or trunking, native vlan and encapsulation) and
# switchport characteristics (speed, duplex).</p>
# <h3>Providers</h3>
# <h4 id="interface-provider-cisco">cisco</h4>
#
# <p>Cisco switch/router provider for interface.</p>
define interface(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The interface’s name.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code> (also called <code>no_shutdown</code>), <code>absent</code> (also called <code>shutdown</code>).</p>

  $ensure,

  # <h2>access_vlan</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Interface static access vlan.</p>
  #
  # <p>Values can match <code>/^\d+/</code>.</p>

  $access_vlan,

  # <h2>allowed_trunk_vlans</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Allowed list of Vlans that this trunk can forward.</p>
  #
  # <p>Valid values are <code>all</code>. Values can match <code>/./</code>.</p>

  $allowed_trunk_vlans,

  # <h2>description</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Interface description.</p>

  $description,

  # <h2>device_url</h2>
  # <p>The URL at which the router or switch can be reached.</p>

  $device_url,

  # <h2>duplex</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Interface duplex.</p>
  #
  # <p>Valid values are <code>auto</code>, <code>full</code>, <code>half</code>.</p>

  $duplex,

  # <h2>encapsulation</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Interface switchport encapsulation.</p>
  #
  # <p>Valid values are <code>none</code>, <code>dot1q</code>, <code>isl</code>, <code>negotiate</code>.</p>

  $encapsulation,

  # <h2>etherchannel</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Channel group this interface is part of.</p>
  #
  # <p>Values can match <code>/^\d+/</code>.</p>

  $etherchannel,

  # <h2>ipaddress</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>IP Address of this interface. Note that it might not be possible to set
  # an interface IP address; it depends on the interface type and device type.</p>
  #
  # <p>Valid format of ip addresses are:</p>
  #
  # <ul>
  #   <li>IPV4, like 127.0.0.1</li>
  #   <li>IPV4/prefixlength like 127.0.1.1/24</li>
  #   <li>IPV6/prefixlength like FE80::21A:2FFF:FE30:ECF0/128</li>
  #   <li>an optional suffix for IPV6 addresses from this list: <code>eui-64</code>, <code>link-local</code></li>
  # </ul>
  #
  # <p>It is also possible to supply an array of values.</p>

  $ipaddress,

  # <h2>mode</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Interface switchport mode.</p>
  #
  # <p>Valid values are <code>access</code>, <code>trunk</code>, <code>dynamic auto</code>, <code>dynamic desirable</code>.</p>

  $mode,

  # <h2>native_vlan</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Interface native vlan when trunking.</p>
  #
  # <p>Values can match <code>/^\d+/</code>.</p>

  $native_vlan,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>interface</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#interface-provider-cisco"><code>cisco</code></a></li>
  # </ul>

  $provider,

  # <h2>speed</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Interface speed.</p>
  #
  # <p>Valid values are <code>auto</code>. Values can match <code>/^\d+/</code>.</p>

  $speed,

){}

# <h2>k5login</h2>
# <p>Manage the <code>.k5login</code> file for a user.  Specify the full path to
# the <code>.k5login</code> file as the name, and an array of principals as the
# <code>principals</code> attribute.</p>
# <h3>Providers</h3>
# <h4 id="k5login-provider-k5login">k5login</h4>
#
# <p>The k5login provider is the only provider for the k5login
# type.</p>
define k5login(
  # <h2>path</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The path to the <code>.k5login</code> file to manage.  Must be fully qualified.</p>

  $path,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>mode</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The desired permissions mode of the <code>.k5login</code> file. Defaults to <code>644</code>.</p>

  $mode,

  # <h2>principals</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The principals present in the <code>.k5login</code> file. This should be specified as an array.</p>

  $principals,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>k5login</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#k5login-provider-k5login"><code>k5login</code></a></li>
  # </ul>

  $provider,

){}

# <h2>macauthorization</h2>
# <p>Manage the Mac OS X authorization database. See the
# <a href="https://developer.apple.com/documentation/Security/Conceptual/Security_Overview/Security_Services/chapter_4_section_5.html">Apple developer site</a>
# for more information.</p>
#
# <p>Note that authorization store directives with hyphens in their names have
# been renamed to use underscores, as Puppet does not react well to hyphens
# in identifiers.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the <code>/etc/authorization</code> file, each
# macauthorization resource will autorequire it.</p>
# <h3>Providers</h3>
# <h4 id="macauthorization-provider-macauthorization">macauthorization</h4>
#
# <p>Manage Mac OS X authorization database rules and rights.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/security</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>darwin</code>.</li>
# </ul>
define macauthorization(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the right or rule to be managed.
  # Corresponds to <code>key</code> in Authorization Services. The key is the name
  # of a rule. A key uses the same naming conventions as a right. The
  # Security Server uses a rule’s key to match the rule with a right.
  # Wildcard keys end with a ‘.’. The generic rule has an empty key value.
  # Any rights that do not match a specific rule use the generic rule.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>allow_root</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Corresponds to <code>allow-root</code> in the authorization store. Specifies
  # whether a right should be allowed automatically if the requesting process
  # is running with <code>uid == 0</code>.  AuthorizationServices defaults this attribute
  # to false if not specified.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $allow_root,

  # <h2>auth_class</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Corresponds to <code>class</code> in the authorization store; renamed due
  # to ‘class’ being a reserved word in Puppet.</p>
  #
  # <p>Valid values are <code>user</code>, <code>evaluate-mechanisms</code>, <code>allow</code>, <code>deny</code>, <code>rule</code>.</p>

  $auth_class,

  # <h2>auth_type</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Type — this can be a <code>right</code> or a <code>rule</code>. The <code>comment</code> type has
  # not yet been implemented.</p>
  #
  # <p>Valid values are <code>right</code>, <code>rule</code>.</p>

  $auth_type,

  # <h2>authenticate_user</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Corresponds to <code>authenticate-user</code> in the authorization store.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $authenticate_user,

  # <h2>comment</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The <code>comment</code> attribute for authorization resources.</p>

  $comment,

  # <h2>group</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A group which the user must authenticate as a member of. This
  # must be a single group.</p>

  $group,

  # <h2>k_of_n</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>How large a subset of rule mechanisms must succeed for successful
  # authentication. If there are ‘n’ mechanisms, then ‘k’ (the integer value
  # of this parameter) mechanisms must succeed. The most common setting for
  # this parameter is <code>1</code>. If <code>k-of-n</code> is not set, then every mechanism —
  # that is, ‘n-of-n’ — must succeed.</p>

  $k_of_n,

  # <h2>mechanisms</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>An array of suitable mechanisms.</p>

  $mechanisms,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>macauthorization</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#macauthorization-provider-macauthorization"><code>macauthorization</code></a></li>
  # </ul>

  $provider,

  # <h2>rule</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The rule(s) that this right refers to.</p>

  $rule,

  # <h2>session_owner</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether the session owner automatically matches this rule or right.
  # Corresponds to <code>session-owner</code> in the authorization store.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $session_owner,

  # <h2>shared</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether the Security Server should mark the credentials used to gain
  # this right as shared. The Security Server may use any shared credentials
  # to authorize this right. For maximum security, set sharing to false so
  # credentials stored by the Security Server for one application may not be
  # used by another application.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $shared,

  # <h2>timeout</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The number of seconds in which the credential used by this rule will
  # expire. For maximum security where the user must authenticate every time,
  # set the timeout to 0. For minimum security, remove the timeout attribute
  # so the user authenticates only once per session.</p>

  $timeout,

  # <h2>tries</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The number of tries allowed.</p>

  $tries,

){}

# <h2>mailalias</h2>
# <p>Creates an email alias in the local alias database.</p>
# <h3>Providers</h3>
# <h4 id="mailalias-provider-aliases">aliases</h4>
define mailalias(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The alias name.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>file</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A file containing the alias’s contents.  The file and the
  # recipient entries are mutually exclusive.</p>

  $file,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>mailalias</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#mailalias-provider-aliases"><code>aliases</code></a></li>
  # </ul>

  $provider,

  # <h2>recipient</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Where email should be sent.  Multiple values
  # should be specified as an array.  The file and the
  # recipient entries are mutually exclusive.</p>

  $recipient,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The file in which to store the aliases.  Only used by
  # those providers that write to disk.</p>

  $target,

){}

# <h2>maillist</h2>
# <p>Manage email lists.  This resource type can only create
# and remove lists; it cannot currently reconfigure them.</p>
# <h3>Providers</h3>
# <h4 id="maillist-provider-mailman">mailman</h4>
#
# <ul>
#   <li>Required binaries: <code>/var/lib/mailman/mail/mailman</code>, <code>list_lists</code>, <code>newlist</code>, <code>rmlist</code>.</li>
# </ul>
define maillist(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the email list.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>, <code>purged</code>.</p>

  $ensure,

  # <h2>admin</h2>
  # <p>The email address of the administrator.</p>

  $admin,

  # <h2>description</h2>
  # <p>The description of the mailing list.</p>

  $description,

  # <h2>mailserver</h2>
  # <p>The name of the host handling email for the list.</p>

  $mailserver,

  # <h2>password</h2>
  # <p>The admin password.</p>

  $password,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>maillist</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#maillist-provider-mailman"><code>mailman</code></a></li>
  # </ul>

  $provider,

  # <h2>webserver</h2>
  # <p>The name of the host providing web archives and the administrative interface.</p>

  $webserver,

){}

# <h2>mcx</h2>
# <p>MCX object management using DirectoryService on OS X.</p>
#
# <p>The default provider of this type merely manages the XML plist as
# reported by the <code>dscl -mcxexport</code> command.  This is similar to the
# content property of the file type in Puppet.</p>
#
# <p>The recommended method of using this type is to use Work Group Manager
# to manage users and groups on the local computer, record the resulting
# puppet manifest using the command <code>puppet resource mcx</code>, then deploy it
# to other machines.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the user, group, or computer that these
# MCX settings refer to, the MCX resource will autorequire that user, group, or computer.</p>
# <h3>Providers</h3>
# <h4 id="mcx-provider-mcxcontent">mcxcontent</h4>
#
# <p>MCX Settings management using DirectoryService on OS X.</p>
#
# <p>This provider manages the entire MCXSettings attribute available
# to some directory services nodes.  This management is ‘all or nothing’
# in that discrete application domain key value pairs are not managed
# by this provider.</p>
#
# <p>It is recommended to use WorkGroup Manager to configure Users, Groups,
# Computers, or ComputerLists, then use ‘ralsh mcx’ to generate a puppet
# manifest from the resulting configuration.</p>
#
# <p>Original Author: Jeff McCune (mccune.jeff@gmail.com)</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/dscl</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>darwin</code>.</li>
#   <li>Supported features: <code>manages_content</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>manages_content</code> — The provider can manage MCXSettings as a string.</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>manages content</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>mcxcontent</td>
#       <td><em>X</em> </td>
#     </tr>
#   </tbody>
# </table>
define mcx(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the resource being managed.
  # The default naming convention follows Directory Service paths:</p>
  #
  # <pre><code>/Computers/localhost
  # /Groups/admin
  # /Users/localadmin
  # </code></pre>
  #
  # <p>The <code>ds_type</code> and <code>ds_name</code> type parameters are not necessary if the
  # default naming convention is followed.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Create or remove the MCX setting.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>content</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The XML Plist used as the value of MCXSettings in DirectoryService.
  # This is the standard output from the system command:</p>
  #
  # <pre><code>dscl localhost -mcxexport /Local/Default/&lt;ds_type&gt;/ds_name
  # </code></pre>
  #
  # <p>Note that <code>ds_type</code> is capitalized and plural in the dscl command.</p>
  #
  # <p>Requires features manages_content.</p>

  $content,

  # <h2>ds_name</h2>
  # <p>The name to attach the MCX Setting to. (For example, <code>localhost</code>
  # when <code>ds_type =&gt; computer</code>.) This setting is not required, as it can be
  # automatically discovered when the resource name is parseable.  (For
  # example, in <code>/Groups/admin</code>, <code>group</code> will be used as the dstype.)</p>

  $ds_name,

  # <h2>ds_type</h2>
  # <p>The DirectoryService type this MCX setting attaches to.</p>
  #
  # <p>Valid values are <code>user</code>, <code>group</code>, <code>computer</code>, <code>computerlist</code>.</p>

  $ds_type,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>mcx</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#mcx-provider-mcxcontent"><code>mcxcontent</code></a></li>
  # </ul>

  $provider,

){}

# <h2>mount</h2>
# <p>Manages mounted filesystems, including putting mount
# information into the mount table. The actual behavior depends
# on the value of the ‘ensure’ parameter.</p>
#
# <p><strong>Refresh:</strong> <code>mount</code> resources can respond to refresh events (via
# <code>notify</code>, <code>subscribe</code>, or the <code>~&gt;</code> arrow). If a <code>mount</code> receives an event
# from another resource <strong>and</strong> its <code>ensure</code> attribute is set to <code>mounted</code>,
# Puppet will try to unmount then remount that filesystem.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing any parents of a mount resource —
# that is, other mount points higher up in the filesystem — the child
# mount will autorequire them.</p>
#
# <p><strong>Autobefores:</strong>  If Puppet is managing any child file paths of a mount
# point, the mount resource will autobefore them.</p>
# <h3>Providers</h3>
# <h4 id="mount-provider-parsed">parsed</h4>
#
# <ul>
#   <li>Required binaries: <code>mount</code>, <code>umount</code>.</li>
#   <li>Supported features: <code>refreshable</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>refreshable</code> — The provider can remount the filesystem.</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>refreshable</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>parsed</td>
#       <td><em>X</em> </td>
#     </tr>
#   </tbody>
# </table>
define mount(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The mount path for the mount.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Control what to do with this mount. Set this attribute to
  # <code>unmounted</code> to make sure the filesystem is in the filesystem table
  # but not mounted (if the filesystem is currently mounted, it will be
  # unmounted).  Set it to <code>absent</code> to unmount (if necessary) and remove
  # the filesystem from the fstab.  Set to <code>mounted</code> to add it to the
  # fstab and mount it. Set to <code>present</code> to add to fstab but not change
  # mount/unmount status.</p>
  #
  # <p>Valid values are <code>defined</code> (also called <code>present</code>), <code>unmounted</code>, <code>absent</code>, <code>mounted</code>.</p>

  $ensure,

  # <h2>atboot</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether to mount the mount at boot.  Not all platforms
  # support this.</p>

  $atboot,

  # <h2>blockdevice</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The device to fsck.  This is property is only valid
  # on Solaris, and in most cases will default to the correct
  # value.</p>

  $blockdevice,

  # <h2>device</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The device providing the mount.  This can be whatever
  # device is supporting by the mount, including network
  # devices or devices specified by UUID rather than device
  # path, depending on the operating system.</p>

  $device,

  # <h2>dump</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether to dump the mount.  Not all platform support this.
  # Valid values are <code>1</code> or <code>0</code> (or <code>2</code> on FreeBSD). Default is <code>0</code>.</p>
  #
  # <p>Values can match <code>/(0|1)/</code>.</p>

  $dump,

  # <h2>fstype</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The mount type.  Valid values depend on the
  # operating system.  This is a required option.</p>

  $fstype,

  # <h2>options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A single string containing options for the mount, as they would
  # appear in fstab. For many platforms this is a comma delimited string.
  # Consult the fstab(5) man page for system-specific details.</p>

  $options,

  # <h2>pass</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The pass in which the mount is checked.</p>

  $pass,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>mount</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#mount-provider-parsed"><code>parsed</code></a></li>
  # </ul>

  $provider,

  # <h2>remounts</h2>
  # <p>Whether the mount can be remounted  <code>mount -o remount</code>.  If
  # this is false, then the filesystem will be unmounted and remounted
  # manually, which is prone to failure.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $remounts,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The file in which to store the mount table.  Only used by
  # those providers that write to disk.</p>

  $target,

){}

# <h2>nagios_command</h2>
# <p>The Nagios type command.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_command.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_command-provider-naginator">naginator</h4>
define nagios_command(
  # <h2>command_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_command resource.</p>

  $command_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>command_line</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $command_line,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_command resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_command resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_command resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>poller_tag</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $poller_tag,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_command</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_command-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_contact</h2>
# <p>The Nagios type contact.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_contact.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_contact-provider-naginator">naginator</h4>
define nagios_contact(
  # <h2>contact_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_contact resource.</p>

  $contact_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>address1</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $address1,

  # <h2>address2</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $address2,

  # <h2>address3</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $address3,

  # <h2>address4</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $address4,

  # <h2>address5</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $address5,

  # <h2>address6</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $address6,

  # <h2>alias</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $alias,

  # <h2>can_submit_commands</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $can_submit_commands,

  # <h2>contactgroups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contactgroups,

  # <h2>email</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $email,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_contact resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>host_notification_commands</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_notification_commands,

  # <h2>host_notification_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_notification_options,

  # <h2>host_notification_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_notification_period,

  # <h2>host_notifications_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_notifications_enabled,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_contact resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_contact resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>pager</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $pager,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_contact</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_contact-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>retain_nonstatus_information</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retain_nonstatus_information,

  # <h2>retain_status_information</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retain_status_information,

  # <h2>service_notification_commands</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_notification_commands,

  # <h2>service_notification_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_notification_options,

  # <h2>service_notification_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_notification_period,

  # <h2>service_notifications_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_notifications_enabled,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_contactgroup</h2>
# <p>The Nagios type contactgroup.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_contactgroup.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_contactgroup-provider-naginator">naginator</h4>
define nagios_contactgroup(
  # <h2>contactgroup_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_contactgroup resource.</p>

  $contactgroup_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>alias</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $alias,

  # <h2>contactgroup_members</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contactgroup_members,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_contactgroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>members</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $members,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_contactgroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_contactgroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_contactgroup</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_contactgroup-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_host</h2>
# <p>The Nagios type host.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_host.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_host-provider-naginator">naginator</h4>
define nagios_host(
  # <h2>host_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_host resource.</p>

  $host_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>action_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $action_url,

  # <h2>active_checks_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $active_checks_enabled,

  # <h2>address</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $address,

  # <h2>alias</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $alias,

  # <h2>business_impact</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $business_impact,

  # <h2>check_command</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_command,

  # <h2>check_freshness</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_freshness,

  # <h2>check_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_interval,

  # <h2>check_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_period,

  # <h2>contact_groups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contact_groups,

  # <h2>contacts</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contacts,

  # <h2>display_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $display_name,

  # <h2>event_handler</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $event_handler,

  # <h2>event_handler_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $event_handler_enabled,

  # <h2>failure_prediction_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $failure_prediction_enabled,

  # <h2>first_notification_delay</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $first_notification_delay,

  # <h2>flap_detection_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $flap_detection_enabled,

  # <h2>flap_detection_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $flap_detection_options,

  # <h2>freshness_threshold</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $freshness_threshold,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_host resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>high_flap_threshold</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $high_flap_threshold,

  # <h2>hostgroups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $hostgroups,

  # <h2>icon_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image,

  # <h2>icon_image_alt</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image_alt,

  # <h2>initial_state</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $initial_state,

  # <h2>low_flap_threshold</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $low_flap_threshold,

  # <h2>max_check_attempts</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $max_check_attempts,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_host resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes,

  # <h2>notes_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes_url,

  # <h2>notification_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_interval,

  # <h2>notification_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_options,

  # <h2>notification_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_period,

  # <h2>notifications_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notifications_enabled,

  # <h2>obsess_over_host</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $obsess_over_host,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_host resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>parents</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $parents,

  # <h2>passive_checks_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $passive_checks_enabled,

  # <h2>poller_tag</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $poller_tag,

  # <h2>process_perf_data</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $process_perf_data,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_host</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_host-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>realm</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $realm,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>retain_nonstatus_information</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retain_nonstatus_information,

  # <h2>retain_status_information</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retain_status_information,

  # <h2>retry_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retry_interval,

  # <h2>stalking_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $stalking_options,

  # <h2>statusmap_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $statusmap_image,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

  # <h2>vrml_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $vrml_image,

){}

# <h2>nagios_hostdependency</h2>
# <p>The Nagios type hostdependency.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_hostdependency.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_hostdependency-provider-naginator">naginator</h4>
define nagios_hostdependency(
  # <h2>_naginator_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_hostdependency resource.</p>

  $_naginator_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>dependency_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $dependency_period,

  # <h2>dependent_host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $dependent_host_name,

  # <h2>dependent_hostgroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $dependent_hostgroup_name,

  # <h2>execution_failure_criteria</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $execution_failure_criteria,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_hostdependency resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_name,

  # <h2>hostgroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $hostgroup_name,

  # <h2>inherits_parent</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $inherits_parent,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_hostdependency resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notification_failure_criteria</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_failure_criteria,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_hostdependency resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_hostdependency</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_hostdependency-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_hostescalation</h2>
# <p>The Nagios type hostescalation.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_hostescalation.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_hostescalation-provider-naginator">naginator</h4>
define nagios_hostescalation(
  # <h2>_naginator_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_hostescalation resource.</p>

  $_naginator_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>contact_groups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contact_groups,

  # <h2>contacts</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contacts,

  # <h2>escalation_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $escalation_options,

  # <h2>escalation_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $escalation_period,

  # <h2>first_notification</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $first_notification,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_hostescalation resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_name,

  # <h2>hostgroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $hostgroup_name,

  # <h2>last_notification</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $last_notification,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_hostescalation resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notification_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_interval,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_hostescalation resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_hostescalation</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_hostescalation-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_hostextinfo</h2>
# <p>The Nagios type hostextinfo.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_hostextinfo.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_hostextinfo-provider-naginator">naginator</h4>
define nagios_hostextinfo(
  # <h2>host_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_hostextinfo resource.</p>

  $host_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_hostextinfo resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>icon_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image,

  # <h2>icon_image_alt</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image_alt,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_hostextinfo resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes,

  # <h2>notes_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes_url,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_hostextinfo resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_hostextinfo</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_hostextinfo-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>statusmap_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $statusmap_image,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

  # <h2>vrml_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $vrml_image,

){}

# <h2>nagios_hostgroup</h2>
# <p>The Nagios type hostgroup.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_hostgroup.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_hostgroup-provider-naginator">naginator</h4>
define nagios_hostgroup(
  # <h2>hostgroup_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_hostgroup resource.</p>

  $hostgroup_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>action_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $action_url,

  # <h2>alias</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $alias,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_hostgroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>hostgroup_members</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $hostgroup_members,

  # <h2>members</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $members,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_hostgroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes,

  # <h2>notes_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes_url,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_hostgroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_hostgroup</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_hostgroup-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>realm</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $realm,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_service</h2>
# <p>The Nagios type service.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_service.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_service-provider-naginator">naginator</h4>
define nagios_service(
  # <h2>_naginator_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_service resource.</p>

  $_naginator_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>action_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $action_url,

  # <h2>active_checks_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $active_checks_enabled,

  # <h2>business_impact</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $business_impact,

  # <h2>check_command</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_command,

  # <h2>check_freshness</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_freshness,

  # <h2>check_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_interval,

  # <h2>check_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $check_period,

  # <h2>contact_groups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contact_groups,

  # <h2>contacts</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contacts,

  # <h2>display_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $display_name,

  # <h2>event_handler</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $event_handler,

  # <h2>event_handler_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $event_handler_enabled,

  # <h2>failure_prediction_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $failure_prediction_enabled,

  # <h2>first_notification_delay</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $first_notification_delay,

  # <h2>flap_detection_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $flap_detection_enabled,

  # <h2>flap_detection_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $flap_detection_options,

  # <h2>freshness_threshold</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $freshness_threshold,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_service resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>high_flap_threshold</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $high_flap_threshold,

  # <h2>host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_name,

  # <h2>hostgroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $hostgroup_name,

  # <h2>icon_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image,

  # <h2>icon_image_alt</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image_alt,

  # <h2>initial_state</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $initial_state,

  # <h2>is_volatile</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $is_volatile,

  # <h2>low_flap_threshold</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $low_flap_threshold,

  # <h2>max_check_attempts</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $max_check_attempts,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_service resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>normal_check_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $normal_check_interval,

  # <h2>notes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes,

  # <h2>notes_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes_url,

  # <h2>notification_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_interval,

  # <h2>notification_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_options,

  # <h2>notification_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_period,

  # <h2>notifications_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notifications_enabled,

  # <h2>obsess_over_service</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $obsess_over_service,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_service resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>parallelize_check</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $parallelize_check,

  # <h2>passive_checks_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $passive_checks_enabled,

  # <h2>poller_tag</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $poller_tag,

  # <h2>process_perf_data</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $process_perf_data,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_service</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_service-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>retain_nonstatus_information</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retain_nonstatus_information,

  # <h2>retain_status_information</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retain_status_information,

  # <h2>retry_check_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retry_check_interval,

  # <h2>retry_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $retry_interval,

  # <h2>service_description</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_description,

  # <h2>servicegroups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $servicegroups,

  # <h2>stalking_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $stalking_options,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_servicedependency</h2>
# <p>The Nagios type servicedependency.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_servicedependency.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_servicedependency-provider-naginator">naginator</h4>
define nagios_servicedependency(
  # <h2>_naginator_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_servicedependency resource.</p>

  $_naginator_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>dependency_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $dependency_period,

  # <h2>dependent_host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $dependent_host_name,

  # <h2>dependent_hostgroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $dependent_hostgroup_name,

  # <h2>dependent_service_description</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $dependent_service_description,

  # <h2>execution_failure_criteria</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $execution_failure_criteria,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_servicedependency resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_name,

  # <h2>hostgroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $hostgroup_name,

  # <h2>inherits_parent</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $inherits_parent,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_servicedependency resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notification_failure_criteria</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_failure_criteria,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_servicedependency resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_servicedependency</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_servicedependency-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>service_description</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_description,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_serviceescalation</h2>
# <p>The Nagios type serviceescalation.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_serviceescalation.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_serviceescalation-provider-naginator">naginator</h4>
define nagios_serviceescalation(
  # <h2>_naginator_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_serviceescalation resource.</p>

  $_naginator_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>contact_groups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contact_groups,

  # <h2>contacts</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $contacts,

  # <h2>escalation_options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $escalation_options,

  # <h2>escalation_period</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $escalation_period,

  # <h2>first_notification</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $first_notification,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_serviceescalation resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_name,

  # <h2>hostgroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $hostgroup_name,

  # <h2>last_notification</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $last_notification,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_serviceescalation resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notification_interval</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notification_interval,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_serviceescalation resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_serviceescalation</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_serviceescalation-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>service_description</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_description,

  # <h2>servicegroup_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $servicegroup_name,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_serviceextinfo</h2>
# <p>The Nagios type serviceextinfo.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_serviceextinfo.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_serviceextinfo-provider-naginator">naginator</h4>
define nagios_serviceextinfo(
  # <h2>_naginator_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_serviceextinfo resource.</p>

  $_naginator_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>action_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $action_url,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_serviceextinfo resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>host_name</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $host_name,

  # <h2>icon_image</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image,

  # <h2>icon_image_alt</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $icon_image_alt,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_serviceextinfo resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes,

  # <h2>notes_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes_url,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_serviceextinfo resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_serviceextinfo</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_serviceextinfo-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>service_description</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $service_description,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_servicegroup</h2>
# <p>The Nagios type servicegroup.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_servicegroup.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_servicegroup-provider-naginator">naginator</h4>
define nagios_servicegroup(
  # <h2>servicegroup_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_servicegroup resource.</p>

  $servicegroup_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>action_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $action_url,

  # <h2>alias</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $alias,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_servicegroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>members</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $members,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_servicegroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>notes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes,

  # <h2>notes_url</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $notes_url,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_servicegroup resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_servicegroup</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_servicegroup-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>servicegroup_members</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $servicegroup_members,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

){}

# <h2>nagios_timeperiod</h2>
# <p>The Nagios type timeperiod.  This resource type is autogenerated using the
# model developed in Naginator, and all of the Nagios types are generated using the
# same code and the same library.</p>
#
# <p>This type generates Nagios configuration statements in Nagios-parseable configuration
# files.  By default, the statements will be added to <code>/etc/nagios/nagios_timeperiod.cfg</code>, but
# you can send them to a different file by setting their <code>target</code> attribute.</p>
#
# <p>You can purge Nagios resources using the <code>resources</code> type, but <em>only</em>
# in the default file locations.  This is an architectural limitation.</p>
# <h3>Providers</h3>
# <h4 id="nagios_timeperiod-provider-naginator">naginator</h4>
define nagios_timeperiod(
  # <h2>timeperiod_name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of this nagios_timeperiod resource.</p>

  $timeperiod_name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>alias</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $alias,

  # <h2>exclude</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $exclude,

  # <h2>friday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $friday,

  # <h2>group</h2>
  # <p>The desired group of the config file for this nagios_timeperiod resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $group,

  # <h2>mode</h2>
  # <p>The desired mode of the config file for this nagios_timeperiod resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $mode,

  # <h2>monday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $monday,

  # <h2>owner</h2>
  # <p>The desired owner of the config file for this nagios_timeperiod resource.</p>
  #
  # <p>NOTE: If the target file is explicitly managed by a file resource in your manifest,
  # this parameter has no effect. If a parent directory of the target is managed by
  # a recursive file resource, this limitation does not apply (i.e., this parameter
  # takes precedence, and if purge is used, the target file is exempt).</p>

  $owner,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>nagios_timeperiod</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#nagios_timeperiod-provider-naginator"><code>naginator</code></a></li>
  # </ul>

  $provider,

  # <h2>register</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $register,

  # <h2>saturday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $saturday,

  # <h2>sunday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $sunday,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The target.</p>

  $target,

  # <h2>thursday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $thursday,

  # <h2>tuesday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $tuesday,

  # <h2>use</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $use,

  # <h2>wednesday</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Nagios configuration file parameter.</p>

  $wednesday,

){}

# <h2>notify</h2>
# <p>Sends an arbitrary message to the agent run-time log.</p>
define notify(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>An arbitrary tag for your own reference; the name of the message.</p>

  $name,

  # <h2>message</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The message to be sent to the log.</p>

  $message,

  # <h2>withpath</h2>
  # <p>Whether to show the full object path. Defaults to false.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $withpath,

){}

# <h2>package</h2>
# <p>Manage packages.  There is a basic dichotomy in package
# support right now:  Some package types (e.g., yum and apt) can
# retrieve their own package files, while others (e.g., rpm and sun)
# cannot.  For those package formats that cannot retrieve their own files,
# you can use the <code>source</code> parameter to point to the correct file.</p>
#
# <p>Puppet will automatically guess the packaging format that you are
# using based on the platform you are on, but you can override it
# using the <code>provider</code> parameter; each provider defines what it
# requires in order to function, and you must meet those requirements
# to use a given provider.</p>
#
# <p>You can declare multiple package resources with the same <code>name</code>, as long
# as they specify different providers and have unique titles.</p>
#
# <p>Note that you must use the <em>title</em> to make a reference to a package
# resource; <code>Package[&lt;NAME&gt;]</code> is not a synonym for <code>Package[&lt;TITLE&gt;]</code> like
# it is for many other resource types.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the files specified as a
# package’s <code>adminfile</code>, <code>responsefile</code>, or <code>source</code>, the package
# resource will autorequire those files.</p>
# <h3>Providers</h3>
# <h4 id="package-provider-aix">aix</h4>
#
# <p>Installation from an AIX software directory, using the AIX <code>installp</code>
# command.  The <code>source</code> parameter is required for this provider, and should
# be set to the absolute path (on the puppet agent machine) of a directory
# containing one or more BFF package files.</p>
#
# <p>The <code>installp</code> command will generate a table of contents file (named <code>.toc</code>)
# in this directory, and the <code>name</code> parameter (or resource title) that you
# specify for your <code>package</code> resource must match a package name that exists
# in the <code>.toc</code> file.</p>
#
# <p>Note that package downgrades are <em>not</em> supported; if your resource specifies
# a specific version number and there is already a newer version of the package
# installed on the machine, the resource will fail with an error message.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/lslpp</code>, <code>/usr/sbin/installp</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>aix</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-appdmg">appdmg</h4>
#
# <p>Package management which copies application bundles to a target.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/curl</code>, <code>/usr/bin/ditto</code>, <code>/usr/bin/hdiutil</code>.</li>
#   <li>Supported features: <code>installable</code>.</li>
# </ul>
#
# <h4 id="package-provider-apple">apple</h4>
#
# <p>Package management based on OS X’s built-in packaging system.  This is
# essentially the simplest and least functional package system in existence –
# it only supports installation; no deletion or upgrades.  The provider will
# automatically add the <code>.pkg</code> extension, so leave that off when specifying
# the package name.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/installer</code>.</li>
#   <li>Supported features: <code>installable</code>.</li>
# </ul>
#
# <h4 id="package-provider-apt">apt</h4>
#
# <p>Package management via <code>apt-get</code>.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to apt-get.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/apt-cache</code>, <code>/usr/bin/apt-get</code>, <code>/usr/bin/debconf-set-selections</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>debian</code>.</li>
#   <li>Supported features: <code>holdable</code>, <code>install_options</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-aptitude">aptitude</h4>
#
# <p>Package management via <code>aptitude</code>.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/apt-cache</code>, <code>/usr/bin/aptitude</code>.</li>
#   <li>Supported features: <code>holdable</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-aptrpm">aptrpm</h4>
#
# <p>Package management via <code>apt-get</code> ported to <code>rpm</code>.</p>
#
# <ul>
#   <li>Required binaries: <code>apt-cache</code>, <code>apt-get</code>, <code>rpm</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-blastwave">blastwave</h4>
#
# <p>Package management using Blastwave.org’s <code>pkg-get</code> command on Solaris.</p>
#
# <ul>
#   <li>Required binaries: <code>pkg-get</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-dnf">dnf</h4>
#
# <p>Support via <code>dnf</code>.</p>
#
# <p>Using this provider’s <code>uninstallable</code> feature will not remove dependent packages. To
# remove dependent packages with this provider use the <code>purgeable</code> feature, but note this
# feature is destructive and should be used with the utmost care.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to dnf.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>dnf</code>, <code>rpm</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>fedora</code> and <code>operatingsystemmajrelease</code> == <code>22, 23, 24, 25</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>, <code>virtual_packages</code>.</li>
# </ul>
#
# <h4 id="package-provider-dpkg">dpkg</h4>
#
# <p>Package management via <code>dpkg</code>.  Because this only uses <code>dpkg</code>
# and not <code>apt</code>, you must specify the source of any packages you want
# to manage.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/dpkg-deb</code>, <code>/usr/bin/dpkg-query</code>, <code>/usr/bin/dpkg</code>.</li>
#   <li>Supported features: <code>holdable</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-fink">fink</h4>
#
# <p>Package management via <code>fink</code>.</p>
#
# <ul>
#   <li>Required binaries: <code>/sw/bin/apt-cache</code>, <code>/sw/bin/apt-get</code>, <code>/sw/bin/dpkg-query</code>, <code>/sw/bin/fink</code>.</li>
#   <li>Supported features: <code>holdable</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-freebsd">freebsd</h4>
#
# <p>The specific form of package management on FreeBSD.  This is an
# extremely quirky packaging system, in that it freely mixes between
# ports and packages.  Apparently all of the tools are written in Ruby,
# so there are plans to rewrite this support to directly use those
# libraries.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/pkg_add</code>, <code>/usr/sbin/pkg_delete</code>, <code>/usr/sbin/pkg_info</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-gem">gem</h4>
#
# <p>Ruby Gem support.  If a URL is passed via <code>source</code>, then that URL is used as the
# remote gem repository; if a source is present but is not a valid URL, it will be
# interpreted as the path to a local gem file.  If source is not present at all,
# the gem will be installed from the default gem repositories.</p>
#
# <p>This provider supports the <code>install_options</code> and <code>uninstall_options</code> attributes,
# which allow command-line flags to be passed to the gem command.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>gem</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstall_options</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-hpux">hpux</h4>
#
# <p>HP-UX’s packaging system.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/swinstall</code>, <code>/usr/sbin/swlist</code>, <code>/usr/sbin/swremove</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>hp-ux</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>.</li>
# </ul>
#
# <h4 id="package-provider-macports">macports</h4>
#
# <p>Package management using MacPorts on OS X.</p>
#
# <p>Supports MacPorts versions and revisions, but not variants.
# Variant preferences may be specified using
# <a href="http://guide.macports.org/chunked/internals.configuration-files.html#internals.configuration-files.variants-conf">the MacPorts variants.conf file</a>.</p>
#
# <p>When specifying a version in the Puppet DSL, only specify the version, not the revision.
# Revisions are only used internally for ensuring the latest version/revision of a port.</p>
#
# <ul>
#   <li>Required binaries: <code>/opt/local/bin/port</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-nim">nim</h4>
#
# <p>Installation from an AIX NIM LPP source.  The <code>source</code> parameter is required
# for this provider, and should specify the name of a NIM <code>lpp_source</code> resource
# that is visible to the puppet agent machine.  This provider supports the
# management of both BFF/installp and RPM packages.</p>
#
# <p>Note that package downgrades are <em>not</em> supported; if your resource specifies
# a specific version number and there is already a newer version of the package
# installed on the machine, the resource will fail with an error message.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/lslpp</code>, <code>/usr/sbin/nimclient</code>, <code>rpm</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-openbsd">openbsd</h4>
#
# <p>OpenBSD’s form of <code>pkg_add</code> support.</p>
#
# <p>This provider supports the <code>install_options</code> and <code>uninstall_options</code>
# attributes, which allow command-line flags to be passed to pkg_add and pkg_delete.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>pkg_add</code>, <code>pkg_delete</code>, <code>pkg_info</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>openbsd</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstall_options</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-opkg">opkg</h4>
#
# <p>Opkg packaging support. Common on OpenWrt and OpenEmbedded platforms</p>
#
# <ul>
#   <li>Required binaries: <code>opkg</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>openwrt</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-pacman">pacman</h4>
#
# <p>Support for the Package Manager Utility (pacman) used in Archlinux.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to pacman.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/pacman</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>archlinux, manjarolinux</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstall_options</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>virtual_packages</code>.</li>
# </ul>
#
# <h4 id="package-provider-pip">pip</h4>
#
# <p>Python packages via <code>pip</code>.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to pip.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-pip3">pip3</h4>
#
# <p>Python packages via <code>pip3</code>.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to pip3.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-pkg">pkg</h4>
#
# <p>OpenSolaris image packaging system. See pkg(5) for more information.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/pkg</code>.</li>
#   <li>Default for <code>kernelrelease</code> == <code>5.11, 5.12</code> and <code>osfamily</code> == <code>solaris</code>.</li>
#   <li>Supported features: <code>holdable</code>, <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-pkgdmg">pkgdmg</h4>
#
# <p>Package management based on Apple’s Installer.app and DiskUtility.app.</p>
#
# <p>This provider works by checking the contents of a DMG image for Apple pkg or
# mpkg files. Any number of pkg or mpkg files may exist in the root directory
# of the DMG file system, and Puppet will install all of them. Subdirectories
# are not checked for packages.</p>
#
# <p>This provider can also accept plain .pkg (but not .mpkg) files in addition
# to .dmg files.</p>
#
# <p>Notes:</p>
#
# <ul>
#   <li>The <code>source</code> attribute is mandatory. It must be either a local disk path
# or an HTTP, HTTPS, or FTP URL to the package.</li>
#   <li>The <code>name</code> of the resource must be the filename (without path) of the DMG file.</li>
#   <li>When installing the packages from a DMG, this provider writes a file to
# disk at <code>/var/db/.puppet_pkgdmg_installed_NAME</code>. If that file is present,
# Puppet assumes all packages from that DMG are already installed.</li>
#   <li>
#     <p>This provider is not versionable and uses DMG filenames to determine
# whether a package has been installed. Thus, to install new a version of a
# package, you must create a new DMG with a different filename.</p>
#   </li>
#   <li>Required binaries: <code>/usr/bin/curl</code>, <code>/usr/bin/hdiutil</code>, <code>/usr/sbin/installer</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>darwin</code>.</li>
#   <li>Supported features: <code>installable</code>.</li>
# </ul>
#
# <h4 id="package-provider-pkgin">pkgin</h4>
#
# <p>Package management using pkgin, a binary package manager for pkgsrc.</p>
#
# <ul>
#   <li>Required binaries: <code>pkgin</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>dragonfly, smartos, netbsd</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-pkgng">pkgng</h4>
#
# <p>A PkgNG provider for FreeBSD and DragonFly.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/local/sbin/pkg</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>freebsd</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-pkgutil">pkgutil</h4>
#
# <p>Package management using Peter Bonivart’s <code>pkgutil</code> command on Solaris.</p>
#
# <ul>
#   <li>Required binaries: <code>pkgutil</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-portage">portage</h4>
#
# <p>Provides packaging support for Gentoo’s portage system.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/eix-update</code>, <code>/usr/bin/eix</code>, <code>/usr/bin/emerge</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>gentoo</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>reinstallable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-ports">ports</h4>
#
# <p>Support for FreeBSD’s ports.  Note that this, too, mixes packages and ports.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/local/sbin/pkg_deinstall</code>, <code>/usr/local/sbin/portupgrade</code>, <code>/usr/local/sbin/portversion</code>, <code>/usr/sbin/pkg_info</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-portupgrade">portupgrade</h4>
#
# <p>Support for FreeBSD’s ports using the portupgrade ports management software.
# Use the port’s full origin as the resource name. eg (ports-mgmt/portupgrade)
# for the portupgrade port.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/local/sbin/pkg_deinstall</code>, <code>/usr/local/sbin/portinstall</code>, <code>/usr/local/sbin/portupgrade</code>, <code>/usr/local/sbin/portversion</code>, <code>/usr/sbin/pkg_info</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-puppet_gem">puppet_gem</h4>
#
# <p>Puppet Ruby Gem support. This provider is useful for managing
# gems needed by the ruby provided in the puppet-agent package.</p>
#
# <ul>
#   <li>Required binaries: <code>/opt/puppetlabs/puppet/bin/gem</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstall_options</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-rpm">rpm</h4>
#
# <p>RPM packaging support; should work anywhere with a working <code>rpm</code>
# binary.</p>
#
# <p>This provider supports the <code>install_options</code> and <code>uninstall_options</code>
# attributes, which allow command-line flags to be passed to rpm.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>rpm</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstall_options</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>, <code>virtual_packages</code>.</li>
# </ul>
#
# <h4 id="package-provider-rug">rug</h4>
#
# <p>Support for suse <code>rug</code> package manager.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/rug</code>, <code>rpm</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-sun">sun</h4>
#
# <p>Sun’s packaging system.  Requires that you specify the source for
# the packages you’re managing.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to pkgadd.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/pkginfo</code>, <code>/usr/sbin/pkgadd</code>, <code>/usr/sbin/pkgrm</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>solaris</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-sunfreeware">sunfreeware</h4>
#
# <p>Package management using sunfreeware.com’s <code>pkg-get</code> command on Solaris.
# At this point, support is exactly the same as <code>blastwave</code> support and
# has not actually been tested.</p>
#
# <ul>
#   <li>Required binaries: <code>pkg-get</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-tdnf">tdnf</h4>
#
# <p>Support via <code>tdnf</code>.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to tdnf.
# These options should be spcified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}), or an
# array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>rpm</code>, <code>tdnf</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>PhotonOS</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>, <code>virtual_packages</code>.</li>
# </ul>
#
# <h4 id="package-provider-up2date">up2date</h4>
#
# <p>Support for Red Hat’s proprietary <code>up2date</code> package update
# mechanism.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/up2date-nox</code>.</li>
#   <li>Default for <code>lsbdistrelease</code> == <code>2.1, 3, 4</code> and <code>osfamily</code> == <code>redhat</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>.</li>
# </ul>
#
# <h4 id="package-provider-urpmi">urpmi</h4>
#
# <p>Support via <code>urpmi</code>.</p>
#
# <ul>
#   <li>Required binaries: <code>rpm</code>, <code>urpme</code>, <code>urpmi</code>, <code>urpmq</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>mandriva, mandrake</code>.</li>
#   <li>Supported features: <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-windows">windows</h4>
#
# <p>Windows package management.</p>
#
# <p>This provider supports either MSI or self-extracting executable installers.</p>
#
# <p>This provider requires a <code>source</code> attribute when installing the package.
# It accepts paths to local files, mapped drives, or UNC paths.</p>
#
# <p>This provider supports the <code>install_options</code> and <code>uninstall_options</code>
# attributes, which allow command-line flags to be passed to the installer.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <p>If the executable requires special arguments to perform a silent install or
# uninstall, then the appropriate arguments should be specified using the
# <code>install_options</code> or <code>uninstall_options</code> attributes, respectively.  Puppet
# will automatically quote any option that contains spaces.</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>windows</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstall_options</code>, <code>uninstallable</code>, <code>versionable</code>.</li>
# </ul>
#
# <h4 id="package-provider-yum">yum</h4>
#
# <p>Support via <code>yum</code>.</p>
#
# <p>Using this provider’s <code>uninstallable</code> feature will not remove dependent packages. To
# remove dependent packages with this provider use the <code>purgeable</code> feature, but note this
# feature is destructive and should be used with the utmost care.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to yum.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>rpm</code>, <code>yum</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>redhat</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>purgeable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>, <code>virtual_packages</code>.</li>
# </ul>
#
# <h4 id="package-provider-zypper">zypper</h4>
#
# <p>Support for SuSE <code>zypper</code> package manager. Found in SLES10sp2+ and SLES11.</p>
#
# <p>This provider supports the <code>install_options</code> attribute, which allows command-line flags to be passed to zypper.
# These options should be specified as a string (e.g. ‘–flag’), a hash (e.g. {‘–flag’ =&gt; ‘value’}),
# or an array where each element is either a string or a hash.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/zypper</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>suse, sles, sled, opensuse</code>.</li>
#   <li>Supported features: <code>install_options</code>, <code>installable</code>, <code>uninstallable</code>, <code>upgradeable</code>, <code>versionable</code>, <code>virtual_packages</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>holdable</code> — The provider is capable of placing packages on hold such that they are not automatically upgraded as a result of other package dependencies unless explicit action is taken by a user or another package. Held is considered a superset of installed.</li>
#   <li><code>install_options</code> — The provider accepts options to be passed to the installer command.</li>
#   <li><code>installable</code> — The provider can install packages.</li>
#   <li><code>package_settings</code> — The provider accepts package_settings to be ensured for the given package. The meaning and format of these settings is provider-specific.</li>
#   <li><code>purgeable</code> — The provider can purge packages.  This generally means that all traces of the package are removed, including existing configuration files.  This feature is thus destructive and should be used with the utmost care.</li>
#   <li><code>reinstallable</code> — The provider can reinstall packages.</li>
#   <li><code>uninstall_options</code> — The provider accepts options to be passed to the uninstaller command.</li>
#   <li><code>uninstallable</code> — The provider can uninstall packages.</li>
#   <li><code>upgradeable</code> — The provider can upgrade to the latest version of a package.  This feature is used by specifying <code>latest</code> as the desired value for the package.</li>
#   <li><code>versionable</code> — The provider is capable of interrogating the package database for installed version(s), and can select which out of a set of available versions of a package to install if asked.</li>
#   <li><code>virtual_packages</code> — The provider accepts virtual package names for install and uninstall.</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>holdable</th>
#       <th>install options</th>
#       <th>installable</th>
#       <th>package settings</th>
#       <th>purgeable</th>
#       <th>reinstallable</th>
#       <th>uninstall options</th>
#       <th>uninstallable</th>
#       <th>upgradeable</th>
#       <th>versionable</th>
#       <th>virtual packages</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>aix</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>appdmg</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>apple</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>apt</td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>aptitude</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>aptrpm</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>blastwave</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>dnf</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>dpkg</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>fink</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>freebsd</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>gem</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>hpux</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>macports</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>nim</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>openbsd</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>opkg</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pacman</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>pip</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pip3</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pkg</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pkgdmg</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pkgin</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pkgng</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>pkgutil</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>portage</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>ports</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>portupgrade</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>puppet_gem</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>rpm</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>rug</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>sun</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>sunfreeware</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>tdnf</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>up2date</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>urpmi</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>windows</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>yum</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>zypper</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#     </tr>
#   </tbody>
# </table>
define package(
  # <h2>provider</h2>
  # <p><em>(<strong>Secondary namevar:</strong> This resource type allows you to manage multiple resources with the same name as long as their providers are different.)</em></p>
  #
  # <p>The specific backend to use for this <code>package</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#package-provider-aix"><code>aix</code></a></li>
  #   <li><a href="#package-provider-appdmg"><code>appdmg</code></a></li>
  #   <li><a href="#package-provider-apple"><code>apple</code></a></li>
  #   <li><a href="#package-provider-apt"><code>apt</code></a></li>
  #   <li><a href="#package-provider-aptitude"><code>aptitude</code></a></li>
  #   <li><a href="#package-provider-aptrpm"><code>aptrpm</code></a></li>
  #   <li><a href="#package-provider-blastwave"><code>blastwave</code></a></li>
  #   <li><a href="#package-provider-dnf"><code>dnf</code></a></li>
  #   <li><a href="#package-provider-dpkg"><code>dpkg</code></a></li>
  #   <li><a href="#package-provider-fink"><code>fink</code></a></li>
  #   <li><a href="#package-provider-freebsd"><code>freebsd</code></a></li>
  #   <li><a href="#package-provider-gem"><code>gem</code></a></li>
  #   <li><a href="#package-provider-hpux"><code>hpux</code></a></li>
  #   <li><a href="#package-provider-macports"><code>macports</code></a></li>
  #   <li><a href="#package-provider-nim"><code>nim</code></a></li>
  #   <li><a href="#package-provider-openbsd"><code>openbsd</code></a></li>
  #   <li><a href="#package-provider-opkg"><code>opkg</code></a></li>
  #   <li><a href="#package-provider-pacman"><code>pacman</code></a></li>
  #   <li><a href="#package-provider-pip3"><code>pip3</code></a></li>
  #   <li><a href="#package-provider-pip"><code>pip</code></a></li>
  #   <li><a href="#package-provider-pkg"><code>pkg</code></a></li>
  #   <li><a href="#package-provider-pkgdmg"><code>pkgdmg</code></a></li>
  #   <li><a href="#package-provider-pkgin"><code>pkgin</code></a></li>
  #   <li><a href="#package-provider-pkgng"><code>pkgng</code></a></li>
  #   <li><a href="#package-provider-pkgutil"><code>pkgutil</code></a></li>
  #   <li><a href="#package-provider-portage"><code>portage</code></a></li>
  #   <li><a href="#package-provider-ports"><code>ports</code></a></li>
  #   <li><a href="#package-provider-portupgrade"><code>portupgrade</code></a></li>
  #   <li><a href="#package-provider-puppet_gem"><code>puppet_gem</code></a></li>
  #   <li><a href="#package-provider-rpm"><code>rpm</code></a></li>
  #   <li><a href="#package-provider-rug"><code>rug</code></a></li>
  #   <li><a href="#package-provider-sun"><code>sun</code></a></li>
  #   <li><a href="#package-provider-sunfreeware"><code>sunfreeware</code></a></li>
  #   <li><a href="#package-provider-tdnf"><code>tdnf</code></a></li>
  #   <li><a href="#package-provider-up2date"><code>up2date</code></a></li>
  #   <li><a href="#package-provider-urpmi"><code>urpmi</code></a></li>
  #   <li><a href="#package-provider-windows"><code>windows</code></a></li>
  #   <li><a href="#package-provider-yum"><code>yum</code></a></li>
  #   <li><a href="#package-provider-zypper"><code>zypper</code></a></li>
  # </ul>

  $provider,

  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The package name.  This is the name that the packaging
  # system uses internally, which is sometimes (especially on Solaris)
  # a name that is basically useless to humans.  If a package goes by
  # several names, you can use a single title and then set the name
  # conditionally:</p>
  #
  # <pre><code># In the 'openssl' class
  # $ssl = $operatingsystem ? {
  #   solaris =&gt; SMCossl,
  #   default =&gt; openssl
  # }
  #
  # package { 'openssl':
  #   ensure =&gt; installed,
  #   name   =&gt; $ssl,
  # }
  #
  # . etc. .
  #
  # $ssh = $operatingsystem ? {
  #   solaris =&gt; SMCossh,
  #   default =&gt; openssh
  # }
  #
  # package { 'openssh':
  #   ensure  =&gt; installed,
  #   name    =&gt; $ssh,
  #   require =&gt; Package['openssl'],
  # }
  # </code></pre>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>What state the package should be in. On packaging systems that can
  # retrieve new packages on their own, you can choose which package to
  # retrieve by specifying a version number or <code>latest</code> as the ensure
  # value. On packaging systems that manage configuration files separately
  # from “normal” system files, you can uninstall config files by
  # specifying <code>purged</code> as the ensure value. This defaults to <code>installed</code>.</p>
  #
  # <p>Version numbers must match the full version to install, including
  # release if the provider uses a release moniker. Ranges or semver
  # patterns are not accepted except for the <code>gem</code> package provider. For
  # example, to install the bash package from the rpm
  # <code>bash-4.1.2-29.el6.x86_64.rpm</code>, use the string <code>'4.1.2-29.el6'</code>.</p>
  #
  # <p>Valid values are <code>present</code> (also called <code>installed</code>), <code>absent</code>, <code>purged</code>, <code>held</code>, <code>latest</code>. Values can match <code>/./</code>.</p>

  $ensure,

  # <h2>adminfile</h2>
  # <p>A file containing package defaults for installing packages.</p>
  #
  # <p>This attribute is only used on Solaris. Its value should be a path to a
  # local file stored on the target system. Solaris’s package tools expect
  # either an absolute file path or a relative path to a file in
  # <code>/var/sadm/install/admin</code>.</p>
  #
  # <p>The value of <code>adminfile</code> will be passed directly to the <code>pkgadd</code> or
  # <code>pkgrm</code> command with the <code>-a &lt;ADMINFILE&gt;</code> option.</p>

  $adminfile,

  # <h2>allow_virtual</h2>
  # <p>Specifies if virtual package names are allowed for install and uninstall.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>
  #
  # <p>Requires features virtual_packages.</p>

  $allow_virtual,

  # <h2>allowcdrom</h2>
  # <p>Tells apt to allow cdrom sources in the sources.list file.
  # Normally apt will bail if you try this.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $allowcdrom,

  # <h2>category</h2>
  # <p>A read-only parameter set by the package.</p>

  $category,

  # <h2>configfiles</h2>
  # <p>Whether to keep or replace modified config files when installing or
  # upgrading a package. This only affects the <code>apt</code> and <code>dpkg</code> providers.
  # Defaults to <code>keep</code>.</p>
  #
  # <p>Valid values are <code>keep</code>, <code>replace</code>.</p>

  $configfiles,

  # <h2>description</h2>
  # <p>A read-only parameter set by the package.</p>

  $description,

  # <h2>flavor</h2>
  # <p>OpenBSD supports ‘flavors’, which are further specifications for
  # which type of package you want.</p>

  $flavor,

  # <h2>install_options</h2>
  # <p>An array of additional options to pass when installing a package. These
  # options are package-specific, and should be documented by the software
  # vendor.  One commonly implemented option is <code>INSTALLDIR</code>:</p>
  #
  # <pre><code>package { 'mysql':
  #   ensure          =&gt; installed,
  #   source          =&gt; 'N:/packages/mysql-5.5.16-winx64.msi',
  #   install_options =&gt; [ '/S', { 'INSTALLDIR' =&gt; 'C:\mysql-5.5' } ],
  # }
  # </code></pre>
  #
  # <p>Each option in the array can either be a string or a hash, where each
  # key and value pair are interpreted in a provider specific way.  Each
  # option will automatically be quoted when passed to the install command.</p>
  #
  # <p>With Windows packages, note that file paths in an install option must
  # use backslashes. (Since install options are passed directly to the
  # installation command, forward slashes won’t be automatically converted
  # like they are in <code>file</code> resources.) Note also that backslashes in
  # double-quoted strings <em>must</em> be escaped and backslashes in single-quoted
  # strings <em>can</em> be escaped.</p>
  #
  # <p>Requires features install_options.</p>

  $install_options,

  # <h2>instance</h2>
  # <p>A read-only parameter set by the package.</p>

  $instance,

  # <h2>package_settings</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Settings that can change the contents or configuration of a package.</p>
  #
  # <p>The formatting and effects of package_settings are provider-specific; any
  # provider that implements them must explain how to use them in its
  # documentation. (Our general expectation is that if a package is
  # installed but its settings are out of sync, the provider should
  # re-install that package with the desired settings.)</p>
  #
  # <p>An example of how package_settings could be used is FreeBSD’s port build
  # options — a future version of the provider could accept a hash of options,
  # and would reinstall the port if the installed version lacked the correct
  # settings.</p>
  #
  # <pre><code>package { 'www/apache22':
  #   package_settings =&gt; { 'SUEXEC' =&gt; false }
  # }
  # </code></pre>
  #
  # <p>Again, check the documentation of your platform’s package provider to see
  # the actual usage.</p>
  #
  # <p>Requires features package_settings.</p>

  $package_settings,

  # <h2>platform</h2>
  # <p>A read-only parameter set by the package.</p>

  $platform,

  # <h2>reinstall_on_refresh</h2>
  # <p>Whether this resource should respond to refresh events (via <code>subscribe</code>,
  # <code>notify</code>, or the <code>~&gt;</code> arrow) by reinstalling the package. Only works for
  # providers that support the <code>reinstallable</code> feature.</p>
  #
  # <p>This is useful for source-based distributions, where you may want to
  # recompile a package if the build options change.</p>
  #
  # <p>If you use this, be careful of notifying classes when you want to restart
  # services. If the class also contains a refreshable package, doing so could
  # cause unnecessary re-installs.</p>
  #
  # <p>Defaults to <code>false</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $reinstall_on_refresh,

  # <h2>responsefile</h2>
  # <p>A file containing any necessary answers to questions asked by
  # the package.  This is currently used on Solaris and Debian.  The
  # value will be validated according to system rules, but it should
  # generally be a fully qualified path.</p>

  $responsefile,

  # <h2>root</h2>
  # <p>A read-only parameter set by the package.</p>

  $root,

  # <h2>source</h2>
  # <p>Where to find the package file. This is only used by providers that don’t
  # automatically download packages from a central repository. (For example:
  # the <code>yum</code> and <code>apt</code> providers ignore this attribute, but the <code>rpm</code> and
  # <code>dpkg</code> providers require it.)</p>
  #
  # <p>Different providers accept different values for <code>source</code>. Most providers
  # accept paths to local files stored on the target system. Some providers
  # may also accept URLs or network drive paths. Puppet will not
  # automatically retrieve source files for you, and usually just passes the
  # value of <code>source</code> to the package installation command.</p>
  #
  # <p>You can use a <code>file</code> resource if you need to manually copy package files
  # to the target system.</p>

  $source,

  # <h2>status</h2>
  # <p>A read-only parameter set by the package.</p>

  $status,

  # <h2>uninstall_options</h2>
  # <p>An array of additional options to pass when uninstalling a package. These
  # options are package-specific, and should be documented by the software
  # vendor.  For example:</p>
  #
  # <pre><code>package { 'VMware Tools':
  #   ensure            =&gt; absent,
  #   uninstall_options =&gt; [ { 'REMOVE' =&gt; 'Sync,VSS' } ],
  # }
  # </code></pre>
  #
  # <p>Each option in the array can either be a string or a hash, where each
  # key and value pair are interpreted in a provider specific way.  Each
  # option will automatically be quoted when passed to the uninstall
  # command.</p>
  #
  # <p>On Windows, this is the <strong>only</strong> place in Puppet where backslash
  # separators should be used.  Note that backslashes in double-quoted
  # strings <em>must</em> be double-escaped and backslashes in single-quoted
  # strings <em>may</em> be double-escaped.</p>
  #
  # <p>Requires features uninstall_options.</p>

  $uninstall_options,

  # <h2>vendor</h2>
  # <p>A read-only parameter set by the package.</p>

  $vendor,

){}

# <h2>resources</h2>
# <p>This is a metatype that can manage other resource types.  Any
# metaparams specified here will be passed on to any generated resources,
# so you can purge unmanaged resources but set <code>noop</code> to true so the
# purging is only logged and does not actually happen.</p>
define resources(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the type to be managed.</p>

  $name,

  # <h2>purge</h2>
  # <p>Whether to purge unmanaged resources.  When set to <code>true</code>, this will
  # delete any resource that is not specified in your configuration and is not
  # autorequired by any managed resources. <strong>Note:</strong> The <code>ssh_authorized_key</code>
  # resource type can’t be purged this way; instead, see the <code>purge_ssh_keys</code>
  # attribute of the <code>user</code> type.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $purge,

  # <h2>unless_system_user</h2>
  # <p>This keeps system users from being purged.  By default, it
  # does not purge users whose UIDs are less than the minimum UID for the system (typically 500 or 1000), but you can specify
  # a different UID as the inclusive limit.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>. Values can match <code>/^\d+$/</code>.</p>

  $unless_system_user,

  # <h2>unless_uid</h2>
  # <p>This keeps specific uids or ranges of uids from being purged when purge is true.
  # Accepts integers, integer strings, and arrays of integers or integer strings.
  # To specify a range of uids, consider using the range() function from stdlib.</p>

  $unless_uid,

){}

# <h2>router</h2>
# <p>Manages connected router.</p>
define router(
  # <h2>url</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>An SSH or telnet URL at which to access the router, in the form
  # <code>ssh://user:pass:enable@host/</code> or <code>telnet://user:pass:enable@host/</code>.</p>

  $url,

){}

# <h2>schedule</h2>
# <p>Define schedules for Puppet. Resources can be limited to a schedule by using the
# <a href="https://docs.puppetlabs.com/puppet/latest/reference/metaparameter.html#schedule"><code>schedule</code></a>
# metaparameter.</p>
#
# <p>Currently, <strong>schedules can only be used to stop a resource from being
# applied;</strong> they cannot cause a resource to be applied when it otherwise
# wouldn’t be, and they cannot accurately specify a time when a resource
# should run.</p>
#
# <p>Every time Puppet applies its configuration, it will apply the
# set of resources whose schedule does not eliminate them from
# running right then, but there is currently no system in place to
# guarantee that a given resource runs at a given time.  If you
# specify a very  restrictive schedule and Puppet happens to run at a
# time within that schedule, then the resources will get applied;
# otherwise, that work may never get done.</p>
#
# <p>Thus, it is advisable to use wider scheduling (e.g., over a couple of
# hours) combined with periods and repetitions.  For instance, if you
# wanted to restrict certain resources to only running once, between
# the hours of two and 4 AM, then you would use this schedule:</p>
#
# <pre><code>schedule { 'maint':
#   range  =&gt; '2 - 4',
#   period =&gt; daily,
#   repeat =&gt; 1,
# }
# </code></pre>
#
# <p>With this schedule, the first time that Puppet runs between 2 and 4 AM,
# all resources with this schedule will get applied, but they won’t
# get applied again between 2 and 4 because they will have already
# run once that day, and they won’t get applied outside that schedule
# because they will be outside the scheduled range.</p>
#
# <p>Puppet automatically creates a schedule for each of the valid periods
# with the same name as that period (e.g., hourly and daily).
# Additionally, a schedule named <code>puppet</code> is created and used as the
# default, with the following attributes:</p>
#
# <pre><code>schedule { 'puppet':
#   period =&gt; hourly,
#   repeat =&gt; 2,
# }
# </code></pre>
#
# <p>This will cause resources to be applied every 30 minutes by default.</p>
define schedule(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the schedule.  This name is used when assigning the schedule
  # to a resource with the <code>schedule</code> metaparameter:</p>
  #
  # <pre><code>schedule { 'everyday':
  #   period =&gt; daily,
  #   range  =&gt; '2 - 4',
  # }
  #
  # exec { '/usr/bin/apt-get update':
  #   schedule =&gt; 'everyday',
  # }
  # </code></pre>

  $name,

  # <h2>period</h2>
  # <p>The period of repetition for resources on this schedule. The default is
  # for resources to get applied every time Puppet runs.</p>
  #
  # <p>Note that the period defines how often a given resource will get
  # applied but not when; if you would like to restrict the hours
  # that a given resource can be applied (e.g., only at night during
  # a maintenance window), then use the <code>range</code> attribute.</p>
  #
  # <p>If the provided periods are not sufficient, you can provide a
  # value to the <em>repeat</em> attribute, which will cause Puppet to
  # schedule the affected resources evenly in the period the
  # specified number of times.  Take this schedule:</p>
  #
  # <pre><code>schedule { 'veryoften':
  #   period =&gt; hourly,
  #   repeat =&gt; 6,
  # }
  # </code></pre>
  #
  # <p>This can cause Puppet to apply that resource up to every 10 minutes.</p>
  #
  # <p>At the moment, Puppet cannot guarantee that level of repetition; that
  # is, the resource can applied <em>up to</em> every 10 minutes, but internal
  # factors might prevent it from actually running that often (e.g. if a
  # Puppet run is still in progress when the next run is scheduled to start,
  # that next run will be suppressed).</p>
  #
  # <p>See the <code>periodmatch</code> attribute for tuning whether to match
  # times by their distance apart or by their specific value.</p>
  #
  # <p>Valid values are <code>hourly</code>, <code>daily</code>, <code>weekly</code>, <code>monthly</code>, <code>never</code>.</p>

  $period,

  # <h2>periodmatch</h2>
  # <p>Whether periods should be matched by number (e.g., the two times
  # are in the same hour) or by distance (e.g., the two times are
  # 60 minutes apart).</p>
  #
  # <p>Valid values are <code>number</code>, <code>distance</code>.</p>

  $periodmatch,

  # <h2>range</h2>
  # <p>The earliest and latest that a resource can be applied.  This is
  # always a hyphen-separated range within a 24 hour period, and hours
  # must be specified in numbers between 0 and 23, inclusive.  Minutes and
  # seconds can optionally be provided, using the normal colon as a
  # separator. For instance:</p>
  #
  # <pre><code>schedule { 'maintenance':
  #   range =&gt; '1:30 - 4:30',
  # }
  # </code></pre>
  #
  # <p>This is mostly useful for restricting certain resources to being
  # applied in maintenance windows or during off-peak hours. Multiple
  # ranges can be applied in array context. As a convenience when specifying
  # ranges, you may cross midnight (e.g.: range =&gt; “22:00 - 04:00”).</p>

  $range,

  # <h2>repeat</h2>
  # <p>How often a given resource may be applied in this schedule’s <code>period</code>.
  # Defaults to 1; must be an integer.</p>

  $repeat,

  # <h2>weekday</h2>
  # <p>The days of the week in which the schedule should be valid.
  # You may specify the full day name (Tuesday), the three character
  # abbreviation (Tue), or a number corresponding to the day of the
  # week where 0 is Sunday, 1 is Monday, etc. Multiple days can be specified
  # as an array. If not specified, the day of the week will not be
  # considered in the schedule.</p>
  #
  # <p>If you are also using a range match that spans across midnight
  # then this parameter will match the day that it was at the start
  # of the range, not necessarily the day that it is when it matches.
  # For example, consider this schedule:</p>
  #
  # <pre><code>schedule { 'maintenance_window':
  #   range   =&gt; '22:00 - 04:00',
  #   weekday =&gt; 'Saturday',
  # }
  # </code></pre>
  #
  # <p>This will match at 11 PM on Saturday and 2 AM on Sunday, but not
  # at 2 AM on Saturday.</p>

  $weekday,

){}

# <h2>scheduled_task</h2>
# <p>Installs and manages Windows Scheduled Tasks.  All attributes
# except <code>name</code>, <code>command</code>, and <code>trigger</code> are optional; see the description
# of the <code>trigger</code> attribute for details on setting schedules.</p>
# <h3>Providers</h3>
# <h4 id="scheduled_task-provider-win32_taskscheduler">win32_taskscheduler</h4>
#
# <p>This provider manages scheduled tasks on Windows.</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>windows</code>.</li>
# </ul>
define scheduled_task(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name assigned to the scheduled task.  This will uniquely
  # identify the task on the system.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>arguments</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Any arguments or flags that should be passed to the command. Multiple arguments
  # should be specified as a space-separated string.</p>

  $arguments,

  # <h2>command</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The full path to the application to run, without any arguments.</p>

  $command,

  # <h2>enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether the triggers for this task should be enabled. This attribute
  # affects every trigger for the task; triggers cannot be enabled or
  # disabled individually.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $enabled,

  # <h2>password</h2>
  # <p>The password for the user specified in the ‘user’ attribute.
  # This is only used if specifying a user other than ‘SYSTEM’.
  # Since there is no way to retrieve the password used to set the
  # account information for a task, this parameter will not be used
  # to determine if a scheduled task is in sync or not.</p>

  $password,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>scheduled_task</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#scheduled_task-provider-win32_taskscheduler"><code>win32_taskscheduler</code></a></li>
  # </ul>

  $provider,

  # <h2>trigger</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>One or more triggers defining when the task should run. A single trigger is
  # represented as a hash, and multiple triggers can be specified with an array of
  # hashes.</p>
  #
  # <p>A trigger can contain the following keys:</p>
  #
  # <ul>
  #   <li>For all triggers:
  #     <ul>
  #       <li><code>schedule</code> <strong>(Required)</strong> — What kind of trigger this is.
  # Valid values are <code>daily</code>, <code>weekly</code>, <code>monthly</code>, or <code>once</code>. Each kind
  # of trigger is configured with a different set of keys; see the
  # sections below. (<code>once</code> triggers only need a start time/date.)</li>
  #       <li><code>start_time</code> <strong>(Required)</strong> — The time of day when the trigger should
  # first become active. Several time formats will work, but we
  # suggest 24-hour time formatted as HH:MM.</li>
  #       <li><code>start_date</code> —  The date when the trigger should first become active.
  # Defaults to the current date. You should format dates as YYYY-MM-DD,
  # although other date formats may work. (Under the hood, this uses <code>Date.parse</code>.)</li>
  #       <li><code>minutes_interval</code> — The repeat interval in minutes.</li>
  #       <li><code>minutes_duration</code> — The duration in minutes, needs to be greater than the
  # minutes_interval.</li>
  #     </ul>
  #   </li>
  #   <li>For <code>daily</code> triggers:
  #     <ul>
  #       <li><code>every</code> — How often the task should run, as a number of days. Defaults
  # to 1. (“2” means every other day, “3” means every three days, etc.)</li>
  #     </ul>
  #   </li>
  #   <li>For <code>weekly</code> triggers:
  #     <ul>
  #       <li><code>every</code> — How often the task should run, as a number of weeks. Defaults
  # to 1. (“2” means every other week, “3” means every three weeks, etc.)</li>
  #       <li><code>day_of_week</code> — Which days of the week the task should run, as an array.
  # Defaults to all days. Each day must be one of <code>mon</code>, <code>tues</code>,
  # <code>wed</code>, <code>thurs</code>, <code>fri</code>, <code>sat</code>, <code>sun</code>, or <code>all</code>.</li>
  #     </ul>
  #   </li>
  #   <li>For <code>monthly</code> (by date) triggers:
  #     <ul>
  #       <li><code>months</code> — Which months the task should run, as an array. Defaults to
  # all months. Each month must be an integer between 1 and 12.</li>
  #       <li><code>on</code> <strong>(Required)</strong> — Which days of the month the task should run,
  # as an array. Each day must be either an integer between 1 and 31,
  # or the special value <code>last,</code> which is always the last day of the month.</li>
  #     </ul>
  #   </li>
  #   <li>For <code>monthly</code> (by weekday) triggers:
  #     <ul>
  #       <li><code>months</code> — Which months the task should run, as an array. Defaults to
  # all months. Each month must be an integer between 1 and 12.</li>
  #       <li><code>day_of_week</code> <strong>(Required)</strong> — Which day of the week the task should
  # run, as an array with only one element. Each day must be one of <code>mon</code>,
  # <code>tues</code>, <code>wed</code>, <code>thurs</code>, <code>fri</code>, <code>sat</code>, <code>sun</code>, or <code>all</code>.</li>
  #       <li><code>which_occurrence</code> <strong>(Required)</strong> — The occurrence of the chosen weekday
  # when the task should run. Must be one of <code>first</code>, <code>second</code>, <code>third</code>,
  # <code>fourth</code>, <code>fifth</code>, or <code>last</code>.</li>
  #     </ul>
  #   </li>
  # </ul>
  #
  # <p>Examples:</p>
  #
  # <pre><code># Run at 8am on the 1st, 15th, and last day of the month in January, March,
  # # May, July, September, and November, starting after August 31st, 2011.
  # trigger =&gt; {
  #   schedule   =&gt; monthly,
  #   start_date =&gt; '2011-08-31',   # Defaults to current date
  #   start_time =&gt; '08:00',        # Must be specified
  #   months     =&gt; [1,3,5,7,9,11], # Defaults to all
  #   on         =&gt; [1, 15, last],  # Must be specified
  # }
  #
  # # Run at 8am on the first Monday of the month for January, March, and May,
  # # starting after August 31st, 2011.
  # trigger =&gt; {
  #   schedule         =&gt; monthly,
  #   start_date       =&gt; '2011-08-31', # Defaults to current date
  #   start_time       =&gt; '08:00',      # Must be specified
  #   months           =&gt; [1,3,5],      # Defaults to all
  #   which_occurrence =&gt; first,        # Must be specified
  #   day_of_week      =&gt; [mon],        # Must be specified
  # }
  #
  # # Run daily repeating every 30 minutes between 9am and 5pm (480 minutes) starting after August 31st, 2011.
  # trigger =&gt; {
  #   schedule         =&gt; daily,
  #   start_date       =&gt; '2011-08-31', # Defaults to current date
  #   start_time       =&gt; '8:00',       # Must be specified
  #   minutes_interval =&gt; 30,
  #   minutes_duration =&gt; 480,
  # }
  # </code></pre>

  $trigger,

  # <h2>user</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user to run the scheduled task as.  Please note that not
  # all security configurations will allow running a scheduled task
  # as ‘SYSTEM’, and saving the scheduled task under these
  # conditions will fail with a reported error of ‘The operation
  # completed successfully’.  It is recommended that you either
  # choose another user to run the scheduled task, or alter the
  # security policy to allow v1 scheduled tasks to run as the
  # ‘SYSTEM’ account.  Defaults to ‘SYSTEM’.</p>
  #
  # <p>Please also note that Puppet must be running as a privileged user
  # in order to manage <code>scheduled_task</code> resources. Running as an
  # unprivileged user will result in ‘access denied’ errors.</p>

  $user,

  # <h2>working_dir</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The full path of the directory in which to start the command.</p>

  $working_dir,

){}

# <h2>selboolean</h2>
# <p>Manages SELinux booleans on systems with SELinux support.  The supported booleans
# are any of the ones found in <code>/selinux/booleans/</code>.</p>
# <h3>Providers</h3>
# <h4 id="selboolean-provider-getsetsebool">getsetsebool</h4>
#
# <p>Manage SELinux booleans using the getsebool and setsebool binaries.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/getsebool</code>, <code>/usr/sbin/setsebool</code>.</li>
# </ul>
define selboolean(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the SELinux boolean to be managed.</p>

  $name,

  # <h2>persistent</h2>
  # <p>If set true, SELinux booleans will be written to disk and persist across reboots.
  # The default is <code>false</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $persistent,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>selboolean</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#selboolean-provider-getsetsebool"><code>getsetsebool</code></a></li>
  # </ul>

  $provider,

  # <h2>value</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether the SELinux boolean should be enabled or disabled.</p>
  #
  # <p>Valid values are <code>on</code>, <code>off</code>.</p>

  $value,

){}

# <h2>selmodule</h2>
# <p>Manages loading and unloading of SELinux policy modules
# on the system.  Requires SELinux support.  See man semodule(8)
# for more information on SELinux policy modules.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the file containing this SELinux
# policy module (which is either explicitly specified in the <code>selmodulepath</code>
# attribute or will be found at {<code>selmoduledir</code>}/{<code>name</code>}.pp), the selmodule
# resource will autorequire that file.</p>
# <h3>Providers</h3>
# <h4 id="selmodule-provider-semodule">semodule</h4>
#
# <p>Manage SELinux policy modules using the semodule binary.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/semodule</code>.</li>
# </ul>
define selmodule(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the SELinux policy to be managed.  You should not
  # include the customary trailing .pp extension.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>selmodule</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#selmodule-provider-semodule"><code>semodule</code></a></li>
  # </ul>

  $provider,

  # <h2>selmoduledir</h2>
  # <p>The directory to look for the compiled pp module file in.
  # Currently defaults to <code>/usr/share/selinux/targeted</code>.  If the
  # <code>selmodulepath</code> attribute is not specified, Puppet will expect to find
  # the module in <code>&lt;selmoduledir&gt;/&lt;name&gt;.pp</code>, where <code>name</code> is the value of the
  # <code>name</code> parameter.</p>

  $selmoduledir,

  # <h2>selmodulepath</h2>
  # <p>The full path to the compiled .pp policy module.  You only need to use
  # this if the module file is not in the <code>selmoduledir</code> directory.</p>

  $selmodulepath,

  # <h2>syncversion</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>If set to <code>true</code>, the policy will be reloaded if the
  # version found in the on-disk file differs from the loaded
  # version.  If set to <code>false</code> (the default) the only check
  # that will be made is if the policy is loaded at all or not.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $syncversion,

){}

# <h2>service</h2>
# <p>Manage running services.  Service support unfortunately varies
# widely by platform — some platforms have very little if any concept of a
# running service, and some have a very codified and powerful concept.
# Puppet’s service support is usually capable of doing the right thing, but
# the more information you can provide, the better behaviour you will get.</p>
#
# <p>Puppet 2.7 and newer expect init scripts to have a working status command.
# If this isn’t the case for any of your services’ init scripts, you will
# need to set <code>hasstatus</code> to false and possibly specify a custom status
# command in the <code>status</code> attribute. As a last resort, Puppet will attempt to
# search the process table by calling whatever command is listed in the <code>ps</code>
# fact. The default search pattern is the name of the service, but you can
# specify it with the <code>pattern</code> attribute.</p>
#
# <p><strong>Refresh:</strong> <code>service</code> resources can respond to refresh events (via
# <code>notify</code>, <code>subscribe</code>, or the <code>~&gt;</code> arrow). If a <code>service</code> receives an
# event from another resource, Puppet will restart the service it manages.
# The actual command used to restart the service depends on the platform and
# can be configured:</p>
#
# <ul>
#   <li>If you set <code>hasrestart</code> to true, Puppet will use the init script’s restart command.</li>
#   <li>You can provide an explicit command for restarting with the <code>restart</code> attribute.</li>
#   <li>If you do neither, the service’s stop and start commands will be used.</li>
# </ul>
# <h3>Providers</h3>
# <h4 id="service-provider-base">base</h4>
#
# <p>The simplest form of Unix service support.</p>
#
# <p>You have to specify enough about your service for this to work; the
# minimum you can specify is a binary for starting the process, and this
# same binary will be searched for in the process table to stop the
# service.  As with <code>init</code>-style services, it is preferable to specify start,
# stop, and status commands.</p>
#
# <ul>
#   <li>Required binaries: <code>kill</code>.</li>
#   <li>Supported features: <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-bsd">bsd</h4>
#
# <p>Generic BSD form of <code>init</code>-style service management with <code>rc.d</code>.</p>
#
# <p>Uses <code>rc.conf.d</code> for service enabling and disabling.</p>
#
# <ul>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-daemontools">daemontools</h4>
#
# <p>Daemontools service management.</p>
#
# <p>This provider manages daemons supervised by D.J. Bernstein daemontools.
# When detecting the service directory it will check, in order of preference:</p>
#
# <ul>
#   <li><code>/service</code></li>
#   <li><code>/etc/service</code></li>
#   <li><code>/var/lib/svscan</code></li>
# </ul>
#
# <p>The daemon directory should be in one of the following locations:</p>
#
# <ul>
#   <li><code>/var/lib/service</code></li>
#   <li><code>/etc</code></li>
# </ul>
#
# <p>…or this can be overridden in the resource’s attributes:</p>
#
# <pre><code>service { 'myservice':
#   provider =&gt; 'daemontools',
#   path     =&gt; '/path/to/daemons',
# }
# </code></pre>
#
# <p>This provider supports out of the box:</p>
#
# <ul>
#   <li>start/stop (mapped to enable/disable)</li>
#   <li>enable/disable</li>
#   <li>restart</li>
#   <li>status</li>
# </ul>
#
# <p>If a service has <code>ensure =&gt; "running"</code>, it will link /path/to/daemon to
# /path/to/service, which will automatically enable the service.</p>
#
# <p>If a service has <code>ensure =&gt; "stopped"</code>, it will only shut down the service, not
# remove the <code>/path/to/service</code> link.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/svc</code>, <code>/usr/bin/svstat</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-debian">debian</h4>
#
# <p>Debian’s form of <code>init</code>-style management.</p>
#
# <p>The only differences from <code>init</code> are support for enabling and disabling
# services via <code>update-rc.d</code> and the ability to determine enabled status via
# <code>invoke-rc.d</code>.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/invoke-rc.d</code>, <code>/usr/sbin/service</code>, <code>/usr/sbin/update-rc.d</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>cumuluslinux</code> and <code>operatingsystemmajrelease</code> == <code>1, 2</code>. Default for <code>operatingsystem</code> == <code>debian</code> and <code>operatingsystemmajrelease</code> == <code>5, 6, 7</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-freebsd">freebsd</h4>
#
# <p>Provider for FreeBSD and DragonFly BSD. Uses the <code>rcvar</code> argument of init scripts and parses/edits rc files.</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>freebsd, dragonfly</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-gentoo">gentoo</h4>
#
# <p>Gentoo’s form of <code>init</code>-style service management.</p>
#
# <p>Uses <code>rc-update</code> for service enabling and disabling.</p>
#
# <ul>
#   <li>Required binaries: <code>/sbin/rc-update</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-init">init</h4>
#
# <p>Standard <code>init</code>-style service management.</p>
#
# <ul>
#   <li>Supported features: <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-launchd">launchd</h4>
#
# <p>This provider manages jobs with <code>launchd</code>, which is the default service
# framework for Mac OS X (and may be available for use on other platforms).</p>
#
# <p>For <code>launchd</code> documentation, see:</p>
#
# <ul>
#   <li><a href="https://developer.apple.com/macosx/launchd.html">https://developer.apple.com/macosx/launchd.html</a></li>
#   <li><a href="http://launchd.macosforge.org/">http://launchd.macosforge.org/</a></li>
# </ul>
#
# <p>This provider reads plists out of the following directories:</p>
#
# <ul>
#   <li><code>/System/Library/LaunchDaemons</code></li>
#   <li><code>/System/Library/LaunchAgents</code></li>
#   <li><code>/Library/LaunchDaemons</code></li>
#   <li><code>/Library/LaunchAgents</code></li>
# </ul>
#
# <p>…and builds up a list of services based upon each plist’s “Label” entry.</p>
#
# <p>This provider supports:</p>
#
# <ul>
#   <li>ensure =&gt; running/stopped,</li>
#   <li>enable =&gt; true/false</li>
#   <li>status</li>
#   <li>restart</li>
# </ul>
#
# <p>Here is how the Puppet states correspond to <code>launchd</code> states:</p>
#
# <ul>
#   <li>stopped — job unloaded</li>
#   <li>started — job loaded</li>
#   <li>enabled — ‘Disable’ removed from job plist file</li>
#   <li>disabled — ‘Disable’ added to job plist file</li>
# </ul>
#
# <p>Note that this allows you to do something <code>launchctl</code> can’t do, which is to
# be in a state of “stopped/enabled” or “running/disabled”.</p>
#
# <p>Note that this provider does not support overriding ‘restart’</p>
#
# <ul>
#   <li>Required binaries: <code>/bin/launchctl</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>darwin</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-openbsd">openbsd</h4>
#
# <p>Provider for OpenBSD’s rc.d daemon control scripts</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/rcctl</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>openbsd</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>flaggable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-openrc">openrc</h4>
#
# <p>Support for Gentoo’s OpenRC initskripts</p>
#
# <p>Uses rc-update, rc-status and rc-service to manage services.</p>
#
# <ul>
#   <li>Required binaries: <code>/bin/rc-status</code>, <code>/sbin/rc-service</code>, <code>/sbin/rc-update</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>gentoo</code>. Default for <code>operatingsystem</code> == <code>funtoo</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-openwrt">openwrt</h4>
#
# <p>Support for OpenWrt flavored init scripts.</p>
#
# <p>Uses /etc/init.d/service_name enable, disable, and enabled.</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>openwrt</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-rcng">rcng</h4>
#
# <p>RCng service management with rc.d</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>netbsd, cargos</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-redhat">redhat</h4>
#
# <p>Red Hat’s (and probably many others’) form of <code>init</code>-style service
# management. Uses <code>chkconfig</code> for service enabling and disabling.</p>
#
# <ul>
#   <li>Required binaries: <code>/sbin/chkconfig</code>, <code>/sbin/service</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>redhat</code>. Default for <code>operatingsystemmajrelease</code> == <code>10, 11</code> and <code>osfamily</code> == <code>suse</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-runit">runit</h4>
#
# <p>Runit service management.</p>
#
# <p>This provider manages daemons running supervised by Runit.
# When detecting the service directory it will check, in order of preference:</p>
#
# <ul>
#   <li><code>/service</code></li>
#   <li><code>/etc/service</code></li>
#   <li><code>/var/service</code></li>
# </ul>
#
# <p>The daemon directory should be in one of the following locations:</p>
#
# <ul>
#   <li><code>/etc/sv</code></li>
#   <li><code>/var/lib/service</code></li>
# </ul>
#
# <p>or this can be overridden in the service resource parameters:</p>
#
# <pre><code>service { 'myservice':
#   provider =&gt; 'runit',
#   path     =&gt; '/path/to/daemons',
# }
# </code></pre>
#
# <p>This provider supports out of the box:</p>
#
# <ul>
#   <li>start/stop</li>
#   <li>enable/disable</li>
#   <li>restart</li>
#   <li>
#     <p>status</p>
#   </li>
#   <li>Required binaries: <code>/usr/bin/sv</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-service">service</h4>
#
# <p>The simplest form of service support.</p>
#
# <ul>
#   <li>Supported features: <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-smf">smf</h4>
#
# <p>Support for Sun’s new Service Management Framework.</p>
#
# <p>Starting a service is effectively equivalent to enabling it, so there is
# only support for starting and stopping services, which also enables and
# disables them, respectively.</p>
#
# <p>By specifying <code>manifest =&gt; "/path/to/service.xml"</code>, the SMF manifest will
# be imported if it does not exist.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/svcs</code>, <code>/usr/sbin/svcadm</code>, <code>/usr/sbin/svccfg</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>solaris</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-src">src</h4>
#
# <p>Support for AIX’s System Resource controller.</p>
#
# <p>Services are started/stopped based on the <code>stopsrc</code> and <code>startsrc</code>
# commands, and some services can be refreshed with <code>refresh</code> command.</p>
#
# <p>Enabling and disabling services is not supported, as it requires
# modifications to <code>/etc/inittab</code>. Starting and stopping groups of subsystems
# is not yet supported.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/lssrc</code>, <code>/usr/bin/refresh</code>, <code>/usr/bin/startsrc</code>, <code>/usr/bin/stopsrc</code>, <code>/usr/sbin/chitab</code>, <code>/usr/sbin/lsitab</code>, <code>/usr/sbin/mkitab</code>, <code>/usr/sbin/rmitab</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>aix</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-systemd">systemd</h4>
#
# <p>Manages <code>systemd</code> services using <code>systemctl</code>.</p>
#
# <p>Because <code>systemd</code> defaults to assuming the <code>.service</code> unit type, the suffix
# may be omitted.  Other unit types (such as <code>.path</code>) may be managed by
# providing the proper suffix.</p>
#
# <ul>
#   <li>Required binaries: <code>systemctl</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>archlinux</code>. Default for <code>operatingsystemmajrelease</code> == <code>7</code> and <code>osfamily</code> == <code>redhat</code>. Default for <code>operatingsystem</code> == <code>fedora</code> and <code>osfamily</code> == <code>redhat</code>. Default for <code>osfamily</code> == <code>suse</code>. Default for <code>osfamily</code> == <code>coreos</code>. Default for <code>operatingsystem</code> == <code>debian</code> and <code>operatingsystemmajrelease</code> == <code>8, stretch/sid, 9, buster/sid</code>. Default for <code>operatingsystem</code> == <code>ubuntu</code> and <code>operatingsystemmajrelease</code> == <code>15.04, 15.10, 16.04, 16.10</code>. Default for <code>operatingsystem</code> == <code>cumuluslinux</code> and <code>operatingsystemmajrelease</code> == <code>3</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>maskable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-upstart">upstart</h4>
#
# <p>Ubuntu service management with <code>upstart</code>.</p>
#
# <p>This provider manages <code>upstart</code> jobs on Ubuntu. For <code>upstart</code> documentation,
# see <a href="http://upstart.ubuntu.com/">http://upstart.ubuntu.com/</a>.</p>
#
# <ul>
#   <li>Required binaries: <code>/sbin/initctl</code>, <code>/sbin/restart</code>, <code>/sbin/start</code>, <code>/sbin/status</code>, <code>/sbin/stop</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>ubuntu</code> and <code>operatingsystemmajrelease</code> == <code>10.04, 12.04, 14.04, 14.10</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
#
# <h4 id="service-provider-windows">windows</h4>
#
# <p>Support for Windows Service Control Manager (SCM). This provider can
# start, stop, enable, and disable services, and the SCM provides working
# status methods for all services.</p>
#
# <p>Control of service groups (dependencies) is not yet supported, nor is running
# services as a specific user.</p>
#
# <ul>
#   <li>Required binaries: <code>net.exe</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>windows</code>.</li>
#   <li>Supported features: <code>enableable</code>, <code>refreshable</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>controllable</code> — The provider uses a control variable.</li>
#   <li><code>enableable</code> — The provider can enable and disable the service</li>
#   <li><code>flaggable</code> — The provider can pass flags to the service.</li>
#   <li><code>maskable</code> — The provider can ‘mask’ the service.</li>
#   <li><code>refreshable</code> — The provider can restart the service.</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>controllable</th>
#       <th>enableable</th>
#       <th>flaggable</th>
#       <th>maskable</th>
#       <th>refreshable</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>base</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>bsd</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>daemontools</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>debian</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>freebsd</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>gentoo</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>init</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>launchd</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>openbsd</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>openrc</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>openwrt</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>rcng</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>redhat</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>runit</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>service</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>smf</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>src</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>systemd</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>upstart</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>windows</td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#   </tbody>
# </table>
define service(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the service to run.</p>
  #
  # <p>This name is used to find the service; on platforms where services
  # have short system names and long display names, this should be the
  # short name. (To take an example from Windows, you would use “wuauserv”
  # rather than “Automatic Updates.”)</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether a service should be running.</p>
  #
  # <p>Valid values are <code>stopped</code> (also called <code>false</code>), <code>running</code> (also called <code>true</code>).</p>

  $ensure,

  # <h2>binary</h2>
  # <p>The path to the daemon.  This is only used for
  # systems that do not support init scripts.  This binary will be
  # used to start the service if no <code>start</code> parameter is
  # provided.</p>

  $binary,

  # <h2>control</h2>
  # <p>The control variable used to manage services (originally for HP-UX).
  # Defaults to the upcased service name plus <code>START</code> replacing dots with
  # underscores, for those providers that support the <code>controllable</code> feature.</p>

  $control,

  # <h2>enable</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether a service should be enabled to start at boot.
  # This property behaves quite differently depending on the platform;
  # wherever possible, it relies on local tools to enable or disable
  # a given service.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>manual</code>, <code>mask</code>.</p>
  #
  # <p>Requires features enableable.</p>

  $enable,

  # <h2>flags</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Specify a string of flags to pass to the startup script.</p>
  #
  # <p>Requires features flaggable.</p>

  $flags,

  # <h2>hasrestart</h2>
  # <p>Specify that an init script has a <code>restart</code> command.  If this is
  # false and you do not specify a command in the <code>restart</code> attribute,
  # the init script’s <code>stop</code> and <code>start</code> commands will be used.</p>
  #
  # <p>Defaults to false.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $hasrestart,

  # <h2>hasstatus</h2>
  # <p>Declare whether the service’s init script has a functional status
  # command; defaults to <code>true</code>. This attribute’s default value changed in
  # Puppet 2.7.0.</p>
  #
  # <p>The init script’s status command must return 0 if the service is
  # running and a nonzero value otherwise. Ideally, these exit codes
  # should conform to <a href="http://refspecs.linuxfoundation.org/LSB_4.1.0/LSB-Core-generic/LSB-Core-generic/iniscrptact.html">the LSB’s specification</a> for init
  # script status actions, but Puppet only considers the difference
  # between 0 and nonzero to be relevant.</p>
  #
  # <p>If a service’s init script does not support any kind of status command,
  # you should set <code>hasstatus</code> to false and either provide a specific
  # command using the <code>status</code> attribute or expect that Puppet will look for
  # the service name in the process table. Be aware that ‘virtual’ init
  # scripts (like ‘network’ under Red Hat systems) will respond poorly to
  # refresh events from other resources if you override the default behavior
  # without providing a status command.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $hasstatus,

  # <h2>manifest</h2>
  # <p>Specify a command to config a service, or a path to a manifest to do so.</p>

  $manifest,

  # <h2>path</h2>
  # <p>The search path for finding init scripts.  Multiple values should
  # be separated by colons or provided as an array.</p>

  $path,

  # <h2>pattern</h2>
  # <p>The pattern to search for in the process table.
  # This is used for stopping services on platforms that do not
  # support init scripts, and is also used for determining service
  # status on those service whose init scripts do not include a status
  # command.</p>
  #
  # <p>Defaults to the name of the service. The pattern can be a simple string
  # or any legal Ruby pattern, including regular expressions (which should
  # be quoted without enclosing slashes).</p>

  $pattern,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>service</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#service-provider-base"><code>base</code></a></li>
  #   <li><a href="#service-provider-bsd"><code>bsd</code></a></li>
  #   <li><a href="#service-provider-daemontools"><code>daemontools</code></a></li>
  #   <li><a href="#service-provider-debian"><code>debian</code></a></li>
  #   <li><a href="#service-provider-freebsd"><code>freebsd</code></a></li>
  #   <li><a href="#service-provider-gentoo"><code>gentoo</code></a></li>
  #   <li><a href="#service-provider-init"><code>init</code></a></li>
  #   <li><a href="#service-provider-launchd"><code>launchd</code></a></li>
  #   <li><a href="#service-provider-openbsd"><code>openbsd</code></a></li>
  #   <li><a href="#service-provider-openrc"><code>openrc</code></a></li>
  #   <li><a href="#service-provider-openwrt"><code>openwrt</code></a></li>
  #   <li><a href="#service-provider-rcng"><code>rcng</code></a></li>
  #   <li><a href="#service-provider-redhat"><code>redhat</code></a></li>
  #   <li><a href="#service-provider-runit"><code>runit</code></a></li>
  #   <li><a href="#service-provider-service"><code>service</code></a></li>
  #   <li><a href="#service-provider-smf"><code>smf</code></a></li>
  #   <li><a href="#service-provider-src"><code>src</code></a></li>
  #   <li><a href="#service-provider-systemd"><code>systemd</code></a></li>
  #   <li><a href="#service-provider-upstart"><code>upstart</code></a></li>
  #   <li><a href="#service-provider-windows"><code>windows</code></a></li>
  # </ul>

  $provider,

  # <h2>restart</h2>
  # <p>Specify a <em>restart</em> command manually.  If left
  # unspecified, the service will be stopped and then started.</p>

  $restart,

  # <h2>start</h2>
  # <p>Specify a <em>start</em> command manually.  Most service subsystems
  # support a <code>start</code> command, so this will not need to be
  # specified.</p>

  $start,

  # <h2>status</h2>
  # <p>Specify a <em>status</em> command manually.  This command must
  # return 0 if the service is running and a nonzero value otherwise.
  # Ideally, these exit codes should conform to <a href="http://refspecs.linuxfoundation.org/LSB_4.1.0/LSB-Core-generic/LSB-Core-generic/iniscrptact.html">the LSB’s
  # specification</a> for init script status actions, but
  # Puppet only considers the difference between 0 and nonzero to be
  # relevant.</p>
  #
  # <p>If left unspecified, the status of the service will be determined
  # automatically, usually by looking for the service in the process
  # table.</p>

  $status,

  # <h2>stop</h2>
  # <p>Specify a <em>stop</em> command manually.</p>

  $stop,

){}

# <h2>ssh_authorized_key</h2>
# <p>Manages SSH authorized keys. Currently only type 2 keys are supported.</p>
#
# <p>In their native habitat, SSH keys usually appear as a single long line, in
# the format <code>&lt;TYPE&gt; &lt;KEY&gt; &lt;NAME/COMMENT&gt;</code>. This resource type requires you
# to split that line into several attributes. Thus, a key that appears in
# your <code>~/.ssh/id_rsa.pub</code> file like this…</p>
#
# <pre><code>ssh-rsa AAAAB3Nza[...]qXfdaQ== nick@magpie.example.com
# </code></pre>
#
# <p>…would translate to the following resource:</p>
#
# <pre><code>ssh_authorized_key { 'nick@magpie.example.com':
#   ensure =&gt; present,
#   user   =&gt; 'nick',
#   type   =&gt; 'ssh-rsa',
#   key    =&gt; 'AAAAB3Nza[...]qXfdaQ==',
# }
# </code></pre>
#
# <p>To ensure that only the currently approved keys are present, you can purge
# unmanaged SSH keys on a per-user basis. Do this with the <code>user</code> resource
# type’s <code>purge_ssh_keys</code> attribute:</p>
#
# <pre><code>user { 'nick':
#   ensure         =&gt; present,
#   purge_ssh_keys =&gt; true,
# }
# </code></pre>
#
# <p>This will remove any keys in <code>~/.ssh/authorized_keys</code> that aren’t being
# managed with <code>ssh_authorized_key</code> resources. See the documentation of the
# <code>user</code> type for more details.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the user account in which this
# SSH key should be installed, the <code>ssh_authorized_key</code> resource will autorequire
# that user.</p>
# <h3>Providers</h3>
# <h4 id="ssh_authorized_key-provider-parsed">parsed</h4>
#
# <p>Parse and generate authorized_keys files for SSH.</p>
define ssh_authorized_key(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The SSH key comment. This can be anything, and doesn’t need to match
  # the original comment from the <code>.pub</code> file.</p>
  #
  # <p>Due to internal limitations, this must be unique across all user accounts;
  # if you want to specify one key for multiple users, you must use a different
  # comment for each instance.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>key</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The public key itself; generally a long string of hex characters. The <code>key</code>
  # attribute may not contain whitespace.</p>
  #
  # <p>Make sure to omit the following in this attribute (and specify them in
  # other attributes):</p>
  #
  # <ul>
  #   <li>Key headers (e.g. ‘ssh-rsa’) — put these in the <code>type</code> attribute.</li>
  #   <li>Key identifiers / comments (e.g. ‘joe@joescomputer.local’) — put these in
  # the <code>name</code> attribute/resource title.</li>
  # </ul>

  $key,

  # <h2>options</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Key options; see sshd(8) for possible values. Multiple values
  # should be specified as an array.</p>

  $options,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>ssh_authorized_key</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#ssh_authorized_key-provider-parsed"><code>parsed</code></a></li>
  # </ul>

  $provider,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The absolute filename in which to store the SSH key. This
  # property is optional and should only be used in cases where keys
  # are stored in a non-standard location (i.e.<code> not in
  # </code>~user/.ssh/authorized_keys`).</p>

  $target,

  # <h2>type</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The encryption type used.</p>
  #
  # <p>Valid values are <code>ssh-dss</code> (also called <code>dsa</code>), <code>ssh-rsa</code> (also called <code>rsa</code>), <code>ecdsa-sha2-nistp256</code>, <code>ecdsa-sha2-nistp384</code>, <code>ecdsa-sha2-nistp521</code>, <code>ssh-ed25519</code> (also called <code>ed25519</code>).</p>

  $type,

  # <h2>user</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user account in which the SSH key should be installed. The resource
  # will autorequire this user if it is being managed as a <code>user</code> resource.</p>

  $user,

){}

# <h2>sshkey</h2>
# <p>Installs and manages ssh host keys.  By default, this type will
# install keys into <code>/etc/ssh/ssh_known_hosts</code>. To manage ssh keys in a
# different <code>known_hosts</code> file, such as a user’s personal <code>known_hosts</code>,
# pass its path to the <code>target</code> parameter. See the <code>ssh_authorized_key</code>
# type to manage authorized keys.</p>
# <h3>Providers</h3>
# <h4 id="sshkey-provider-parsed">parsed</h4>
#
# <p>Parse and generate host-wide known hosts files for SSH.</p>
define sshkey(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The host name that the key is associated with.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>host_aliases</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Any aliases the host might have.  Multiple values must be
  # specified as an array.</p>

  $host_aliases,

  # <h2>key</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The key itself; generally a long string of uuencoded characters.</p>

  $key,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>sshkey</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#sshkey-provider-parsed"><code>parsed</code></a></li>
  # </ul>

  $provider,

  # <h2>target</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The file in which to store the ssh key.  Only used by
  # the <code>parsed</code> provider.</p>

  $target,

  # <h2>type</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The encryption type used.  Probably ssh-dss or ssh-rsa.</p>
  #
  # <p>Valid values are <code>ssh-dss</code> (also called <code>dsa</code>), <code>ssh-ed25519</code> (also called <code>ed25519</code>), <code>ssh-rsa</code> (also called <code>rsa</code>), <code>ecdsa-sha2-nistp256</code>, <code>ecdsa-sha2-nistp384</code>, <code>ecdsa-sha2-nistp521</code>.</p>

  $type,

){}

# <h2>stage</h2>
# <p>A resource type for creating new run stages.  Once a stage is available,
# classes can be assigned to it by declaring them with the resource-like syntax
# and using
# <a href="https://docs.puppetlabs.com/puppet/latest/reference/metaparameter.html#stage">the <code>stage</code> metaparameter</a>.</p>
#
# <p>Note that new stages are not useful unless you also declare their order
# in relation to the default <code>main</code> stage.</p>
#
# <p>A complete run stage example:</p>
#
# <pre><code>stage { 'pre':
#   before =&gt; Stage['main'],
# }
#
# class { 'apt-updates':
#   stage =&gt; 'pre',
# }
# </code></pre>
#
# <p>Individual resources cannot be assigned to run stages; you can only set stages
# for classes.</p>
define stage(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the stage. Use this as the value for the <code>stage</code> metaparameter
  # when assigning classes to this stage.</p>

  $name,

){}

# <h2>tidy</h2>
# <p>Remove unwanted files based on specific criteria.  Multiple
# criteria are OR’d together, so a file that is too large but is not
# old enough will still get tidied.</p>
#
# <p>If you don’t specify either <code>age</code> or <code>size</code>, then all files will
# be removed.</p>
#
# <p>This resource type works by generating a file resource for every file
# that should be deleted and then letting that resource perform the
# actual deletion.</p>
define tidy(
  # <h2>path</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The path to the file or directory to manage.  Must be fully
  # qualified.</p>

  $path,

  # <h2>age</h2>
  # <p>Tidy files whose age is equal to or greater than
  # the specified time.  You can choose seconds, minutes,
  # hours, days, or weeks by specifying the first letter of any
  # of those words (e.g., ‘1w’).</p>
  #
  # <p>Specifying 0 will remove all files.</p>

  $age,

  # <h2>backup</h2>
  # <p>Whether tidied files should be backed up.  Any values are passed
  # directly to the file resources used for actual file deletion, so consult
  # the <code>file</code> type’s backup documentation to determine valid values.</p>

  $backup,

  # <h2>matches</h2>
  # <p>One or more (shell type) file glob patterns, which restrict
  # the list of files to be tidied to those whose basenames match
  # at least one of the patterns specified. Multiple patterns can
  # be specified using an array.</p>
  #
  # <p>Example:</p>
  #
  # <pre><code>tidy { '/tmp':
  #   age     =&gt; '1w',
  #   recurse =&gt; 1,
  #   matches =&gt; [ '[0-9]pub*.tmp', '*.temp', 'tmpfile?' ],
  # }
  # </code></pre>
  #
  # <p>This removes files from <code>/tmp</code> if they are one week old or older,
  # are not in a subdirectory and match one of the shell globs given.</p>
  #
  # <p>Note that the patterns are matched against the basename of each
  # file – that is, your glob patterns should not have any ‘/’
  # characters in them, since you are only specifying against the last
  # bit of the file.</p>
  #
  # <p>Finally, note that you must now specify a non-zero/non-false value
  # for recurse if matches is used, as matches only apply to files found
  # by recursion (there’s no reason to use static patterns match against
  # a statically determined path).  Requiring explicit recursion clears
  # up a common source of confusion.</p>

  $matches,

  # <h2>recurse</h2>
  # <p>If target is a directory, recursively descend
  # into the directory looking for files to tidy.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>inf</code>. Values can match <code>/^[0-9]+$/</code>.</p>

  $recurse,

  # <h2>rmdirs</h2>
  # <p>Tidy directories in addition to files; that is, remove
  # directories whose age is older than the specified criteria.
  # This will only remove empty directories, so all contained
  # files must also be tidied before a directory gets removed.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $rmdirs,

  # <h2>size</h2>
  # <p>Tidy files whose size is equal to or greater than
  # the specified size.  Unqualified values are in kilobytes, but
  # <em>b</em>, <em>k</em>, <em>m</em>, <em>g</em>, and <em>t</em> can be appended to specify <em>bytes</em>,
  # <em>kilobytes</em>, <em>megabytes</em>, <em>gigabytes</em>, and <em>terabytes</em>, respectively.
  # Only the first character is significant, so the full word can also
  # be used.</p>

  $size,

  # <h2>type</h2>
  # <p>Set the mechanism for determining age. Default: atime.</p>
  #
  # <p>Valid values are <code>atime</code>, <code>mtime</code>, <code>ctime</code>.</p>

  $type,

){}

# <h2>user</h2>
# <p>Manage users.  This type is mostly built to manage system
# users, so it is lacking some features useful for managing normal
# users.</p>
#
# <p>This resource type uses the prescribed native tools for creating
# groups and generally uses POSIX APIs for retrieving information
# about them.  It does not directly modify <code>/etc/passwd</code> or anything.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the user’s primary group (as
# provided in the <code>gid</code> attribute) or any group listed in the <code>groups</code>
# attribute then the user resource will autorequire that group. If Puppet
# is managing any role accounts corresponding to the user’s roles, the
# user resource will autorequire those role accounts.</p>
# <h3>Providers</h3>
# <h4 id="user-provider-aix">aix</h4>
#
# <p>User management for AIX.</p>
#
# <ul>
#   <li>Required binaries: <code>/bin/chpasswd</code>, <code>/usr/bin/chuser</code>, <code>/usr/bin/mkuser</code>, <code>/usr/sbin/lsgroup</code>, <code>/usr/sbin/lsuser</code>, <code>/usr/sbin/rmuser</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>aix</code>.</li>
#   <li>Supported features: <code>manages_aix_lam</code>, <code>manages_expiry</code>, <code>manages_homedir</code>, <code>manages_password_age</code>, <code>manages_passwords</code>, <code>manages_shell</code>.</li>
# </ul>
#
# <h4 id="user-provider-directoryservice">directoryservice</h4>
#
# <p>User management on OS X.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/bin/dscacheutil</code>, <code>/usr/bin/dscl</code>, <code>/usr/bin/dsimport</code>, <code>/usr/bin/uuidgen</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>darwin</code>.</li>
#   <li>Supported features: <code>manages_password_salt</code>, <code>manages_passwords</code>, <code>manages_shell</code>.</li>
# </ul>
#
# <h4 id="user-provider-hpuxuseradd">hpuxuseradd</h4>
#
# <p>User management for HP-UX. This provider uses the undocumented <code>-F</code>
# switch to HP-UX’s special <code>usermod</code> binary to work around the fact that
# its standard <code>usermod</code> cannot make changes while the user is logged in.
# New functionality provides for changing trusted computing passwords and
# resetting password expirations under trusted computing.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sam/lbin/useradd.sam</code>, <code>/usr/sam/lbin/userdel.sam</code>, <code>/usr/sam/lbin/usermod.sam</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>hp-ux</code>.</li>
#   <li>Supported features: <code>allows_duplicates</code>, <code>manages_homedir</code>, <code>manages_passwords</code>.</li>
# </ul>
#
# <h4 id="user-provider-ldap">ldap</h4>
#
# <p>User management via LDAP.</p>
#
# <p>This provider requires that you have valid values for all of the
# LDAP-related settings in <code>puppet.conf</code>, including <code>ldapbase</code>.  You will
# almost definitely need settings for <code>ldapuser</code> and <code>ldappassword</code> in order
# for your clients to write to LDAP.</p>
#
# <p>Note that this provider will automatically generate a UID for you if
# you do not specify one, but it is a potentially expensive operation,
# as it iterates across all existing users to pick the appropriate next one.</p>
#
# <ul>
#   <li>Supported features: <code>manages_passwords</code>, <code>manages_shell</code>.</li>
# </ul>
#
# <h4 id="user-provider-openbsd">openbsd</h4>
#
# <p>User management via <code>useradd</code> and its ilk for OpenBSD. Note that you
# will need to install Ruby’s shadow password library (package known as
# <code>ruby-shadow</code>) if you wish to manage user passwords.</p>
#
# <ul>
#   <li>Required binaries: <code>passwd</code>, <code>useradd</code>, <code>userdel</code>, <code>usermod</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>openbsd</code>.</li>
#   <li>Supported features: <code>manages_expiry</code>, <code>manages_homedir</code>, <code>manages_shell</code>, <code>system_users</code>.</li>
# </ul>
#
# <h4 id="user-provider-pw">pw</h4>
#
# <p>User management via <code>pw</code> on FreeBSD and DragonFly BSD.</p>
#
# <ul>
#   <li>Required binaries: <code>pw</code>.</li>
#   <li>Default for <code>operatingsystem</code> == <code>freebsd, dragonfly</code>.</li>
#   <li>Supported features: <code>allows_duplicates</code>, <code>manages_expiry</code>, <code>manages_homedir</code>, <code>manages_passwords</code>, <code>manages_shell</code>.</li>
# </ul>
#
# <h4 id="user-provider-user_role_add">user_role_add</h4>
#
# <p>User and role management on Solaris, via <code>useradd</code> and <code>roleadd</code>.</p>
#
# <ul>
#   <li>Required binaries: <code>passwd</code>, <code>roleadd</code>, <code>roledel</code>, <code>rolemod</code>, <code>useradd</code>, <code>userdel</code>, <code>usermod</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>solaris</code>.</li>
#   <li>Supported features: <code>allows_duplicates</code>, <code>manages_homedir</code>, <code>manages_password_age</code>, <code>manages_passwords</code>, <code>manages_shell</code>, <code>manages_solaris_rbac</code>.</li>
# </ul>
#
# <h4 id="user-provider-useradd">useradd</h4>
#
# <p>User management via <code>useradd</code> and its ilk.  Note that you will need to
# install Ruby’s shadow password library (often known as <code>ruby-libshadow</code>)
# if you wish to manage user passwords.</p>
#
# <ul>
#   <li>Required binaries: <code>chage</code>, <code>luseradd</code>, <code>useradd</code>, <code>userdel</code>, <code>usermod</code>.</li>
#   <li>Supported features: <code>allows_duplicates</code>, <code>manages_expiry</code>, <code>manages_homedir</code>, <code>manages_shell</code>, <code>system_users</code>.</li>
# </ul>
#
# <h4 id="user-provider-windows_adsi">windows_adsi</h4>
#
# <p>Local user management for Windows.</p>
#
# <ul>
#   <li>Default for <code>operatingsystem</code> == <code>windows</code>.</li>
#   <li>Supported features: <code>manages_homedir</code>, <code>manages_passwords</code>.</li>
# </ul>
# <h3>Provider Features</h3>
# <p>Available features:</p>
#
# <ul>
#   <li><code>allows_duplicates</code> — The provider supports duplicate users with the same UID.</li>
#   <li><code>libuser</code> — Allows local users to be managed on systems that also use some other remote NSS method of managing accounts.</li>
#   <li><code>manages_aix_lam</code> — The provider can manage AIX Loadable Authentication Module (LAM) system.</li>
#   <li><code>manages_expiry</code> — The provider can manage the expiry date for a user.</li>
#   <li><code>manages_homedir</code> — The provider can create and remove home directories.</li>
#   <li><code>manages_loginclass</code> — The provider can manage the login class for a user.</li>
#   <li><code>manages_password_age</code> — The provider can set age requirements and restrictions for passwords.</li>
#   <li><code>manages_password_salt</code> — The provider can set a password salt. This is for providers that implement PBKDF2 passwords with salt properties.</li>
#   <li><code>manages_passwords</code> — The provider can modify user passwords, by accepting a password hash.</li>
#   <li><code>manages_shell</code> — The provider allows for setting shell and validates if possible</li>
#   <li><code>manages_solaris_rbac</code> — The provider can manage roles and normal users</li>
#   <li><code>system_users</code> — The provider allows you to create system users with lower UIDs.</li>
# </ul>
#
# <p>Provider support:</p>
#
# <table>
#   <thead>
#     <tr>
#       <th>Provider</th>
#       <th>allows duplicates</th>
#       <th>libuser</th>
#       <th>manages aix lam</th>
#       <th>manages expiry</th>
#       <th>manages homedir</th>
#       <th>manages loginclass</th>
#       <th>manages password age</th>
#       <th>manages password salt</th>
#       <th>manages passwords</th>
#       <th>manages shell</th>
#       <th>manages solaris rbac</th>
#       <th>system users</th>
#     </tr>
#   </thead>
#   <tbody>
#     <tr>
#       <td>aix</td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>directoryservice</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>hpuxuseradd</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>ldap</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>openbsd</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>pw</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>user_role_add</td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#     </tr>
#     <tr>
#       <td>useradd</td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#     </tr>
#     <tr>
#       <td>windows_adsi</td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#       <td><em>X</em> </td>
#       <td> </td>
#       <td> </td>
#       <td> </td>
#     </tr>
#   </tbody>
# </table>
define user(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The user name. While naming limitations vary by operating system,
  # it is advisable to restrict names to the lowest common denominator,
  # which is a maximum of 8 characters beginning with a letter.</p>
  #
  # <p>Note that Puppet considers user names to be case-sensitive, regardless
  # of the platform’s own rules; be sure to always use the same case when
  # referring to a given user.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic state that the object should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>, <code>role</code>.</p>

  $ensure,

  # <h2>allowdupe</h2>
  # <p>Whether to allow duplicate UIDs. Defaults to <code>false</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $allowdupe,

  # <h2>attribute_membership</h2>
  # <p>Whether specified attribute value pairs should be treated as the
  # <strong>complete list</strong> (<code>inclusive</code>) or the <strong>minimum list</strong> (<code>minimum</code>) of
  # attribute/value pairs for the user. Defaults to <code>minimum</code>.</p>
  #
  # <p>Valid values are <code>inclusive</code>, <code>minimum</code>.</p>

  $attribute_membership,

  # <h2>attributes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Specify AIX attributes for the user in an array of attribute = value pairs.</p>
  #
  # <p>Requires features manages_aix_lam.</p>

  $attributes,

  # <h2>auth_membership</h2>
  # <p>Whether specified auths should be considered the <strong>complete list</strong>
  # (<code>inclusive</code>) or the <strong>minimum list</strong> (<code>minimum</code>) of auths the user
  # has. Defaults to <code>minimum</code>.</p>
  #
  # <p>Valid values are <code>inclusive</code>, <code>minimum</code>.</p>

  $auth_membership,

  # <h2>auths</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The auths the user has.  Multiple auths should be
  # specified as an array.</p>
  #
  # <p>Requires features manages_solaris_rbac.</p>

  $auths,

  # <h2>comment</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A description of the user.  Generally the user’s full name.</p>

  $comment,

  # <h2>expiry</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The expiry date for this user. Must be provided in
  # a zero-padded YYYY-MM-DD format — e.g. 2010-02-19.
  # If you want to ensure the user account never expires,
  # you can pass the special value <code>absent</code>.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^\d{4}-\d{2}-\d{2}$/</code>.</p>
  #
  # <p>Requires features manages_expiry.</p>

  $expiry,

  # <h2>forcelocal</h2>
  # <p>Forces the management of local accounts when accounts are also
  # being managed by some other NSS</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>
  #
  # <p>Requires features libuser.</p>

  $forcelocal,

  # <h2>gid</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user’s primary group.  Can be specified numerically or by name.</p>
  #
  # <p>This attribute is not supported on Windows systems; use the <code>groups</code>
  # attribute instead. (On Windows, designating a primary group is only
  # meaningful for domain accounts, which Puppet does not currently manage.)</p>

  $gid,

  # <h2>groups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The groups to which the user belongs.  The primary group should
  # not be listed, and groups should be identified by name rather than by
  # GID.  Multiple groups should be specified as an array.</p>

  $groups,

  # <h2>home</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The home directory of the user.  The directory must be created
  # separately and is not currently checked for existence.</p>

  $home,

  # <h2>ia_load_module</h2>
  # <p>The name of the I&amp;A module to use to manage this user.</p>
  #
  # <p>Requires features manages_aix_lam.</p>

  $ia_load_module,

  # <h2>iterations</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>This is the number of iterations of a chained computation of the
  # <a href="https://en.wikipedia.org/wiki/PBKDF2">PBKDF2 password hash</a>. This parameter
  # is used in OS X, and is required for managing passwords on OS X 10.8 and
  # newer.</p>
  #
  # <p>Requires features manages_password_salt.</p>

  $iterations,

  # <h2>key_membership</h2>
  # <p>Whether specified key/value pairs should be considered the
  # <strong>complete list</strong> (<code>inclusive</code>) or the <strong>minimum list</strong> (<code>minimum</code>) of
  # the user’s attributes. Defaults to <code>minimum</code>.</p>
  #
  # <p>Valid values are <code>inclusive</code>, <code>minimum</code>.</p>

  $key_membership,

  # <h2>keys</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Specify user attributes in an array of key = value pairs.</p>
  #
  # <p>Requires features manages_solaris_rbac.</p>

  $keys,

  # <h2>loginclass</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The name of login class to which the user belongs.</p>
  #
  # <p>Requires features manages_loginclass.</p>

  $loginclass,

  # <h2>managehome</h2>
  # <p>Whether to manage the home directory when managing the user.
  # This will create the home directory when <code>ensure =&gt; present</code>, and
  # delete the home directory when <code>ensure =&gt; absent</code>. Defaults to <code>false</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $managehome,

  # <h2>membership</h2>
  # <p>If <code>minimum</code> is specified, Puppet will ensure that the user is a
  # member of all specified groups, but will not remove any other groups
  # that the user is a part of.</p>
  #
  # <p>If <code>inclusive</code> is specified, Puppet will ensure that the user is a
  # member of <strong>only</strong> specified groups.</p>
  #
  # <p>Defaults to <code>minimum</code>.</p>
  #
  # <p>Valid values are <code>inclusive</code>, <code>minimum</code>.</p>

  $membership,

  # <h2>password</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user’s password, in whatever encrypted format the local system
  # requires. Consult your operating system’s documentation for acceptable password
  # encryption formats and requirements.</p>
  #
  # <ul>
  #   <li>Mac OS X 10.5 and 10.6, and some older Linux distributions, use salted SHA1
  # hashes. You can use Puppet’s built-in <code>sha1</code> function to generate a salted SHA1
  # hash from a password.</li>
  #   <li>Mac OS X 10.7 (Lion), and many recent Linux distributions, use salted SHA512
  # hashes. The Puppet Labs <a href="https://github.com/puppetlabs/puppetlabs-stdlib/">stdlib</a> module contains a <code>str2saltedsha512</code> function
  # which can generate password hashes for these operating systems.</li>
  #   <li>OS X 10.8 and higher use salted SHA512 PBKDF2 hashes. When managing passwords
  # on these systems, the <code>salt</code> and <code>iterations</code> attributes need to be specified as
  # well as the password.</li>
  #   <li>Windows passwords can only be managed in cleartext, as there is no Windows API
  # for setting the password hash.</li>
  # </ul>
  #
  # <p>Enclose any value that includes a dollar sign ($) in single quotes (‘) to avoid
  # accidental variable interpolation.</p>
  #
  # <p>Requires features manages_passwords.</p>

  $password,

  # <h2>password_max_age</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The maximum number of days a password may be used before it must be changed.</p>
  #
  # <p>Requires features manages_password_age.</p>

  $password_max_age,

  # <h2>password_min_age</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The minimum number of days a password must be used before it may be changed.</p>
  #
  # <p>Requires features manages_password_age.</p>

  $password_min_age,

  # <h2>profile_membership</h2>
  # <p>Whether specified roles should be treated as the <strong>complete list</strong>
  # (<code>inclusive</code>) or the <strong>minimum list</strong> (<code>minimum</code>) of roles
  # of which the user is a member. Defaults to <code>minimum</code>.</p>
  #
  # <p>Valid values are <code>inclusive</code>, <code>minimum</code>.</p>

  $profile_membership,

  # <h2>profiles</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The profiles the user has.  Multiple profiles should be
  # specified as an array.</p>
  #
  # <p>Requires features manages_solaris_rbac.</p>

  $profiles,

  # <h2>project</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The name of the project associated with a user.</p>
  #
  # <p>Requires features manages_solaris_rbac.</p>

  $project,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>user</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#user-provider-aix"><code>aix</code></a></li>
  #   <li><a href="#user-provider-directoryservice"><code>directoryservice</code></a></li>
  #   <li><a href="#user-provider-hpuxuseradd"><code>hpuxuseradd</code></a></li>
  #   <li><a href="#user-provider-ldap"><code>ldap</code></a></li>
  #   <li><a href="#user-provider-openbsd"><code>openbsd</code></a></li>
  #   <li><a href="#user-provider-pw"><code>pw</code></a></li>
  #   <li><a href="#user-provider-user_role_add"><code>user_role_add</code></a></li>
  #   <li><a href="#user-provider-useradd"><code>useradd</code></a></li>
  #   <li><a href="#user-provider-windows_adsi"><code>windows_adsi</code></a></li>
  # </ul>

  $provider,

  # <h2>purge_ssh_keys</h2>
  # <p>Whether to purge authorized SSH keys for this user if they are not managed
  # with the <code>ssh_authorized_key</code> resource type. Allowed values are:</p>
  #
  # <ul>
  #   <li><code>false</code> (default) — don’t purge SSH keys for this user.</li>
  #   <li><code>true</code> — look for keys in the <code>.ssh/authorized_keys</code> file in the user’s
  # home directory. Purge any keys that aren’t managed as <code>ssh_authorized_key</code>
  # resources.</li>
  #   <li>An array of file paths — look for keys in all of the files listed. Purge
  # any keys that aren’t managed as <code>ssh_authorized_key</code> resources. If any of
  # these paths starts with <code>~</code> or <code>%h</code>, that token will be replaced with
  # the user’s home directory.</li>
  # </ul>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $purge_ssh_keys,

  # <h2>role_membership</h2>
  # <p>Whether specified roles should be considered the <strong>complete list</strong>
  # (<code>inclusive</code>) or the <strong>minimum list</strong> (<code>minimum</code>) of roles the user
  # has. Defaults to <code>minimum</code>.</p>
  #
  # <p>Valid values are <code>inclusive</code>, <code>minimum</code>.</p>

  $role_membership,

  # <h2>roles</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The roles the user has.  Multiple roles should be
  # specified as an array.</p>
  #
  # <p>Requires features manages_solaris_rbac.</p>

  $roles,

  # <h2>salt</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>This is the 32-byte salt used to generate the PBKDF2 password used in
  # OS X. This field is required for managing passwords on OS X &gt;= 10.8.</p>
  #
  # <p>Requires features manages_password_salt.</p>

  $salt,

  # <h2>shell</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user’s login shell.  The shell must exist and be
  # executable.</p>
  #
  # <p>This attribute cannot be managed on Windows systems.</p>
  #
  # <p>Requires features manages_shell.</p>

  $shell,

  # <h2>system</h2>
  # <p>Whether the user is a system user, according to the OS’s criteria;
  # on most platforms, a UID less than or equal to 500 indicates a system
  # user. This parameter is only used when the resource is created and will
  # not affect the UID when the user is present. Defaults to <code>false</code>.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>, <code>yes</code>, <code>no</code>.</p>

  $system,

  # <h2>uid</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The user ID; must be specified numerically. If no user ID is
  # specified when creating a new user, then one will be chosen
  # automatically. This will likely result in the same user having
  # different UIDs on different systems, which is not recommended. This is
  # especially noteworthy when managing the same user on both Darwin and
  # other platforms, since Puppet does UID generation on Darwin, but
  # the underlying tools do so on other platforms.</p>
  #
  # <p>On Windows, this property is read-only and will return the user’s
  # security identifier (SID).</p>

  $uid,

){}

# <h2>vlan</h2>
# <p>Manages a VLAN on a router or switch.</p>
# <h3>Providers</h3>
# <h4 id="vlan-provider-cisco">cisco</h4>
#
# <p>Cisco switch/router provider for vlans.</p>
define vlan(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The numeric VLAN ID.</p>
  #
  # <p>Values can match <code>/^\d+/</code>.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>description</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The VLAN’s name.</p>

  $description,

  # <h2>device_url</h2>
  # <p>The URL of the router or switch maintaining this VLAN.</p>

  $device_url,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>vlan</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#vlan-provider-cisco"><code>cisco</code></a></li>
  # </ul>

  $provider,

){}

# <h2>yumrepo</h2>
# <p>The client-side description of a yum repository. Repository
# configurations are found by parsing <code>/etc/yum.conf</code> and
# the files indicated by the <code>reposdir</code> option in that file
# (see <code>yum.conf(5)</code> for details).</p>
#
# <p>Most parameters are identical to the ones documented
# in the <code>yum.conf(5)</code> man page.</p>
#
# <p>Continuation lines that yum supports (for the <code>baseurl</code>, for example)
# are not supported. This type does not attempt to read or verify the
# existence of files listed in the <code>include</code> attribute.</p>
# <h3>Providers</h3>
# <h4 id="yumrepo-provider-inifile">inifile</h4>
#
# <p>Manage yum repo configurations by parsing yum INI configuration files.</p>
# <h3>Fetching instances</h3>
# <p>When fetching repo instances, directory entries in ‘/etc/yum/repos.d’,
# ‘/etc/yum.repos.d’, and the directory optionally specified by the reposdir
# key in ‘/etc/yum.conf’ will be checked. If a given directory does not exist it
# will be ignored. In addition, all sections in ‘/etc/yum.conf’ aside from
# ‘main’ will be created as sections.</p>
# <h3>Storing instances</h3>
# <p>When creating a new repository, a new section will be added in the first
# yum repo directory that exists. The custom directory specified by the
# ‘/etc/yum.conf’ reposdir property is checked first, followed by
# ‘/etc/yum/repos.d’, and then ‘/etc/yum.repos.d’. If none of these exist, the
# section will be created in ‘/etc/yum.conf’.</p>
define yumrepo(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the repository.  This corresponds to the
  # <code>repositoryid</code> parameter in <code>yum.conf(5)</code>.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>assumeyes</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Determines if yum prompts for confirmation of critical actions.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $assumeyes,

  # <h2>bandwidth</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Use to specify the maximum available network bandwidth
  #       in bytes/second. Used with the <code>throttle</code> option. If <code>throttle</code>
  #       is a percentage and <code>bandwidth</code> is <code>0</code> then bandwidth throttling
  #       will be disabled. If <code>throttle</code> is expressed as a data rate then
  #       this option is ignored.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^\d+[kMG]?$/</code>.</p>

  $bandwidth,

  # <h2>baseurl</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The URL for this repository. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $baseurl,

  # <h2>cost</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Cost of this repository. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^\d+$/</code>.</p>

  $cost,

  # <h2>deltarpm_metadata_percentage</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Percentage value that determines when to download deltarpm metadata.
  # When the deltarpm metadata is larger than this percentage value of the
  # package, deltarpm metadata is not downloaded.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^\d+$/</code>.</p>

  $deltarpm_metadata_percentage,

  # <h2>deltarpm_percentage</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Percentage value that determines when to use deltas for this repository.
  # When the delta is larger than this percentage value of the package, the
  # delta is not used.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^\d+$/</code>.</p>

  $deltarpm_percentage,

  # <h2>descr</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>A human-readable description of the repository.
  # This corresponds to the name parameter in <code>yum.conf(5)</code>.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $descr,

  # <h2>enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether this repository is enabled.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $enabled,

  # <h2>enablegroups</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether yum will allow the use of package groups for this
  # repository.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $enablegroups,

  # <h2>exclude</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>List of shell globs. Matching packages will never be
  # considered in updates or installs for this repo.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $exclude,

  # <h2>failovermethod</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The failover method for this repository; should be either
  # <code>roundrobin</code> or <code>priority</code>. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^roundrobin|priority$/</code>.</p>

  $failovermethod,

  # <h2>gpgcakey</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The URL for the GPG CA key for this repository. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $gpgcakey,

  # <h2>gpgcheck</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether to check the GPG signature on packages installed
  # from this repository.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $gpgcheck,

  # <h2>gpgkey</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The URL for the GPG key with which packages from this
  # repository are signed. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $gpgkey,

  # <h2>http_caching</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>What to cache from this repository. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(packages|all|none)$/</code>.</p>

  $http_caching,

  # <h2>include</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The URL of a remote file containing additional yum configuration
  # settings. Puppet does not check for this file’s existence or validity.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $include,

  # <h2>includepkgs</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>List of shell globs. If this is set, only packages
  # matching one of the globs will be considered for
  # update or install from this repository. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $includepkgs,

  # <h2>keepalive</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether HTTP/1.1 keepalive should be used with this repository.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $keepalive,

  # <h2>metadata_expire</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Number of seconds after which the metadata will expire.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^([0-9]+[dhm]?|never)$/</code>.</p>

  $metadata_expire,

  # <h2>metalink</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Metalink for mirrors. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $metalink,

  # <h2>mirrorlist</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The URL that holds the list of mirrors for this repository.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $mirrorlist,

  # <h2>mirrorlist_expire</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Time (in seconds) after which the mirrorlist locally cached
  #       will expire.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^[0-9]+$/</code>.</p>

  $mirrorlist_expire,

  # <h2>priority</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Priority of this repository from 1-99. Requires that
  # the <code>priorities</code> plugin is installed and enabled.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $priority,

  # <h2>protect</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Enable or disable protection for this repository. Requires
  # that the <code>protectbase</code> plugin is installed and enabled.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $protect,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>yumrepo</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#yumrepo-provider-inifile"><code>inifile</code></a></li>
  # </ul>

  $provider,

  # <h2>proxy</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>URL of a proxy server that Yum should use when accessing this repository.
  # This attribute can also be set to <code>'_none_'</code>, which will make Yum bypass any
  # global proxy settings when accessing this repository.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $proxy,

  # <h2>proxy_password</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Password for this proxy. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $proxy_password,

  # <h2>proxy_username</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Username for this proxy. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $proxy_username,

  # <h2>repo_gpgcheck</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether to check the GPG signature on repodata.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $repo_gpgcheck,

  # <h2>retries</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Set the number of times any attempt to retrieve a file should
  #       retry before returning an error. Setting this to <code>0</code> makes yum
  #      try forever.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^[0-9]+$/</code>.</p>

  $retries,

  # <h2>s3_enabled</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Access the repository via S3.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $s3_enabled,

  # <h2>skip_if_unavailable</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Should yum skip this repository if unable to reach it.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $skip_if_unavailable,

  # <h2>sslcacert</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Path to the directory containing the databases of the
  # certificate authorities yum should use to verify SSL certificates.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $sslcacert,

  # <h2>sslclientcert</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Path  to the SSL client certificate yum should use to connect
  # to repositories/remote sites. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $sslclientcert,

  # <h2>sslclientkey</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Path to the SSL client key yum should use to connect
  # to repositories/remote sites. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/.*/</code>.</p>

  $sslclientkey,

  # <h2>sslverify</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Should yum verify SSL certificates/hosts at all.
  # Valid values are: false/0/no or true/1/yes.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^(true|false|0|1|no|yes)$/</code>.</p>

  $sslverify,

  # <h2>target</h2>
  # <p>The filename to write the yum repository to.</p>

  $target,

  # <h2>throttle</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Enable bandwidth throttling for downloads. This option
  #       can be expressed as a absolute data rate in bytes/sec or a
  #       percentage <code>60%</code>. An SI prefix (k, M or G) may be appended
  #       to the data rate values.
  # Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^\d+[kMG%]?$/</code>.</p>

  $throttle,

  # <h2>timeout</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Number of seconds to wait for a connection before timing
  # out. Set this to <code>absent</code> to remove it from the file completely.</p>
  #
  # <p>Valid values are <code>absent</code>. Values can match <code>/^\d+$/</code>.</p>

  $timeout,

){}

# <h2>zfs</h2>
# <p>Manage zfs. Create destroy and set properties on zfs instances.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the zpool at the root of this zfs
# instance, the zfs resource will autorequire it. If Puppet is managing any
# parent zfs instances, the zfs resource will autorequire them.</p>
# <h3>Providers</h3>
# <h4 id="zfs-provider-zfs">zfs</h4>
#
# <p>Provider for zfs.</p>
#
# <ul>
#   <li>Required binaries: <code>zfs</code>.</li>
# </ul>
define zfs(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The full name for this filesystem (including the zpool).</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>aclinherit</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The aclinherit property. Valid values are <code>discard</code>, <code>noallow</code>, <code>restricted</code>, <code>passthrough</code>, <code>passthrough-x</code>.</p>

  $aclinherit,

  # <h2>aclmode</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The aclmode property. Valid values are <code>discard</code>, <code>groupmask</code>, <code>passthrough</code>.</p>

  $aclmode,

  # <h2>atime</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The atime property. Valid values are <code>on</code>, <code>off</code>.</p>

  $atime,

  # <h2>canmount</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The canmount property. Valid values are <code>on</code>, <code>off</code>, <code>noauto</code>.</p>

  $canmount,

  # <h2>checksum</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The checksum property. Valid values are <code>on</code>, <code>off</code>, <code>fletcher2</code>, <code>fletcher4</code>, <code>sha256</code>.</p>

  $checksum,

  # <h2>compression</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The compression property. Valid values are <code>on</code>, <code>off</code>, <code>lzjb</code>, <code>gzip</code>, <code>gzip-[1-9]</code>, <code>zle</code>.</p>

  $compression,

  # <h2>copies</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The copies property. Valid values are <code>1</code>, <code>2</code>, <code>3</code>.</p>

  $copies,

  # <h2>dedup</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The dedup property. Valid values are <code>on</code>, <code>off</code>.</p>

  $dedup,

  # <h2>devices</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The devices property. Valid values are <code>on</code>, <code>off</code>.</p>

  $devices,

  # <h2>exec</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The exec property. Valid values are <code>on</code>, <code>off</code>.</p>

  $exec,

  # <h2>logbias</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The logbias property. Valid values are <code>latency</code>, <code>throughput</code>.</p>

  $logbias,

  # <h2>mountpoint</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The mountpoint property. Valid values are <code>&lt;path&gt;</code>, <code>legacy</code>, <code>none</code>.</p>

  $mountpoint,

  # <h2>nbmand</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The nbmand property. Valid values are <code>on</code>, <code>off</code>.</p>

  $nbmand,

  # <h2>primarycache</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The primarycache property. Valid values are <code>all</code>, <code>none</code>, <code>metadata</code>.</p>

  $primarycache,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>zfs</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#zfs-provider-zfs"><code>zfs</code></a></li>
  # </ul>

  $provider,

  # <h2>quota</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The quota property. Valid values are <code>&lt;size&gt;</code>, <code>none</code>.</p>

  $quota,

  # <h2>readonly</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The readonly property. Valid values are <code>on</code>, <code>off</code>.</p>

  $readonly,

  # <h2>recordsize</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The recordsize property. Valid values are powers of two between 512 and 128k.</p>

  $recordsize,

  # <h2>refquota</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The refquota property. Valid values are <code>&lt;size&gt;</code>, <code>none</code>.</p>

  $refquota,

  # <h2>refreservation</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The refreservation property. Valid values are <code>&lt;size&gt;</code>, <code>none</code>.</p>

  $refreservation,

  # <h2>reservation</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The reservation property. Valid values are <code>&lt;size&gt;</code>, <code>none</code>.</p>

  $reservation,

  # <h2>secondarycache</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The secondarycache property. Valid values are <code>all</code>, <code>none</code>, <code>metadata</code>.</p>

  $secondarycache,

  # <h2>setuid</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The setuid property. Valid values are <code>on</code>, <code>off</code>.</p>

  $setuid,

  # <h2>shareiscsi</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The shareiscsi property. Valid values are <code>on</code>, <code>off</code>, <code>type=&lt;type&gt;</code>.</p>

  $shareiscsi,

  # <h2>sharenfs</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The sharenfs property. Valid values are <code>on</code>, <code>off</code>, share(1M) options</p>

  $sharenfs,

  # <h2>sharesmb</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The sharesmb property. Valid values are <code>on</code>, <code>off</code>, sharemgr(1M) options</p>

  $sharesmb,

  # <h2>snapdir</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The snapdir property. Valid values are <code>hidden</code>, <code>visible</code>.</p>

  $snapdir,

  # <h2>version</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The version property. Valid values are <code>1</code>, <code>2</code>, <code>3</code>, <code>4</code>, <code>current</code>.</p>

  $version,

  # <h2>volsize</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The volsize property. Valid values are <code>&lt;size&gt;</code></p>

  $volsize,

  # <h2>vscan</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The vscan property. Valid values are <code>on</code>, <code>off</code>.</p>

  $vscan,

  # <h2>xattr</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The xattr property. Valid values are <code>on</code>, <code>off</code>.</p>

  $xattr,

  # <h2>zoned</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The zoned property. Valid values are <code>on</code>, <code>off</code>.</p>

  $zoned,

){}

# <h2>zone</h2>
# <p>Manages Solaris zones.</p>
#
# <p><strong>Autorequires:</strong> If Puppet is managing the directory specified as the root of
# the zone’s filesystem (with the <code>path</code> attribute), the zone resource will
# autorequire that directory.</p>
# <h3>Providers</h3>
# <h4 id="zone-provider-solaris">solaris</h4>
#
# <p>Provider for Solaris Zones.</p>
#
# <ul>
#   <li>Required binaries: <code>/usr/sbin/zoneadm</code>, <code>/usr/sbin/zonecfg</code>.</li>
#   <li>Default for <code>osfamily</code> == <code>solaris</code>.</li>
# </ul>
define zone(
  # <h2>name</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name of the zone.</p>

  $name,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The running state of the zone.  The valid states directly reflect
  # the states that <code>zoneadm</code> provides.  The states are linear,
  # in that a zone must be <code>configured</code>, then <code>installed</code>, and
  # only then can be <code>running</code>.  Note also that <code>halt</code> is currently
  # used to stop zones.</p>
  #
  # <p>Valid values are <code>absent</code>, <code>configured</code>, <code>installed</code>, <code>running</code>.</p>

  $ensure,

  # <h2>autoboot</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Whether the zone should automatically boot.</p>
  #
  # <p>Valid values are <code>true</code>, <code>false</code>.</p>

  $autoboot,

  # <h2>clone</h2>
  # <p>Instead of installing the zone, clone it from another zone.
  # If the zone root resides on a zfs file system, a snapshot will be
  # used to create the clone; if it resides on a ufs filesystem, a copy of the
  # zone will be used. The zone from which you clone must not be running.</p>

  $clone,

  # <h2>create_args</h2>
  # <p>Arguments to the <code>zonecfg</code> create command.  This can be used to create branded zones.</p>

  $create_args,

  # <h2>dataset</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The list of datasets delegated to the non-global zone from the
  # global zone.  All datasets must be zfs filesystem names which are
  # different from the mountpoint.</p>

  $dataset,

  # <h2>id</h2>
  # <p>The numerical ID of the zone.  This number is autogenerated
  # and cannot be changed.</p>

  $id,

  # <h2>inherit</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The list of directories that the zone inherits from the global
  # zone.  All directories must be fully qualified.</p>

  $inherit,

  # <h2>install_args</h2>
  # <p>Arguments to the <code>zoneadm</code> install command.  This can be used to create branded zones.</p>

  $install_args,

  # <h2>ip</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The IP address of the zone.  IP addresses <strong>must</strong> be specified
  # with an interface, and may optionally be specified with a default router
  # (sometimes called a defrouter). The interface, IP address, and default
  # router should be separated by colons to form a complete IP address string.
  # For example: <code>bge0:192.168.178.200</code> would be a valid IP address string
  # without a default router, and <code>bge0:192.168.178.200:192.168.178.1</code> adds a
  # default router to it.</p>
  #
  # <p>For zones with multiple interfaces, the value of this attribute should be
  # an array of IP address strings (each of which must include an interface
  # and may include a default router).</p>

  $ip,

  # <h2>iptype</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The IP stack type of the zone.</p>
  #
  # <p>Valid values are <code>shared</code>, <code>exclusive</code>.</p>

  $iptype,

  # <h2>path</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The root of the zone’s filesystem.  Must be a fully qualified
  # file name.  If you include <code>%s</code> in the path, then it will be
  # replaced with the zone’s name.  Currently, you cannot use
  # Puppet to move a zone. Consequently this is a readonly property.</p>

  $path,

  # <h2>pool</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The resource pool for this zone.</p>

  $pool,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>zone</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#zone-provider-solaris"><code>solaris</code></a></li>
  # </ul>

  $provider,

  # <h2>realhostname</h2>
  # <p>The actual hostname of the zone.</p>

  $realhostname,

  # <h2>shares</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Number of FSS CPU shares allocated to the zone.</p>

  $shares,

  # <h2>sysidcfg</h2>
  # <p>The text to go into the <code>sysidcfg</code> file when the zone is first
  # booted.  The best way is to use a template:</p>
  #
  # <pre><code># $confdir/modules/site/templates/sysidcfg.erb
  # system_locale=en_US
  # timezone=GMT
  # terminal=xterms
  # security_policy=NONE
  # root_password=&lt;%= password %&gt;
  # timeserver=localhost
  # name_service=DNS {domain_name=&lt;%= domain %&gt; name_server=&lt;%= nameserver %&gt;}
  # network_interface=primary {hostname=&lt;%= realhostname %&gt;
  #   ip_address=&lt;%= ip %&gt;
  #   netmask=&lt;%= netmask %&gt;
  #   protocol_ipv6=no
  #   default_route=&lt;%= defaultroute %&gt;}
  # nfs4_domain=dynamic
  # </code></pre>
  #
  # <p>And then call that:</p>
  #
  # <pre><code>zone { 'myzone':
  #   ip           =&gt; 'bge0:192.168.0.23',
  #   sysidcfg     =&gt; template('site/sysidcfg.erb'),
  #   path         =&gt; '/opt/zones/myzone',
  #   realhostname =&gt; 'fully.qualified.domain.name',
  # }
  # </code></pre>
  #
  # <p>The <code>sysidcfg</code> only matters on the first booting of the zone,
  # so Puppet only checks for it at that time.</p>

  $sysidcfg,

){}

# <h2>zpool</h2>
# <p>Manage zpools. Create and delete zpools. The provider WILL NOT SYNC, only report differences.</p>
#
# <p>Supports vdevs with mirrors, raidz, logs and spares.</p>
define zpool(
  # <h2>pool</h2>
  # <p><em>(<strong>Namevar:</strong> If omitted, this attribute’s value defaults to the resource’s title.)</em></p>
  #
  # <p>The name for this pool.</p>

  $pool,

  # <h2>ensure</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The basic property that the resource should be in.</p>
  #
  # <p>Valid values are <code>present</code>, <code>absent</code>.</p>

  $ensure,

  # <h2>disk</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>The disk(s) for this pool. Can be an array or a space separated string.</p>

  $disk,

  # <h2>log</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Log disks for this pool. This type does not currently support mirroring of log disks.</p>

  $log,

  # <h2>mirror</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>List of all the devices to mirror for this pool. Each mirror should be a
  # space separated string:</p>
  #
  # <pre><code>mirror =&gt; ["disk1 disk2", "disk3 disk4"],
  # </code></pre>

  $mirror,

  # <h2>provider</h2>
  # <p>The specific backend to use for this <code>zpool</code>
  # resource. You will seldom need to specify this — Puppet will usually
  # discover the appropriate provider for your platform.</p>
  #
  # <p>Available providers are:</p>
  #
  # <ul>
  #   <li><a href="#zpool-provider-zpool"><code>zpool</code></a></li>
  # </ul>

  $provider,

  # <h2>raid_parity</h2>
  # <p>Determines parity when using the <code>raidz</code> parameter.</p>

  $raid_parity,

  # <h2>raidz</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>List of all the devices to raid for this pool. Should be an array of
  # space separated strings:</p>
  #
  # <pre><code>raidz =&gt; ["disk1 disk2", "disk3 disk4"],
  # </code></pre>

  $raidz,

  # <h2>spare</h2>
  # <p><em>(<strong>Property:</strong> This attribute represents concrete state on the target system.)</em></p>
  #
  # <p>Spare disk(s) for this pool.</p>

  $spare,

){}

