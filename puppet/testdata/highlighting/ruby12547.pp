case $ensure {
   'present' : {
      nginx::install_site { $name:
      content => $content
      }
   }
 }