package com.github.paweladamski.httpclientmock.action;

import com.github.paweladamski.httpclientmock.Request;
import java.io.IOException;
import org.apache.hc.core5.http.ClassicHttpResponse;

public class HeaderAction implements Action {

  private final Action parentAction;
  private final String name;
  private final String value;

  public HeaderAction(Action parentAction, String name, String value) {
    this.parentAction = parentAction;
    this.name = name;
    this.value = value;
  }

  @Override
  public ClassicHttpResponse getResponse(Request r) throws IOException {
    ClassicHttpResponse response = parentAction.getResponse(r);
    response.addHeader(name, value);
    return response;
  }
}
