package es.csic.cnb.shared.error;

import java.io.Serializable;

import es.csic.cnb.shared.JobStatus;

/**
 *
 * @author Pablo D. Sánchez
 *
 */
public class ServerException extends Exception implements Serializable {
  private static final long serialVersionUID = 1L;

  private JobStatus status = null;

  public ServerException() {

  }

  public ServerException(String msg) {
    super(msg);
  }

  public ServerException(String msg, JobStatus status) {
    super(msg);
    this.status = status;
  }

  public ServerException(String msg, Throwable cause) {
    this(msg, null, cause);
  }

  public ServerException(String msg, JobStatus status, Throwable cause) {
    super(msg, cause);
    this.status = status;
  }

  public JobStatus getStatus() {
    return status;
  }

  public void setStatus(JobStatus status) {
    this.status = status;
  }
}
