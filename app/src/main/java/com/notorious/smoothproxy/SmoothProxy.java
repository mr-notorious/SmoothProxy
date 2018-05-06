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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import fi.iki.elonen.NanoHTTPD;

final class SmoothProxy extends NanoHTTPD {
    private final String host;
    private final int port;
    private final Ipc ipc;
    private String username;
    private String password;
    private String service;
    private String server;
    private int quality;
    private String url;
    private String auth;
    private long time;

    SmoothProxy(String host, int port, Ipc ipc) {
        super(host, port);
        this.host = host;
        this.port = port;
        this.ipc = ipc;
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
        String txt = null;

        String path = session.getUri();
        if (path.endsWith(".ts") || path.equals("/chunks.m3u8")) {
            res = getResponse(url + path + "?" + session.getQueryParameterString());

        } else if (path.equals("/playlist.m3u8")) {
            List<String> ch = session.getParameters().get("ch");

            if (ch != null) {
                url = "https://" + server + ".smoothstreams.tv/" + service + "/ch" + ch.get(0) + "q" + (Integer.valueOf(ch.get(0)) < 61 ? quality : 1) + ".stream";
                res = getResponse(url + path + "?wmsAuthSign=" + getAuth());

            } else {
                res = getPlaylist();
                txt = "Playlist";
            }

        } else if (path.equals("/sports.m3u8")) {
            res = getSports();
            txt = "Playlist";

        } else if (path.equals("/epg.xml.gz")) {
            res = getResponse("https://guide.smoothstreams.tv/altepg/xmltv2.xml.gz");
            txt = "EPG";

        } else if (path.equals("/sports.xml")) {
            res = getResponse("https://guide.smoothstreams.tv/feed.xml");
            txt = "EPG";
        }

        if (txt != null) ipc.setNotification("Now serving: " + txt);
        res.addHeader("Access-Control-Allow-Origin", "*");
        return res;
    }

    private Response getResponse(String url) {
        HttpClient.Content c = HttpClient.getContent(url);
        return c != null
                ? newFixedLengthResponse(Response.Status.OK, c.type, c.response, c.length)
                : newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 NOT FOUND");
    }

    private String getAuth() {
        long now = System.currentTimeMillis();
        if (auth == null || time < now) {
            JsonObject jO = HttpClient.getJson((service.contains("mma") ? "https://www.mma-tv.net/loginForm.php" : "https://auth.smoothstreams.tv/hash_api.php")
                    + "?username=" + HttpClient.encode(username) + "&password=" + HttpClient.encode(password) + "&site=" + service);

            if (jO != null) {
                if (jO.getAsJsonPrimitive("code").getAsInt() == 1) {
                    auth = jO.getAsJsonPrimitive("hash").getAsString();
                    time = now + 7200000;
                } else ipc.setNotification("Authentication error: Unauthorized");
            } else ipc.setNotification("Authentication error: Unreachable");
        }
        return auth;
    }

    private Response getPlaylist() {
        StringBuilder out = new StringBuilder("#EXTM3U\n");
        List<Channel> channels = new ArrayList<>();

        JsonObject map = HttpClient.getJson("https://guide.smoothstreams.tv/altepg/channels.json");
        if (map != null) {
            for (String key : map.keySet()) {
                JsonObject jO = map.getAsJsonObject(key);

                String id = jO.getAsJsonPrimitive("xmltvid").getAsString();
                int num = jO.getAsJsonPrimitive("channum").getAsInt();
                String name = HttpClient.decode(jO.getAsJsonPrimitive("channame").getAsString());
                int group = jO.getAsJsonPrimitive("247").getAsInt();

                channels.add(new Channel(id, num, name, group == 1));
            }
        } else {
            map = HttpClient.getJson("https://guide.smoothstreams.tv/feed.json");
            if (map != null) {
                for (String key : map.keySet()) {
                    JsonObject jO = map.getAsJsonObject(key);

                    int num = jO.getAsJsonPrimitive("channel_id").getAsInt();
                    String name = HttpClient.decode(jO.getAsJsonPrimitive("name").getAsString().substring(5).trim());

                    channels.add(new Channel(String.valueOf(num), num, name, num < 61));
                }
            }
        }

        Collections.sort(channels);

        for (Channel c : channels) {
            out.append(String.format(Locale.US, "#EXTINF:-1 group-title=\"%s\" tvg-id=\"%s\" tvg-logo=\"https://guide.smoothstreams.tv/assets/images/channels/%s.png\",%s.\nhttp://%s:%s/playlist.m3u8?ch=%02d\n",
                    c.group ? "24/7 channels" : "Empty channels", c.id, c.num, c.name, host, port, c.num));
        }

        return newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", out.toString());
    }

