package javaforce.webui;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

import javaforce.*;

/**
 * XML is a TreeModel data model that encapsules a complete XML file.<br> Each
 * XML tag (element) is treated as a node in the tree. Once read() it can be
 * viewed and edited with a JTree. Then you can write() it back to a file. XML
 * will monitor changes made and update nodes as needed. The read() functions
 * include a callback interface so you can further tweak the layout of the XML
 * tree.<br> Typical XML Tag:<br> &lt;name [attributes...]&gt; content |
 * children &lt;/name&gt;<br> Singleton XML Tag: (no children)<br> &lt;name
 * [attributes...] /&gt;<br> Caveats:<br> Only leaf nodes can contain actual
 * data (content) (in other words @XmlMixed is not supported).<br>
 * Mixed tags are read, but when written the
 * content is lost.<br> There must be only one root tag.<br> Support for the
 * standard XML header is provided (see header).<br> readClass() and
 * writeClass() support : int, short, byte, float, double, boolean, String,
 * Color and Custom Classes.<br> Arrays of any of these.<br> All classes and
 * fields MUST be public. static and transient members are skipped. No special
 * annotations are required.
 */

public class XMLTree implements TreeModelListener {
  private TreeModel treemodel;
  private boolean useContentForName = false;
  private boolean useNameAttributeForName = false;
  private boolean fireEvents = true;
  private boolean ignoreEvents = false;

  private class XMLTagPart {
    private String content;
    private String attrs;

    public XMLTagPart() {
      content = "";
      attrs = "";
    }
  }

  /**
   * XMLEvent is an interface for a callback handler used during XML loading.
   */
  public interface XMLEvent {
    public void XMLTagAdded(XMLTag tag);
    public void XMLTagRenamed(XMLTag tag);
  };

  /**
   * XMLAttr is one attribute that is listed in each XML tag.
   */
  public class XMLAttr {
    public String name, value;

    public XMLAttr() {
      name = "";
      value = "";
    }
  };

  /*
   * XMLTag is one node in the tree that represents one XML element or 'tag'.
   *
   * name = the XML tag name
   * attrs = an ArrayList of XMLAttr
   * content = the data within the tags head/tail
   * isLeaf = set to force JTree to view node as a leaf
   * isNotLeaf = set to force JTree to view node that is expandable (even if it has no children)
   * isReadOnly = ignores edits from JTree
   */
  public class XMLTag extends TreeNode {
    public String name = "";
    public ArrayList<XMLAttr> attrs;
    public String content = "";
    /**
     * Tag is a singleton
     */
    public boolean isSingle = false;
    public boolean isNotLeaf = false;
    public boolean isLeaf = false;
    public boolean isReadOnly = false;
    public boolean useContentForName = false;

    /**
     * Constructs a new XMLTag
     */
    public XMLTag() {
      attrs = new ArrayList<XMLAttr>();
    }

    /**
     * Returns the unique name of the tag.
     */
    public String toString() {
      return getName();
    }

    /**
     * Returns the parent of the tag. This method just overrides the default
     * method but returns XMLTag.
     */
    public XMLTag getParent() {
      if (this == treemodel.getRoot()) {
        return null;  //in case setRoot() moved the root up
      }
      return (XMLTag) super.getParent();
    }

    /**
     * Returns true if the node is a leaf. This method just overrides the
     * default method and allows better leaf control.
     */
    public boolean isLeaf() {
      if (isNotLeaf) {
        return false;
      }
      if (isLeaf) {
        return true;
      }
      return (getChildCount() == 0);
    }

    /**
     * Returns a unique name for this node.
     */
    public String getName() {
      if (useContentForName) {
        return content;
      }
      if (useNameAttributeForName) {
        String argName = getArg("name");
        if (argName != null) {
          return argName;
        }
      }
      return name;
    }

