class database {
package {
'libmysqlclient-dev': ensure => present

}
class { 'mysql::server':
config_hash => { name => '0.0.0.0' }
}
}