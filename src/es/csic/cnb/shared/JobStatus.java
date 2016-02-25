package es.csic.cnb.shared;

import java.io.Serializable;

/**
 * <p>Class responsible for storing the status data.
 * <p>The server stores here the status information from the merge process
 * and the client display the information stored.
 *
 * @author Pablo D. Sánchez
 *
 */
public class JobStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  private String info = "";

  public JobStatus() {}

  /**
   * Log text.
   *
   * @return the log text.
   */
  public String getInfo() {
    return info;
  }

  /**
   * Append the log text.
   *
   * @param info - the text to append.
   */
  public void setInfo(String info) {
    this.info = this.info + info;
  }
}
