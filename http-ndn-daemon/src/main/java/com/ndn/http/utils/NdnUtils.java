package com.ndn.http.utils;

import java.io.IOException;

import com.ndn.http.consts.Keys;

import java.io.*;
import java.net.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.TlvWireFormat;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.security.SafeBag;

public class NdnUtils {

    private static NdnUtils instance_ = new NdnUtils();

    public static NdnUtils get() {
        return instance_;
    }

    public final Name BASE_URI = new Name("/ndn/http-ndn");

    private static class Echo implements OnInterestCallback, OnRegisterFailed {
        KeyChain keyChain_;
        Name certificateName_;
        int responseCount_ = 0;
        String serverUrl;
        int port = 0;

        public Echo(KeyChain keyChain, String serverUrl, Integer port) {
            keyChain_ = keyChain;
            this.serverUrl = serverUrl;
            this.port = port;
        }

        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                InterestFilter filter) {
            ++responseCount_;
            System.out.println("\n\n GOT NEW INTEREST " + interest.getName() + "\n");
            try {
                HttpClient client = HttpClient.newHttpClient();
                String url = "http://" + serverUrl + ":" + port + "/test-get";
                System.out.println("trying to serve: " + url);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                final HttpRequestResponseParser parser = new HttpRequestResponseParser();

                System.out.println(response.body());
                parser.parseRequest(response.body());

                System.out.println("\nREPLY: " + parser.getRequestLine());

                // Make and sign a Data packet.
                Data data = new Data(interest.getName());
                // String content = "Echo " + interest.getName().toUri();
                String content = response.body();
                data.setContent(new Blob(content));

                try {
                    keyChain_.sign(data);
                } catch (Exception exception) {
                    // Don't expect this to happen.
                    System.out.println("Security exception in sign: " + exception.getMessage());
                }
                try {
                    face.putData(data);
                    // System.out.println("Sent content " + content);
                } catch (IOException ex) {
                    System.out.println("Echo: IOException in sending data " + ex.getMessage());
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        public void onRegisterFailed(Name prefix) {
            ++responseCount_;
            System.out.println("Register failed for prefix " + prefix.toUri());
        }
    }

    public NdnUtils() {
        Interest.setDefaultCanBePrefix(true);
    }

    public void registerPrefix(String serverUrl, Integer port) {
        new Thread() {
            public void run() {
                try {
                    Face face = new Face();

                    // For now, when setting face.setCommandSigningInfo, use a key chain with
                    // a default private key instead of the system default key chain. This
                    // is OK for now because NFD is configured to skip verification, so it
                    // ignores the system default key chain.
                    KeyChain keyChain = new KeyChain("pib-memory:", "tpm-memory:");
                    keyChain.importSafeBag(new SafeBag(new Name("/testname/KEY/123"),
                            new Blob(Keys.DEFAULT_RSA_PRIVATE_KEY_DER, false),
                            new Blob(Keys.DEFAULT_RSA_PUBLIC_KEY_DER, false)));

                    face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                    Echo echo = new Echo(keyChain, serverUrl, port);
                    Name prefix = BASE_URI;
                    System.out.println("Register prefix  " + prefix.toUri());
                    face.registerPrefix(prefix, echo, echo, TlvWireFormat.get());

                    // The main event loop.
                    // Wait to receive one interest for the prefix.
                    while (echo.responseCount_ < 1) {
                        face.processEvents();

                        // We need to sleep for a few milliseconds so we don't use 100% of
                        // the CPU.
                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    System.out.println("exception: " + e.getMessage());
                }
            }
        }.start();
    }

    class Counter1 implements OnData, OnTimeout {
        public void onData(Interest interest, Data data) {
            ++callbackCount_;
            // System.out.println("Got data packet with name " + data.getName().toUri());
            ByteBuffer content = data.getContent().buf();
            for (int i = content.position(); i < content.limit(); ++i)
                System.out.print((char) content.get(i));
            System.out.println("");
        }

        public int callbackCount_ = 0;

        public void onTimeout(Interest interest) {
            ++callbackCount_;
            System.out.println("Time out for interest " + interest.getName().toUri());
        }

    }

    public void sendInterest(final Interest interest, InterestCallback callback) {
        // Silence the warning from Interest wire encode.
        Interest.setDefaultCanBePrefix(true);

        Face sendFace = new Face();
        // send interest
        System.out.println("Sending interest for: " + interest.getName().toUri());
        try {

            System.out.println("Express name " + interest.getName().toUri());
            sendFace.expressInterest(interest.getName(), callback, callback);

            while (callback.callbackCount_ < 1) {
                sendFace.processEvents();
                // We need to sleep for a few milliseconds so we don't use 100% of
                // the CPU.
                Thread.sleep(5);
            }
        } catch (final Exception e) {
            System.out.println("Failure while sending interest: " + e);
        }
    }
}