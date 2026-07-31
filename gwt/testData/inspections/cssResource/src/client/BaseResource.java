package client;

import com.google.gwt.resources.client.CssResource;

public interface BaseResource extends CssResource {
  String baseClass();

  String unresolvedBaseClass();
}