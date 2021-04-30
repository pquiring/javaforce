package jfnetboot;

/** Client File Handle
 *
 * @author pquiring
 */

public class CHandle {
  public String serial;  //client serial
  public short arch;  //client arch
  public long handle;  //file system handle

  public Client client;
  public FileSystem fs;

  public CHandle(String serial, short arch, long handle) {
    this.serial = serial;
    this.arch = arch;
    this.handle = handle;

    client = Clients.getClient(serial, Arch.toString(arch));
    fs = client.getFileSystem();
  }

  public String toString() {
    return String.format("%s:%s:%016x", serial, Arch.toString(arch), handle);
  }
}
