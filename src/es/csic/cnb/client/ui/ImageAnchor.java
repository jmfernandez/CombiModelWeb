package es.csic.cnb.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Image;

public class ImageAnchor extends Anchor {

    public ImageAnchor(String url, String href) {
      super();

      super.setHref(href);

      Image img = new Image(url);
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
}
