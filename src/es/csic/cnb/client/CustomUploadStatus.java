package es.csic.cnb.client;

import gwtupload.client.HasProgress;
import gwtupload.client.BaseUploadStatus;
import gwtupload.client.IUploadStatus;
import gwtupload.client.IUploader;
import gwtupload.client.Utils;

import java.text.ParseException;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *<p>
 * Basic widget that implements the IUploadStatus interface.
 * It renders a simple progress bar.
 * This class that can be overwritten to create more complex widgets.
 * Modified to include coef box.
 *</p>
 *
 * @author Manolo Carrasco Moñino
 * @author pdsanchez
 *
 */
public class CustomUploadStatus extends BaseUploadStatus {
  /**
   * Label with the coef text.
   */
  protected Label coefLabel = null;

  /**
   * Box with the coef.
   */
  protected IntegerBox coefBox = null;

  /**
   * Label with the PH text.
   */
  protected Label phLabel = null;

  /**
   * Box with the PH.
   */
  protected DoubleBox phBox = null;

  /**
   * Default Constructor.
   */
  public CustomUploadStatus() {
	  super();
    coefLabel.setStyleName("coeflabel");
    coefBox.setStyleName("coefbox");
    phLabel.setStyleName("coeflabel");
    phBox.setStyleName("coefbox");
  }
	
	@Override
  protected void addElementsToPanel() {
    panel.add(cancelLabel);
    panel.add(fileNameLabel);

	if(phLabel==null) {
		phLabel = getPHLabel();
	}
    panel.add(phLabel);
	if(phBox==null) {
		phBox = getPHBox();
	}
    panel.add(phBox);
    phBox.setValue(7.2);
	if(coefLabel==null) {
		coefLabel = getCoefLabel();
	}
    panel.add(coefLabel);
	if(coefBox==null) {
		coefBox = getCoefBox();
	}
    panel.add(coefBox);
    coefBox.setValue(1);

    panel.add(statusLabel);
  }
  
	@Override
  protected Panel getPanel() {
    HorizontalPanel hp = new HorizontalPanel();
    hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    return hp;
  }

  protected HTML getCoefLabel() {
    return new HTML("Coef");
  }

  protected IntegerBox getCoefBox() {
    return new IntegerBox();
  }

  protected HTML getPHLabel() {
    return new HTML("PH");
  }

  protected DoubleBox getPHBox() {
    return new DoubleBox();
  }

  /* (non-Javadoc)
   * @see gwtupload.client.IUploadStatus#newInstance()
   */
   @Override
  public IUploadStatus newInstance() {
    IUploadStatus ret = new CustomUploadStatus();
    ret.setCancelConfiguration(cancelCfg);
    return ret;
  }

  /**
   * Thought to be overridable by the user when extending this.
   *
   * @param showProgress
   * @param message
   */
   @Override
  protected void updateStatusPanel(boolean showProgress, String message) {
	  super.updateStatusPanel(showProgress,message);
    coefBox.setVisible(!showProgress);
    coefLabel.setVisible(!showProgress);
    phBox.setVisible(!showProgress);
    phLabel.setVisible(!showProgress);
  }

  /**
   * @return el valor del coef (o 1 si existe algún error)
   */
  public int getCoef() {
    //return coefBox.getValue();
    try {
      return coefBox.getValueOrThrow();
    }
    catch (ParseException e) {
      coefBox.setValue(1);
    }
    return 1;
  }

  /**
   * @return el valor del PH (o 7.2 si existe algún error)
   */
  public double getPh() {
    //return phBox.getValue();
    try {
      return phBox.getValueOrThrow();
    }
    catch (ParseException e) {
      phBox.setValue(7.2);
    }
    return 7.2;
  }

}