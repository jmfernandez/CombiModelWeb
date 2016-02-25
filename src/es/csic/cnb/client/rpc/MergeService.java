package es.csic.cnb.client.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import es.csic.cnb.shared.ClientData;
import es.csic.cnb.shared.error.ServerException;

@RemoteServiceRelativePath("merger")
public interface MergeService extends RemoteService {
  ClientData run(String sessionId, ClientData cdata) throws ServerException;
}
