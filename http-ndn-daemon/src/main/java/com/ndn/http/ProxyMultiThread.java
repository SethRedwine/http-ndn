package com.ndn.http;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
// import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.ndn.http.utils.HttpFormatException;
import com.ndn.http.utils.HttpRequestResponseParser;
import com.ndn.http.utils.InterestCallback;
import com.ndn.http.utils.NdnUtils;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.Data;

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

            // TestPublishAsyncNfd.extracted();
            // Print a start-up message
            System.out.println(
                    "Starting HTTP-NDN translator for " + host + ":" + remoteport + " on port " + localHttpPort);
            server = new ServerSocket(localHttpPort);

            NdnUtils ndnUtils = NdnUtils.get();

            while (true) {
                new ThreadProxy(server.accept(), host, remoteport);
                ndnUtils.registerTestPrefix(host, remoteport);
            }
        } catch (final Exception e) {
            System.err.println(e);
            System.err.println("Usage: java ProxyMultiThread " + "<host> <remoteport> <localHttpPort>");
        }
    }
}

/**
 * Handles a socket connection to the proxy server from the client and uses 2
 * threads to proxy between server and client
 *
 */
class ThreadProxy extends Thread {
    private final Socket sClient;
    private final String SERVER_URL;
    private final int SERVER_PORT;
    // private final NdnUtils ndnUtils;

    ThreadProxy(final Socket sClient, final String ServerUrl, final int ServerPort) {
        this.SERVER_URL = ServerUrl;
        this.SERVER_PORT = ServerPort;
        this.sClient = sClient;
        // this.ndnUtils = ndnUtils;
        this.start();
    }

    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            final InputStream inFromClient = sClient.getInputStream();
            final OutputStream outToClient = sClient.getOutputStream();

            // a new thread for uploading to the server
            new Thread() {
                public void run() {
                    int bytes_read;
                    try {
                        while ((bytes_read = inFromClient.read(request)) != -1) {
                            final HttpRequestResponseParser parser = new HttpRequestResponseParser();
                            final String req = new String(request);
                            parser.parseRequest(req);
                            System.out.println("\nREQUEST: " + parser.getRequestUrl());

                            NdnUtils ndnUtils = NdnUtils.get();

                            final Name name = new Name(ndnUtils.BASE_URI + "/" + parser.getHttpVersion() + "/"
                                    + parser.getRequestMethod() + "/" + parser.getRequestUrl());
                            final Interest interest = new Interest(name);

                            InterestCallback call = new InterestCallback(outToClient, SERVER_URL, SERVER_PORT);
                            ndnUtils.sendInterest(interest, call);

                            // outToServer.write(request, 0, bytes_read);
                            // outToServer.flush();
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                    // try {
                    // outToServer.close();
                    // System.out.println("Closed stream: outToServer");
                    // } catch (final IOException e) {
                    // e.printStackTrace();
                    // }
                }
            }.start();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
