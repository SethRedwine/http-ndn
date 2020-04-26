package com.ndn.http;

import com.ndn.http.utils.HttpRequestResponseParser;
import com.ndn.http.utils.HttpUtils;
import com.ndn.http.utils.NdnUtils;
import com.ndn.http.utils.RequestInterestCallback;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ProxyMultiThread {
    private static ServerSocket server;

    public static void main(final String[] args) {
        try {
            // NOTE: To take args from cmd
            // if (args.length != 3)
            // throw new IllegalArgumentException("insuficient arguments");
            // and the local port that we listen for connections on
            // final String host = args[0];
            // final int remoteport = Integer.parseInt(args[1]);
            // final int localHttpPort = Integer.parseInt(args[2]);

            final String host = "127.0.0.1";
            final int remoteport = 3000;
            final int localHttpPort = 9999;

            // Print a start-up message
            System.out.println(
                    "Starting HTTP-NDN translator for " + host + ":" + remoteport + " on port " + localHttpPort);
            server = new ServerSocket(localHttpPort);

            NdnUtils ndnUtils = NdnUtils.get();
            NdnUtils.registerPrefix(ndnUtils.BASE_URI,false);

            HttpUtils.allowHttpMethods("GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "CONNECT");

            while (true) {
                new ThreadProxy(server.accept(), host, remoteport);
            }
        } catch (final Exception e) {
            System.err.println(e);
//            System.err.println("Usage: java ProxyMultiThread " + "<host> <remoteport> <localHttpPort>");
        }
    }
}

class ThreadProxy extends Thread {
    private final Socket sClient;
    private final String SERVER_URL;
    private final int SERVER_PORT;

    ThreadProxy(final Socket sClient, final String ServerUrl, final int ServerPort) {
        this.SERVER_URL = ServerUrl;
        this.SERVER_PORT = ServerPort;
        this.sClient = sClient;
        this.start();
    }

    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            final InputStream inFromClient = sClient.getInputStream();
            final OutputStream outToClient = sClient.getOutputStream();

            // a new thread for uploading to the server
            new Thread(() -> {
                try {
                    while (inFromClient.read(request) != -1) {
                        final HttpRequestResponseParser parser = new HttpRequestResponseParser();
                        final String req = new String(request);
                        parser.parseRequest(req);

                        if (parser.getRequestUrl().contains("https://")) {
                            System.out.println("Error: HTTPS not supported");
                            return;
                        }

                        System.out.println("\nREQUEST: " + parser.getRequestUrl() + "\n" + parser.getMessageBody());

                        String url = parser.getRequestUrl().replace("http://", "");
                        if (url.charAt(0) == '/') {
                            url = SERVER_URL + ":" + SERVER_PORT + url;
                        }

                        RequestInterestCallback call = new RequestInterestCallback(outToClient);

                        if (parser.getMessageBody() == null || parser.getMessageBody().trim().isBlank()) {
                            final Name name = new Name(String.format("%s/%s/%s/%s/%s",
                                NdnUtils.BASE_URI, parser.getHttpVersion(), parser.getRequestMethod(),
                                HttpUtils.buildHeaderString(parser.getHeaders()), url));
                            final Interest interest = new Interest(name);
                            NdnUtils.sendInterest(interest, call);
                        } else {
                            // Handle requests with bodies
                            final Name name = new Name(String.format("%s/%s/multi/%s/%s/%s/%s",
                                NdnUtils.BASE_URI, parser.getHttpVersion(), NdnUtils.generateRandomString(), parser.getRequestMethod(),
                                HttpUtils.buildHeaderString(parser.getHeaders()), url));
                            final Interest interest = new Interest(name);
                            NdnUtils.handleMultipacketRequest(interest, call, parser.getMessageBody());
                        }
                    }
                } catch (final SocketException e) {
                    // Socket Closed
                    // e.printStackTrace();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
