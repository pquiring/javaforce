package javaforce.slf;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;

import javaforce.*;

/** ServiceProvider
 *
 * @author peter.quiring
 */

public class ServiceProvider implements org.slf4j.spi.SLF4JServiceProvider {

  public ILoggerFactory getLoggerFactory() {
    JFLog.log("javaforce.slf.ServiceProvider.getLoggerFactory");
    return new LoggerFactory();
  }

  public IMarkerFactory getMarkerFactory() {
    JFLog.log("javaforce.slf.ServiceProvider.getMarkerFactory");
    return new BasicMarkerFactory();
  }

  public MDCAdapter getMDCAdapter() {
    JFLog.log("javaforce.slf.ServiceProvider.getMDCAdapeter");
    return new BasicMDCAdapter();
  }

  public String getRequestedApiVersion() {
    return "2.0";
  }

  public void initialize() {
  }
}
