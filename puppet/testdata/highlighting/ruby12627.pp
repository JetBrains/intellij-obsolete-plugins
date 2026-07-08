class nginx {
  $nginxversion = $nginxversion ? {
    undef => '1.0.0',
    default => $nginxversion
  }
}