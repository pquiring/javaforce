package javaforce;

/** LLRP Events interface
 *
 * @author Peter Quiring
 */

public interface LLRPEvent {
  /** Provides EPC in hex format. */
  public void tagRead(String epc);
  /** Triggered when a GPI event occurs. */
  public void gpiEvent(int port, boolean event);
  /** Triggered when RO spec is started. */
  public void readStarted();
  /** Triggered when RO spec has ended. */
  public void readEnded();
}
