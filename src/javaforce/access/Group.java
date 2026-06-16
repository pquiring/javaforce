package javaforce.access;

/** User Group
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Group implements Name, Serializable {
  private static final long serialVersionUID = 1L;
  public static Group[] ARRAY_TYPE = new Group[0];

  public String name;
  public String desc;
  public ArrayList<String> users = new ArrayList<>();
  public HashMap<String, String> props = new HashMap<>();

  public Group() {
  }

  public Group(String name, String desc) {
    this.name = name;
    this.desc = desc;
  }

  public Group(String name, String desc, String[] user_list) {
    this.name = name;
    this.desc = desc;
    for(String user : user_list) {
      users.add(user);
    }
  }

  public void validate() {
    if (users == null) {
      users = new ArrayList<>();
    }
    if (props == null) {
      props = new HashMap<>();
    }
  }

  public String getName() {
    return name;
  }

  public String[] getUsers() {
    return users.toArray(JF.StringArrayType);
  }

  public void addUser(String user) {
    if (users.contains(user)) return;
    users.add(user);
  }

  public void removeUser(String user) {
    users.remove(user);
  }

  public boolean contains(String user) {
    return users.contains(user);
  }

  public String getProperty(String name) {
    return props.get(name);
  }

  public void setProperty(String name, String value) {
    props.put(name, value);
  }
}
