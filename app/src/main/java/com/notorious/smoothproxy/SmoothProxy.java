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
    private boolean epg;
    private String auth;
    private long time;

    SmoothProxy(String host, int port, Pipe pipe) {
        super(host, port);
        this.host = host;
        this.port = port;
        this.pipe = pipe;
    }

    void init(String username, String password, String service, String server, int quality, boolean epg) {
        this.username = username;
        this.password = password;
        this.service = service;
        this.server = server;
        this.quality = quality;
        this.epg = epg;
        auth = null;
        time = 0;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response res = super.serve(session);

        String uri = session.getUri();
        if (uri.equals("/epg.xml")) {
            pipe.setNotification("Now serving: EPG");
            res = newFixedLengthResponse(Response.Status.REDIRECT, "application/xml", null);
            res.addHeader("Location", epg ? "http://sstv.fog.pt/feed.xml" : "http://speed.guide.smoothstreams.tv/feed.xml");

        } else if (uri.startsWith("/playlist.m3u8")) {
            List<String> ch = session.getParameters().get("ch");

            if (ch == null) {
                pipe.setNotification("Now serving: Playlist");
                res = newFixedLengthResponse(Response.Status.OK, "application/x-mpegURL", getM3U8());
                res.addHeader("Content-Disposition", "attachment; filename=\"playlist.m3u8\"");

            } else {
                pipe.setNotification("Now serving: Channel " + ch.get(0));
                res = newFixedLengthResponse(Response.Status.REDIRECT, "application/x-mpegURL", null);
                res.addHeader("Location", String.format("http://%s.smoothstreams.tv:9100/%s/ch%sq%s.stream/playlist.m3u8?wmsAuthSign=%s==",
                        server, service, ch.get(0), quality, getAuth()));
            }
        }
        return res;
    }

    private String getAuth() {
        long NOW = System.currentTimeMillis();
        if (auth == null || time < NOW) {
            JsonObject jO = Utils.getJson(String.format("http://%s?username=%s&password=%s&site=%s",
                    service.contains("mma") ? "www.mma-tv.net/loginForm.php" : "auth.smoothstreams.tv/hash_api.php",
                    Utils.encoder(username), Utils.encoder(password), service));

            if (jO != null && jO.getAsJsonPrimitive("code").getAsInt() == 1) {
                auth = jO.getAsJsonPrimitive("hash").getAsString();
                time = NOW + 14100000;
            }
        }
        return auth;
    }

    private String getM3U8() {
        StringBuilder m3u8 = new StringBuilder("#EXTM3U\n");

        JsonObject map = Utils.getJson("http://sstv.fog.pt/channels.json");
        if (map != null) for (String key : map.keySet()) {
            JsonObject jO = map.getAsJsonObject(key);

            String id = jO.getAsJsonPrimitive("zap2it").getAsString();
            String ch = jO.getAsJsonPrimitive("channum").getAsString();
            String icon = jO.getAsJsonPrimitive("icon").getAsString();
            String name = jO.getAsJsonPrimitive("channame").getAsString();

            m3u8.append(String.format("#EXTINF:-1 tvg-id=\"%s\" tvg-logo=\"%s\",%s\nhttp://%s:%s/playlist.m3u8?ch=%s\n",
                    epg ? id : ch, icon, !name.isEmpty() ? name : "Empty", host, port, ch.length() == 1 ? "0" + ch : ch));
        }
        return m3u8.toString();
    }
}
