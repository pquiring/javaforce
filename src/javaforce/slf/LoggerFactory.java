package javaforce.slf;

import org.slf4j.Logger;

/** LoggerFactory
 *
 * @author peter.quiring
 */

public class LoggerFactory implements org.slf4j.ILoggerFactory {

  public Logger getLogger(String name) {
    return new JFLogger(name);
  }
}
