# SmoothProxy

![SmoothProxy Icon](http://i.imgur.com/cWxIruq.png "Icon courtesy of Obs")

---

## Instructions
1. Install and launch SmoothProxy on your Android device.
2. Fill out **[Username]**, **[Password]**, **[Service]**, and **[Server]** fields.
3. **[Save]** and/or back out of SmoothProxy. Note, **[Stop]** will terminate SmoothProxy.
![SmoothProxy Screenshot](https://i.imgur.com/9CwzSnh.png)
4. To connect SmoothProxy with an IPTV player of your choosing, use the following URLs in verbatim:
    * Playlist URL: **http://localhost:8888/playlist.m3u8**
    * EPG URL: **http://localhost:8888/epg.xml.gz**
    * NOTE! If **localhost:8888** does not work, try **127.0.0.1:8888**
5. *Enjoy!*

## Warning
At this time, SmoothProxy is barebones. It is guaranteed to break on the stupidest of reasons. If you want to avoid unnecessary frustrations, refrain yourself from using SmoothProxy. SmoothProxy is not endorsed by SmoothStreams and I am not affiliated with SmoothStreams.

---

## Dependencies
* [google-gson v2.8.2](https://github.com/google/gson) - JSON parser.
* [jsoup v1.11.3](https://github.com/jhy/jsoup) - HTML parser.
* [OkHttp v3.10.0](https://github.com/square/okhttp) - HTTP client.
* [NanoHTTPD v2.3.1](https://github.com/NanoHttpd/nanohttpd) - HTTP server.

## License
```
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
```
