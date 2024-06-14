package javaforce.slf;

import javaforce.JFLog;

import org.slf4j.Logger;
import org.slf4j.Marker;

/** JFLog compatible with SLF
 *
 * @author peter.quiring
 */

public class JFLogger implements Logger {

  private String name;

  public JFLogger(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public boolean isTraceEnabled() {
    return true;
  }

  public void trace(String msg) {
    JFLog.log(JFLog.TRACE, msg);
  }

  public void trace(String format, Object arg) {
    JFLog.log(JFLog.TRACE, String.format(format, arg));
  }

  public void trace(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.TRACE, String.format(format, arg1, arg2));
  }

  public void trace(String format, Object... args) {
    JFLog.log(JFLog.TRACE, String.format(format, args));
  }

  public void trace(String msg, Throwable t) {
    JFLog.log(JFLog.TRACE, msg + t.toString());
  }

  public boolean isTraceEnabled(Marker marker) {
    return false;
  }

  public void trace(Marker marker, String msg) {
    JFLog.log(JFLog.TRACE, msg);
  }

  public void trace(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.TRACE, String.format(format, arg));
  }

  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.TRACE, String.format(format, arg1, arg2));
  }

  public void trace(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.TRACE, String.format(format, args));
  }

  public void trace(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.TRACE, msg + t);
  }

  public boolean isDebugEnabled() {
    return true;
  }

  public void debug(String msg) {
    JFLog.log(JFLog.DEBUG, msg);
  }

  public void debug(String format, Object arg) {
    JFLog.log(JFLog.DEBUG, String.format(format, arg));
  }

  public void debug(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.DEBUG, String.format(format, arg1, arg2));
  }

  public void debug(String format, Object... args) {
    JFLog.log(JFLog.DEBUG, String.format(format, args));
  }

  public void debug(String msg, Throwable t) {
    JFLog.log(JFLog.DEBUG, msg + t);
  }

  public boolean isDebugEnabled(Marker marker) {
    return false;
  }

  public void debug(Marker marker, String msg) {
    JFLog.log(JFLog.DEBUG, msg);
  }

  public void debug(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.DEBUG, String.format(format, arg));
  }

  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.DEBUG, String.format(format, arg1, arg2));
  }

  public void debug(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.DEBUG, String.format(format, args));
  }

  public void debug(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.DEBUG, msg + t);
  }

  public boolean isInfoEnabled() {
    return true;
  }

  public void info(String msg) {
    JFLog.log(JFLog.INFO, msg);
  }

  public void info(String format, Object arg) {
    JFLog.log(JFLog.INFO, String.format(format, arg));
  }

  public void info(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.INFO, String.format(format, arg1, arg2));
  }

  public void info(String format, Object... args) {
    JFLog.log(JFLog.INFO, String.format(format, args));
  }

  public void info(String msg, Throwable t) {
    JFLog.log(JFLog.INFO, msg + t);
  }

  public boolean isInfoEnabled(Marker marker) {
    return false;
  }

  public void info(Marker marker, String msg) {
    JFLog.log(JFLog.INFO, msg);
  }

  public void info(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.INFO, String.format(format, arg));
  }

  public void info(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.INFO, String.format(format, arg1, arg2));
  }

  public void info(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.INFO, String.format(format, args));
  }

  public void info(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.INFO, msg + t);
  }

  public boolean isWarnEnabled() {
    return true;
  }

  public void warn(String msg) {
    JFLog.log(JFLog.WARN, msg);
  }

  public void warn(String format, Object arg) {
    JFLog.log(JFLog.WARN, String.format(format, arg));
  }

  public void warn(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.WARN, String.format(format, arg1, arg2));
  }

  public void warn(String format, Object... args) {
    JFLog.log(JFLog.WARN, String.format(format, args));
  }

  public void warn(String msg, Throwable t) {
    JFLog.log(JFLog.WARN, msg + t);
  }

  public boolean isWarnEnabled(Marker marker) {
    return false;
  }

  public void warn(Marker marker, String msg) {
    JFLog.log(JFLog.WARN, msg);
  }

  public void warn(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.WARN, String.format(format, arg));
  }

  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.WARN, String.format(format, arg1, arg2));
  }

  public void warn(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.WARN, String.format(format, args));
  }

  public void warn(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.WARN, msg + t);
  }

  public boolean isErrorEnabled() {
    return true;
  }

  public void error(String msg) {
    JFLog.log(JFLog.ERROR, msg);
  }

  public void error(String format, Object arg) {
    JFLog.log(JFLog.ERROR, String.format(format, arg));
  }

  public void error(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.ERROR, String.format(format, arg1, arg2));
  }

  public void error(String format, Object... args) {
    JFLog.log(JFLog.ERROR, String.format(format, args));
  }

  public void error(String msg, Throwable t) {
    JFLog.log(JFLog.ERROR, msg + t);
  }

  public boolean isErrorEnabled(Marker marker) {
    return false;
  }

  public void error(Marker marker, String msg) {
    JFLog.log(JFLog.ERROR, msg);
  }

  public void error(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.ERROR, String.format(format, arg));
  }

  public void error(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.ERROR, String.format(format, arg1, arg2));
  }

  public void error(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.ERROR, String.format(format, args));
  }

  public void error(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.ERROR, msg + t);
  }
}
