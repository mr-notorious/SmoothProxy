/*
    MIT License

    Copyright (c) 2017 mr-notorious

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

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

final class HttpClient {
    private static final SAXReader READER = new SAXReader();
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

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
            return new Content(rB.byteStream(), rB.contentLength(), rB.contentType().toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static JsonObject getJson(String url) {
        try {
            ResponseBody rB = getResponseBody(url);
            return new Gson().fromJson(rB.charStream(), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Document getXml(String url) {
        try {
            READER.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return systemId.contains("xmltv.dtd") ? new InputSource(new StringReader("")) : null;
                }
            });
            return READER.read(url);
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
