package javaforce.webui;

/** Color Chooser Popup
 *
 *  Shows a popup dialog to select a RGB color.
 *
 * @author pquiring
 */

public class ColorChooserPopup extends PopupPanel {
  public ColorChooserPopup() {
    super("Select Color");
    setModal(true);

    Row row = new Row();
    add(row);
    Column col;

    col = new Column();
    row.add(col);
    Label rl = new Label("R");
    rl.setAlign(CENTER);
    col.add(rl);
    r = new Slider(Slider.VERTICAL, 0, 255, 1);
    r.addChangedListener((c) -> {
      rv.setText(Integer.toString(r.getPos()));
      setClr();
    });
    col.add(r);
    rv = new Label("0");
    rv.setAlign(CENTER);
    rv.setSize(32 * 3, 32);
    col.add(rv);

    col = new Column();
    row.add(col);
    Label gl = new Label("G");
    gl.setAlign(CENTER);
    col.add(gl);
    g = new Slider(Slider.VERTICAL, 0, 255, 1);
    g.addChangedListener((c) -> {
      gv.setText(Integer.toString(g.getPos()));
      setClr();
    });
    col.add(g);
    gv = new Label("0");
    gv.setAlign(CENTER);
    gv.setSize(32 * 3, 32);
    col.add(gv);

    col = new Column();
    row.add(col);
    Label bl = new Label("B");
    bl.setAlign(CENTER);
    col.add(bl);
    b = new Slider(Slider.VERTICAL, 0, 255, 1);
    b.addChangedListener((c) -> {
      bv.setText(Integer.toString(b.getPos()));
      setClr();
    });
    col.add(b);
    bv = new Label("0");
    bv.setAlign(CENTER);
    bv.setSize(32 * 3, 32);
    col.add(bv);

    row = new Row();
    add(row);
    ok = new Button("OK");
    row.add(ok);
    cancel = new Button("Cancel");
    row.add(cancel);
    clr = new Button("-");
    row.add(clr);

    ok.addClickListener((e, c) -> {
      setVisible(false);
      action();
    });
    cancel.addClickListener((e, c) -> {
      setVisible(false);
    });
  }
  public void setValue(int rgb) {
    int _r = (rgb & 0xff0000) >> 16;
    int _g = (rgb & 0xff00) >> 8;
    int _b = (rgb & 0xff);

    rv.setText(Integer.toString(_r));
    r.setPos(_r);
    gv.setText(Integer.toString(_g));
    g.setPos(_g);
    bv.setText(Integer.toString(_b));
    b.setPos(_b);
    clr.setColor(rgb);
    clr.setBackColor(rgb);
  }
  public int getValue() {
    int rgb = 0;
    rgb |= r.getPos() << 16;
    rgb |= g.getPos() << 8;
    rgb |= b.getPos();
    return rgb;
  }
  public void setComponentsSize(int width, int height) {
    ok.setSize(width, height);
    cancel.setSize(width*2, height);
    clr.setSize(width, height);
  }
  private void setClr() {
    clr.setBackColor(getValue());
  }
  private Label rv, gv, bv;
  private Slider r, g, b;
  private Button ok, cancel;
  private Button clr;
}
