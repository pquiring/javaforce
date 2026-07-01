package javaforce.lxc;

import javaforce.linux.*;

/** Linux Container Manager interface
 *
 * @author pquiring
 */

public interface LxcContainerManager {
  //Images
  public LxcImage[] listImages();
  public Pty pullImage(LxcImage arch_name_version);
  public Pty createImage(String script_file, LxcImage arch_name_version, String src_folder);

  //Containers
  public LxcContainer[] listContainers();
  public LxcContainer createContainer(LxcImage image, String[] cmd);
  public LxcContainer createContainer(LxcImage image, String[] cmd, String[] options);
  public LxcContainer createContainer(LxcImage image, String[] cmd, LxcOption[] options);
}
