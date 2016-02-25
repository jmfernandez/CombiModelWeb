package es.csic.cnb.client.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import es.csic.cnb.client.CustomUploadStatus;
import es.csic.cnb.shared.ModelData;
import gwtupload.client.IFileInput.FileInputType;
import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.MultiUploader;

/**
 * Panel with the buttons used to upload models.
 *
 * @author Pablo D. Sánchez
 *
 */
public class FileUploaderPanel extends Composite {
  private static final String TXT = "Click the 'Load model' button to upload the models from your disk drive.";

  private static final String TXT_ERROR = "An error occurred. Please try again";

  private static final String TXT_ERRORSBML = " is not valid SBML. Please remove it";

  private static final String TXT_TRYME =
          "<p>Click <a href=\"resources/models.zip\">here</a> to download a set of example models and combine them.</p>" +
          "<p>The models provided are:" +
          "<br>- <em>Escherichia coli K12, Mutant His<sup>-</sup></em>, auxotroph for Histidine" +
          "<br>- <em>Escherichia coli K12, Mutant Ile<sup>-</sup></em>, auxotroph for Isoleucine" +
          "<br>- <em>Escherichia coli K12, Mutant Lys<sup>-</sup></em>, auxotroph for Lysine" +
          "<br>These mutants are defective for aminoacid production, therefore they should not " +
          "<br>be able to grow in isolation.</p>" +
          "<p>Please follow these steps:" +
          "<ol>" +
          "<li>Unzip the downloaded file.</li>" +
          "<li>Click on the <strong>'Load model'</strong> button to upload the models.</li>" +
          "<li>Check out the optional <strong>biomass</strong> and <strong>minimum media</strong> parameters.</li>" +
          "<li>Include your <strong>email</strong> account (optional)</li>" +
          "<li>Click on the <strong>'Combine Models'</strong> button for running Cmodel.</li>" +
          "</ol>" +
          "Your results will be ready in a few minutes.</p>";

  private Label lbError;
  private Button submitButton;

  private Map<String,ModelData> dataList;
  private Map<String,CustomUploadStatus> coefList; // Para obtener coeficientes introducidos por el user para cada modelo

  public FileUploaderPanel(String sessionId) {
    dataList = new LinkedHashMap<String,ModelData>();
    coefList = new HashMap<String,CustomUploadStatus>();

//    final AlertDialog help = new AlertDialog("Upload Model");

    VerticalPanel verticalPanel = new VerticalPanel();
    initWidget(verticalPanel);

    Grid gridHead = new Grid(1, 3);
    gridHead.setStyleName("labelInfo");
    gridHead.setCellPadding(3);
    gridHead.setWidth("100%");
    verticalPanel.add(gridHead);

    Image image = new Image("images/icon/search.png");
    gridHead.setWidget(0, 0, image);
//    final Image imgHelp = new Image("images/icon/helpr.png");
//    imgHelp.setStyleName("pointer");
//    imgHelp.addClickHandler(new ClickHandler() {
//      @Override
//      public void onClick(ClickEvent event) {
//        help.showMsg(TXT_TRYME);
//      }
//    });
//    gridHead.setWidget(0, 0, imgHelp);

    HTML lblHead = new HTML(TXT);
    gridHead.setWidget(0, 1, lblHead);

//    final Image imgHelp = new Image("images/icon/help1.png");
//    imgHelp.setStyleName("pointer");
//    imgHelp.addClickHandler(new ClickHandler() {
//      @Override
//      public void onClick(ClickEvent event) {
//        help.showMsg(TXT_TRYME);
//      }
//    });
////    imgHelp.addMouseOverHandler(new MouseOverHandler() {
////      @Override
////      public void onMouseOver(MouseOverEvent event) {
////        imgHelp.getElement().getStyle().setCursor(Cursor.POINTER);
////      }
////    });
//    gridHead.setWidget(0, 2, imgHelp);

    SimplePanel spacer = new SimplePanel();
    spacer.setHeight("10px");
    verticalPanel.add(spacer);

    Grid grid = new Grid(3,1);
    grid.setCellPadding(3);
    grid.setCellSpacing(0);
    grid.getRowFormatter().setVerticalAlign(0, HasVerticalAlignment.ALIGN_MIDDLE);

    Button b = new Button("Load model");
    b.addStyleName("btn-upld");
    b.setSize("150px", "25px");

    MultiUploader uploader = new MultiUploader(FileInputType.CUSTOM.with(b));
    uploader.setStatusWidget(new CustomUploadStatus()); // Usar custom para incluir textArea para el coef
    uploader.setValidExtensions("xml");
//    uploader.addOnStartUploadHandler(onStartUploaderHandler);
    uploader.addOnFinishUploadHandler(onFinishUploaderHandler);
    uploader.addOnCancelUploadHandler(onCancelUploaderHandler);
    uploader.addOnChangeUploadHandler(onChangeUploaderHandler);

    Hidden hSessionId = new Hidden("SessionId");
    hSessionId.setValue(sessionId);
    // Add hidden fields
    uploader.add(hSessionId);
    grid.setWidget(0, 0, uploader);


    lbError = new Label();
    grid.setWidget(1, 0, lbError);


    DisclosurePanel disclosurePanel = new DisclosurePanel("Try me");
    disclosurePanel.setStyleName("labelInfo");
    disclosurePanel.setOpen(false);
    HTML htmlLabel = new HTML(TXT_TRYME);
    htmlLabel.setStyleName("labelTry");
    disclosurePanel.setContent(htmlLabel);
    grid.setWidget(2, 0, disclosurePanel);


    verticalPanel.add(grid);
  }

