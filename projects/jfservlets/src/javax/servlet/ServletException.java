package javax.servlet;

/** ServletException
 *
 * @author peter.quiring
 */

public class ServletException extends Exception {
  private static final long serialVersionUID = 1L;
  public ServletException() {}
  public ServletException(String message) {
    super(message);
  }
  public ServletException(String message, Throwable rootCause) {
    super(message, rootCause);
  }
  public ServletException(Throwable rootCause) {
    super(rootCause);
  }
  public Throwable getRootCause() {
    return getCause();
  }
}
