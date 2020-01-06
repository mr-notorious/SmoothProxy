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

import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.FormBody;
import okhttp3.ResponseBody;

final class HttpServer extends NanoHTTPD {
    private final String host;
    private final int port;
    private final Bind bind;
    private String username;
    private String password;
    private String service;
    private String server;
    private int quality;
    private String auth;
    private long time;

    HttpServer(String host, int port, Bind bind) {
        super(host, port);
        this.host = host;
        this.port = port;
        this.bind = bind;
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
        String path = session.getUri();

        if (path.endsWith(".ts") || path.equals("/chunks.m3u8")) {
            res = getResponse(getUrl(session.getParameters().get("n").get(0), path, session.getQueryParameterString()));

        } else if (path.equals("/playlist.m3u8")) {
            res = getResponse(getUrl(session.getParameters().get("n").get(0), path, session.getQueryParameterString() + "&wmsAuthSign=" + getAuth()));

        } else if (path.equals("/master.xml")) {
            res = getResponse("https://guide.smoothstreams.tv/altepg/xmltv2.xml");

        } else if (path.equals("/sports.xml")) {
            res = getResponse("https://guide.smoothstreams.tv/feed.xml");

        } else if (path.equals("/master.m3u8")) {
            res = getMaster();

        } else if (path.equals("/sports.m3u8")) {
            res = getSports();
        }

        res.addHeader("Access-Control-Allow-Origin", "*");
        return res;
    }

    private String getUrl(String num, String path, String query) {
        return "https://" + server + ".smoothstreams.tv/" + service + "/ch" + num + "q" + (NumberUtils.toInt(num) < 70 ? quality : 1) + ".stream" + path + "?" + query;
    }

    private Response getResponse(String url) {
        ResponseBody body = HttpClient.getBody(url);
        return body != null ? newFixedLengthResponse(Response.Status.OK, Objects.toString(body.contentType(), NanoHTTPD.MIME_PLAINTEXT), body.byteStream(), body.contentLength()) : getNotFoundResponse();
    }

    private Response getNotFoundResponse() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    private synchronized String getAuth() {
        long now = System.currentTimeMillis();
        if (auth == null || time < now) {

            JsonObject json = HttpClient.getJson("https://auth.smoothstreams.tv/hash_api.php", new FormBody.Builder().add("username", username).add("password", password).add("site", service).build());
            if (json != null) {

                if (json.getAsJsonPrimitive("code").getAsInt() == 1) {
                    auth = json.getAsJsonPrimitive("hash").getAsString();
                    time = now + 7200000;

                } else bind.setNotification("Unauthorized");

            } else bind.setNotification("Unreachable");
        }
        return auth;
    }

    private Response getMaster() {
        List<Channel> channels = new ArrayList<>();

        JsonObject json = HttpClient.getJson("https://guide.smoothstreams.tv/altepg/channels.json");
        if (json != null) {

            for (String key : json.keySet()) {
                JsonObject jO = json.getAsJsonObject(key);

                String id = jO.getAsJsonPrimitive("xmltvid").getAsString();
                int num = jO.getAsJsonPrimitive("channum").getAsInt();
                String name = jO.getAsJsonPrimitive("channame").getAsString();
                channels.add(new Channel(id, num, HttpClient.decode(name)));
            }

        } else {
            json = HttpClient.getJson("https://guide.smoothstreams.tv/feed.json");
            if (json != null) {

                for (String key : json.keySet()) {
                    JsonObject jO = json.getAsJsonObject(key);

                    String id = jO.getAsJsonPrimitive("channel_id").getAsString();
                    String name = jO.getAsJsonPrimitive("name").getAsString().substring(5).trim();
                    channels.add(new Channel(id, NumberUtils.toInt(id), HttpClient.decode(name)));
                }

            } else return getNotFoundResponse();
        }

        Collections.sort(channels);
        StringBuilder out = new StringBuilder("#EXTM3U\n");

        for (Channel c : channels)
            out.append(String.format(Locale.US, "#EXTINF:-1 group-title=\"SSTV channels\" tvg-id=\"%s\" tvg-logo=\"https://guide.smoothstreams.tv/assets/images/channels/%s.png\",%s.\nhttp://%s:%s/playlist.m3u8?n=%02d\n",
                    c.id, c.num, c.name, host, port, c.num));

        return newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", out.toString());
    }

    private Response getSports() {
        List<Sport> sports = new ArrayList<>();
        Date now = new Date();

        JsonObject json = HttpClient.getJson("https://guide.smoothstreams.tv/feed.json");
        if (json != null) {

            for (String key : json.keySet()) {

                JsonArray jA = json.getAsJsonObject(key).getAsJsonArray("items");
                if (jA != null) {

                    for (JsonElement jE : jA) {
                        JsonObject jO = jE.getAsJsonObject();

                        Date time = Sport.parseTime(jO.getAsJsonPrimitive("time").getAsString());
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

        } else return getNotFoundResponse();

        int i = 1;
        Collections.sort(sports);
        Sport.set24HourFormat(bind.is24HourFormat());
        StringBuilder out = new StringBuilder("#EXTM3U\n");

        for (Sport s : sports)
            out.append(String.format(Locale.US, "#EXTINF:-1 group-title=\"%s\" tvg-id=\"%s\" tvg-logo=\"https://guide.smoothstreams.tv/assets/images/channels/%s.png\",%s\nhttp://%s:%s/playlist.m3u8?n=%02d&i=%02d\n",
                    s.group, s.num, s.num, s, host, port, s.num, ++i));

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
            return Integer.compare(num, c.num);
        }
    }

    static final class Sport implements Comparable<Sport> {
        private static final FastDateFormat IN_DATE = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("America/New_York"), Locale.US);
        private static FastDateFormat OUT_DATE = FastDateFormat.getInstance();

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

        static Date parseTime(String text) {
            try {
                return IN_DATE.parse(text);
            } catch (Exception e) {
                return null;
            }
        }

        static boolean isSameDate(Date d_1, Date d_2) {
            return DateUtils.truncatedEquals(d_1, d_2, Calendar.DATE);
        }

        static void set24HourFormat(boolean is24HourFormat) {
            OUT_DATE = FastDateFormat.getInstance(is24HourFormat ? "HH:mm" : "hh:mm a", Locale.US);
        }

        @Override
        public int compareTo(@NonNull Sport s) {
            int i = group.compareTo(s.group);
            if (i == 0) i = time.compareTo(s.time);
            if (i == 0) i = name.compareTo(s.name);
            if (i == 0) i = Integer.compare(num, s.num);
            return i;
        }

        @Override
        public String toString() {
            boolean q = !quality.isEmpty();
            boolean l = !language.isEmpty();
            return OUT_DATE.format(time) + " | " + StringUtils.replace(name, ",", "") + " " + (q || l ? "(" + (q ? quality : "") + (q && l ? "/" : "") + (l ? language : "") + ")" : "").toUpperCase();
        }
    }
}
