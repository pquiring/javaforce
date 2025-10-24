package javaforce.access;

/** List of Users.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class Users implements Serializable {
  private static final long serialVersionUID = 1L;

  public ArrayList<User> users = new ArrayList<>();

  public User[] getUsers() {
    return users.toArray(User.ARRAY_TYPE);
  }

  public boolean exists(String name) {
    for(User user : users) {
      if (user.name.equals(name)) return true;
    }
    return false;
  }

  public User getUser(String name) {
    for(User user : users) {
      if (user.name.equals(name)) return user;
    }
    return null;
  }

  public boolean addUser(User user) {
    if (exists(user.name)) return false;
    users.add(user);
    return true;
  }

  public boolean removeUser(String name) {
    if (name.equals("admin")) return false;
    for(User user : users) {
      if (user.name.equals(name)) {
        users.remove(user);
        return true;
      }
    }
    return false;
  }
}
