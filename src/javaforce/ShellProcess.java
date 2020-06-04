/**
 * Provides services to run shell processes with input/output and sudo password
 * authentication.
 *
 * Created : Feb 18, 2012
 *
 * Note : The responses will not work if the process spawns a child process that
 * expects input.
 *
 */
package javaforce;

import java.lang.reflect.*;
import java.util.*;
import java.io.*;

public class ShellProcess {

  private Process p;
  private InputStream is, es;
  private OutputStream os;
  private StringBuffer output;
  private boolean active;
  private ArrayList<Response> script = new ArrayList<Response>();
  private ArrayList<Variable> envAdd = new ArrayList<Variable>();
  private ArrayList<Variable> envRemove = new ArrayList<Variable>();
  private ShellProcessListener listener = null;
  private File path;
  private int errorLevel;
  private boolean keepOutput = true;
  public static boolean log = false;
  public static boolean logPrompt = false;
  public String command;

  private static class Response {
    public String prompt;
    public String reply;
    public boolean repeat, used, regex, term;
  };

  private static class Variable {
    public Variable(String name, String value) {
      this.name = name;
      this.value = value;
    }
    public String name, value;
  }

  /**
   * Registers a response to a prompt. If repeat is true the response can be
   * used many times.
   */
  public void addResponse(String prompt, String reply, boolean repeat) {
    Response res = new Response();
    res.prompt = prompt.trim();
    res.reply = reply;
    res.repeat = repeat;
    res.regex = false;
    res.term = false;
    script.add(res);
  }

  /**
   * Terminates the program if prompt is detected.
   */
  public void addTerminate(String prompt) {
    Response res = new Response();
    res.prompt = prompt.trim();
    res.reply = "";
    res.repeat = false;
    res.regex = false;
    res.term = true;
    script.add(res);
  }

  /**
   * Registers a response to a prompt in the form of a regex. If repeat is true
   * the response can be used many times.
   */
  public void addRegexResponse(String prompt_regex, String reply, boolean repeat) {
    Response res = new Response();
    res.prompt = prompt_regex.trim();
    res.reply = reply;
    res.repeat = repeat;
    res.regex = true;
    res.term = false;
    script.add(res);
  }

  /**
   * Terminates the program if prompt_regex is detected.
   */
  public void addRegexTerminate(String prompt_regex) {
    Response res = new Response();
    res.prompt = prompt_regex.trim();
    res.reply = "";
    res.repeat = false;
    res.regex = true;
    res.term = true;
    script.add(res);
  }

  /**
   * The listener will receive all output.
   */
  public void addListener(ShellProcessListener listener) {
    this.listener = listener;
  }

  public ShellProcessListener getListener() {
    return listener;
  }

  public void addEnvironmentVariable(String name, String value) {
    envAdd.add(new Variable(name, value));
  }

  public void removeEnvironmentVariable(String name) {
    envRemove.add(new Variable(name, null));
  }

  /**
   * Sets init working folder.
   */
  public void setFolder(File path) {
    this.path = path;
  }

  /**
   * Discards all output. Note that responses are disabled when this is enabled.
   * This is intended for long processes that are monitored with a
   * ShellProcessListener
   */
  public void keepOutput(boolean state) {
    keepOutput = state;
  }

  /**
   * See run(String cmd[], boolean redirStderr)
   */
  public String run(List<String> cmd, boolean redirStderr) {
    return run(cmd.toArray(new String[0]), redirStderr);
  }

