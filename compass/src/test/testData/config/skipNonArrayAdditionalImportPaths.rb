on_stylesheet_error do |filename, message|
  Growl.notify {
    self.message = "#{File.basename(filename)}: #{message}"
    self.icon = '/path/to/fail.jpg'
    sticky!
  }
end

additional_import_paths = "/Users/chris/work/shared_sass_second"
