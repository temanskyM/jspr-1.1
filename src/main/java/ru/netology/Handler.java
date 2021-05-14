package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class Handler implements Runnable {
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        final String requestLine;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            requestLine = in.readLine();

            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var fullPath = parts[1];
            final var path = fullPath.split("\\?")[0];
            final Map<String, String> queryParams = getQueryParams(fullPath);

            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                var template = Files.readString(filePath);
                template = template.replace(
                        "{time}",
                        LocalDateTime.now().toString());
                template = template.replace(
                        "{user}",
                        queryParams.get("name"));
                var content = template.getBytes();

                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());

                out.write(content);
                out.flush();
                return;
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
            socket.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private Map<String, String> getQueryParams(String path) {
        Map<String, String> queryParams = new HashMap<>();
        String paramsUrl = path.substring(path.indexOf("?")).substring(1);
        URLEncodedUtils
                .parse(paramsUrl, StandardCharsets.UTF_8)
                .forEach(item -> queryParams.put(item.getName(), item.getValue()));

        return queryParams;
    }
}
