package javaforce.linux;

import javaforce.api.linux.*;

/** Unix Socket with OOB FD support.
 *
 * Java16 added Unix Sockets support but does not include out-of-bound file descriptor support.
 *
 * @author pquiring
 */

public class UnixSocket {
  private UnixSocketAPI api = UnixSocketAPI.getInstance();
  private int fd;

  public UnixSocket() {}

  private UnixSocket(int fd) {
    this.fd = fd;
  }

  /** Return native unix socket file descriptor. */
  public int get_fd() {
    return fd;
  }

  /** Creates a new unbound Unix Socket. */
  public boolean open() {
    if (fd != 0) return false;
    fd = api.usOpen();
    return fd >= 0;
  }

  /** Binds Socket to a path (max 108 chars). */
  public boolean bind(String name) {
    return api.usBind(fd, name);
  }

  /** Starts listening for client connections. */
  public boolean listen() {
    return api.usListen(fd);
  }

  /** Accepts a client connection. */
  public UnixSocket accept() {
    int client = api.usAccept(fd);
    if (client <= 0) return null;
    return new UnixSocket(client);
  }

  /** Connects to another Socket. */
  public boolean connect(String name) {
    return api.usConnect(fd, name);
  }

  /** Read data and file descriptors.
   *
   * Note : any fds received should be closed.
   *
   * @param fd = socket fd
   * @param data = buffer to receive data
   * @param offset_data = offset into data
   * @param len_data = [0] = size of data (on success returns length read)
   * @param fds = buffer to receive file descriptors
   * @param offset_fd = offset into fds
   * @param len_fd = [0] = # of fds to read (on success returns # of fds read)
   */
  public boolean read(byte[] data, int offset_data, int[] len_data, int[] fds, int offset_fd, int[] len_fd) {
    return api.usRead(fd, data, offset_data, len_data, fds, offset_fd, len_fd);
  }

  /** Write data and file descriptors.
   * @param fd = socket fd
   * @param data = buffer of data to send
   * @param offset_data = offset into data
   * @param len_data = [0] = size of data (on success returns length written)
   * @param fds = buffer of file descriptors to send
   * @param offset_fd = offset into fds
   * @param len_fd = [0] = # of fds to write (on success returns # of fds written)
   */
  public boolean write(byte[] data, int offset_data, int[] len_data, int[] fds, int offset_fd, int[] len_fd) {
    return api.usWrite(fd, data, offset_data, len_data, fds, offset_fd, len_fd);
  }

  /** Close unix socket. */
  public boolean close() {
    if (fd == 0) return false;
    boolean res = api.usClose(fd);
    fd = 0;
    return res;
  }

  /** Close file descriptors. */
  public boolean close(int[] fds, int offset, int len_fd) {
    int end = len_fd + offset;
    for(int a=offset;a<end;a++) {
      api.usClose(fds[a]);
    }
    return true;
  }

}
