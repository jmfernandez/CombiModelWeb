package es.csic.cnb.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BiomassPanel extends Composite {
  private static final String TXT = "Check to work with a biomass joint function (forcing the growth of all species)<br>" +
          "By default the individual biomass function of each model will be used.";

  private static final String HELP = "<p class=\"help-header\">Objective functions</p>" +
          "<p><strong>Cmodel</strong> generates new objective functions which can be set to:</p>" +
          "<p style=\"margin-bottom: 0cm; margin-left: 30px;\">" +
          "- <strong>Optimizing maximal production of biomass</strong>: A new objective function is created that optimizes " +
          "the sum of all individual biomasses. This will produce a model that will look for the maximum biomass " +
          "production. Therefore, it is not necessary that all species grow. This is the option by default." +
          "</p>" +
          "<p style=\"margin-bottom: 0cm; margin-left: 30px;\">" +
          "- <strong>Optimizing growth for all species</strong>: A new reaction of generation of global biomass is " +
          "created, combining the individual biomass reactions:" +
          "</p>" +
          "<p style=\"margin-bottom: 0cm; margin-left: 30px;\">" +
          "<em>Biomass 1 + Biomass 2 + &#8230; + Biomass n -&gt; global_biomass</em>" +
          "</p>" +
          "<p style=\"margin-bottom: 0cm; margin-left: 30px;\">By setting the objective function to the optimization of " +
          "this reaction, we force the combined model to maximize the creation of global biomass, while keeping the growth " +
          "of all individual species. If no global biomass is produced, then the community is not stable in the given " +
          "conditions and hence there is no growth.</p>";

  private CheckBox chckbx;

  public BiomassPanel() {
    final AlertDialog help = new AlertDialog("Biomass");

    VerticalPanel verticalPanel = new VerticalPanel();
    initWidget(verticalPanel);

    Grid grid = new Grid(2, 3);
    grid.setStyleName("labelInfo");
    grid.setCellPadding(3);
    grid.setWidth("100%");
    verticalPanel.add(grid);

    Image image = new Image("images/icon/user.png");
    grid.setWidget(0, 0, image);
//    final Image imgHelp = new Image("images/icon/helpr.png");
//    imgHelp.setStyleName("pointer");
//    imgHelp.addClickHandler(new ClickHandler() {
//      @Override
//      public void onClick(ClickEvent event) {
//        help.showMsg(""); // TODO
//      }
//    });
//    grid.setWidget(0, 0, imgHelp);

    HTML lblHead = new HTML(TXT);
    grid.setWidget(0, 1, lblHead);

    final Image imgHelp = new Image("images/icon/help1.png");
    imgHelp.setStyleName("pointer");
    imgHelp.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        help.showMsg(HELP); // TODO
      }
    });
    grid.setWidget(0, 2, imgHelp);

    chckbx = new CheckBox("Biomass joint function");
    chckbx.setTitle("Biomass joint function");
    chckbx.setStyleName("labelBold");
    grid.setWidget(1, 1, chckbx);
  }

  public boolean isSelected() {
    return chckbx.getValue();
  }
}
