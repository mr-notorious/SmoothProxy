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

import com.google.gson.JsonObject;

import java.util.List;

import fi.iki.elonen.NanoHTTPD;

class SmoothProxy extends NanoHTTPD {
    private final String host;
    private final int port;
    private final Pipe pipe;
    private String username;
    private String password;
    private String service;
    private String server;
    private int quality;
    private String path;
    private String auth;
    private long time;

    SmoothProxy(String host, int port, Pipe pipe) {
        super(host, port);
        this.host = host;
        this.port = port;
        this.pipe = pipe;
    }

    void init(String username, String password, String service, String server, int quality) {
        this.username = username;
        this.password = password;
        this.service = service;
        this.server = server;
        this.quality = quality;
        auth = null;
        time = 0;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response res = super.serve(session);

        String uri = session.getUri();
        if (uri.equals("/epg.xml")) {
            pipe.setNotification("Now serving: EPG");
            res = newChunkedResponse(Response.Status.OK, "application/xml", Utils.getInputStream("http://sstv.fog.pt/feed.xml"));

        } else if (uri.equals("/playlist.m3u8")) {
            List<String> ch = session.getParameters().get("ch");

            if (ch == null) {
                pipe.setNotification("Now serving: Playlist");
                res = newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", getM3U8());

            } else {
                pipe.setNotification("Now serving: Channel " + ch.get(0));
                path = String.format("https://%s.smoothstreams.tv/%s/ch%sq%s.stream", server, service, ch.get(0), quality);
                res = newChunkedResponse(Response.Status.OK, "application/vnd.apple.mpegurl", Utils.getInputStream(String.format("%s%s?wmsAuthSign=%s", path, uri, getAuth())));
            }

        } else if (uri.equals("/chunks.m3u8")) {
            res = newChunkedResponse(Response.Status.OK, "application/vnd.apple.mpegurl", Utils.getInputStream(String.format("%s%s?%s", path, uri, session.getQueryParameterString())));

        } else if (uri.endsWith(".ts")) {
            res = newChunkedResponse(Response.Status.OK, "video/m2ts", Utils.getInputStream(String.format("%s%s?%s", path, uri, session.getQueryParameterString())));
        }
        return res;
    }

    private String getAuth() {
        long now = System.currentTimeMillis();
        if (auth == null || time < now) {
            JsonObject jO = Utils.getJsonObject(String.format("https://%s?username=%s&password=%s&site=%s",
                    service.contains("mma") ? "www.mma-tv.net/loginForm.php" : "auth.smoothstreams.tv/hash_api.php", Utils.encoder(username), Utils.encoder(password), service));

            if (jO != null && jO.getAsJsonPrimitive("code").getAsInt() == 1) {
                auth = jO.getAsJsonPrimitive("hash").getAsString();
                time = now + 14100000;
            }
        }
        return auth;
    }

    private String getM3U8() {
        String m3u8 = "#EXTM3U\n";

        JsonObject map = Utils.getJsonObject("http://sstv.fog.pt/channels.json");
        if (map != null) for (String key : map.keySet()) {
            JsonObject jO = map.getAsJsonObject(key);

            String id = jO.getAsJsonPrimitive("xmltvid").getAsString();
            String num = jO.getAsJsonPrimitive("channum").getAsString();
            String name = jO.getAsJsonPrimitive("channame").getAsString();
            String ch = num.length() == 1 ? "0" + num : num;

            m3u8 += String.format("#EXTINF:-1 tvg-id=\"%s\" tvg-logo=\"https://guide.smoothstreams.tv/assets/images/channels/%s.png\",%s\nhttp://%s:%s/playlist.m3u8?ch=%s\n",
                    id, num, name, host, port, ch);
        }
        return m3u8;
    }
}
