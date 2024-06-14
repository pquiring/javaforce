package javaforce.slf;

import org.slf4j.Logger;

/**
 *
 * @author peter.quiring
 */

public class LoggerFactory implements org.slf4j.ILoggerFactory {

  public Logger getLogger(String name) {
    return new JFLogger(name);
  }
}
