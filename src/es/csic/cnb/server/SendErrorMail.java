package es.csic.cnb.server;

import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import es.csic.cnb.shared.ClientData;
import es.csic.cnb.shared.ModelData;

/**
 * This class prepare the email and send it when an exception is thrown.
 *
 * @author Pablo D. Sánchez
 *
 */
public class SendErrorMail {
  private static Logger log = Logger.getLogger(SendErrorMail.class.getName());

  private static final String DATEFORMAT = "yyyy/MM/dd HH:mm:ss";

  private static final String mail_smtp_host = "rel7.cnb.csic.es"; //"www.cnb.csic.es";

  private Properties properties;

  public SendErrorMail(Properties properties) {
    this.properties = properties;
  }

  public void sendMail(String msg, ClientData cdata) {
    SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
    String initDate = sdf.format(cdata.getInitDate());

    // Create html message
    String html = mailHtmlTxt(cdata, initDate, msg);

    // Create alternative text message
    String txt = mailTxt(initDate, msg);

    try {
      // Create the email message
      HtmlEmail email = createRelayMail();
      email.setSubject("Combination model error");
      email.addTo(cdata.getMail());
      // Set the html message
      email.setHtmlMsg(html);
      // Set the alternative message
      email.setTextMsg(txt);

      // Send the email
      email.send();

      // log
      log.log(Level.INFO, "Sending notification error email to {0} (user) using relay", cdata.getMail());
    }
    catch (EmailException e) {
      log.log(Level.WARNING, "Mail error (relay): {0} => use gmail", e.getMessage());
      log.log(Level.WARNING, "Mail error (relay)", e);

      // Send using gmail
      try {
        HtmlEmail email = createGMail();
        email.setSubject("Combination model error");
        email.addTo(cdata.getMail());
        // Set the html message
        email.setHtmlMsg(html);
        // Set the alternative message
        email.setTextMsg(txt);

        // Send the email
        email.send();

        // log
        log.log(Level.INFO, "Sending notification error email to {0} (user) using gmail", cdata.getMail());
      }
      catch (EmailException e1) {
        log.log(Level.SEVERE, "Mail error (gmail): {0} => User must be informed using other ways", e.getMessage());
      }
    }
  }

  public void notifyDeveloper(String sessionId, Throwable ex, ClientData cdata) {
    SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
    String initDate = sdf.format(cdata.getInitDate());

    // Create html message
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta content=\"text/html; charset=ISO-8859-1\" http-equiv=\"content-type\">");
    html.append("</head>");
    html.append("<body>");
    html.append("<br><span style=\"font-weight: bold;\">Session ID: </span> ").append(sessionId);
    html.append("<br>");
    html.append("<br>");
    html.append("<br><span style=\"font-weight: bold;\">Creation date: </span> ").append(initDate);
    html.append("<br>");
    html.append("<br><span style=\"font-weight: bold;\">User email: </span> ").append(cdata.getMail());
    html.append("<br>");
    for (ModelData md : cdata.getModelDataList()) {
      html.append("<br><span style= \"font-weight: bold;\">Model: </span> ").append(md.getFilename());
    }
    html.append("<br>");

    int mm = cdata.getMinmedia();
    if (mm == ClientData.BASICMM) {
      html.append("<br><span style=\"font-weight: bold;\">- Basic minimum media selected</span> ");
    }
    else if (mm == ClientData.INTERSECMM) {
      html.append("<br><span style=\"font-weight: bold;\">- Intersection minimum media selected</span> ");
    }
    else if (mm == ClientData.COMBIMM) {
      html.append("<br><span style=\"font-weight: bold;\">- Combination minimum media selected</span> ");
    }

    if (cdata.isNewobj()) {
      html.append("<br><span style=\"font-weight: bold;\">- Biomass joint function selected</span> ");
    }
    html.append("<br>");
    html.append("<br><span style=\"font-weight: bold;\">Log: </span> ").append( ex.toString());
    html.append("<br>");
    for (StackTraceElement ste : ex.getStackTrace()) {
      html.append("<br>").append(ste.toString());
    }
    html.append("</body>");
    html.append("</html>");

    // Create alternative text message
    StringBuilder txt = new StringBuilder();
    txt.append("Session ID: ").append(sessionId).append("\n");
    txt.append("Creation date: ").append(initDate).append("\n");

