package javaforce.slf;

import javaforce.JFLog;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

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
    JFLog.log(JFLog.TRACE, MessageFormatter.format(format, arg).getMessage());
  }

  public void trace(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.TRACE, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void trace(String format, Object... args) {
    JFLog.log(JFLog.TRACE, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void trace(String msg, Throwable t) {
    JFLog.log(JFLog.TRACE, msg);
    if (t != null) JFLog.log(JFLog.TRACE, t);
  }

  public boolean isTraceEnabled(Marker marker) {
    return false;
  }

  public void trace(Marker marker, String msg) {
    JFLog.log(JFLog.TRACE, msg);
  }

  public void trace(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.TRACE, MessageFormatter.format(format, arg).getMessage());
  }

  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.TRACE, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void trace(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.TRACE, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void trace(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.TRACE, msg);
    if (t != null) JFLog.log(JFLog.TRACE, t);
  }

  public boolean isDebugEnabled() {
    return true;
  }

  public void debug(String msg) {
    JFLog.log(JFLog.DEBUG, msg);
  }

  public void debug(String format, Object arg) {
    JFLog.log(JFLog.DEBUG, MessageFormatter.format(format, arg).getMessage());
  }

  public void debug(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.DEBUG, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void debug(String format, Object... args) {
    JFLog.log(JFLog.DEBUG, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void debug(String msg, Throwable t) {
    JFLog.log(JFLog.DEBUG, msg);
    if (t != null) JFLog.log(JFLog.DEBUG, t);
  }

  public boolean isDebugEnabled(Marker marker) {
    return false;
  }

  public void debug(Marker marker, String msg) {
    JFLog.log(JFLog.DEBUG, msg);
  }

  public void debug(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.DEBUG, MessageFormatter.format(format, arg).getMessage());
  }

  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.DEBUG, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void debug(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.DEBUG, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void debug(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.DEBUG, msg);
    if (t != null) JFLog.log(JFLog.DEBUG, t);
  }

  public boolean isInfoEnabled() {
    return true;
  }

  public void info(String msg) {
    JFLog.log(JFLog.INFO, msg);
  }

  public void info(String format, Object arg) {
    JFLog.log(JFLog.INFO, MessageFormatter.format(format, arg).getMessage());
  }

  public void info(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.INFO, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void info(String format, Object... args) {
    JFLog.log(JFLog.INFO, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void info(String msg, Throwable t) {
    JFLog.log(JFLog.INFO, msg);
    if (t != null) JFLog.log(JFLog.INFO, t);
  }

  public boolean isInfoEnabled(Marker marker) {
    return false;
  }

  public void info(Marker marker, String msg) {
    JFLog.log(JFLog.INFO, msg);
  }

  public void info(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.INFO, MessageFormatter.format(format, arg).getMessage());
  }

  public void info(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.INFO, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void info(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.INFO, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void info(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.INFO, msg);
    if (t != null) JFLog.log(JFLog.INFO, t);
  }

  public boolean isWarnEnabled() {
    return true;
  }

  public void warn(String msg) {
    JFLog.log(JFLog.WARN, msg);
  }

  public void warn(String format, Object arg) {
    JFLog.log(JFLog.WARN, MessageFormatter.format(format, arg).getMessage());
  }

  public void warn(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.WARN, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void warn(String format, Object... args) {
    JFLog.log(JFLog.WARN, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void warn(String msg, Throwable t) {
    JFLog.log(JFLog.WARN, msg);
    if (t != null) JFLog.log(JFLog.WARN, t);
  }

  public boolean isWarnEnabled(Marker marker) {
    return false;
  }

  public void warn(Marker marker, String msg) {
    JFLog.log(JFLog.WARN, msg);
  }

  public void warn(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.WARN, MessageFormatter.format(format, arg).getMessage());
  }

  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.WARN, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void warn(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.WARN, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void warn(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.WARN, msg);
    if (t != null) JFLog.log(JFLog.WARN, t);
  }

  public boolean isErrorEnabled() {
    return true;
  }

  public void error(String msg) {
    JFLog.log(JFLog.ERROR, msg);
  }

  public void error(String format, Object arg) {
    JFLog.log(JFLog.ERROR, MessageFormatter.format(format, arg).getMessage());
  }

  public void error(String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.ERROR, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void error(String format, Object... args) {
    JFLog.log(JFLog.ERROR, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void error(String msg, Throwable t) {
    JFLog.log(JFLog.ERROR, msg);
    if (t != null) JFLog.log(JFLog.ERROR, t);
  }

  public boolean isErrorEnabled(Marker marker) {
    return false;
  }

  public void error(Marker marker, String msg) {
    JFLog.log(JFLog.ERROR, msg);
  }

  public void error(Marker marker, String format, Object arg) {
    JFLog.log(JFLog.ERROR, MessageFormatter.format(format, arg).getMessage());
  }

  public void error(Marker marker, String format, Object arg1, Object arg2) {
    JFLog.log(JFLog.ERROR, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  public void error(Marker marker, String format, Object... args) {
    JFLog.log(JFLog.ERROR, MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void error(Marker marker, String msg, Throwable t) {
    JFLog.log(JFLog.ERROR, msg);
    if (t != null) JFLog.log(JFLog.ERROR, t);
  }
}
