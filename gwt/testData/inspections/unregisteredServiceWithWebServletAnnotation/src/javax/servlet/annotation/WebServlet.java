package javax.servlet.annotation;

// Minimal stub of the Servlet 3.0 annotation for tests.
public @interface WebServlet {
  String[] value() default {};

  String[] urlPatterns() default {};
}
