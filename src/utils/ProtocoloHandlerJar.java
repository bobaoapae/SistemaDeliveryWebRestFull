/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package utils;

import com.teamdev.jxbrowser.chromium.ProtocolHandler;
import com.teamdev.jxbrowser.chromium.URLRequest;
import com.teamdev.jxbrowser.chromium.URLResponse;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * @author jvbor
 */
public class ProtocoloHandlerJar implements ProtocolHandler {

    @Override
    public URLResponse onRequest(URLRequest request) {
        try {
            URLResponse response = new URLResponse();
            URL path = new URL(request.getURL());
            InputStream inputStream = path.openStream();
            DataInputStream stream = new DataInputStream(inputStream);
            byte[] data = new byte[stream.available()];
            stream.readFully(data);
            response.setData(data);
            String mimeType = getMimeType(path.toString());
            response.getHeaders().setHeader("Content-Type", mimeType);
            return response;
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) {
            return "text/html";
        }
        if (path.endsWith(".css")) {
            return "text/css";
        }
        if (path.endsWith(".js")) {
            return "text/javascript";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (path.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "text/html";
    }

}
