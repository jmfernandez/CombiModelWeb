package es.csic.cnb.client.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;

import es.csic.cnb.shared.ClientData;

public interface MergeServiceAsync {
  void run(String sessionId, ClientData cdata, AsyncCallback<ClientData> callback);
}
