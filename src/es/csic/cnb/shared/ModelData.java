package es.csic.cnb.shared;

import java.io.Serializable;

public class ModelData implements Serializable {
  private static final long serialVersionUID = 1L;

  private String ctype;
  private String field;
  private String filename;
  private String filepath;
  private String minmedia;
  private int size;
  private int coef = 1;
  private double ph = 7.2;

  private boolean error = false;


  /**
   * @return the ctype
   */
  public String getCtype() {
    return ctype;
  }
  /**
   * @param ctype the ctype to set
   */
  public void setCtype(String ctype) {
    this.ctype = ctype;
  }
  /**
   * @return the field
   */
  public String getField() {
    return field;
  }
  /**
   * @param field the field to set
   */
  public void setField(String field) {
    this.field = field;
  }
  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }
  /**
   * @param filename the filename to set
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }
  /**
   * @return the filepath
   */
  public String getFilepath() {
    return filepath;
  }
  /**
   * @param filepath the filepath to set
   */
  public void setFilepath(String filepath) {
    this.filepath = filepath;
  }
  /**
   * @return the minmedia
   */
  public String getMinmedia() {
    return minmedia;
  }
  /**
   * @param minmedia the minmedia to set
   */
  public void setMinmedia(String minmedia) {
    this.minmedia = minmedia;
  }
  /**
   * @return the size
   */
  public int getSize() {
    return size;
  }
  /**
   * @param size the size to set
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * @return the coef
   */
  public int getCoef() {
    return coef;
  }
  /**
   * @param coef the coef to set
   */
  public void setCoef(int coef) {
    this.coef = coef;
  }
  /**
   * @return the ph
   */
  public double getPh() {
    return ph;
  }
  /**
   * @param ph the ph to set
   */
  public void setPh(double ph) {
    this.ph = ph;
  }
  /**
   * @return the error
   */
  public boolean isError() {
    return error;
  }

  /**
   * @param error the error to set
   */
  public void setError(boolean error) {
    this.error = error;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(field).append(" >>> ").append(filename);
    sb.append(" [").append(filepath).append("] ").append(coef);
    return sb.toString();
  }
}
