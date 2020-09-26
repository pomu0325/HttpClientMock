package com.github.paweladamski.httpclientmock.action;

import com.github.paweladamski.httpclientmock.Request;
import java.io.IOException;
import org.apache.hc.core5.http.ClassicHttpResponse;

public class ExceptionAction implements Action {

  private final IOException exception;

  public ExceptionAction(IOException e) {
    this.exception = e;
  }

  @Override
  public ClassicHttpResponse getResponse(Request request) throws IOException {
    throw exception;
  }
}
