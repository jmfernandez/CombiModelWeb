package es.csic.cnb.shared;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ClientData implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final int DEFAULTMM  = 1;
  public static final int BASICMM    = 2;
  public static final int INTERSECMM = 3;
  public static final int COMBIMM    = 4;

  private LinkedList<ModelData> modelDataList;
  private int minmedia;
  private boolean newobj;
  private String mail;
  private Date initDate;

  private String cmodelPath;

  public ClientData() {}

  public ClientData(Collection<ModelData> modelDataList, int minmedia, boolean newobj) {
    this(modelDataList, minmedia, newobj, null);
  }

  public ClientData(Collection<ModelData> modelDataList, int minmedia, boolean newobj, String mail) {
    this.modelDataList = new LinkedList<ModelData>();
    this.modelDataList.addAll(modelDataList);

    this.minmedia = minmedia;
    this.newobj = newobj;
    this.mail = mail;

    this.initDate = new Date();
  }

  /**
   * @return the modelDataList
   */
  public List<ModelData> getModelDataList() {
    return modelDataList;
  }

  /**
   * @return the minmedia
   */
  public int getMinmedia() {
    return minmedia;
  }

  /**
   * @return the newobj
   */
  public boolean isNewobj() {
    return newobj;
  }

  /**
   * @return the mail
   */
  public String getMail() {
    return mail;
  }

  /**
   * @return the initDate
   */
  public Date getInitDate() {
    return initDate;
  }

  /**
   * @return the cmodelPath
   */
  public String getCmodelPath() {
    return cmodelPath;
  }

  /**
   * @param cmodelPath the cmodelPath to set
   */
  public void setCmodelPath(String cmodelPath) {
    this.cmodelPath = cmodelPath;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("CDATA: ").append(minmedia);
    sb.append(" - ").append(newobj);
    return sb.toString();
  }
}
