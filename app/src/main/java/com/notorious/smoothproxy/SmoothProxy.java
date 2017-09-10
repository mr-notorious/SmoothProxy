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
import com.google.gson.JsonPrimitive;

import org.dom4j.Document;
import org.dom4j.Node;

import java.util.List;
import java.util.TreeMap;

import fi.iki.elonen.NanoHTTPD;

class SmoothProxy extends NanoHTTPD {
    private final String host;
    private final int port;
    private final Pipe pipe;
    private String username;
    private String password;
    private String service;
    private String server;
    private Document epg;
    private String auth;
    private long eTime;
    private long aTime;

    SmoothProxy(String host, int port, Pipe pipe) {
        super(host, port);
        this.host = host;
        this.port = port;
        this.pipe = pipe;
    }

    void login(String username, String password, String service, String server) {
        this.username = username;
        this.password = password;
        this.service = service;
        this.server = server;
        auth = null;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response res = super.serve(session);
        String uri = session.getUri();

        if (uri.equals("/epg.xml")) {
            pipe.setNotification("Now serving: EPG");
            res = newFixedLengthResponse(Response.Status.OK, "application/xml", getEpg().asXML());
            res.addHeader("Content-Disposition", "attachment; filename=\"epg.xml\"");

        } else if (uri.startsWith("/playlist.m3u8")) {
            List<String> ch = session.getParameters().get("ch");

            if (ch == null) {
                pipe.setNotification("Now serving: Playlist");
                res = newFixedLengthResponse(Response.Status.OK, "application/x-mpegURL", getM3U8());
                res.addHeader("Content-Disposition", "attachment; filename=\"playlist.m3u8\"");

            } else {
                pipe.setNotification("Now serving: Channel " + ch.get(0));
                res = newFixedLengthResponse(Response.Status.REDIRECT, "application/x-mpegURL", null);
                res.addHeader("Location", String.format("http://%s.smoothstreams.tv:9100/%s/ch%sq1.stream/playlist.m3u8?wmsAuthSign=%s==",
                        server, service, ch.get(0), getAuth()));
            }
        }
        return res;
    }

    private Document getEpg() {
        long NOW = System.currentTimeMillis();
        if (epg == null || eTime < NOW) {
            epg = Utils.getXml("http://sstv.fog.pt/feed.xml");
            eTime = NOW + 86100000;
        }
        return epg;
    }

    private String getM3U8() {
        JsonObject feed = Utils.getJson("http://fast-guide.smoothstreams.tv/feed.json");

        TreeMap<Integer, String> map = new TreeMap<>();
        for (Node node : getEpg().selectNodes("/tv/channel"))
            map.put(Integer.valueOf(node.valueOf("display-name")), node.valueOf("@id"));

        StringBuilder m3u8 = new StringBuilder("#EXTM3U\n");
        for (Integer ch : map.keySet()) {
            JsonObject jO = feed.getAsJsonObject("" + ch);

            String name = jO.getAsJsonPrimitive("name").getAsString().substring(5).trim();
            if (name.isEmpty()) name = "Empty";

            String img = jO.getAsJsonPrimitive("img").getAsString();
            if (!img.endsWith("png")) img = "http://mystreams.tv/wp-content/themes/mystreams/img/video-player.png";

            m3u8.append(String.format("#EXTINF:-1 tvg-id=\"%s\" tvg-name=\"%s\" tvg-logo=\"%s\",%s\nhttp://%s:%s/playlist.m3u8?ch=%s\n",
                    map.get(ch), ch, img, name, host, port, (ch < 10 ? "0" : "") + ch));
        }
        return m3u8.toString();
    }

    private String getAuth() {
        long NOW = System.currentTimeMillis();
        if (auth == null || aTime < NOW) {
            JsonPrimitive jP = Utils.getJson(String.format("http://%s?username=%s&password=%s&site=%s",
                    service.contains("mma") ? "www.mma-tv.net/loginForm.php" : "auth.smoothstreams.tv/hash_api.php",
                    Utils.encoder(username), Utils.encoder(password), service)).getAsJsonPrimitive("hash");
            if (jP != null) auth = jP.getAsString();
            aTime = NOW + 14100000;
        }
        return auth;
    }
}
