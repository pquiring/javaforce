package javaforce.api.linux;

import javaforce.ffm.*;

/** Unix Sockets native API.
 *
 * @author pquiring
 */

public interface UnixSocketAPI {

  public static UnixSocketAPI getInstance() {
    return UnixSocketFFM.getInstance();
  }

  public int usOpen();

  public boolean usBind(int fd, String name);

  public boolean usListen(int fd);

  public int usAccept(int fd);

  public boolean usConnect(int fd, String name);

  public boolean usRead(int fd, int[] len_data, byte[] data, int[] len_fd, int[] fds);

  public boolean usWrite(int fd, int[] len_data, byte[] data, int[] len_fd, int[] fds);

  public boolean usClose(int fd);
}
