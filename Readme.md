# SmoothProxy

![SmoothProxy Icon](http://i.imgur.com/cWxIruq.png "Icon courtesy of Obs")

---

## What?
SmoothProxy is an on-demand playlist/playback daemon for SmoothStreams, running on Android (4.0+).

## Why?
Kodi is slow, clunky, and leaves much to be desired. Better options exist, however, authentication tokens issued by SmoothStreams are only valid for 4 hours to prevent abuse of service. Traditional playlist generators requires you to manually refresh on expiration. This is not the case for SmoothProxy.

## How?
SmoothProxy generates a specialized playlist that 'points' to itself rather than SmoothStreams. When SmoothProxy receives a playback request, it determines the context of the request and responds with a 301 redirect, along with your valid authentication token, to the appropriate SmoothStreams resource. 

## Who?
Jack of all trades, master of none when it comes to programming. This is my first shot at Android development, so bare with me.

---

## Instructions
1. Install and launch SmoothProxy on your Android device. 
2. Fill out **[Username]**, **[Password]**, **[Service]**, and **[Server]** fields.
3. **[Save]** and/or back out of SmoothProxy. Note, **[Exit]** will terminate SmoothProxy.
![SmoothProxy Screenshot](https://i.imgur.com/UBUKTPB.png)
4. To connect SmoothProxy with an IPTV player of your choosing, use the following URLs in verbatim:
    * Playlist URL: **http://localhost:8888/playlist.m3u8**
    * Fog's EPG URL: **http://localhost:8888/epg.xml**
5. *Enjoy!*

## Warning
At this time, SmoothProxy is barebones. It is guaranteed to break on the stupidest of reasons. If want to avoid unnecessary frustrations, refrain yourself from using SmoothProxy.

## Feedback
Direct your comments, questions, and concerns to the SmoothStreams forum. Let us keep it within the community. SmoothProxy is not endorsed by SmoothStreams, so do not bother the staff.

---

## Acknowledgments
* [@SmoothStreamsTV](https://twitter.com/smoothstreamstv) for the awesome service. 
* The community for being awesome testers.
* Obs for the awesome icon.
* Fog for the awesome EPG.

## Dependencies 
* [google-gson v2.8.1](https://github.com/google/gson) - JSON string to Java object.
* [OkHttp v3.8.1](https://github.com/square/okhttp) - Retrieve JSON string from URL.
* [NanoHTTPD v2.3.1](https://github.com/NanoHttpd/nanohttpd) - Lightweight HTTP web server. 

## License
```
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
```
