package javaforce.service;

/**
 *
 * @author pquiring
 */

public interface MQTTEvents {
  public boolean message(String topic, String msg);
}
