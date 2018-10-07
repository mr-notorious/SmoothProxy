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

import android.support.annotation.NonNull;

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
        this.username = HttpClient.encode(username);
        this.password = HttpClient.encode(password);
        this.service = HttpClient.encode(service);
        this.server = HttpClient.encode(server);
        this.quality = quality;
        auth = null;
        time = 0;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response res = super.serve(session);

        String path = session.getUri();
        if (path.endsWith(".ts") || path.equals("/chunks.m3u8")) {
            res = getResponse(url + path + "?" + session.getQueryParameterString());

        } else if (path.equals("/playlist.m3u8")) {
            List<String> ch = session.getParameters().get("ch");

            if (ch != null) {
                url = "https://" + server + ".smoothstreams.tv/" + service + "/ch" + ch.get(0) + "q" + (Integer.valueOf(ch.get(0)) < 70 ? quality : 1) + ".stream";
                res = getResponse(url + path + "?wmsAuthSign=" + getAuth());

            } else {
                ipc.setNotification("Now serving: Playlist");
                res = getPlaylist();
            }

        } else if (path.equals("/sports.m3u8")) {
            ipc.setNotification("Now serving: Playlist");
            res = getSports();

        } else if (path.equals("/epg.xml")) {
            ipc.setNotification("Now serving: EPG");
            res = getResponse("https://guide.smoothstreams.tv/altepg/xmltv2.xml");

        } else if (path.equals("/epg.xml.gz")) {
            ipc.setNotification("Now serving: EPG");
            res = getResponse("https://guide.smoothstreams.tv/altepg/xmltv2.xml.gz");

        } else if (path.equals("/sports.xml")) {
            ipc.setNotification("Now serving: EPG");
            res = getResponse("https://guide.smoothstreams.tv/feed.xml");
        }

        res.addHeader("Access-Control-Allow-Origin", "*");
        return res;
    }

    private Response getResponse(String url) {
        HttpClient.Content c = HttpClient.getContent(url);
        return c != null
                ? newFixedLengthResponse(Response.Status.OK, c.type, c.response, c.length)
                : newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404");
    }

    private String getAuth() {
        long now = System.currentTimeMillis();
        if (auth == null || time < now) {
            JsonObject jO = HttpClient.getJson((service.contains("mma") ? "https://www.mma-tv.net/loginForm.php" : "https://auth.smoothstreams.tv/hash_api.php")
                    + "?username=" + username + "&password=" + password + "&site=" + service);

            if (jO != null && jO.has("code")) {
                if (jO.getAsJsonPrimitive("code").getAsInt() == 1) {
                    auth = jO.getAsJsonPrimitive("hash").getAsString();
                    time = now + 7200000;

                } else ipc.setNotification("Authentication error: Unauthorized");

            } else ipc.setNotification("Authentication error: Unreachable");
        }
        return auth;
    }

    private Response getPlaylist() {
        List<Channel> channels = new ArrayList<>();

        JsonObject map = HttpClient.getJson("https://guide.smoothstreams.tv/altepg/channels.json");
        if (map != null) {
            for (String key : map.keySet()) {
                JsonObject jO = map.getAsJsonObject(key);

                String id = jO.getAsJsonPrimitive("xmltvid").getAsString();
                int num = jO.getAsJsonPrimitive("channum").getAsInt();
                String name = jO.getAsJsonPrimitive("channame").getAsString();
                channels.add(new Channel(id, num, HttpClient.decode(name)));
            }
        } else {
            map = HttpClient.getJson("https://guide.smoothstreams.tv/feed.json");
            if (map != null) {
                for (String key : map.keySet()) {
                    JsonObject jO = map.getAsJsonObject(key);

                    String id = jO.getAsJsonPrimitive("channel_id").getAsString();
                    String name = jO.getAsJsonPrimitive("name").getAsString().substring(5).trim();
                    channels.add(new Channel(id, Integer.valueOf(id), HttpClient.decode(name)));
                }
            } else return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404");
        }

        Collections.sort(channels);
        StringBuilder out = new StringBuilder("#EXTM3U\n");

        for (Channel c : channels)
            out.append(String.format(Locale.US, "#EXTINF:-1 group-title=\"%s\" tvg-id=\"%s\" tvg-logo=\"https://guide.smoothstreams.tv/assets/images/channels/%s.png\",%s.\nhttp://%s:%s/playlist.m3u8?ch=%02d\n",
                    "SSTV channels", c.id, c.num, c.name, host, port, c.num));

        return newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", out.toString());
    }

    private Response getSports() {
        List<Sport> sports = new ArrayList<>();
        Date now = new Date();

        JsonObject map = HttpClient.getJson("https://guide.smoothstreams.tv/feed.json");
        if (map != null) {
            for (String key : map.keySet()) {
                JsonObject jO = map.getAsJsonObject(key);

                JsonArray jA = jO.getAsJsonArray("items");
                if (jA != null) {
                    for (JsonElement jE : jA) {
                        jO = jE.getAsJsonObject();

                        Date time = Sport.getAsDate(jO.getAsJsonPrimitive("time").getAsString());
                        if (Sport.isSameDate(now, time)) {
                            int num = jO.getAsJsonPrimitive("channel").getAsInt();
                            String name = jO.getAsJsonPrimitive("name").getAsString();
                            String group = jO.getAsJsonPrimitive("category").getAsString();
                            String quality = jO.getAsJsonPrimitive("quality").getAsString();
                            String language = jO.getAsJsonPrimitive("language").getAsString();
                            sports.add(new Sport(time, num, HttpClient.decode(name), !group.isEmpty() ? group : "_Unknown", quality, language));
                        }
                    }
                }
            }
        } else return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404");

        int n = 0;
        Collections.sort(sports);
        Sport.setPattern(ipc.getPattern());
        StringBuilder out = new StringBuilder("#EXTM3U\n");

        for (Sport s : sports)
            out.append(String.format(Locale.US, "#EXTINF:-1 group-title=\"%s\" tvg-id=\"%s\" tvg-logo=\"https://guide.smoothstreams.tv/assets/images/channels/%s.png\",%s\nhttp://%s:%s/playlist.m3u8?ch=%02d&n=%02d\n",
                    s.group, s.num, s.num, s, host, port, s.num, ++n));

        return newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", out.toString());
    }

    static final class Channel implements Comparable<Channel> {
        final String id;
        final int num;
        final String name;

        Channel(String id, int num, String name) {
            this.id = id;
            this.num = num;
            this.name = name;
        }

        @Override
        public int compareTo(@NonNull Channel c) {
            return num - c.num;
        }
    }

    static final class Sport implements Comparable<Sport> {
        private static final SimpleDateFormat IN_SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        private static final SimpleDateFormat OUT_SDF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        private static final TimeZone NY_TZ = TimeZone.getTimeZone("America/New_York");
        private static SimpleDateFormat SDF = new SimpleDateFormat("HH:mm", Locale.US);

        final Date time;
        final int num;
        final String name;
        final String group;
        final String quality;
        final String language;

        Sport(Date time, int num, String name, String group, String quality, String language) {
            this.time = time;
            this.num = num;
            this.name = name;
            this.group = group;
            this.quality = quality;
            this.language = language;
        }

        static void setPattern(String pattern) {
            SDF = new SimpleDateFormat(pattern, Locale.US);
        }

        static Date getAsDate(String text) {
            try {
                IN_SDF.setTimeZone(NY_TZ);
                return IN_SDF.parse(text);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        static boolean isSameDate(Date d_1, Date d_2) {
            return OUT_SDF.format(d_1).equals(OUT_SDF.format(d_2));
        }

        @Override
        public int compareTo(@NonNull Sport s) {
            int n = group.compareTo(s.group);
            if (n == 0) n = time.compareTo(s.time);
            if (n == 0) n = name.compareTo(s.name);
            if (n == 0) n = num - s.num;
            return n;
        }

        @Override
        public String toString() {
            boolean q = !quality.isEmpty();
            boolean l = !language.isEmpty();
            return SDF.format(time) + " | " + name.replace(",", "") + " " + (q || l ? "(" + (q ? quality : "") + (q && l ? "/" : "") + (l ? language : "") + ")" : "").toUpperCase();
        }
    }
}