    /**
     * Sets the name for this node.
     */
    public void setName(String newName) {
      //check if name="..." is in attrs, else use name
      XMLAttr attr;
      boolean ok = false;
      for (Iterator i = attrs.iterator(); i.hasNext();) {
        attr = (XMLAttr) i.next();
        if (attr.name.equals("name")) {
          attr.value = newName;
          ok = true;
          break;
        }
      }
      if (!ok) {
        name = newName;
      }
      changedTag(this);
    }

    /**
     * Returns the child tag at index. This method just overrides the default
     * method but returns XMLTag.
     */
    public XMLTag getChildAt(int index) {
      return (XMLTag) super.getChildAt(index);
    }

    /**
     * Returns value of argument.
     */
    public String getArg(String name) {
      for (int a = 0; a < attrs.size(); a++) {
        if (attrs.get(a).name.equals(name)) {
          return attrs.get(a).value;
        }
      }
      return null;
    }

    /**
     * Sets argument to value.
     */
    public void setArg(String name, String value) {
      for (int a = 0; a < attrs.size(); a++) {
        if (attrs.get(a).name.equals(name)) {
          attrs.get(a).value = value;
          return;
        }
      }
      XMLAttr attr = new XMLAttr();
      attr.name = name;
      attr.value = value;
      attrs.add(attr);
    }

    /**
     * Returns the content of this node.
     */
    public String getContent() {
      return content;
    }

    /**
     * Sets content.
     */
    public void setContent(String newContent) {
      content = newContent;
    }
  };
  /**
   * The XML header tag.
   */
  public XMLTag header = new XMLTag();
  /**
   * The root tag.
   */
  public XMLTag root = new XMLTag();

  /**
   * Constructs a new XML object.
   */
  public XMLTree() {
    treemodel = new TreeModel(root);
    treemodel.addTreeModelListener(this);
    treemodel.setRoot(root);
  }

  /**
   * Returns the TreeModel that can be passed to WebUI Tree constructor.
   */
  public TreeModel getTreeModel() {
    return treemodel;
  }

  private final int XML_OPEN = 1;
  private final int XML_DATA = 2;
  private final int XML_CLOSE = 3;
  private final int XML_SINGLE = 4;
  private int type;
  private int line, pos;
  private byte[] buf;

  private XMLTagPart readtag() {
    boolean quote = false, isAttrs = false;
    char ch;
    XMLTagPart tag = new XMLTagPart();

    type = XML_DATA;
    while (pos < buf.length) {
      ch = (char) buf[pos++];
      if (ch == '\n') line++;
      switch (type) {
        case XML_OPEN:
        case XML_CLOSE:
        case XML_SINGLE:
          if (ch == '\"') {
            quote = !quote;
          }
          if (!quote) {
            if (ch == '/') {
              if (tag.content.length() == 0) {
                type = XML_CLOSE;
              } else {
                type = XML_SINGLE;
              }
              continue;
            }
            if (ch == '>') {
              break;
            }
          }
          if ((!isAttrs) && (Character.isWhitespace(ch))) {
            isAttrs = true;
          }
          if (isAttrs) {
            tag.attrs += ch;
          } else {
            tag.content += ch;
          }
          continue;
        case XML_DATA:
          if ((ch == '<') && (!quote)) {
            if (tag.content.length() > 0) {
              pos--;
              break;
            }
            type = XML_OPEN;
            continue;
          }
          if (ch == '\"') {
            if (quote) {
              quote = false;
            } else {
              quote = true;
            }
          }
          tag.content += ch;
          continue;
      }
      break;
    }
    if (tag.content.length() == 0) {
      return null;  //EOF
    }
    if (type == XML_DATA) {
      tag.content = decodeSafe(tag.content);
    }
    return tag;
  }

