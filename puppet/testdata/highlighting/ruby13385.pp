exec { 'puppet-repo':
  command => 'wget http://apt.puppetlabs.com/puppetlabs-release-precise.deb && \
  dpkg -i puppetlabs-release-precise.deb && \
  rm -f puppetlabs-release-precise.deb && \
  sudo apt-get update',
  path    => [ '/bin/', '/sbin/' , '/usr/bin/', '/usr/sbin/' ],
#  onlyif  => "/bin/bash -c x=$(echo \"${bcCmp}\" | bc); test \"\$x\" != \"1\""
}