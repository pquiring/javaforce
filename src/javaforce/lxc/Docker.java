package javaforce.lxc;

/** Docker implementation of ContainerManager
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.jni.lnx.*;
import javaforce.*;

public class Docker implements LxcContainerManager {

  public static class DockerContainer extends LxcContainer {

    public DockerContainer(String id) {super(id);}

    private LnxPty pty;

    public LnxPty attach() {
      ArrayList<String> cmd = new ArrayList<>();
      cmd.add("attach");
      cmd.add(id);
      cmd.add(null);
      pty = LnxPty.exec("/usr/bin/docker", cmd.toArray(JF.StringArrayType), new String[] {null});
      return pty;
    }

    private static byte[] ctrl_p = {16};
    private static byte[] ctrl_q = {17};

    public boolean detach() {
      if (pty == null) return true;
      //signal docker to detach
      JF.sleep(50);
      pty.write(ctrl_p);
      JF.sleep(50);
      pty.write(ctrl_q);
      JF.sleep(50);
      close();
      return true;
    }

    public boolean close() {
      if (pty == null) return true;
      pty.close();
      pty = null;
      return true;
    }

    public boolean restart() {
      return false;
    }

    public boolean delete() {
      ShellProcess sp = new ShellProcess();
      ArrayList<String> cmd = new ArrayList<>();
      cmd.add("/usr/bin/docker");
      cmd.add("container");
      cmd.add("rm");
      cmd.add(id);
      sp.run(cmd.toArray(JF.StringArrayType), true);
      return sp.getErrorLevel() == 0;
    }
  }

  public static class DockerImage extends LxcImage {

    public static String default_repo = "docker.io";

    public DockerImage() {}

    public DockerImage(String mtag) {super(mtag);}

    public DockerImage(String mtag, String ver) {super(mtag, ver);}

    public boolean delete() {
      if (id == null) return false;
      ShellProcess sp = new ShellProcess();
      ArrayList<String> cmd = new ArrayList<>();
      cmd.add("/usr/bin/docker");
      cmd.add("image");
      cmd.add("rm");
      cmd.add(id);
      sp.run(cmd.toArray(JF.StringArrayType), true);
      return sp.getErrorLevel() == 0;
    }
  }

  public LxcImage[] listImages() {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"/usr/bin/docker", "image", "list"}, true);
/*
REPOSITORY       TAG        IMAGE ID       CREATED        SIZE
amd64/debian     trixie     ff19e76646cc   5 weeks ago    120MB
server/arch/name version    123456781234   age            size
*/
    String[] lns = out.split("\n");
    ArrayList<LxcImage> images = new ArrayList<>();
    for(int i=1;i<lns.length;i++) {
      String[] fs = lns[i].split("\\s+");  //whitespace char
      if (fs.length < 3) continue;
      images.add(new DockerImage(fs[0], fs[1]).setID(fs[2]));
    }
    return images.toArray(new LxcImage[0]);
  }

  public LnxPty pullImage(LxcImage image) {
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("image");
    cmd.add("pull");
    cmd.add(image.toString());
    cmd.add(null);
    return LnxPty.exec("/usr/bin/docker", cmd.toArray(JF.StringArrayType), new String[] {null});
  }

  public LnxPty createImage(String script_file, LxcImage image, String src_folder) {
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("build");
    cmd.add("-f");
    cmd.add(script_file);
    cmd.add("-t");
    cmd.add(image.toString());
    cmd.add(src_folder);
    cmd.add(null);
    return LnxPty.exec("/usr/bin/docker", cmd.toArray(JF.StringArrayType), new String[] {null});
  }

  public LxcContainer[] listContainers() {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"/usr/bin/docker", "container", "list"}, true);
/*
CONTAINER ID   IMAGE                 COMMAND   CREATED         STATUS         PORTS     NAMES
82c8dabc733f   amd64/debian:trixie   "bash"    3 seconds ago   Up 2 seconds             great_roentgen
*/
    String[] lns = out.split("\n");
    ArrayList<LxcContainer> containers = new ArrayList<>();
    for(int i=1;i<lns.length;i++) {
      String[] fs = lns[i].split("\\s+");  //whitespace char
      if (fs.length < 3) continue;
      containers.add(new DockerContainer(fs[0]).setImage(new DockerImage(fs[1])));
    }
    return containers.toArray(new LxcContainer[0]);
  }

  public LxcContainer createContainer(LxcImage image, String[] cmd) {
    return createContainer(image, cmd, new String[0]);
  }

  public LxcContainer createContainer(LxcImage image, String[] cmd, LxcOption[] options) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cl = new ArrayList<>();
    cl.add("/usr/bin/docker");
    cl.add("run");
    cl.add("-itd");  //interactive, tty, detached
    cl.add("--rm");  //remove after process exits
    if (options != null) {
      for(LxcOption opt : options) {
        String value = opt.value;
        switch (opt.name) {
          case LxcOption.restart: {
            cl.add("--restart");
            //TODO : validate allowed values
            cl.add(value);
            break;
          }
          case LxcOption.port: {
            String[] src_dst = opt.splitValue();  //validation
            if (src_dst == null) continue;
            cl.add("-p");
            cl.add(value);
            break;
          }
          case LxcOption.mount: {
            String[] src_dst = opt.splitValue();
            if (src_dst == null) continue;
            String src = src_dst[0];
            String dst = src_dst[1];
            cl.add("--mount");
            cl.add("type=bind,src=" + src+ ",dst=" + dst);
            break;
          }
        }
      }
    }
    cl.add(image.toString());
    for(int i=0;i<cmd.length;i++) {
      cl.add(cmd[i]);
    }
    String out = sp.run(cl.toArray(JF.StringArrayType), true);
    if (sp.getErrorLevel() != 0) {
      return null;
    } else {
      String[] lns = out.split("\n");
      return new DockerContainer(lns[0]);
    }
  }

  public LxcContainer createContainer(LxcImage image, String[] cmd, String[] options) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cl = new ArrayList<>();
    cl.add("/usr/bin/docker");
    cl.add("run");
    cl.add("-itd");  //interactive, tty, detached
    cl.add("--rm");  //remove after process exits
    if (options != null) {
      for(String opt : options) {
        cl.add(opt);
      }
    }
    cl.add(image.toString());
    for(int i=0;i<cmd.length;i++) {
      cl.add(cmd[i]);
    }
    String out = sp.run(cl.toArray(JF.StringArrayType), true);
    if (sp.getErrorLevel() != 0) {
      JFLog.log("Docker:createContainer failed:" + out);
      return null;
    } else {
      String[] lns = out.split("\n");
      return new DockerContainer(lns[0]);
    }
  }
}