  /**
   * Runs a process, sending responses to stdin and returning all stdout. The
   * responses should cause the process to terminate.
   * If cmd[0] is 'sudo' then jfsudo-ask is used if a password is required to run the command.
   * If redirStderr is true then stderr will be redir to stdout.
   */
  public String run(String[] cmd, boolean redirStderr) {
    if (cmd[0].equals("sudo")) {
      //if running sudo add -A option to use jfsudo-ask to request password
      String[] newcmd = new String[cmd.length + 1];
      newcmd[0] = "sudo";
      newcmd[1] = "-A";
      System.arraycopy(cmd, 1, newcmd, 2, cmd.length - 1);
      cmd = newcmd;
    }
    ProcessBuilder pb = new ProcessBuilder(cmd);
    if (redirStderr) {
      pb.redirectErrorStream(true);
    }
    if (path != null) {
      pb.directory(path);
    }
    Map<String, String> env = pb.environment();
    if (cmd[0].equals("sudo")) {
      env.put("SUDO_ASKPASS", "/usr/bin/jfsudo-ask");
    }
    for (int a = 0; a < envAdd.size(); a++) {
      Variable v = envAdd.get(a);
      env.put(v.name, v.value);
    }
    for (int a = 0; a < envRemove.size(); a++) {
      Variable v = envRemove.get(a);
      env.remove(v.name);
    }
    command = "";
    for (int a = 0; a < cmd.length; a++) {
      command += cmd[a] + " ";  //test
    }
    output = new StringBuffer();
    active = true;

    if (log) {
      String msg = "Exec :";
      for (int a = 0; a < cmd.length; a++) {
        msg += " ";
        msg += cmd[a];
      }
      JFLog.log(msg);
    }

    try {
      //start the process
      p = pb.start();
      is = p.getInputStream();
      os = p.getOutputStream();
      es = p.getErrorStream();

      //start worker thread
      Worker wi = new Worker(is, true);
      wi.start();

      Worker we = null;
      if (!redirStderr) {
        we = new Worker(es, false);
        we.start();
      }

      //wait for process to terminate
      p.waitFor();

      active = false;

      //wait for worker threads to exit
      wi.join();
      if (!redirStderr) {
        we.join();
      }
    } catch (Exception e) {
      JFLog.log(e);
      errorLevel = -1;
      return null;
    }

    if (log) {
      JFLog.log("Exec:Complete");
    }

    errorLevel = p.exitValue();

    return output.toString();
  }

  /**
   * Terminates the process.
   */
  public void destroy() {
    if (p != null) {
      p.destroy();
    }
  }

  /**
   * Forcibly Terminates the process.
   */
  public void destroyForcibly() {
    if (p != null) {
      p.destroyForcibly();
    }
  }

  private void _destroy() {
    destroy();
  }

  /**
   * Returns error level from last process run.
   */
  public int getErrorLevel() {
    return errorLevel;
  }

  private class Worker extends Thread {

    private InputStream wis;
    private boolean processScript;

    public Worker(InputStream wis, boolean processScript) {
      this.processScript = processScript;
      this.wis = wis;
    }

    public void run() {
      byte buf[] = new byte[1024];
      int read;
      String str;
      String prompt;
      int in, ir;
      Response res;
      int pidx = 0;
      try {
        while ((active) || (wis.available() > 0)) {  //InputStream may have more data after process is inactive!!!
          read = wis.read(buf);
          if (read == -1) {
            break;
          }
          if (read == 0) {
            continue;
          }
          str = new String(buf, 0, read);
          if (log) {
            JFLog.log(str);
          }
          if (listener != null) {
            try {
              listener.shellProcessOutput(str);
            } catch (Exception e1) {
            }
          }
          if (!keepOutput) {
            continue;
          }
          if (!processScript) {
            continue;
          }
          output.append(str);
          //check for possible response
          prompt = output.substring(pidx).trim();
          if (prompt.length() == 0) {
            continue;
          }
          in = prompt.lastIndexOf("\n");
          ir = prompt.lastIndexOf("\r");
          if ((in != -1) && (in > ir)) {
            prompt = prompt.substring(in + 1).trim();
          }
          if ((ir != -1) && (ir > in)) {
            prompt = prompt.substring(ir + 1).trim();
          }
          if (logPrompt) {
            JFLog.log("prompt=" + prompt);
          }
          for (int a = 0; a < script.size(); a++) {
            res = script.get(a);
            if (res.used) {
              continue;
            }
            boolean matches = false;
            if (res.regex) {
              matches = prompt.matches(res.prompt);
            } else {
              matches = prompt.equals(res.prompt);
            }
            if (matches) {
              pidx = output.length();  //avoid reusing a part of this prompt
              if (res.term) {
                _destroy();
                return;
              }
              if (log) {
                JFLog.log(res.reply);
              }
              os.write(res.reply.getBytes());
              os.flush();
              if (!res.repeat) {
                res.used = true;
              }
              break;
            }
          }
        }
      } catch (Exception e) {
//        if (log) JFLog.log(e);
      }
      if (log) {
        JFLog.log("ShellProcess:Worker thread exiting");
      }
    }
  }

  public OutputStream getOutputStream() {
    return os;
  }

  public boolean isAlive() {
    if (p == null) return false;
    return p.isAlive();
  }

  public Process getProcess() {
    return p;
  }
}
