package javaforce.linux;

/**
 * Created : May 27, 2012
 *
 * @author pquiring
 */
import java.util.*;

import javaforce.*;

/**
 * Retrieves PulseAudio configuration
 */
public class PulseAudio {

  public static class Card {

    public String name;
    public int idx;
    public int card;
    public String activeProfile = "";
    public ArrayList<Profile> profiles = new ArrayList<Profile>();
    public ArrayList<Port> ports = new ArrayList<Port>();  //this if info only, can't use it
  }

  public static class Sink {

    public String name;
    public int idx;
    public int card;
    public int volume;  //%
    public boolean muted;
    public String activePort = "";
    public ArrayList<Port> ports = new ArrayList<Port>();
  }

  public static class Source {

    public String name;
    public int idx;
    public int card;
    public int volume;  //%
    public boolean muted;
    public String activePort = "";
    public ArrayList<Port> ports = new ArrayList<Port>();
  }

  public static class Port {

    public String name, desc;
  }

  public static class Profile {

    public String name, desc;
  }
  public static ArrayList<Card> cards;
  public static ArrayList<Sink> sinks;
  public static ArrayList<Source> sources;

  public static void list() {
    cards = new ArrayList<Card>();
    sinks = new ArrayList<Sink>();
    sources = new ArrayList<Source>();
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[]{"pactl", "list"}, false);
    String[] lns = output.split("\n");
    Card card = null;
    Sink sink = null;
    Source source = null;
    boolean ports = false, profiles = false;
    for (int a = 0; a < lns.length; a++) {
      if (lns[a].length() == 0) {
        continue;
      }
      if (lns[a].charAt(0) == '\t') {
        if (lns[a].startsWith("\tDescription:")) {
          if (sink != null) {
            sink.name = lns[a].substring(14);
          }
          if (source != null) {
            source.name = lns[a].substring(14);
          }
          continue;
        }
        if (lns[a].startsWith("\t\talsa.card = ")) {
          int i1 = lns[a].indexOf("\"");
          int i2 = lns[a].indexOf("\"", i1 + 1);
          int idx = JF.atoi(lns[a].substring(i1 + 1, i2));
          if (sink != null) {
            sink.card = idx;
          }
          if (source != null) {
            source.card = idx;
          }
          if (card != null) {
            card.card = idx;
          }
          continue;
        }
        if (lns[a].startsWith("\t\talsa.card_name = ")) {
          int i1 = lns[a].indexOf("\"");
          int i2 = lns[a].indexOf("\"", i1 + 1);
          if (card != null) {
            card.name = lns[a].substring(i1 + 1, i2);
          }
          continue;
        }
        if (lns[a].startsWith("\tActive Port:")) {
          if (sink != null) {
            sink.activePort = lns[a].substring(14);
          }
          if (source != null) {
            source.activePort = lns[a].substring(14);
          }
          continue;
        }
        if (lns[a].startsWith("\tActive Profile:")) {
          profiles = false;
          if (card != null) {
            card.activeProfile = lns[a].substring(17);
          }
          continue;
        }
        if (lns[a].startsWith("\tVolume:")) {
          String[] f = lns[a].substring(9).split(" +");
          int left = JF.atoi(f[1].substring(0, f[1].length() - 1));
          int right = -1;
          if (f.length > 3) {
            right = JF.atoi(f[3].substring(0, f[3].length() - 1));
          }
          if (sink != null) {
            sink.volume = left;
          }
          if (source != null) {
            source.volume = left;
          }
          continue;
        }
        if (lns[a].startsWith("\tMute:")) {
          boolean muted = lns[a].indexOf("no") == -1;
          if (sink != null) {
            sink.muted = muted;
          }
          if (source != null) {
            source.muted = muted;
          }
        }
        if (lns[a].equals("\tPorts:")) {
          ports = true;
          continue;
        }
        if (lns[a].equals("\tProfiles:")) {
          profiles = true;
          continue;
        }
        if (ports) {
          if (lns[a].startsWith("\t\t\t")) {
            continue;
          }
          int i1 = lns[a].indexOf(":");
          if (i1 == -1) {
            continue;
          }
          int i2 = lns[a].indexOf("(priority", i1);
          if (i2 == -1) {
            continue;
          }
          Port port = new Port();
          port.name = lns[a].substring(2, i1);
          port.desc = lns[a].substring(i1 + 1, i2).trim();
          if (card != null) {
            card.ports.add(port);
          }
          if (sink != null) {
            sink.ports.add(port);
          }
          if (source != null) {
            source.ports.add(port);
          }
          continue;
        }
        if (profiles) {
          Profile profile = new Profile();
          int i1 = lns[a].indexOf(" ");
          int i2 = lns[a].indexOf("(sinks:");
          profile.name = lns[a].substring(2, i1 - 1);
          profile.desc = lns[a].substring(i1 + 1, i2);
          card.profiles.add(profile);
          continue;
        }
      } else {
        sink = null;
        source = null;
        card = null;
        ports = false;
        profiles = false;
        int idx = lns[a].indexOf("#");
        if (idx == -1) {
          continue;
        }
        idx = JF.atoi(lns[a].substring(idx + 1));
        if (lns[a].startsWith("Sink #")) {
          sink = new Sink();
          sink.idx = idx;
          sinks.add(sink);
        }
        if (lns[a].startsWith("Source #")) {
          source = new Source();
          source.idx = idx;
          sources.add(source);
        }
        if (lns[a].startsWith("Card #")) {
          card = new Card();
          card.name = "?";
          card.idx = idx;
          cards.add(card);
        }
      }
    }
  }
}
