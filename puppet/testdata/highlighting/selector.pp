 file { 'sshdconfig':
           path => $operatingsystem ? {
             solaris => '/usr/local/etc/ssh/sshd_config',
             default => '/etc/ssh/sshd_config',
           }
         }