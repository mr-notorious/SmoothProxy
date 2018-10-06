/*
    MIT License

    Copyright (c) 2018 mr-notorious

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

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

final class HttpClient {
    private static final Gson PARSER = new Gson();
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    static String decode(String text) {
        return Parser.unescapeEntities(text, false);
    }

    static String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Content getContent(String url) {
        try {
            ResponseBody rB = getResponseBody(url);
            MediaType mT = rB.contentType();
            return new Content(rB.byteStream(), rB.contentLength(), mT != null ? mT.toString() : "text/plain");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static JsonObject getJson(String url) {
        try {
            ResponseBody rB = getResponseBody(url);
            return PARSER.fromJson(rB.charStream(), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ResponseBody getResponseBody(String url) throws Exception {
        return CLIENT.newCall(new Request.Builder().url(url).build()).execute().body();
    }

    static final class Content {
        final InputStream response;
        final long length;
        final String type;

        Content(InputStream response, long length, String type) {
            this.response = response;
            this.length = length;
            this.type = type;
        }
    }
}
