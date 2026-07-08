$greeting = "Hello world"
notify {name<error descr="'(', '.', ':', '=>', CONSUMES, PRODUCES or '[' expected, got '}'">}</error>
notify {$greeting<error descr="'.', ':' or '[' expected, got '}'">}</error>
notify {'name'<error descr="'.', ':' or '[' expected, got '}'">}</error>
class {}
class {$greeting<error descr="'.', ':' or '[' expected, got '}'">}</error>
class {''<error descr="'.', ':' or '[' expected, got '}'">}</error>



