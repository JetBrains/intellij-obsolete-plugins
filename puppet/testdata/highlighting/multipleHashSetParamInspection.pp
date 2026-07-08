file { '/etc/ololo':
  ensure => present,
  * => { 'mode' => 755, 'owner' => 'ol' },
  group => 'grgr'
}

file { '/etc/tratata':
  ensure => present,
  <error descr="Multiple hash set parameters are prohibited">* => { 'mode' => 755, 'owner' => 'ol' }</error>,
  <error descr="Multiple hash set parameters are prohibited">* => { 'mode' => 644, 'owner' => 'ol' }</error>,
  group => 'grgr'
}

file {
  '/etc/aaa':
    ensure => present,
    * => { 'mode' => 755, 'owner' => 'ol' },
    group => 'grgr';
  '/etc/bbb':
    ensure => present,
    * => { 'mode' => 644, 'owner' => 'ol' },
    group => 'grgr';
}

file {
  '/etc/ccc':
    ensure => present,
    * => { 'mode' => 755, 'owner' => 'ol' },
    group => 'grgr';
  '/etc/ddd':
    ensure => present,
    <error descr="Multiple hash set parameters are prohibited">* => { 'mode' => 644, 'owner' => 'ol' }</error>,
    <error descr="Multiple hash set parameters are prohibited">* => { 'mode' => 644, 'owner' => 'ol' }</error>,
    group => 'grgr';
}
