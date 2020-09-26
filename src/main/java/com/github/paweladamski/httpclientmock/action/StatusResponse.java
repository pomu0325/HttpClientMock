package com.github.paweladamski.httpclientmock.action;

import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;

import com.github.paweladamski.httpclientmock.Request;
import java.io.IOException;
import java.util.Optional;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;

public class StatusResponse implements Action {

  private final Optional<Action> parentAction;
  private final int status;

  public StatusResponse(int status) {
    this.status = status;
    this.parentAction = Optional.empty();
  }

  public StatusResponse(Action parentAction, int status) {
    this.status = status;
    this.parentAction = Optional.of(parentAction);
  }

  @Override
  public ClassicHttpResponse getResponse(Request request) throws IOException {
    ClassicHttpResponse response;
    if (parentAction.isPresent()) {
      response = parentAction.get().getResponse(request);
    } else {
      response = new BasicClassicHttpResponse(status, "");
      if (status != SC_NO_CONTENT) {
        response.setEntity(new StringEntity(""));
      }
    }
    response.setCode(status);
    return response;
  }
}
