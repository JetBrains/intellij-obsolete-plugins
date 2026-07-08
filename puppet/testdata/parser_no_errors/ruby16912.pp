case $ensure {
    /^(running|stopped)$/: {
      # Do nothing.
    }
    default: {
      fail("${title}: Ensure value '${ensure}' is not supported")  #red highlighting here
    }
  }