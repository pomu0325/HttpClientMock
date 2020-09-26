package com.github.paweladamski.httpclientmock;

import static java.util.Collections.emptyList;
import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;

import com.github.paweladamski.httpclientmock.action.Action;
import com.github.paweladamski.httpclientmock.action.StatusResponse;
import com.github.paweladamski.httpclientmock.condition.Condition;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

public class Rule {

  public static final Rule NOT_FOUND = new Rule(new UrlConditions(), emptyList(), notFoundAction());
  private final LinkedList<Action> actions;
  private final List<Condition> conditions;
  private final UrlConditions urlConditions;

  public Rule(UrlConditions urlConditions, List<Condition> conditions, List<Action> actions) {
    this.urlConditions = urlConditions;
    this.conditions = conditions;
    this.actions = new LinkedList<>(actions);
  }

  boolean matches(Request request) {
    return matches(request.getHttpHost(), request.getHttpRequest(), request.getHttpContext());
  }

  boolean matches(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) {
    try {
      return urlConditions.matches(httpRequest.getUri())
          && conditions.stream()
          .allMatch(c -> c.matches(httpHost, httpRequest, httpContext));
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return false;
    }
  }

  ClassicHttpResponse nextResponse(Request request) throws IOException {
    Action action = (actions.size() > 1) ? actions.poll() : actions.peek();
    return action.getResponse(request);
  }

  public void debug(Request request, Debugger debugger) {
    for (Condition condition : conditions) {
      condition.debug(request, debugger);
    }
    urlConditions.debug(request, debugger);
  }

  private static List<Action> notFoundAction() {
    ArrayList<Action> actions = new ArrayList<>();
    actions.add(new StatusResponse(SC_NOT_FOUND));
    return actions;
  }

}