    private Response getSports() {
        StringBuilder out = new StringBuilder("#EXTM3U\n");
        List<Event> events = new ArrayList<>();
        Date now = new Date();

        JsonObject map = HttpClient.getJson("https://guide.smoothstreams.tv/feed.json");
        if (map != null) {
            for (String key : map.keySet()) {
                JsonObject jO = map.getAsJsonObject(key);

                JsonArray jA = jO.getAsJsonArray("items");
                if (jA != null) for (JsonElement jE : jA) {
                    jO = jE.getAsJsonObject();

                    Date time = Event.getDate(jO.getAsJsonPrimitive("time").getAsString());
                    if (Event.isDate(now, time)) {
                        int num = jO.getAsJsonPrimitive("channel").getAsInt();
                        String name = HttpClient.decode(jO.getAsJsonPrimitive("name").getAsString());
                        String group = jO.getAsJsonPrimitive("category").getAsString();
                        String quality = jO.getAsJsonPrimitive("quality").getAsString();
                        String language = jO.getAsJsonPrimitive("language").getAsString();

                        events.add(new Event(time, num, name, !group.isEmpty() ? group : "_Unknown", quality, language));
                    }
                }
            }
        }

        int nonce = 0;
        Collections.sort(events);
        String pattern = ipc.getPattern();

        for (Event e : events) {
            out.append(String.format(Locale.US, "#EXTINF:-1 group-title=\"%s\" tvg-id=\"%s\" tvg-logo=\"https://guide.smoothstreams.tv/assets/images/events/%s.png\",%s\nhttp://%s:%s/playlist.m3u8?ch=%02d&nonce=%02d\n",
                    e.group, e.num, e.num, e.getEvent(pattern), host, port, e.num, ++nonce));
        }

        return newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", out.toString());
    }

    static final class Channel implements Comparable<Channel> {
        final String id;
        final int num;
        final String name;
        final boolean group;

        Channel(String id, int num, String name, boolean group) {
            this.id = id;
            this.num = num;
            this.name = name;
            this.group = group;
        }

        @Override
        public int compareTo(Channel c) {
            return num - c.num;
        }
    }

    static final class Event implements Comparable<Event> {
        private static final SimpleDateFormat IN_SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        private static final SimpleDateFormat OUT_SDF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        private static final TimeZone NY_TZ = TimeZone.getTimeZone("America/New_York");

        final Date time;
        final int num;
        final String name;
        final String group;
        final String quality;
        final String language;

        Event(Date time, int num, String name, String group, String quality, String language) {
            this.time = time;
            this.num = num;
            this.name = name;
            this.group = group;
            this.quality = quality;
            this.language = language;
        }

        static Date getDate(String text) {
            try {
                IN_SDF.setTimeZone(NY_TZ);
                return IN_SDF.parse(text);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        static boolean isDate(Date d_1, Date d_2) {
            return OUT_SDF.format(d_1).equals(OUT_SDF.format(d_2));
        }

        String getEvent(String pattern) {
            boolean q = !quality.isEmpty();
            boolean l = !language.isEmpty();
            return new SimpleDateFormat(pattern, Locale.US).format(time) + " | " + name.replace(",", "") + " "
                    + (q || l ? "(" + (q ? quality : "") + (q && l ? "/" : "") + (l ? language : "") + ")" : "").toUpperCase();
        }

        @Override
        public int compareTo(Event e) {
            int c = group.compareTo(e.group);
            if (c == 0) c = time.compareTo(e.time);
            if (c == 0) c = name.compareTo(e.name);
            if (c == 0) c = num - e.num;
            return c;
        }
    }
}
