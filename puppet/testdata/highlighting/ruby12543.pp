class freebsd inherits unix {
      File['/etc/passwd', '/etc/shadow'] { group => 'wheel' }
    }