  private void string2attrs(XMLTag tag, String attrs) {
    //search for name="value"
    char[] ca = attrs.toCharArray();
    XMLAttr attr;
    String name, value;
    int length = attrs.length();
    int ep;
    tag.attrs.clear();
    for (int a = 0; a < length; a++) {
      if (Character.isWhitespace(ca[a])) {
        continue;  //skip spaces
      }
      ep = attrs.indexOf('=', a);
      if (ep == -1) {
        return;
      }
      name = "";
      for (int b = a; b < ep; b++) {
        name += ca[b];
      }
      a = ep + 1;
      value = "";
      if (ca[a] == '\"') {
        a++;
        ep = attrs.indexOf('\"', a);
        if (ep == -1) {
          return;
        }
        for (int b = a; b < ep; b++) {
          value += ca[b];
        }
        a = ep + 1;
      } else if (ca[a] == '\'') {
        a++;
        ep = attrs.indexOf('\'', a);
        if (ep == -1) {
          return;
        }
        for (int b = a; b < ep; b++) {
          value += ca[b];
        }
        a = ep + 1;
      } else {
        ep = a;
        while (ep < length && !Character.isWhitespace(ca[ep])) {
          ep++;
        }
        if (ep == length) {
          ep = length - 1;
        }
        if (ep <= a) {
          return;
        }
        for (int b = a; b < ep; b++) {
          value += ca[b];
        }
        a = ep + 1;
      }
      attr = new XMLAttr();
      attr.name = name;
      attr.value = value;
      tag.attrs.add(attr);
    }
  }

  /**
   * Reads the entire tree from a XML file from filename. No call handler is
   * used.
   *
   * @param filename name of file to load XML data from
   */
  public boolean read(String filename) {
    return read(filename, null);
  }

