service { 'sshd':
      require => File['sshdconfig', 'sshconfig', 'authorized_keys']
}