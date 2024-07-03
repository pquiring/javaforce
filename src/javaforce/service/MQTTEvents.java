package javaforce.service;

/** MQTT Events
 *
 * @author pquiring
 */

public interface MQTTEvents {
  public boolean message(String topic, String msg);
}
