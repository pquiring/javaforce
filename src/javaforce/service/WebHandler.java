package javaforce.service;

/**
 * Web Server handler interface.
 *
 * @author pquiring
 *
 * Created : Aug 23, 2013
 */

public interface WebHandler {
  /** Process a POST request */
  public void doPost(WebRequest req, WebResponse res);
  /** Process a GET request */
  public void doGet(WebRequest req, WebResponse res);
}
