/*
    MIT License

    Copyright (c) 2020 mr-notorious

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
 */

package com.notorious.smoothproxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jsoup.parser.Parser;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.dnsoverhttps.DnsOverHttps;

final class HttpClient {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().dns(new DnsOverHttps.Builder().client(new OkHttpClient()).url(HttpUrl.get("https://cloudflare-dns.com/dns-query")).includeIPv6(false).resolvePrivateAddresses(true).build()).build();
    private static final Gson PARSER = new Gson();

    static String decode(String text) {
        return Parser.unescapeEntities(text, false);
    }

    static ResponseBody getBody(String url) {
        try {
            return call(url);
        } catch (Exception e) {
            return null;
        }
    }

    static JsonObject getJson(String url) {
        try {
            return PARSER.fromJson(call(url).charStream(), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    static JsonObject getJson(String url, RequestBody body) {
        try {
            return PARSER.fromJson(call(url, body).charStream(), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static ResponseBody call(String url) throws Exception {
        return call(new Request.Builder().url(HttpUrl.get(url)).build());
    }

    private static ResponseBody call(String url, RequestBody body) throws Exception {
        return call(new Request.Builder().url(HttpUrl.get(url)).post(body).build());
    }

    private static ResponseBody call(Request request) throws Exception {
        Response response = CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) throw new Exception();
        return response.body();
    }
}
