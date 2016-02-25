package es.csic.cnb.client;

import java.util.Date;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import es.csic.cnb.client.rpc.MergeService;
import es.csic.cnb.client.rpc.MergeServiceAsync;
import es.csic.cnb.client.ui.BiomassPanel;
import es.csic.cnb.client.ui.FileUploaderPanel;
import es.csic.cnb.client.ui.MailPanel;
import es.csic.cnb.client.ui.MediumPanel;
import es.csic.cnb.client.ui.StatusPanel;
import es.csic.cnb.shared.ClientData;
import es.csic.cnb.shared.ModelData;
import es.csic.cnb.shared.error.FieldVerifier;
import es.csic.cnb.shared.error.ServerException;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class CombiModelWeb implements EntryPoint {
  private static final String DATEFORMAT_FOR_SESSIONID = "yyyyMMddHHmmssSSS";

  /**
   * Create a remote service proxy to talk to the server-side Greeting service.
   */
  private final MergeServiceAsync mergeService = GWT.create(MergeService.class);

  public void onModuleLoad() {
    // Session ID (yyyyMMddHHmmssSSS and 3 digits random number)
    String rnd = NumberFormat.getFormat("000").format(Random.nextInt(999));
    final String sessionId = DateTimeFormat.getFormat(DATEFORMAT_FOR_SESSIONID).format(new Date()) + rnd;

    final Grid mainGridPanel = new Grid(3, 1);
    mainGridPanel.setCellSpacing(0);
    mainGridPanel.setCellPadding(0);
    mainGridPanel.setStyleName("center");
    mainGridPanel.setSize("90%", "");

    Grid grid = new Grid(1, 2);
    grid.setCellSpacing(10);
    grid.setStyleName("title");
    mainGridPanel.setWidget(0, 0, grid);
    grid.setSize("100%", "35px");

    Grid helpGrid = new Grid(2, 1);
    helpGrid.setStyleName("title-help");
    Anchor help = new Anchor("Help", "resources/help.html", "_blank");
    helpGrid.setWidget(0, 0, help);
    Anchor contact = new Anchor("Contact", "resources/help.html#contact", "_blank");
    helpGrid.setWidget(1, 0, contact);
    grid.setWidget(0, 0, helpGrid);

    Label label = new Label("Cmodel");
    label.setStyleName("title-text");
    label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    grid.setWidget(0, 1, label);
    label.setSize("100%", "");
    grid.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);

    // ----------------------------------------------------------
    // 1st page: options
    // ----------------------------------------------------------

    final VerticalPanel optionsPanel = new VerticalPanel();
    optionsPanel.setWidth("100%");
    optionsPanel.setSpacing(15);
    optionsPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

    Grid columnsPanel = new Grid(2, 3);
    optionsPanel.add(columnsPanel);
    columnsPanel.setWidth("100%");

    // COLUMN 1
    VerticalPanel c1Panel = new VerticalPanel();
    c1Panel.setSpacing(15);
    columnsPanel.setWidget(0, 0, c1Panel);
    c1Panel.setWidth("100%");

    ///// Loader
    final FileUploaderPanel uploader = new FileUploaderPanel(sessionId);
    c1Panel.add(uploader);

    // COLUMN 2
    VerticalPanel c2Panel = new VerticalPanel();
    c2Panel.setStyleName("optionsBox");
    c2Panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    c2Panel.setSpacing(15);
    columnsPanel.setWidget(0, 2, c2Panel);
    c2Panel.setWidth("100%");

    ///// Biomass
    final BiomassPanel biomass = new BiomassPanel();
    c2Panel.add(biomass);

    ///// Medium
    final MediumPanel medium = new MediumPanel();
    c2Panel.add(medium);
    c2Panel.add(createHSpacer("5px"));

    ///// Email
    final MailPanel mail = new MailPanel();
    c2Panel.add(mail);

    columnsPanel.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);

    // ROW RUN BUTTON
    //optionsPanel.add(createHSpacer("10px"));

    ///// Run button
    final Button sendButton = new Button("Combine Models");
    sendButton.addStyleName("sendButton");
    sendButton.setWidth("150px");
    optionsPanel.add(sendButton);
    optionsPanel.setCellHorizontalAlignment(sendButton, HasHorizontalAlignment.ALIGN_CENTER);
    optionsPanel.add(createHSpacer("5px"));

    uploader.setSubmitButton(sendButton);
    columnsPanel.getCellFormatter().setVerticalAlignment(0, 2, HasVerticalAlignment.ALIGN_TOP);

    mainGridPanel.setWidget(1, 0, optionsPanel);
    RootPanel.get("id_main").add(mainGridPanel);
    mainGridPanel.getRowFormatter().setVerticalAlign(0, HasVerticalAlignment.ALIGN_MIDDLE);

    // ----------------------------------------------------------
    // 2nd page: status
    // ----------------------------------------------------------
    final StatusPanel statusPanel = new StatusPanel();
    statusPanel.setVisible(false);
    mainGridPanel.setWidget(2, 0, statusPanel);

    // ----------------------------------------------------------
    // Create the popup dialog box
    // ----------------------------------------------------------
    //final AlertDialog alert = new AlertDialog("Model Combination");


    /////////////////////////////////////////////////////////////
    // Submit
    /////////////////////////////////////////////////////////////

    // Create a handler for the sendButton and nameField
    class RunHandler implements ClickHandler {
      /**
       * Fired when the user clicks on the sendButton.
       * Send data to the server and wait for a response.
       */
      public void onClick(ClickEvent event) {
        boolean isValid = true;

        // Errores en el fichero
        for (ModelData data : uploader.getClientData()) {
          if (data.isError()) {
            uploader.setTextError(data.getFilename() + " is not valid SBML. Please remove it");
            return;
          }
        }

        // First, we validate the input
        int tUp = uploader.getTotalUpload();
        if (!FieldVerifier.isModelUploaded(tUp)) {
          if (tUp == 0) {
            uploader.setTextError("Please upload the model files");
          }
          else if (tUp == 1) {
            uploader.setTextError("Please upload more than one model file");
          }
          isValid = false;
        }
//        if (!FieldVerifier.isValidMail(mail.getMail())) {
//          mail.displayError();
//          isValid = false;
//        }


        // Return when exists errors
        if (!isValid)
          return;


        // Hide options panel
        sendButton.setEnabled(false);
        optionsPanel.setVisible(false);

        ClientData cdata = new ClientData(uploader.getClientData(),
                medium.getMinMediaSelected(),
                biomass.isSelected(),
                mail.getMail());

        String path = GWT.getHostPageBaseURL() + "output/" + sessionId + "/index.html";
        cdata.setCmodelPath(path);

        // Cookie to store the email address
        mail.keepMail();

        // show next window (status panel)
        statusPanel.updatePanel(cdata);
        statusPanel.setVisible(true);

        // ----------------------------------------------------------
        // Call the server
        // ----------------------------------------------------------

        // Retrieve results when the process is end
        // ----------------------------------------------------------
        mergeService.run(sessionId, cdata, new AsyncCallback<ClientData>() {
          // FAILURE: handle errors
          public void onFailure(Throwable caught) {

            if (caught instanceof ServerException) {
//              String msg = caught.getMessage();
//
//              // Obtain remaining data
//              JobStatus status = ((ServerException) caught).getStatus();
//              if (status != null) {
//                msg = msg + "<br>" + status.getInfo();
//              }
//
//              else if (caught.getCause() != null) {
//                msg = msg + "<br>" + caught.getCause().getMessage();
//              }
//
//              System.out.println(":::::::::::::::::::::::::::::::::::::::::::::::::::::: ERR1");
//              System.out.println(caught.getMessage());
//              System.out.println(caught.fillInStackTrace());
//              
//              final AlertDialog alert = new AlertDialog("Model Combination");
//              alert.showMsg(caught.getMessage());
            }
            else {
//              System.out.println(":::::::::::::::::::::::::::::::::::::::::::::::::::::: ERR2");
//              System.out.println(caught.getMessage());
//              System.out.println(caught.fillInStackTrace());
            }
          }

          // SUCCESS
          public void onSuccess(final ClientData cdata) {
//            System.out.println(":::::::::::::::::::::::::::::::::::::::::::::::::::::: OK");
          }
        });
      }
    }

    // Add a handler to comunicate with the server
    sendButton.addClickHandler(new RunHandler());
  }


  private SimplePanel createHSpacer(String height) {
    SimplePanel spacer = new SimplePanel();
    spacer.setHeight(height);
    return spacer;
  }
}
