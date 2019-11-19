on_stylesheet_saved do |filename|
  Growl.notify {
     self.message = "#{File.basename(filename)} updated!"
     self.icon = '/path/to/success.jpg'
   }
end

additional_import_paths = ["/Users/chris/work/shared_sass_first"]

on_stylesheet_error do |filename, message|
  Growl.notify {
    self.message = "#{File.basename(filename)}: #{message}"
    self.icon = '/path/to/fail.jpg'
    sticky!
  }
end

additional_import_paths = ["/Users/chris/work/shared_sass_second", "/Users/chris/work/shared_sass_third"]
