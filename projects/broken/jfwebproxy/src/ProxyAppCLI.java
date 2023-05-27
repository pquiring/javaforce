public class ProxyAppCLI implements Logger {
  public Service service;
  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("usage:proxy.jar web-proxy-host jsp/php [-alwayssecure | -neversecure]");
      System.out.println("  jsp/php = Use a JSP or PHP web-proxy");
      System.out.println("  -alwayssecure = Always connect with HTTPS to web-proxy");
      System.out.println("  -neversecure = Never connect with HTTPS to web-proxy (not recommended)");
      return;
    }
    new ProxyAppCLI().init(args);
  }
  public void init(String args[]) {
    log("Starting jfWebProxy [v" + Service.version + "] on port 8080...");
    boolean always = false;
    boolean never = false;
    if (args.length > 2) {
      if (args[2].equals("-alwayssecure")) always = true;
      if (args[2].equals("-neversecure")) never = true;
    }
    service = new Service(8080, args[0], this, always, never , args[1]);
    service.start();
  }
  public void log(String msg) {
    System.out.println(msg);
  }
  public void log(Throwable t) {
    StringBuffer buf = new StringBuffer();
    buf.append(t.toString());
    buf.append("\r\n");
    StackTraceElement ste[] = t.getStackTrace();
    if (ste != null) {
      for(int a=0;a<ste.length;a++) {
        buf.append("\tat ");
        buf.append(ste[a].toString());
        buf.append("\r\n");
      }
    }
    log(buf.toString());
  }
}
