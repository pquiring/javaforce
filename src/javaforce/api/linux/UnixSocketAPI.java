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

  public boolean usRead(int fd, byte[] data, int offset_data, int[] len_data, int[] fds, int offset_fd, int[] len_fd);

  public boolean usWrite(int fd, byte[] data, int offset_data, int[] len_data, int[] fds, int offset_fd, int[] len_fd);

  public boolean usClose(int fd);
}
