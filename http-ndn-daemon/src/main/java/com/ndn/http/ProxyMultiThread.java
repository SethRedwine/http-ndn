package com.ndn.http;

import java.io.*;
import java.net.*;
// import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.ndn.http.utils.HttpFormatException;
import com.ndn.http.utils.HttpRequestResponseParser;
import com.ndn.http.utils.NdnUtils;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
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

                            ndnUtils.sendInterest(interest);
                            // CompletableFuture<Data> data = ndnUtils.sendInterest(interest);
                            // data.thenAccept(d -> {
                            // System.out.println(d.getName() + ": " + d.getContent());
                            // });

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

            /*
             * // connects a socket to the server try { server = new Socket(SERVER_URL,
             * SERVER_PORT); } catch (final IOException e) { final PrintWriter out = new
             * PrintWriter(new OutputStreamWriter(outToClient)); out.flush(); out.close();
             * throw new RuntimeException(e); } // a new thread to manage streams from
             * client to server (UPLOAD) final InputStream inFromServer =
             * server.getInputStream(); final OutputStream outToServer =
             * server.getOutputStream();
             * 
             * // current thread manages streams from server to client (DOWNLOAD) int
             * bytes_read; try { while ((bytes_read = inFromServer.read(reply)) != -1) {
             * final HttpRequestResponseParser parser = new HttpRequestResponseParser();
             * final String rep = new String(reply); parser.parseRequest(rep);
             * 
             * System.out.println("\nREPLY: " + parser.getRequestLine());
             * 
             * outToClient.write(reply, 0, bytes_read); outToClient.flush(); } } catch
             * (final IOException e) { e.printStackTrace(); } catch (final
             * HttpFormatException e) { e.printStackTrace(); } finally { try { if (server !=
             * null) server.close(); if (client != null) client.close(); } catch (final
             * IOException e) { e.printStackTrace(); } } outToClient.close(); //
             * System.out.println("Closed stream: outToClient");
             */
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}