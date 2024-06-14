package javaforce.slf;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;

/**
 *
 * @author peter.quiring
 */

public class ServiceProvider implements org.slf4j.spi.SLF4JServiceProvider {

  public ILoggerFactory getLoggerFactory() {
    return new LoggerFactory();
  }

  public IMarkerFactory getMarkerFactory() {
    return null;
  }

  public MDCAdapter getMDCAdapter() {
    return null;
  }

  public String getRequestedApiVersion() {
    return "2.0";
  }

  public void initialize() {
  }
}
