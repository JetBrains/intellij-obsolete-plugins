  $arch1 = $::kernel ? {
    'windows' => 'windows',
    'sunos'   => 'solaris',
    'darwin'  => 'macosx',
    default   => 'linux',
  }
  if $arch1 != 'macosx' {
    $arch2 = $::architecture ? {
      'x86_64' => 'x86-64',
      'amd64'  => 'x86-64',
      default  => 'x86-32',
    }
  } else {
    $arch2 = $::architecture ? {
      'x86_64' => 'universal-64',
      default  => 'universal-32',
    }
  }