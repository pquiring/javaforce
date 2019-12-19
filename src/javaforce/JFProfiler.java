package javaforce;

/** JFProfiler
 *
 * @author pquiring
 */

public class JFProfiler {
  private long ms[];
  private String names[];
  private int step;
  private String name;
  private StringBuilder sb = new StringBuilder();

  public JFProfiler(String name, int steps) {
    steps++;
    this.name = name;
    ms = new long[steps];
    names = new String[steps];
  }

  public void start() {
    ms[0] = System.currentTimeMillis();
    step = 1;
  }

  public void step(String name) {
    ms[step] = System.currentTimeMillis();
    names[step] = name;
    step++;
  }

  public void stop() {
    sb.setLength(0);
    sb.append(name);
    sb.append(":");
    for(int i=1;i<step;i++) {
      long delta = ms[i] - ms[i - 1];
      sb.append(String.format("%02dms=(%s) ", delta, names[i]));
    }
    System.out.println(sb.toString());
  }
}
