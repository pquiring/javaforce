package javaforce.voip;

public class Codec {

  public final String name;  //"PCMU", "PCMA", "G729" , "JPEG", "H263", "H264", "telephone-event"
  public final int id;       // 0       8       18       26      34      dyn     dyn
  public final int rate;     //8000, 16000, 32000 (-1 = variable/unknown)

  public Codec() {
    name = "";
    id = -1;
    rate = -1;
  }

  public Codec(String name) {
    this.name = name;
    id = -1;
    rate = -1;
  }

  public Codec(String name, int id) {
    this.name = name;
    this.id = id;
    this.rate = -1;
  }

  public Codec(String name, int id, int rate) {
    this.id = id;
    this.rate = rate;
    switch (rate) {
      default:
      case 8000:
        this.name = name;
        break;
      case 16000:
        this.name = name + "16";
        break;
      case 32000:
        this.name = name + "32";
        break;
    }
  }

  public boolean equals(Codec other) {
    return name.equals(other.name) && (rate == -1 || other.rate == -1 || rate == other.rate);
  }

  public String toString() {
    return "{" + name + ":" + id + "}";
  }
}
