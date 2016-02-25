package es.csic.cnb.client.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * <p>Panel with the mail text box.
 * <p>The SuggestBox widget is used to keep in cookies the email addresses.
 *
 * @author Pablo D. Sánchez
 *
 */
public class MailPanel extends Composite {
  private static final String MAIL_HEAD = "If you provide an email address, the results will be sent to your email when ready.";

  private static final long FORGETMEIN = 1000 * 60 * 60 * 72; // 72 hours

  private SuggestBox txtBoxMail;
  private MultiWordSuggestOracle oracle;

  private final Label lblMailErr;

  public MailPanel() {
    VerticalPanel verticalPanel = new VerticalPanel();
    initWidget(verticalPanel);

    Grid grid = new Grid(2, 2);
    grid.setStyleName("labelInfo");
    grid.setWidth("100%");
    grid.setCellPadding(3);
    verticalPanel.add(grid);

    Image image = new Image("images/icon/email.png");
    grid.setWidget(0, 0, image);

    HTML lblHead = new HTML(MAIL_HEAD);
    grid.setWidget(0, 1, lblHead);

    FlexTable flexTable = new FlexTable();
    grid.setWidget(1, 1, flexTable);

    Label lblMail = new Label("Email");
    //lblMail.setWidth("75px");
    lblMail.setStyleName("labelBold");
    flexTable.setWidget(0, 0, lblMail);

    oracle = new MultiWordSuggestOracle();
    txtBoxMail = new SuggestBox(oracle);
    updateSuggestBox();
    txtBoxMail.getTextBox().addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        txtBoxMail.showSuggestionList();
      }
    });
    flexTable.setWidget(0, 1, txtBoxMail);

    lblMailErr = new Label();
    flexTable.setWidget(0, 2, lblMailErr);

//    txtBoxMail.addKeyPressHandler(new KeyPressHandler() {
//      public void onKeyPress(KeyPressEvent event) {
//        lblMailErr.removeStyleName("labelError");
//        lblMailErr.setText("");
//      }
//    });
    txtBoxMail.getTextBox().addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        lblMailErr.removeStyleName("labelError");
        lblMailErr.setText("");
      }
    });
  }

  public String getMail() {
    String mail = txtBoxMail.getValue().trim();
    return (mail.isEmpty()) ? null : mail;
  }

  public void displayError() {
    lblMailErr.setStyleName("labelError");
    lblMailErr.setText("Please enter a valid email address");
    txtBoxMail.getTextBox().selectAll();
  }

  public void keepMail() {
    String username = txtBoxMail.getValue();
    if (username!=null && !username.equals("") && !cookieContains(username)){
      addToCookie(username);
      updateSuggestBox();
    }
  }

  private boolean cookieContains(String s){
    boolean contains = false;
    String users = Cookies.getCookie("usernames");
    if (users!=null){
      for (String username : users.split(":")){
        if (username.equals(s))
          contains= true;
      }
    }
    return contains;
  }

  private void addToCookie(String username){
    String users = Cookies.getCookie("usernames");
    if (users!=null)
      Cookies.setCookie("usernames", users+":"+username, new Date(new Date().getTime() + FORGETMEIN));
    else
      Cookies.setCookie("usernames", username, new Date(new Date().getTime() + FORGETMEIN));
  }

  private List<String> getUsernames(){
    List<String> usernames = new ArrayList<String>();
    String users = Cookies.getCookie("usernames");
    if (users!=null){
      for (String username : users.split(":")){
        usernames.add(username);
      }
    }
    return usernames;
  }

  private void updateSuggestBox(){
    oracle.clear();
    List<String> usernames = getUsernames();
    oracle.setDefaultSuggestionsFromText(usernames);
    for (String username : usernames){
      oracle.add(username);
    }
  }
}