    try {
      // Create the email message
      HtmlEmail email = createRelayMail();
      email.setSubject("Combination model error");
      email.addTo(properties.getProperty("mail.error.user"));
      // Set the html message
      email.setHtmlMsg(html.toString());
      // Set the alternative message
      email.setTextMsg(txt.toString());
      // Add the attachment
      EmailAttachment attachment = new EmailAttachment();
      for (ModelData md : cdata.getModelDataList()) {
        attachment.setPath(md.getFilepath());
        attachment.setDisposition(EmailAttachment.ATTACHMENT);
        attachment.setName(md.getFilename());
        email.attach(attachment);
      }

      // Send the email
      email.send();

      // log
      log.log(Level.INFO, "Sending notification error email to {0} (dev) using relay",
              properties.getProperty("mail.error.user"));
    }
    catch (EmailException e) {
      log.log(Level.WARNING, "Mail error (relay): {0} => use gmail", e.getMessage());
      log.log(Level.WARNING, "Mail error (relay)", e);

      // Send using gmail
      try {
        HtmlEmail email = createGMail();
        email.setSubject("Combination model error");
        email.addTo(properties.getProperty("mail.error.user"));
        // Set the html message
        email.setHtmlMsg(html.toString());
        // Set the alternative message
        email.setTextMsg(txt.toString());
        // Add the attachment
        EmailAttachment attachment = new EmailAttachment();
        for (ModelData md : cdata.getModelDataList()) {
          attachment.setPath(md.getFilepath());
          attachment.setDisposition(EmailAttachment.ATTACHMENT);
          attachment.setName(md.getFilename());
          email.attach(attachment);
        }

        // Send the email
        email.send();

        // log
        log.log(Level.INFO, "Sending notification error email to {0} (dev) using gmail",
                properties.getProperty("mail.error.user"));
      }
      catch (EmailException e1) {
        log.log(Level.SEVERE, "Mail error (gmail): {0} => User must be informed using other ways", e.getMessage());
      }
    }
  }

  private HtmlEmail createRelayMail() throws EmailException {
    HtmlEmail email = new HtmlEmail();
    email.setHostName(mail_smtp_host);
    email.setFrom("mailservice@botero.cnb.csic.es", "cmodel");

    return email;
  }

  private HtmlEmail createGMail() throws EmailException {
    HtmlEmail email = new HtmlEmail();
    email.setHostName(properties.getProperty("mail.smtp.host"));
    email.setSmtpPort(587);
    String user = properties.getProperty("mail.smtp.user");
    String pwd = properties.getProperty("mail.smtp.pwd");
    email.setAuthenticator(new DefaultAuthenticator(user, pwd));
    email.setTLS(true);
    //email.setStartTLSEnabled(true);
    email.setFrom(properties.getProperty("mail.smtp.user"), "cmodel");

    return email;
  }



  private String mailTxt(String initDate, String msg) {
    StringBuilder sb = new StringBuilder();
    sb.append(msg);
    sb.append("Creation date: ").append(initDate).append(".\n");

    return sb.toString();
  }

  private String mailHtmlTxt(ClientData cdata, String initDate, String msg) {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta content=\"text/html; charset=ISO-8859-1\" http-equiv=\"content-type\">");
    html.append("</head>");
    html.append("<body>");
    html.append(msg);
    html.append("<br>Please try again in a few minutes.");
    html.append("<br>");
    html.append("<br><span style=\"font-weight: bold;\">Creation date: </span> ").append(initDate);
    html.append("<br>");
    for (ModelData md : cdata.getModelDataList()) {
      html.append("<br><span style= \"font-weight: bold;\">Model: </span> ").append(md.getFilename());
    }
    html.append("<br>");

    int mm = cdata.getMinmedia();
    if (mm == ClientData.BASICMM) {
      html.append("<br><span style=\"font-weight: bold;\">- Basic minimum media selected</span> ");
    }
    else if (mm == ClientData.INTERSECMM) {
      html.append("<br><span style=\"font-weight: bold;\">- Intersection minimum media selected</span> ");
    }
    else if (mm == ClientData.COMBIMM) {
      html.append("<br><span style=\"font-weight: bold;\">- Combination minimum media selected</span> ");
    }

    if (cdata.isNewobj()) {
      html.append("<br><span style=\"font-weight: bold;\">- Biomass joint function selected</span> ");
    }
    html.append("<br>");
    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }
}