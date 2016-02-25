package es.csic.cnb.server;

import java.io.File;
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
 * This class prepare the email and send it.
 * <p>The email contains a link to the generated pathway page, and
 * the results file attached.
 *
 * @author Pablo D. Sánchez
 *
 */
public class SendMail {
  private static Logger log = Logger.getLogger(SendMail.class.getName());

  private static final String DATEFORMAT = "yyyy/MM/dd HH:mm:ss";

  private static final String mail_smtp_host = "rel7.cnb.csic.es"; //"www.cnb.csic.es";

  private Properties properties;

  public SendMail(Properties properties) {
    this.properties = properties;
  }

  public void sendMail(ClientData cdata, String filepath) throws EmailException {
    String filename = new File(filepath).getName();

    SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
    String initDate = sdf.format(cdata.getInitDate());

    // Create the attachment
    EmailAttachment attachment = new EmailAttachment();
    attachment.setPath(filepath);
    attachment.setDisposition(EmailAttachment.ATTACHMENT);
    attachment.setName(filename);

    // Create html message
    String html = mailHtmlTxt(cdata, filename, initDate);

    // Create alternative text message
    String txt = mailTxt(filename, initDate);

    try {
      // Create the email message
      HtmlEmail email = createRelayMail();
      email.setSubject("Combined model");
      email.addTo(cdata.getMail());
      // Set the html message
      email.setHtmlMsg(html);
      // Set the alternative message
      email.setTextMsg(txt);
      // Add the attachment
      email.attach(attachment);

      // Send the email
      email.send();

      // log
      log.info("Sending email using relay");
    }
    catch (EmailException e) {
      log.log(Level.WARNING, "Mail error (relay): {0} => use gmail", e.getMessage());

      // Send using gmail
      HtmlEmail email = createGMail();
      email.setSubject("Combined model");
      email.addTo(cdata.getMail());
      // Set the html message
      email.setHtmlMsg(html);
      // Set the alternative message
      email.setTextMsg(txt);
      // Add the attachment
      email.attach(attachment);

      // Send the email
      email.send();

      // log
      log.info("Sending email using gmail");
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
    //email.setSmtpPort(25);
    String user = properties.getProperty("mail.smtp.user");
    String pwd = properties.getProperty("mail.smtp.pwd");
    email.setAuthenticator(new DefaultAuthenticator(user, pwd));
    //email.setSSLOnConnect(true);
    email.setTLS(true);
    //email.setStartTLSEnabled(true);
    //email.setStartTLSRequired(false);
    email.setFrom(properties.getProperty("mail.smtp.user"), "cmodel");

    return email;
  }

  private String mailTxt(String filename, String initDate) {
    StringBuilder sb = new StringBuilder();
    sb.append("This email contains the combined model ").append(filename).append(" attached.\n");
    sb.append("Creation date: ").append(initDate).append(".\n");

    return sb.toString();
  }

  private String mailHtmlTxt(ClientData cdata, String filename, String initDate) {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta content=\"text/html; charset=ISO-8859-1\" http-equiv=\"content-type\">");
    html.append("</head>");
    html.append("<body>");
    html.append("This email contains the combined model <b>'").append(filename).append("'</b>");
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