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

  /** Creates a new unbound Unix Socket. */
  public int open() {
    return api.usOpen();
  }

  /** Binds Socket to a path (max 108 chars). */
  public boolean bind(int fd, String name) {
    return api.usBind(fd, name);
  }

  /** Starts listening for client connections. */
  public boolean listen(int fd) {
    return api.usListen(fd);
  }

  /** Accepts a client connection. */
  public int accept(int fd) {
    return api.usAccept(fd);
  }

  /** Connects to another Socket. */
  public boolean connect(int fd, String name) {
    return api.usConnect(fd, name);
  }

  /** Read data and file descriptors.
   *
   * Note : any fds received should be closed.
   *
   * @param fd = socket fd
   * @param len_data = [0] = size of data (on success returns length read)
   * @param data = buffer to receive data
   * @param len_fd = [0] = # of fds to read (on success returns # of fds read)
   * @param fds = buffer to receive file descriptors
   */
  public boolean read(int fd, int[] len_data, byte[] data, int[] len_fd, int[] fds) {
    return api.usRead(fd, len_data, data, len_fd, fds);
  }

  /** Write data and file descriptors.
   * @param fd = socket fd
   * @param len_data = [0] = size of data (on success returns length written)
   * @param data = buffer of data to send
   * @param len_fd = [0] = # of fds to write (on success returns # of fds written)
   * @param fds = buffer of file descriptors to send
   */
  public boolean write(int fd, int[] len_data, byte[] data, int[] len_fd, int[] fds) {
    return api.usWrite(fd, len_data, data, len_fd, fds);
  }

  /** Close unix socket or file descriptor. */
  public boolean close(int fd) {
    return api.usClose(fd);
  }

}
