package javaforce.voip;

public class Codec {

  public final String name;  //"PCMU", "PCMA", "G729" , "JPEG", "H263", "H264", "telephone-event"
  public final int id;       // 0       8       18       26      34      dyn     dyn

  public Codec() {
    name = "";
    id = -1;
  }

  public Codec(String name, int id) {
    this.name = name;
    this.id = id;
  }

  public boolean equals(Codec other) {
    return name.equals(other.name);
  }

  public String toString() {
    return "{" + name + ":" + id + "}";
  }
}
