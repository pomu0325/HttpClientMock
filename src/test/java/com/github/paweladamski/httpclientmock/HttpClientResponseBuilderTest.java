package com.github.paweladamski.httpclientmock;

import static com.github.paweladamski.httpclientmock.Asserts.assertThrows;
import static com.github.paweladamski.httpclientmock.Requests.httpGet;
import static com.github.paweladamski.httpclientmock.Requests.httpPost;
import static com.github.paweladamski.httpclientmock.matchers.HttpResponseMatchers.hasContent;
import static com.github.paweladamski.httpclientmock.matchers.HttpResponseMatchers.hasCookie;
import static com.github.paweladamski.httpclientmock.matchers.HttpResponseMatchers.hasReason;
import static com.github.paweladamski.httpclientmock.matchers.HttpResponseMatchers.hasStatus;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_XML;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;

import com.github.paweladamski.httpclientmock.action.Action;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Test;

public class HttpClientResponseBuilderTest {

  @Test
  public void should_return_status_404_when_no_rule_matches() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock();
    HttpResponse notFound = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    assertThat(notFound, hasStatus(404));
  }

  @Test
  public void should_use_next_action_after_every_call() throws Exception {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost");

    httpClientMock.onGet("/foo")
        .doReturn("first")
        .doReturn("second")
        .doReturn("third");

    httpClientMock.onGet("/bar")
        .doReturn("bar")
        .doReturnStatus(300)
        .doThrowException(new IOException());

    HttpResponse response1 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response2 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response3 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response4 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response5 = httpClientMock.execute(new HttpGet("http://localhost/foo"));

    assertThat(response1, hasContent("first"));
    assertThat(response2, hasContent("second"));
    assertThat(response3, hasContent("third"));
    assertThat(response4, hasContent("third"));
    assertThat(response5, hasContent("third"));

    HttpResponse bar1 = httpClientMock.execute(new HttpGet("http://localhost/bar"));
    HttpResponse bar2 = httpClientMock.execute(new HttpGet("http://localhost/bar"));
    assertThat(bar1, hasContent("bar"));
    assertThat(bar2, hasStatus(300));

    assertThrows(IOException.class, () -> httpClientMock.execute(new HttpGet("http://localhost/bar")));

  }

  @Test
  public void should_support_response_in_different_charsets() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost");

    httpClientMock.onGet("/foo")
        .doReturn("first")
        .doReturn("second", StandardCharsets.UTF_16)
        .doReturn("third", StandardCharsets.US_ASCII);

    HttpResponse response1 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response2 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response3 = httpClientMock.execute(new HttpGet("http://localhost/foo"));

    assertThat(response1, hasContent("first", "UTF-8"));
    assertThat(response2, hasContent("second", "UTF-16"));
    assertThat(response3, hasContent("third", "ASCII"));
  }

  @Test
  public void should_support_response_with_different_contentType() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock
        .onGet("/json").doReturn("{\"a\":1}", Charset.defaultCharset(), APPLICATION_JSON);
    httpClientMock
        .onGet("/xml").doReturn("<a>1</a>", Charset.defaultCharset(), APPLICATION_XML);

    HttpResponse jsonResponse = httpClientMock.execute(httpGet("http://localhost:8080/json"));
    HttpResponse xmlResponse = httpClientMock.execute(httpGet("http://localhost:8080/xml"));

    assertThat(jsonResponse, hasContent("{\"a\":1}"));
    assertThat(jsonResponse.getFirstHeader("Content-type").getValue(), equalTo(APPLICATION_JSON.toString()));
    assertThat(ContentType.get(jsonResponse.getEntity()).toString(), equalTo(APPLICATION_JSON.toString()));

    assertThat(xmlResponse, hasContent("<a>1</a>"));
    assertThat(xmlResponse.getFirstHeader("Content-type").getValue(), equalTo(APPLICATION_XML.toString()));
    assertThat(ContentType.get(xmlResponse.getEntity()).toString(), equalTo(APPLICATION_XML.toString()));

  }

  @Test
  public void should_support_response_in_body_with_status() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost");

    httpClientMock.onGet("/foo")
        .doReturn("first")
        .doReturn(300, "second")
        .doReturn(400, "third");

    HttpResponse response1 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response2 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response3 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response4 = httpClientMock.execute(new HttpGet("http://localhost/foo"));
    HttpResponse response5 = httpClientMock.execute(new HttpGet("http://localhost/foo"));

    assertThat(response1, hasContent("first"));
    assertThat(response1, hasStatus(200));
    assertThat(response2, hasContent("second"));
    assertThat(response2, hasStatus(300));
    assertThat(response3, hasContent("third"));
    assertThat(response3, hasStatus(400));
    assertThat(response4, hasContent("third"));
    assertThat(response4, hasStatus(400));
    assertThat(response5, hasContent("third"));
    assertThat(response5, hasStatus(400));
  }

  @Test(expected = IOException.class)
  public void should_throw_exception_when_throwing_action_matched() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/foo").doThrowException(new IOException());
    httpClientMock.execute(new HttpGet("http://localhost:8080/foo"));
  }

  @Test
  public void should_return_status_corresponding_to_match() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");

    httpClientMock.onGet("/login").doReturnStatus(200);
    httpClientMock.onGet("/abc").doReturnStatus(404);
    httpClientMock.onGet("/error").doReturnStatus(500);

    HttpResponse ok = httpClientMock.execute(new HttpGet("http://localhost:8080/login"));
    HttpResponse notFound = httpClientMock.execute(new HttpGet("http://localhost:8080/abc"));
    HttpResponse error = httpClientMock.execute(new HttpGet("http://localhost:8080/error"));

    assertThat(ok, hasStatus(200));
    assertThat(notFound, hasStatus(404));
    assertThat(error, hasStatus(500));

  }

  @Test
  public void should_do_custom_action() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onPost("/login").doAction(echo());
    HttpResponse response = httpClientMock.execute(httpPost("http://localhost:8080/login", "foo bar"));

    assertThat(response, hasContent("foo bar"));

  }

  @Test
  public void should_add_header_to_response() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onPost("/login")
        .doReturn("foo").withHeader("tracking", "123")
        .doReturn("foo").withHeader("tracking", "456");

    HttpResponse first = httpClientMock.execute(httpPost("http://localhost:8080/login"));
    HttpResponse second = httpClientMock.execute(httpPost("http://localhost:8080/login"));

    assertThat(first.getFirstHeader("tracking").getValue(), equalTo("123"));
    assertThat(second.getFirstHeader("tracking").getValue(), equalTo("456"));
  }

  @Test
  public void should_add_cookie_to_context() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onPost("/login")
        .doReturn("foo").withCookie("cookieName", "cookieValue")
        .doReturn("foo").withCookie("cookieName", "cookieValue2");

    HttpClientContext httpClientContext = new HttpClientContext();
    httpClientMock.execute(httpPost("http://localhost:8080/login"), httpClientContext);
    assertThat(httpClientContext, hasCookie("cookieName", "cookieValue"));

    httpClientMock.execute(httpPost("http://localhost:8080/login"), httpClientContext);
    assertThat(httpClientContext, hasCookie("cookieName", "cookieValue2"));
  }

  @Test
  public void should_add_status_to_response() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/login")
        .doReturn("foo").withStatus(300);
    HttpResponse login = httpClientMock.execute(httpGet("http://localhost:8080/login"));

    assertThat(login, hasContent("foo"));
    assertThat(login, hasStatus(300));

  }

  @Test
  public void should_add_status_and_reason_to_response() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/login")
        .doReturn("foo").withStatus(400, "Bad request");
    HttpResponse login = httpClientMock.execute(httpGet("http://localhost:8080/login"));

    assertThat(login, hasContent("foo"));
    assertThat(login, hasStatus(400));
    assertThat(login, hasReason("Bad request"));
  }

  @Test
  public void should_return_json_with_right_header() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/login")
        .doReturnJSON("{foo:1}", StandardCharsets.UTF_8);
    HttpResponse login = httpClientMock.execute(httpGet("http://localhost:8080/login"));

    assertThat(login, hasContent("{foo:1}"));
    assertThat(login.getFirstHeader("Content-type").getValue(), equalTo(APPLICATION_JSON.toString()));
  }

  @Test
  public void should_return_json_with_right_content_type() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/login")
        .doReturnJSON("{foo:1}", StandardCharsets.UTF_8);
    HttpResponse login = httpClientMock.execute(httpGet("http://localhost:8080/login"));

    assertThat(login, hasContent("{foo:1}"));
    assertThat(ContentType.get(login.getEntity()).toString(), equalTo(APPLICATION_JSON.toString()));
  }

  @Test
  public void should_return_xml_with_right_header() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/login")
        .doReturnXML("<foo>bar</foo>", StandardCharsets.UTF_8);
    HttpResponse login = httpClientMock.execute(httpGet("http://localhost:8080/login"));

    assertThat(login, hasContent("<foo>bar</foo>"));
    assertThat(login.getFirstHeader("Content-type").getValue(), equalTo(APPLICATION_XML.toString()));
  }

  @Test
  public void should_return_xml_with_right_content_type() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/login")
        .doReturnXML("<foo>bar</foo>", StandardCharsets.UTF_8);
    HttpResponse login = httpClientMock.execute(httpGet("http://localhost:8080/login"));

    assertThat(login, hasContent("<foo>bar</foo>"));
    assertThat(ContentType.get(login.getEntity()).toString(), equalTo(APPLICATION_XML.toString()));
  }

  @Test
  public void should_not_set_response_entity_when_status_is_no_content() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onGet("/login")
        .doReturnStatus(SC_NO_CONTENT);

    HttpResponse login = httpClientMock.execute(httpGet("http://localhost:8080/login"));

    assertNull(login.getEntity());
  }

  @Test
  public void should_not_throw_exception_when_body_matcher_is_present_on_post_request() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");
    httpClientMock.onPost("/path1")
        .withBody(equalTo("Body content"))
        .doReturnStatus(200);

    HttpResponse response = httpClientMock.execute(httpGet("http://localhost:8080/path2"));
    Assert.assertThat(response, hasStatus(SC_NOT_FOUND));
  }

  @Test
  public void doReturnFormParams_should_returnResponseEntityWithFormParameters() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");

    List<NameValuePair> expected = Arrays.asList(
        new BasicNameValuePair("one", "1"),
        new BasicNameValuePair("two", "2")
    );
    httpClientMock.onGet("/path1").doReturnFormParams(expected);

    HttpResponse response = httpClientMock.execute(httpGet("http://localhost:8080/path1"));
    List<NameValuePair> actual = URLEncodedUtils.parse(response.getEntity());

    assertThat(response, hasStatus(200));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void doReturnFormParams_empty() throws IOException {
    HttpClientMock httpClientMock = new HttpClientMock("http://localhost:8080");

    List<NameValuePair> expected = Collections.emptyList();
    httpClientMock.onGet("/path1").doReturnFormParams(expected);

    HttpResponse response = httpClientMock.execute(httpGet("http://localhost:8080/path1"));
    List<NameValuePair> actual = URLEncodedUtils.parse(response.getEntity());

    assertThat(response, hasStatus(200));
    Assert.assertEquals(expected, actual);
  }

  private Action echo() {
    return r -> {
      HttpEntity entity = ((HttpEntityEnclosingRequestBase) r.getHttpRequest()).getEntity();
      BasicHttpResponse response = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "ok");
      response.setEntity(entity);
      return response;
    };
  }
}


