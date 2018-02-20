package com.github.paweladamski.httpclientmock;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class UrlParams extends ArrayList<NameValuePair> {

    public static UrlParams parse(String query) {
        return parse(query, Charset.forName("UTF-8"));
    }

    public static UrlParams parse(String query, Charset charset) {
        if (query == null) {
            return new UrlParams();
        } else {
            UrlParams urlParams = new UrlParams();
            urlParams.addAll(URLEncodedUtils.parse(query, charset));
            return urlParams;
        }
    }

    boolean contain(String name) {
        return stream()
                .filter(p -> p.getName().equals(name))
                .findAny()
                .isPresent();
    }
}
