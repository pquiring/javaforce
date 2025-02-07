package javaforce;

/** MQTT Events
 *
 * @author pquiring
 */

public interface MQTTEvents {
  public void onConnect();
  public void onSubscribe(String topic);
  public void onMessage(String topic, String msg);
}