  /**
   * Reads the entire tree from a XML file from filename.
   *
   * @param filename name of file to load XML data from
   * @param event callback handler to process each loaded XML tag
   */
  public boolean read(String filename, XMLEvent event) {
    FileInputStream fis;
    boolean ret;
    try {
      fis = new FileInputStream(filename);
      ret = read(fis, event);
      fis.close();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return ret;
  }

  /**
   * Reads the entire tree from a XML file from the InputStream. No callback
   * handler is used.
   *
   * @param in InputStream to load XML data from
   */
  public boolean read(InputStream in) {
    return read(in, null);
  }

  /**
   * Reads the entire tree from a XML file from the InputStream.
   *
   * @param is InputStream to load XML data from
   * @param event callback handler to process each loaded XML tag
   */
  public boolean read(InputStream is, XMLEvent event) {
    this.event = event;
    try {
      buf = JF.readAll(is);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    deleteAll();
    type = XML_DATA;
    XMLTagPart tagpart;
    XMLTag tag = null, newtag;
    boolean bRoot = false;
    boolean bHeader = false;
    line = 1;
    while (true) {
      tagpart = readtag();
      if (tagpart == null) {
        break;
      }
      switch (type) {
        case XML_OPEN:
          if (tagpart.content.startsWith("?xml")) {
            if (bHeader) {
              JFLog.log("XML:Multiple headers found");
              return false;
            }  //already read the XML header
            header.name = tagpart.content;
            string2attrs(header, tagpart.attrs);
            if (event != null) {
              event.XMLTagAdded(header);
            }
            break;
          }
          if (tagpart.content.startsWith("!--")) {
            //ignore comments
            break;
          }
        //no break
        case XML_SINGLE:
          if (tag == null) {
            //root tag
            if (bRoot) {
              JFLog.log("XML:Multiple root tags found");
              return false;
            }  //already found a root tag
            bRoot = true;
            root.name = tagpart.content;
            string2attrs(root, tagpart.attrs);
            if (event != null) {
              event.XMLTagAdded(root);
            }
            changedTag(root);
            tag = root;
          } else {
            newtag = new XMLTag();
            newtag.name = tagpart.content;
            string2attrs(newtag, tagpart.attrs);
            addTag(tag, newtag);
            if (type == XML_SINGLE) {
              newtag.isSingle = true;
            } else {
              tag = newtag;
            }
          }
          break;
        case XML_CLOSE:
          if (tag == null) {
            JFLog.log("XML:tag closed but never opened:line=" + line);
            return false;
          }  //bad xml file
          if (!tagpart.content.equalsIgnoreCase(tag.name)) {
            JFLog.log("XML:tag closed doesn't match open:tag.open=" + tag.name + ":tag.close=" + tagpart.content + ":line=" + line );
            return false;
          }  //unmatched closing tag
          tag = (XMLTag) tag.getParent();
          break;
        case XML_DATA:
          if (tag == null) {
            continue;  //could happen after header and before root tag
          }
          tag.content += tagpart.content;
          break;
      }
    }
    buf = null;
    if (tag != null) {
      JFLog.log("XML:tag left open");
      return false;
    }  //tag left open
    return true;
  }

  private String attrs2string(XMLTag tag) {
    XMLAttr attr;
    int size = tag.attrs.size();
    String str = "", tmp;
    for (int a = 0; a < size; a++) {
      attr = tag.attrs.get(a);
      tmp = " " + attr.name + "=\"" + attr.value + "\"";
      str += tmp;
    }
    return str;
  }

  private void writestr(OutputStream out, String str) {
    try {
      out.write(str.getBytes("UTF-8"));
    } catch (Exception e) {
    }
  }
  private int indent;

  private void writetag(OutputStream out, XMLTag tag) {
    String tmp;
    tmp = "";
    for (int a = 0; a < indent; a++) {
      tmp += ' ';
    }
    writestr(out, tmp);
    int size = tag.getChildCount();
    String attrs;
    if (size > 0) {
      //write open tag w/ attrs + content
      attrs = attrs2string(tag);
      tmp = "<" + tag.name + attrs + ">\n";
      writestr(out, tmp);
      indent += 2;
      //write children
      for (int a = 0; a < size; a++) {
        writetag(out, (XMLTag) tag.getChildAt(a));
      }
      //write close tag
      indent -= 2;
      tmp = "";
      for (int a = 0; a < indent; a++) {
        tmp += ' ';
      }
      writestr(out, tmp);
      tmp = "</" + tag.name + ">\n";
      writestr(out, tmp);
    } else {
      attrs = attrs2string(tag);
      if (tag.isSingle) {
        tmp = "<" + tag.name + attrs + "/>\n";
      } else {
        tmp = "<" + tag.name + attrs + ">" + encodeSafe(tag.content) + "</" + tag.name + ">\n";
      }
      writestr(out, tmp);
    }
  }

  /**
   * Writes the entire tree as a XML file to the filename.
   */
  public boolean write(String filename) {
    FileOutputStream fos;
    boolean ret;
    try {
      fos = new FileOutputStream(filename);
      ret = write(fos);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return ret;
  }

  /**
   * Writes the entire tree as a XML file to the OutputStream.
   */
  public boolean write(OutputStream os) {
    BufferedOutputStream bos = new BufferedOutputStream(os);
    String tmp, attrs;
    if (root.name.length() == 0) {
      return false;
    }
    if (header.name.length() > 0) {
      attrs = attrs2string(header);
      tmp = "<" + header.name + attrs + ">\n";
      writestr(bos, tmp);
    }
    //write root header
    attrs = attrs2string(root);
    tmp = "<" + root.name + attrs + ">\n";
    writestr(bos, tmp);
    int size = root.getChildCount();
    indent = 2;
    for (int a = 0; a < size; a++) {
      writetag(bos, (XMLTag) root.getChildAt(a));
    }
    //write root tail
    tmp = "</" + root.name + ">\n";
    writestr(bos, tmp);
    try {
      bos.flush();
    } catch (Exception e) {
    }  //must flush or it's lost
    return true;
  }

  private void clearTag(XMLTag tag) {
    tag.name = "";
    tag.attrs = new ArrayList<XMLAttr>();
    tag.content = "";
  }

  /**
   * Deletes the entire tree and resets the root and header tags.
   */
  public void deleteAll() {
    deleteTag(root);
    clearTag(header);
    changedTag(header);
    clearTag(root);
    changedTag(root);
  }

  /**
   * Deletes a node from the parent. Also deletes all it's children.
   */
  public boolean deleteTag(XMLTag tag) {
    //remove children first
    while (tag.getChildCount() > 0) {
      deleteTag((XMLTag) tag.getChildAt(0));
    }
    //now remove itself from parent
    if (tag.getParent() == null) {
      return true;  //can not delete root/header tag itself
    }
    treemodel.removeNodeFromParent(tag);
    return true;
  }
  private XMLEvent event = null;

  /**
   * Creates an empty node that can be inserted into the tree using addTag().
   */
  public XMLTag createTag() {
    return new XMLTag();  //must call addTag() to add to the tree
  }

  /**
   * Adds the node to the targetParent.
   */
  public XMLTag addTag(XMLTag targetParent, XMLTag tag) {
    treemodel.insertNodeInto(tag, targetParent, targetParent.getChildCount());
    changedTag(tag);
    if (event != null) {
      event.XMLTagAdded(tag);
    }
    return tag;
  }

  /**
   * Adds node with the name, attrs and content specified. If another node
   * already exists with the same name the new node's unique name will differ.
   */
  public XMLTag addTag(XMLTag targetParent, String name, String attrs, String content) {
    XMLTag newtag = new XMLTag();
    newtag.name = name;
    newtag.content = content;
    string2attrs(newtag, attrs);
    return addTag(targetParent, newtag);
  }

  /**
   * Adds (a non-existing) or sets (an existing) node with the name, attrs and
   * content specified.
   */
  public XMLTag addSetTag(XMLTag targetParent, String name, String attrs, String content) {
    XMLTag child;
    int len = targetParent.getChildCount();
    for (int a = 0; a < len; a++) {
      child = targetParent.getChildAt(a);
      if (child.name.equals(name)) {
        setTag(child, name, attrs, content);
        return child;
      }
    }
    return addTag(targetParent, name, attrs, content);
  }

  /**
   * Notify the treemodel that you changed a node.
   */
  public void changedTag(XMLTag tag) {
    if (!fireEvents) return;
    ignoreEvents = true;
    treemodel.nodeChanged(tag);
    ignoreEvents = false;
  }

  /**
   * Sets the name, attrs and contents of the true root node.
   */
  public void setRoot(String name, String attrs, String content) {
    root.name = name;
    string2attrs(root, attrs);
    root.content = content;
    changedTag(root);
  }

  /**
   * Returns the unique name of a node.
   */
  public String getName(XMLTag tag) {
    return tag.getName();
  }

  /**
   * Sets the name of a node. It's unique name may differ when shown in a tree.
   */
  public void setName(XMLTag tag, String newName) {
    tag.setName(newName);
  }

  /**
   * Returns a node based on the TreePath path;
   */
  public XMLTag getTag(TreePath path) {
    return getTag(path.getPath());
  }

  /**
   * Returns a node based on the objs[] path. Relative to virtual root tag. (see
   * setRoot())
   */
  public XMLTag getTag(Object[] objs) {
    XMLTag tag = (XMLTag) treemodel.getRoot(), child;
    String name;
    if (objs == null || objs.length == 0) {
      return null;
    }
    name = tag.getName();
    if (!name.equals(objs[0].toString())) {
      return null;
    }
    int idx = 1;
    boolean ok;
    int cnt;
    while (idx < objs.length) {
      ok = false;
      cnt = tag.getChildCount();
      for (int i = 0; i < cnt; i++) {
        child = (XMLTag) tag.getChildAt(i);
        name = child.getName();
        if (name.equals(objs[idx].toString())) {
          ok = true;
          idx++;
          tag = child;
          break;
        }
      }
      if (!ok) {
        return null;  //next path element not found
      }
    }
    return tag;
  }

  /**
   * Sets the name, attrs and content for an existing node.
   */
  public void setTag(XMLTag tag, String name, String attrs, String content) {
    tag.name = name;
    string2attrs(tag, attrs);
    tag.content = content;
    changedTag(tag);
    if (event != null) {
      event.XMLTagAdded(tag);
    }
  }

  /**
   * Sets the root node for the tree. This doesn't effect the true root node.
   * This is usefull in hiding parts of a XML file from view when viewed in a
   * JTree.
   */
  public void setRoot(XMLTag newRoot) {
    treemodel.setRoot(newRoot);
  }

  /**
   * Forces getName() to return the tags content instead of the actual name.
   *
   * @param state
   */
  public void setUseContentForName(boolean state) {
    useContentForName = state;
  }

  /**
   * Forces getName() to return the tags content instead of the actual name.
   */
  public boolean getUseContentForName() {
    return useContentForName;
  }

  /**
   * Forces getName() to return the tags attribute 'name' (if available)
   *
   * @param state
   */
  public void setUseAttributeNameForName(boolean state) {
    useNameAttributeForName = state;
  }

  /**
   * Forces getName() to return the tags content instead of the actual name.
   */
  public boolean getUseAttributeNameForName() {
    return useNameAttributeForName;
  }

  /**
   * Writes all children of tag to object.
   */
  public void writeClass(XMLTag tag, Object obj) {
    Class<?> c = obj.getClass();
    Class<?> fc, fcc;
    Constructor ctor;
    Field f;
    int childCnt = tag.getChildCount();
    XMLTag child;
    Object i, array[], newArray[];
    String name, typeString;
    for (int a = 0; a < childCnt; a++) {
      try {
        child = tag.getChildAt(a);
        int childChildcnt = child.getChildCount();
        name = child.getName();
        f = c.getField(name);
        if (f == null) {
          JFLog.log("XML:field not found:" + name);
          continue;
        }
        if (childChildcnt > 0) {
          fc = f.getType();
          if (fc.isArray()) {
            fcc = fc.getComponentType();
            ctor = fcc.getConstructor();
            i = ctor.newInstance();
            writeClass(child, i);
            array = (Object[]) f.get(obj);
            if ((array == null) || (array.length == 0)) {
              newArray = (Object[]) Array.newInstance(fcc, 1);
              newArray[0] = i;
            } else {
              newArray = Arrays.copyOf(array, array.length + 1);
              newArray[array.length] = i;
            }
            f.set(obj, newArray);
          } else {
            ctor = fc.getConstructor();
            Object childObject = ctor.newInstance();
            writeClass(child, childObject);
            f.set(obj, childObject);
          }
        } else {
          typeString = f.toGenericString();
          if (typeString.indexOf(" int ") != -1) {
            f.setInt(obj, Integer.valueOf(child.content));
          } else if (typeString.indexOf(" short ") != -1) {
            f.setShort(obj, (short) JF.atoi(child.content));
          } else if (typeString.indexOf(" byte ") != -1) {
            f.setByte(obj, (byte) JF.atoi(child.content));
          } else if (typeString.indexOf(" float ") != -1) {
            f.setFloat(obj, Float.valueOf(child.content));
          } else if (typeString.indexOf(" double ") != -1) {
            f.setDouble(obj, Double.valueOf(child.content));
          } else if (typeString.indexOf(" boolean ") != -1) {
            f.setBoolean(obj, child.content.equalsIgnoreCase("true"));
          } else if (typeString.indexOf(" java.lang.String ") != -1) {
            f.set(obj, child.content);
          } else if (typeString.indexOf(" javaforce.webui.Color ") != -1) {
            f.set(obj, new Color(JF.atox(child.content)));
          } else if (typeString.indexOf(" int[] ") != -1) {
            int[] array2 = (int[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new int[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = Integer.valueOf(child.content);
            f.set(obj, array2);
          } else if (typeString.indexOf(" short[] ") != -1) {
            short[] array2 = (short[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new short[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = Short.valueOf(child.content);
            f.set(obj, array2);
          } else if (typeString.indexOf(" byte[] ") != -1) {
            byte[] array2 = (byte[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new byte[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = Byte.valueOf(child.content);
            f.set(obj, array2);
          } else if (typeString.indexOf(" float[] ") != -1) {
            float[] array2 = (float[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new float[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = Float.valueOf(child.content);
            f.set(obj, array2);
          } else if (typeString.indexOf(" double[] ") != -1) {
            double[] array2 = (double[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new double[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = Double.valueOf(child.content);
            f.set(obj, array2);
          } else if (typeString.indexOf(" boolean[] ") != -1) {
            boolean[] array2 = (boolean[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new boolean[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = child.content.equalsIgnoreCase("true");
            f.set(obj, array2);
          } else if (typeString.indexOf(" java.lang.String[] ") != -1) {
            String[] array2 = (String[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new String[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = child.content;
            f.set(obj, array2);
          } else if (typeString.indexOf(" java.awt.Color[] ") != -1) {
            Color[] array2 = (Color[]) f.get(obj);
            int idx;
            if (array2 == null) {
              idx = 0;
              array2 = new Color[1];
            } else {
              idx = array2.length;
              array2 = Arrays.copyOf(array2, array2.length + 1);
            }
            array2[idx] = new Color(JF.atox(child.content));
            f.set(obj, array2);
          } else {
            name = child.getName();
            f = c.getField(name);
            fc = f.getType();
            ctor = fc.getConstructor();
            Object childObject = ctor.newInstance();
            writeClass(child, childObject);
            f.set(obj, childObject);
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  /**
   * Writes entire XML tree to object.
   */
  public void writeClass(Object obj) {
    writeClass(root, obj);
  }

  /**
   * Reads all fields from an object and places into tag.
   */
  public void readClass(XMLTag tag, Object obj) {
    Class<?> c = obj.getClass(), fc;
    Field[] fs;
    String name, typeString;
    try {
      fs = c.getFields();
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    int fieldcnt = fs.length;
    boolean isArray;
    Object[] array;
    for (int a = 0; a < fieldcnt; a++) {
      try {
        if (fs[a].get(obj) == null) {
          continue;
        }
        fc = fs[a].getType();
        isArray = fc.isArray();
        name = fs[a].getName();
        if (isArray) {
          typeString = fs[a].toGenericString();
          if (typeString.indexOf(" static ") != -1) {
            continue;  //ignore static fields
          }
          if (typeString.indexOf(" transient ") != -1) {
            continue;  //ignore transient fields
          }
          array = (Object[]) fs[a].get(obj);
          if (typeString.indexOf(" int[] ") != -1) {
            Integer[] array2 = (Integer[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", "" + array2[b].intValue());
            }
          } else if (typeString.indexOf(" short[] ") != -1) {
            Short[] array2 = (Short[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", "" + array2[b].shortValue());
            }
          } else if (typeString.indexOf(" byte[] ") != -1) {
            Byte[] array2 = (Byte[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", "" + array2[b].byteValue());
            }
          } else if (typeString.indexOf(" boolean[] ") != -1) {
            Boolean[] array2 = (Boolean[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", array2[b].booleanValue() ? "true" : "false");
            }
          } else if (typeString.indexOf(" float[] ") != -1) {
            Float[] array2 = (Float[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", "" + array2[b].floatValue());
            }
          } else if (typeString.indexOf(" double[] ") != -1) {
            Double[] array2 = (Double[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", "" + array2[b].doubleValue());
            }
          } else if (typeString.indexOf(" java.lang.String[] ") != -1) {
            String[] array2 = (String[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", array2[b]);
            }
          } else if (typeString.indexOf(" javaforce.webui.Color[] ") != -1) {
            Color[] array2 = (Color[]) array;
            for (int b = 0; b < array.length; b++) {
              addTag(tag, name, "", Integer.toHexString(array2[b].getRGB()).substring(2));
            }
          } else {
            //go deeper into object
            for (int b = 0; b < array.length; b++) {
              readClass(addTag(tag, name, "", ""), array[b]);
            }
          }
        } else {
          typeString = fs[a].toGenericString();
          if (typeString.indexOf(" static ") != -1) {
            continue;  //ignore static fields
          }
          if (typeString.indexOf(" transient ") != -1) {
            continue;  //ignore transient fields
          }
          if (typeString.indexOf(" int ") != -1) {
            addTag(tag, name, "", "" + fs[a].getInt(obj));
          } else if (typeString.indexOf(" short ") != -1) {
            addTag(tag, name, "", "" + fs[a].getShort(obj));
          } else if (typeString.indexOf(" byte ") != -1) {
            addTag(tag, name, "", "" + fs[a].getByte(obj));
          } else if (typeString.indexOf(" boolean ") != -1) {
            addTag(tag, name, "", fs[a].getBoolean(obj) ? "true" : "false");
          } else if (typeString.indexOf(" float ") != -1) {
            addTag(tag, name, "", "" + fs[a].getFloat(obj));
          } else if (typeString.indexOf(" double ") != -1) {
            addTag(tag, name, "", "" + fs[a].getDouble(obj));
          } else if (typeString.indexOf(" java.lang.String ") != -1) {
            addTag(tag, name, "", (String) fs[a].get(obj));
          } else if (typeString.indexOf(" java.awt.Color ") != -1) {
            addTag(tag, name, "", Integer.toHexString(((Color) fs[a].get(obj)).getRGB()).substring(2));
          } else {
            //go deeper into object
            readClass(addTag(tag, name, "", ""), fs[a].get(obj));
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  /**
   * Reads entire XML tree from object (deletes entire tree first).<br>
   *
   * @param rootName = name to assign to root tag.
   */
  public void readClass(String rootName, Object obj) {
    this.deleteAll();
    root.setName(rootName);
    readClass(root, obj);
  }

  public void setEventListener(XMLEvent event) {
    this.event = event;
  }

  private String encodeSafe(String in) {
    return in.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");  //order matters here
  }

  private String decodeSafe(String in) {
    return in.replaceAll("&gt;", ">").replaceAll("&lt;", "<").replaceAll("&amp;", "&");
  }

//interface TreeModelListener

  public void treeNodesChanged(TreeModelEvent e) {
    if (ignoreEvents) return;
    XMLTag parent = (XMLTag) (e.getTreePath().getLastPathComponent());
    int[] indices = e.getChildIndices();
    if (indices == null || indices.length == 0) {
      return;
    }
    /*
     * If the event lists children, then the changed
     * node is the child of the node we've already
     * gotten.  Otherwise, the changed node and the
     * specified node are the same.
     */
    int index = indices[0];
    XMLTag tag = (XMLTag) (parent.getChildAt(index));
    if (tag.isReadOnly) {
      return;
    }
    if (tag.getUserObject() == null) {
      return;
    }
    fireEvents = false;  //prevent inf loop (setName will call changedTag which calls treemodel.nodeChanged)
    tag.setName(tag.getUserObject().toString());
    fireEvents = true;
    if (event != null) {
      event.XMLTagRenamed(tag);
    }
  }

  public void treeNodesInserted(TreeModelEvent e) {
  }

  public void treeNodesRemoved(TreeModelEvent e) {
  }

  public void treeStructureChanged(TreeModelEvent e) {
  }
};
