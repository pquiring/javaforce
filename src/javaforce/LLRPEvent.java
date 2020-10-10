package javaforce;

/** LLRP Events interface
 *
 * @author Peter Quiring
 */

public interface LLRPEvent {
  /** Provides EPC in hex format. */
  public void tagRead(String epc);
}
