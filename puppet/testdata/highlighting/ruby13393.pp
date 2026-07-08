$puppetDpkgVersion = '3.1.1-1puppetlabs1'

package { 'bc': }

Exec { path => [ '/bin/', '/sbin/' , '/usr/bin/', '/usr/sbin/' ] }

class { 'puppet':
  version   => $puppetDpkgVersion,
  mode      => 'server',
  passenger => true,
}

exec { 'disable puppetmaster daemon':
  command => 'update-rc.d -f puppetmaster remove && service puppetmaster stop'
}

# Minor version comparison for `bc`
$bcCmp = "`dpkg -s puppet | grep 'Version' | cut -c 10- | \
           cut -c -3` < `echo ${puppetDpkgVersion} | cut -c -3`"

exec { 'puppet-repo':
command => 'wget http://apt.puppetlabs.com/puppetlabs-release-precise.deb && \
              dpkg -i puppetlabs-release-precise.deb && \
              rm -f puppetlabs-release-precise.deb && \
              sudo apt-get update',
path    => [ '/bin/', '/sbin/' , '/usr/bin/', '/usr/sbin/' ],
onlyif  => "/bin/bash -c x=$(echo \"${bcCmp}\" | bc); test \"\$x\" != \"1\""
}

Package['bc'] -> Exec['puppet-repo']
Exec['puppet-repo'] -> Class['puppet']
Class['puppet::server']
  -> Exec['disable puppetmaster daemon']
  -> Class['puppet::server::passenger']