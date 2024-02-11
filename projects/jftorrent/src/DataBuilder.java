/** Data Builder (bencoded)
 *
 * @author pquiring
 */

import java.util.*;

public class DataBuilder {
  private byte[] data = new byte[0];

  public void append(byte[] buf) {
    int org = data.length;
    data = Arrays.copyOf(data, data.length + buf.length);
    System.arraycopy(buf, 0, data, org, buf.length);
  }

  public void append(String str) {
    append(str.getBytes());
  }

  public byte[] toByteArray() {
    return data;
  }
}
