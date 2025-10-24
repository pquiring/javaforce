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
}
