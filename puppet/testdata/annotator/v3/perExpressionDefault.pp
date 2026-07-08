file {
  <error descr="Per-expression default is not available in Puppet v3">default:
    ensure => file,
    owner  => "root",
    group  => "wheel",
    mode   => "0600",</error>
;
  ['ssh_host_dsa_key', 'ssh_host_key', 'ssh_host_rsa_key']:
  # use all defaults
;
  ['ssh_config', 'ssh_host_dsa_key.pub', 'ssh_host_key.pub', 'ssh_host_rsa_key.pub', 'sshd_config']:
    # override mode
    mode => "0644",
;
}