  //PRECURSORS: Change upload Handler
  private IUploader.OnChangeUploaderHandler onChangeUploaderHandler = new IUploader.OnChangeUploaderHandler() {
    public void onChange(IUploader uploader) {
      // Remove css styles and text
      lbError.removeStyleName("labelError");
      lbError.setText("");
      submitButton.setEnabled(false);
    }
  };

//  //PRECURSORS: Start upload Handler
//  private IUploader.OnStartUploaderHandler onStartUploaderHandler = new IUploader.OnStartUploaderHandler() {
//    public void onStart(IUploader uploader) {
//      // Remove css styles and text
//      lbError.removeStyleName("labelError");
//      lbError.setText("");
//      submitButton.setEnabled(false);
//    }
//  };

  // PRECURSORS: Finish upload Handler
  private IUploader.OnFinishUploaderHandler onFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
    public void onFinish(IUploader uploader) {
      if (uploader.getStatus() == Status.SUCCESS) {
        ModelData data = new ModelData();
        data.setCtype(uploader.getServerInfo().ctype);
        data.setField(uploader.getServerInfo().field);
        data.setFilename(uploader.getServerInfo().name);
        data.setFilepath(uploader.getServerInfo().message);
        data.setSize(uploader.getServerInfo().size);

        // Para obtener el coef
        CustomUploadStatus cus = (CustomUploadStatus)uploader.getStatusWidget();
        coefList.put(uploader.getServerInfo().field, cus);

        // Ver si existe error en el sbml
        if (uploader.getServerInfo().message.equals("ERROR")) {
          data.setError(true);
          setTextError(uploader.getServerInfo().name + TXT_ERRORSBML);
        }

        dataList.put(uploader.getServerInfo().field, data);
      }
      else if (uploader.getStatus() == Status.ERROR) {
        setTextError(TXT_ERROR);
      }
      submitButton.setEnabled(true);
    }
  };

  private IUploader.OnCancelUploaderHandler onCancelUploaderHandler = new IUploader.OnCancelUploaderHandler() {
    public void onCancel(IUploader uploader) {
      if (uploader.getStatus() == Status.SUCCESS) {
        dataList.remove(uploader.getServerInfo().field);
        coefList.remove(uploader.getServerInfo().field);

        // Remove css styles and text
        lbError.removeStyleName("labelError");
        lbError.setText("");
      }
      else if (uploader.getStatus() == Status.ERROR) {
        setTextError(TXT_ERROR);
      }
    }
  };

  public void setTextError(String txt) {
    lbError.setStyleName("labelError");
    lbError.setText(txt);
  }

  public int getTotalUpload() {
    return dataList.size();
  }

  public Collection<ModelData> getClientData() {
    // Actualizar los coef
    for (String key : coefList.keySet()) {
      CustomUploadStatus cus = coefList.get(key);
      dataList.get(key).setCoef(cus.getCoef());
      dataList.get(key).setPh(cus.getPh());
    }

    return dataList.values();
  }

  /**
   * @param submitButton the submitButton to set
   */
  public void setSubmitButton(Button submitButton) {
    this.submitButton = submitButton;
  }
}