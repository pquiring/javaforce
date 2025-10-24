package javaforce.access;

/** List of Groups.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class Groups implements Serializable {
  private static final long serialVersionUID = 1L;

  public ArrayList<Group> groups = new ArrayList<>();

  public Group[] getGroups() {
    return groups.toArray(Group.ARRAY_TYPE);
  }

  public boolean exists(String name) {
    for(Group group : groups) {
      if (group.name.equals(name)) return true;
    }
    return false;
  }

  public Group getGroup(String name) {
    for(Group group : groups) {
      if (group.name.equals(name)) {
        return group;
      }
    }
    return null;
  }

  public void addGroup(String name, String[] users) {
    if (exists(name)) return;
    Group group = new Group();
    group.name = name;
    for(String user : users) {
      group.addUser(user);
    }
  }

  public void addGroup(Group group) {
    if (exists(group.name)) return;
    groups.add(group);
  }

  public boolean removeGroup(String name) {
    if (name.equals("administrators")) return false;
    for(Group group : groups) {
      if (group.name.equals(name)) {
        groups.remove(group);
        return true;
      }
    }
    return false;
  }
}
