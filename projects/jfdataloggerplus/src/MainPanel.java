/** MainPanel
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class MainPanel extends CenteredPanel {
  public MainPanel() {
    Label label = new Label("jfDataLogger Plus");
    label.setFontSize(32);
    add(label);
    add(new Block());
    config = new Button("Config");
    config.addClickListener((me, c) -> {
      System.out.println("Config");
      c.getClient().setPanel(new ConfigPanel());
    });
    config.setFontSize(24);
    add(config);
    add(new Block());
    report = new Button("Reports");
    report.addClickListener((me, c) -> {
      c.getClient().setPanel(new ReportsPanel());
    });
    report.setFontSize(24);
    add(report);
  }
  public Button config, report;
}
