on_stylesheet_saved do |filename|
  Growl.notify {
     self.message = "#{File.basename(filename)} updated!"
     self.icon = '/path/to/success.jpg'
   }
end

add_import_path "/Users/chris/work/shared_sass"

on_stylesheet_error do |filename, message|
  Growl.notify {
    self.message = "#{File.basename(filename)}: #{message}"
    self.icon = '/path/to/fail.jpg'
    sticky!
  }
end
