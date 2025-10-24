package javaforce.access;

/** Access Control.
 *
 * Maintains a list of users and groups.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class AccessControl {

  private String cfg_folder;
  private File users_file;
  private File groups_file;
  public Object lock = new Object();

  private Users users;
  private Groups groups;

  /** Set folder where users and groups are stored.
   * Folder should be in a secure location since it will store hashed passwords.
   *
   * @param folder = folder to store lists
   * @return if users/groups exists (otherwise defaults were created)
   */
  public boolean setConfigFolder(String folder) {
    this.cfg_folder = folder;
    users_file = new File(folder + "/users.dat");
    groups_file = new File(folder + "/groups.dat");
    boolean exists = true;
    synchronized (lock) {
      if (!loadUsers()) exists = false;
      if (!loadGroups()) exists = false;
    }
    return exists;
  }

  private boolean loadUsers() {
    if (users_file.exists()) {
      users = (Users)Compression.deserialize(users_file);
      return true;
    } else {
      //create defaults
      users = new Users();
      users.addUser(new User("admin", "admin", "Built-in admin account"));
      saveUsers();
      return false;
    }
  }

  public void saveUsers() {
    synchronized (lock) {
      Compression.serialize(users_file, users);
    }
  }

  private boolean loadGroups() {
    if (groups_file.exists()) {
      groups = (Groups)Compression.deserialize(groups_file);
      return true;
    } else {
      //create defaults
      groups = new Groups();
      groups.addGroup(new Group("administrators", "Built-in administrators group", new String[] {"admin"}));
      saveGroups();
      return false;
    }
  }

  public void saveGroups() {
    synchronized (lock) {
      Compression.serialize(groups_file, groups);
    }
  }

  public void saveAll() {
    synchronized (lock) {
      saveUsers();
      saveGroups();
    }
  }

  /** Get folder where user/group settings are stored. */
  public String getConfigFolder() {
    return cfg_folder;
  }

  /** Returns list of groups user is a member of. */
  public String[] getGroups(String user) {
    if (cfg_folder == null) return new String[0];
    ArrayList<String> list = new ArrayList<>();
    for(Group group : groups.groups) {
      if (group.contains(user)) {
        list.add(group.getName());
      }
    }
    return list.toArray(JF.StringArrayType);
  }

  public Users getUsersList() {
    return users;
  }

  public User[] getUsers() {
    synchronized (lock) {
      return users.users.toArray(User.ARRAY_TYPE);
    }
  }

  public User getUser(String name) {
    synchronized (lock) {
      for(User user : getUsers()) {
        if (user.name.equals(name)) {
          return user;
        }
      }
    }
    JFLog.log("AccessControl:user not found:" + name);
    return null;
  }

  public Group getGroup(String name) {
    synchronized (lock) {
      for(Group group : getGroups()) {
        if (group.name.equals(name)) {
          return group;
        }
      }
    }
    return null;
  }

  public Group[] getGroups() {
    synchronized (lock) {
      return groups.groups.toArray(Group.ARRAY_TYPE);
    }
  }

  public Groups getGroupsList() {
    return groups;
  }

  public void addUser(User user) {
    synchronized (lock) {
      users.addUser(user);
      saveUsers();
    }
  }

  public void removeUser(String user) {
    if (user.equals("admin")) return;
    synchronized (lock) {
      users.removeUser(user);
      saveUsers();
    }
  }

  public void addGroup(String name, String desc) {
    synchronized (lock) {
      groups.addGroup(new Group(name, desc));
      saveGroups();
    }
  }

  public void removeGroup(String name) {
    if (name.equals("administrators")) return;
    synchronized (lock) {
      groups.removeGroup(name);
      saveGroups();
    }
  }

  public void addGroupMember(String name, String user) {
    synchronized (lock) {
      Group group = getGroup(name);
      if (group == null) return;
      group.addUser(user);
      saveGroups();
    }
  }

  public void setUserPassword(String name, String pass) {
    synchronized (lock) {
      User user = getUser(name);
      if (user == null) return;
      user.setPassword(pass);
      saveUsers();
    }
  }

  public void updateUser(String name, String desc) {
    synchronized (lock) {
      User user = getUser(name);
      if (user == null) return;
      user.desc = desc;
      saveUsers();
    }
  }

  public void updateGroup(String name, String desc) {
    synchronized (lock) {
      Group group = getGroup(name);
      if (group == null) return;
      group.desc = desc;
      saveGroups();
    }
  }

  /** Validates login attempt. */
  public boolean login(String user, String pass) {
    if (cfg_folder == null) {
      JFLog.log("AccessControl.login:cfg_folder == null");
      return false;
    }
    User acct = getUser(user);
    if (acct == null) return false;
    return acct.checkPassword(pass);
  }
}
