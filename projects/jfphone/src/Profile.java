public class Profile {
  private long start;
  private String name;
  public static boolean enabled = true;
  public Profile(String name) {
    this.name = name;
  }
  public void start() {
    start = System.currentTimeMillis();
  }
  public void stop(String msg) {
    long current = System.currentTimeMillis();
    if (enabled) System.out.println(name + ":" + (current - start) + ":" + msg);
    start = current;
  }
}
