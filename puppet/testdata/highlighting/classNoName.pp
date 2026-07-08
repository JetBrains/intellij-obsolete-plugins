class  {
  'apt-keys': stage => first;
  'sendmail': stage => main;
  'apache':   stage => last;
}