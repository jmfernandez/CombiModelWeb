package es.csic.cnb.client.ui;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import es.csic.cnb.shared.ClientData;

public class StatusPanel extends Composite {
  private VerticalPanel infoPanel;

//  private Grid imgGrid;

  public StatusPanel() {
    final VerticalPanel verticalPanel = new VerticalPanel();
    verticalPanel.setWidth("100%");
    verticalPanel.setSpacing(15);
    verticalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    initWidget(verticalPanel);

    infoPanel = new VerticalPanel();
    infoPanel.setWidth("100%");
    verticalPanel.add(infoPanel);

    verticalPanel.add(createHSpacer("30px"));

    FlowPanel flowPanel = new FlowPanel();
    verticalPanel.add(flowPanel);
    verticalPanel.setCellHorizontalAlignment(flowPanel, HasHorizontalAlignment.ALIGN_CENTER);

    //Anchor anchor = new Anchor("Home", Window.Location.getHref());
    //Anchor anchor = new Anchor("Home", "");
    //flowPanel.add(anchor);

    ImageAnchor imgAnchor = new ImageAnchor("images/icon/home.png", "");
    imgAnchor.setTitle("Home");
    flowPanel.add(imgAnchor);
  }

  public void updatePanel(ClientData cdata) {
    infoPanel.addStyleName("labelInfo");
//    if (cdata.isNewobj() || cdata.isMinmedia()) {
//      Label lblOpts = new Label("Options:");
//      lblOpts.addStyleName("labelBold");
//      infoPanel.add(lblOpts);
//      if (cdata.isNewobj()) {
//        infoPanel.add(new Label("- Biomass joint function selected"));
//      }
//      if (cdata.isMinmedia()) {
//        infoPanel.add(new Label("- Minimum Medium selected"));
//      }
//    }
//
//    infoPanel.add(createHSpacer("5px"));
//
//    Label lblModels = new Label("Models:");
//    lblModels.addStyleName("labelBold");
//    infoPanel.add(lblModels);
//
//    List<ModelData> list = cdata.getModelDataList();
//
//    imgGrid = new Grid(list.size(), 2);
//
//    int cont = 0;
//    for (ModelData md : list) {
//      imgGrid.setWidget(cont, 0, new Image("images/icon/bullet.png"));
//      imgGrid.setWidget(cont, 1, new Label(md.getFilename()));
//      cont++;
//    }
//
//    infoPanel.add(imgGrid);

    infoPanel.add(createHSpacer("10px"));

    // TODO Cambiar 72 por dato del properties (schedule.limit.hours)
    StringBuilder sb = new StringBuilder();
    sb.append("<p>To check the progress of your job, please follow this link:</p>");
    sb.append("<p><a href=\"").append(cdata.getCmodelPath()).append("\" target=\"_blank\">");
    sb.append(cdata.getCmodelPath()).append("</a></p>");
    sb.append("<p style=\"font-family: Arial, Monospace; color: #ff0000;\">This link will be available for 72 hours</p>");
    if (cdata.getMail() != null) {
      sb.append("<br><br>On completion an email containing the results will be sent to <b>");
      sb.append(cdata.getMail());
      sb.append("</b>");
    }


    HTML html = new HTML(sb.toString());
    html.getElement().getStyle().setProperty("textAlign", "center");
    infoPanel.add(html);

//    // Volver al home a los 30 seg. (Da warning si el proceso no ha terminado)
//    Timer timer = new Timer() {
//      @Override
//      public void run() {
//        Window.Location.replace(Window.Location.getHref());
//      }
//    }; // End timer
//    timer.schedule(30000);
  }

  private SimplePanel createHSpacer(String height) {
    SimplePanel spacer = new SimplePanel();
    spacer.setHeight(height);
    return spacer;
  }
}
