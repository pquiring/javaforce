package javaforce.access;

/** User account.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class User implements Name, Serializable {
  private static final long serialVersionUID = 1L;
  public static User[] ARRAY_TYPE = new User[0];

  public String name;
  public String full;
  public String desc;
  public String pass;
  public ArrayList<Contact> contacts = new ArrayList<>();
  public int pass_hash_type;

  public static final int PASSWORD_HASH_NONE = 0;
  public static final int PASSWORD_HASH_MD5 = 1;

  public User(String name, String pass, String desc) {
    this.name = name;
    this.full = "";
    this.desc = desc;
    this.pass_hash_type = PASSWORD_HASH_MD5;
    this.pass = md5(pass);
  }

  public User(String name, String pass, String desc, int password_hash_type) {
    this.name = name;
    this.full = "";
    this.desc = desc;
    this.pass_hash_type = password_hash_type;
    setPassword(pass);
  }

  /** Encode a password using md5. */
  public static String md5(String pass) {
    MD5 md5 = new MD5();
    md5.add(pass);
    return new String(md5.done());
  }

  public boolean checkPassword(String pass) {
    switch (pass_hash_type) {
      case PASSWORD_HASH_NONE: break;
      case PASSWORD_HASH_MD5: pass = md5(pass); break;
      default: return false;
    }
    return pass.equals(this.pass);
  }

  public String getName() {
    return name;
  }

  public void setPassword(String pass) {
    switch (pass_hash_type) {
      case PASSWORD_HASH_NONE: break;
      case PASSWORD_HASH_MD5: pass = md5(pass); break;
    }
    this.pass = pass;
  }

  public void addContact(Contact contact) {
    //TODO : check existing
    contacts.add(contact);
  }
}
