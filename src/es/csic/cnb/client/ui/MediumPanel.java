package es.csic.cnb.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

import es.csic.cnb.shared.ClientData;

public class MediumPanel extends Composite {
//  public static final int DEFAULTMM  = 1;
//  public static final int BASICMM    = 2;
//  public static final int INTERSECMM = 3;
//  public static final int COMBIMM    = 4;

  private static final String TXT = "Check to use a minimum media for the combined model.<br>" +
          "By default the initial growth media of the individual models will be used.";

  private static final String HELP = "<p class=\"help-header\">Determination of growth medium</p>" +
          "<p><strong>Cmodel</strong> first looks for the minimal medium allowing the growth of each of the individual " +
          "species. It starts assuming a preferred minimum set of nutrients: Glucose or Glycerol (Carbon sources), Ammonia " +
          "(Nitrogen source), Phosphate (Phosphorous source), Sulfate (Sulfur source), and water. If the model predicts " +
          "growth in these conditions, this is set as the minimum medium for that species. Otherwise, it determines the " +
          "minimum set of chemicals needed for growth by combinatorially exploring the space of all possible exchange " +
          "reactions, by means of a genetic algorithm. Once all the minimal media for individual species has been found, " +
          "it is possible to select for:" +
          "</p>" +
          "<p style=\"margin-bottom: 0cm; margin-left: 30px;\">" +
          "- <strong>Medium containing all metabolites found in the initial media of each species</strong>. This is the " +
          "option by default." +
          "</p>" +
          "<p style=\"margin-bottom: 0cm; margin-left: 30px;\">" +
          "- <strong>Minimum growth medium</strong>, containing the union of all the minimal requirements for individual " +
          "species." +
          "</p>";

  private RadioButton rbDmm;
  private RadioButton rbBmm;
  private RadioButton rbImm;
  private RadioButton rbCmm;

  public MediumPanel() {
    final AlertDialog help = new AlertDialog("Medium");

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

    VerticalPanel mmPanel = new VerticalPanel();
    grid.setWidget(1, 1, mmPanel);
    mmPanel.setSize("100%", "36px");

    rbDmm = new RadioButton("mmedia", "Initial growth media");
    rbDmm.setTitle("TODO Default min media"); // TODO
    rbDmm.setValue(true);
    rbDmm.setStyleName("labelBold");
    mmPanel.add(rbDmm);

    rbBmm = new RadioButton("mmedia", "Basic minimum media");
    rbBmm.setTitle("TODO Basic min media"); // TODO
    rbBmm.setStyleName("labelBold");
    mmPanel.add(rbBmm);

    rbImm = new RadioButton("mmedia", "Intersection minimal media");
    rbImm.setTitle("TODO Intersection min media"); // TODO
    rbImm.setStyleName("labelBold");
    mmPanel.add(rbImm);

    rbCmm = new RadioButton("mmedia", "Combined model minimal media");
    rbCmm.setTitle("TODO Combined min media"); // TODO
    rbCmm.setStyleName("labelBold");
    mmPanel.add(rbCmm);
  }

  public int getMinMediaSelected() {
    if (rbDmm.getValue()) {
      return ClientData.DEFAULTMM;
    }
    else if (rbBmm.getValue()) {
      return ClientData.BASICMM;
    }
    else if (rbImm.getValue()) {
      return ClientData.INTERSECMM;
    }
    else if (rbCmm.getValue()) {
      return ClientData.COMBIMM;
    }
    return -1;
  }
}
