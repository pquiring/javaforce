package javaforce.webui;

/** Test WebUI.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.webui.event.*;

public class Test implements WebUIHandler {
  public Resource img;

  public Test() {
    JFImage i = new JFImage(64,64);
    i.line(0, 0, 63, 63, Color.red);
    i.line(63, 0, 0, 63, Color.red);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    i.savePNG(baos);
    img = Resource.registerResource(baos.toByteArray(), Resource.PNG);
  }

  public static void main(String args[]) {
    new WebUIServer().start(new Test(), 8080, false);
  }

  public void clientConnected(WebUIClient client) {}
  public void clientDisconnected(WebUIClient client) {
    System.exit(0);
  }

  public byte[] getResource(String url) {
    //TODO : return static images, etc needed by webpage
    return null;
  }

  private int value = 50;

  public Panel getRootPanel(WebUIClient client) {
    Panel panel = new Panel();

    //add a menubar
    MenuBar menubar = new MenuBar();
    panel.add(menubar);
    Menu m1 = new Menu("File");
    menubar.add(m1);
    MenuItem m1a = new MenuItem("Open");
    m1.add(m1a);
    MenuItem m1b = new MenuItem("Save");
    m1.add(m1b);
    MenuItem m1c = new MenuItem("Save As");
    m1.add(m1c);

    Menu m2 = new Menu("Edit");
    menubar.add(m2);
    MenuItem m2a = new MenuItem("Cut");
    m2.add(m2a);
    MenuItem m2b = new MenuItem("Copy");
    m2.add(m2b);
    MenuItem m2c = new MenuItem("Paste");
    m2.add(m2c);
    Menu m3 = new Menu("More");
    m2.add(m3);
    MenuItem m3a = new MenuItem("X1");
    m3.add(m3a);
    MenuItem m3b = new MenuItem("X2");
    m3.add(m3b);
    MenuItem m3c = new MenuItem("X3");
    m3.add(m3c);

    Row row1 = new Row();
    panel.add(row1);
    //add : togglebutton
    ToggleButton t = new ToggleButton("Toggle Me!");
    row1.add(t);
    //add : button
    Button b = new Button("Click Me!");
    row1.add(b);
    //add : label
    Label l = new Label("Ready!");
    row1.add(l);
    //create event handler for button
    b.addClickListener((MouseEvent e, Component button) -> {
      Integer times = (Integer)client.getProperty("times");
      if (times == null) times = 1;
      l.setText("You clicked it " + times + " times!");
      client.setProperty("times", times + 1);
      ProgressBar bar = (ProgressBar)client.getProperty("bar");
      bar.setValue(++value);
    });
    //another button to popup window
    Button b2 = new Button("Popup");
    row1.add(b2);
    //add popuppanel
    PopupPanel pp = new PopupPanel("Test");
    panel.add(pp);
    b2.addClickListener((MouseEvent e, Component button) -> {
      System.out.println("Popup Panel");
      pp.setPosition(100, 100);
      pp.setVisible(true);
    });
    //add something to popup panel
    Label ppl1 = new Label("Popup Panel!");
    pp.add(ppl1);
    //add : combobox
    ComboBox comboBox = new ComboBox();
    comboBox.add("option1", "Option #1");
    comboBox.add("option2", "Option #2");
    comboBox.add("option3", "Option #3");
    comboBox.addChangedListener((Component comp) -> {
      ComboBox cb = (ComboBox)comp;
      System.out.println("comboxbox index=" + cb.getSelectedIndex());
    });
    row1.add(comboBox);
    //add : checkbox
    CheckBox checkBox = new CheckBox("Enable Option");
    row1.add(checkBox);
    //add : flexbox
    row1.add(new FlexBox());
    //add : button
    row1.add(new Button("Far Right"));
    //add second row
    Row row2 = new Row();
    panel.add(row2);

    //add : tab panel
    TabPanel tab = new TabPanel();
    row2.add(tab);
//    row2.addClass("pad");

    //TAB #1
    Table table = new Table(50, 50, 3, 3);
    table.setBorder(true);
    table.add(new Label("cell_0_0"), 0, 0);
    table.add(new Label("cell_1_1"), 1, 1);
    table.add(new Label("cell_2_2_______________too_long"), 2, 2);

    Panel t1 = new Panel();
    tab.addTab(t1, "Tab#1");
    t1.add(table);

    //TAB #2
    Column col = new Column();
    Label l2 = new Label("Another Label");
    col.add(l2);
    TextField tf1 = new TextField("init text");
    tf1.addChangedListener((Component comp) -> {
      TextField tf = (TextField)comp;
      System.out.println("textfield text=" + tf.getText());
    });
    col.add(tf1);
    TextArea ta1 = new TextArea("init text");
    ta1.addChangedListener((Component comp) -> {
      TextArea ta = (TextArea)comp;
      System.out.println("textarea text=" + ta.getText());
    });
    col.add(ta1);

    Panel t2 = new Panel();
    tab.addTab(t2, "Tab#2");
    t2.add(col);

    //TAB #3

    ListBox list = new ListBox();
    list.setSize(256, 256);
    list.add("Option #1");
    list.add("Option #2");
    list.add("Option #3");

    Button b_add = new Button("Add");
    Button b_remove = new Button("Remove");

    Panel t3 = new Panel();
    t3.add(list);
    t3.add(b_add);
    t3.add(b_remove);
    tab.addTab(t3, "Tab#3");

    b_add.addClickListener((MouseEvent e, Component button) -> {
      list.add("Option #" + (list.count()+1));
    });

    b_remove.addClickListener((MouseEvent e, Component button) -> {
      list.remove(list.count() - 1);
    });


    //TAB #4

    Tree tree = new Tree();
    tree.setSize(256, 256);
    TreeNode node = tree.getRootNode();
    node.setData("root");
    TreeNode child = new TreeNode();
    child.setData("t1");
    node.addNode(child);
    child = new TreeNode();
    child.setData("t2");
    node.addNode(child);
    node = child;
    child = new TreeNode();
    child.setData("t3");
    node.addNode(child);

    Panel t4 = new Panel();
    t4.add(tree);
    tab.addTab(t4, "Tab#4");

    //TAB #5
    ScrollPanel scroll = new ScrollPanel();
    for(int a=0;a<50;a++) {
      Row r = new Row();
      r.add(new Label("X" + a));
      scroll.add(r);
    }
    scroll.setSize(256, 256);

    Panel t5 = new Panel();
    t5.add(scroll);
    tab.addTab(t5, "Tab#5");

    //add third row
    Row row3 = new Row();
    panel.add(row3);
    Image i1 = new Image(img);
    row3.add(i1);

    SplitPanel sp = new SplitPanel(SplitPanel.VERTICAL);
    sp.setLeftComponent(new Label("AA<br>aa"));
    sp.setRightComponent(new Label("BB<br>bb"));
    row3.add(sp);

    Image i2 = new Image(img);
    row3.add(i2);

    //add forth row
    Row row4 = new Row();
    panel.add(row4);

/*
    LayersPanel lp = new LayersPanel();
    Label layer1 = new Label("NO");
    lp.add(layer1);
    Label layer2 = new Label("-----YES");
    lp.add(layer2);
    row4.add(lp);
*/

    //add fifth row
    Row row5 = new Row();
    panel.add(row5);

    Slider vslider = new Slider(Slider.VERTICAL, 0, 100, 0);
    vslider.addChangedListener((Slider) -> {System.out.println("vslider=" + vslider.getPos());});
    row5.add(vslider);
    Slider hslider = new Slider(Slider.HORIZONTAL, 0, 100, 0);
    hslider.addChangedListener((Slider) -> {System.out.println("hslider=" + hslider.getPos());});
    row5.add(hslider);

    ProgressBar bar = new ProgressBar(ProgressBar.HORIZONTAL, 100, 16);
    bar.setValue(50);
    row5.add(bar);
    client.setProperty("bar", bar);

//    Pad pad2 = new Pad();
//    panel.add(pad2);
    return panel;
  }
}
