package es.csic.cnb.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Window used to display messages.
 *
 * @author Pablo D. Sánchez
 *
 */
public class AlertDialog extends DialogBox implements ClickHandler {
  private static final String WIDTH = "750px";
  private static final String HEIGHT = "450px";
  /**
   * The message displayed to the user when the server cannot be reached or returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
          + "attempting to contact the server. Please check your network "
          + "connection and try again.";

  private HTML serverResponseLabel;

  public AlertDialog(String title) {
    super(true);

    //this.setText(title);
    this.setHTML("<b>" + title + "</b> <img src=\"images/icon/help.png\" class=\"gwt-Image\">");
    this.setAnimationEnabled(true);
    //this.setWidth(WIDTH);
    //this.setHeight(HEIGHT);

    final Button closeButton = new Button("Close");
    closeButton.setStyleName("btn-upld");
    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    // Add a handler to close the DialogBox
    closeButton.addClickHandler(this);
    closeButton.setFocus(true);

    serverResponseLabel = new HTML();

    ScrollPanel scrollPanel = new ScrollPanel(serverResponseLabel);
    scrollPanel.setWidth(WIDTH);
    scrollPanel.setHeight(HEIGHT);

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(scrollPanel);
    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    setWidget(dialogVPanel);
  }

  @Override
  public void onClick(ClickEvent event) {
    // Hide the window when user click the 'close' button
    this.hide();
  }

  /**
   * Window with a default failure message.
   */
  public void showFailureMsg() {
    this.showFailureMsg(SERVER_ERROR);
  }

  /**
   * Window with a failure message.
   *
   * @param text - the failure message.
   */
  public void showFailureMsg(String text) {
    // Show error message to the user
    this.setText(this.getText() + " - Failure");
    serverResponseLabel.addStyleName("serverResponseLabelError");
    serverResponseLabel.setHTML(text);
    this.center();
  }

  /**
   * Window with a message.
   * @param text - the message.
   */
  public void showMsg(String text) {
    serverResponseLabel.removeStyleName("serverResponseLabelError");
    serverResponseLabel.setHTML(text);
    this.center();
  }
}
