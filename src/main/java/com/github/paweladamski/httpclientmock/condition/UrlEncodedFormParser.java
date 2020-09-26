package com.github.paweladamski.httpclientmock.condition;

import com.github.paweladamski.httpclientmock.Request;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

public class UrlEncodedFormParser {

  List<NameValuePair> parse(Request request) {
    if (!requestHasBody(request)) {
      return Collections.emptyList();
    }

    HttpEntityContainer httpRequest = (HttpEntityContainer) request.getHttpRequest();
    HttpEntity entity = httpRequest.getEntity();
    if (entity == null) {
      return Collections.emptyList();
    }

    try {
      /*
       * The method below returns an empty list if the Content-Type of the
       * request is not "application/x-www-form-urlencoded". So, requests with
       * other kinds of data in the body will correctly be ignored here.
       */
      Scanner s = new Scanner(entity.getContent()).useDelimiter("\\A");
      String result = s.hasNext() ? s.next() : "";
      return URLEncodedUtils.parse(result, Charset.defaultCharset());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean requestHasBody(Request r) {
    return (r.getHttpRequest() instanceof HttpEntityContainer);
  }
}
