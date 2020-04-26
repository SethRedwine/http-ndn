package com.ndn.http.utils;

import com.ndn.http.consts.Keys;
import net.named_data.jndn.*;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SafeBag;
import net.named_data.jndn.util.Blob;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpRequest;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RequestHandler implements OnInterestCallback, OnRegisterFailed {
    KeyChain keyChain_;
    int responseCount_ = 0;

    private static ConcurrentMap<String, String> interactions = new ConcurrentHashMap<>() {};

    public static void addInteraction(String interactionId, String body) {
        if (!interactions.containsKey(interactionId)) {
            interactions.put(interactionId, body);
        }
    }
    public static void removeInteraction(String interactionId) {
        if (interactions.containsKey(interactionId)) {
            interactions.remove(interactionId);
        }
    }

    private static final RequestHandler instance_ = new RequestHandler();

    public static RequestHandler get() {
        return instance_;
    }

    public RequestHandler() {
        Face face = new Face();
        KeyChain keyChain = null;
        try {
            keyChain = new KeyChain("pib-memory:", "tpm-memory:");
            keyChain.importSafeBag(new SafeBag(new Name("/testname/KEY/123"),
                new Blob(Keys.DEFAULT_RSA_PRIVATE_KEY_DER, false),
                new Blob(Keys.DEFAULT_RSA_PUBLIC_KEY_DER, false)));
            face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch (Exception e) {
            System.out.println("exception: " + e.getMessage());
        }
        keyChain_ = keyChain;
    }

    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                           InterestFilter filter) {
        ++responseCount_;
        if (!hasBody(interest)) {
            System.out.println("\n\nGOT NEW INTEREST " + interest.getName() + "\n");
            handleRequest(interest, face);
        } else {
            String interactionId = NdnUtils.getSubname(interest,5,5).toUri();
            System.out.println("\n\nGOT NEW MULTI INTEREST " + interest.getName() + "\nisBody(): " + isBody(interest) + "\ninteractions.containsKey(): " + interactions.containsKey(interactionId));
//            System.out.println(prefix.toUri());
            if (isBody(interest) && interactions.containsKey(interactionId)) {
                // Response for body request
                sendBodyResponse(interest, face, interactions.get(interactionId));
                return;
            } else if (isBody(interest)) {
                // Not the registered prefix for the body
                return;
            }
            // Send interest for request body
            new Thread(() -> {
                Face sendFace = new Face();
                Name bodyPrefix = new Name(NdnUtils.getSubname(interest,0,5).toUri() + "/get-body/");
                Interest bodyInterest = new Interest(bodyPrefix);
                System.out.println("Sending interest for: " + bodyInterest.getName().toUri());
                try {
                    BodyInterestCallback call = new BodyInterestCallback(body -> handleRequest(interest, face, body));
                    sendFace.expressInterest(bodyInterest.getName(), call, call);

                    while (call.callbackCount_ < 1) {
                        sendFace.processEvents();
                        // We need to sleep for a few milliseconds so we don't use 100% of
                        // the CPU.
                        Thread.sleep(5);
                    }
                } catch (final Exception e) {
                    System.out.println("Failure while sending interest: " + e);
                }
            }).start();
        }
    }

    private boolean hasBody(Interest interest) {
        return NdnUtils.getSubname(interest,4,4).toUri().contains("/multi");
    }

    private boolean isBody(Interest interest) {
        return interest.getName().toUri().contains("/get-body");
    }

    private void handleRequest(Interest interest, Face face) {
        try {
            Integer nameComponentsBeforeUrl = getComponentsBeforeUrl(interest);
            String url = getUrlFromInterest(interest, nameComponentsBeforeUrl);
            String method = getMethodFromInterest(interest, nameComponentsBeforeUrl);
            // System.out.println("trying to serve: " + url);

//            HttpClient client = HttpClient.newHttpClient();
//            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
//                .method(method, HttpRequest.BodyPublishers.noBody())
//                .uri(URI.create(url));
//            addHeaders(interest, nameComponentsBeforeUrl, requestBuilder);
//            HttpRequest request = requestBuilder.build();
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String response = "";
            try {
                URL myUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) myUrl.openConnection();
                conn.setRequestMethod(method);
                conn.setUseCaches(false);
                addHeaders(interest, nameComponentsBeforeUrl, conn);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String content = HttpUtils.buildResponse(interest, response);
            sendResponse(interest, face, content);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(Interest interest, Face face, String body) {
        // Being used for /multi requests
        try {
            body = body.trim();
            Integer nameComponentsBeforeUrl = getComponentsBeforeUrl(interest);
            String url = getUrlFromInterest(interest, nameComponentsBeforeUrl);
            String method = getMethodFromInterest(interest, nameComponentsBeforeUrl);
            // System.out.println("trying to serve: " + url);
//            System.out.println(method + "\n" + body);
//
//            HttpClient client = HttpClient.newHttpClient();
//            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
//                .method(method, HttpRequest.BodyPublishers.ofString(body.trim()))
//                .uri(URI.create(url));
//            // TODO: Figure out a way around adding this
//            requestBuilder.header("Content-type", "application/x-www-form-urlencoded");
//            addHeaders(interest, nameComponentsBeforeUrl, requestBuilder);
//            HttpRequest request = requestBuilder.build();
//
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            String content = HttpUtils.buildResponse(interest, response);
//            sendResponse(interest, face, content);

            String response = "";
            try {
                URL myUrl = new URL(url);

                HttpURLConnection conn = (HttpURLConnection) myUrl.openConnection();
                conn.setRequestMethod(method);
                conn.setDoOutput(true);
                addHeaders(interest, nameComponentsBeforeUrl, conn);
                // TODO: Figure out a way around adding this
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", Integer.toString(body.length()));
                conn.setUseCaches(false);

                try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                    dos.writeBytes(body);
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String content = HttpUtils.buildResponse(interest, response);
            sendResponse(interest, face, content);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void addHeaders(Interest interest, Integer nameComponentsBeforeUrl, HttpRequest.Builder requestBuilder) {
        String headerStr = getHeadersFromInterest(interest, nameComponentsBeforeUrl);
        Hashtable<String, String> headers = HttpUtils.parseHeaderString(headerStr);
        for (String header : headers.keySet()) {
            if (HttpUtils.isValidHeader(header)) {
                requestBuilder.header(header, headers.get(header));
            }
        }
    }

    private void addHeaders(Interest interest, Integer nameComponentsBeforeUrl, HttpURLConnection conn) {
        String headerStr = getHeadersFromInterest(interest, nameComponentsBeforeUrl);
        Hashtable<String, String> headers = HttpUtils.parseHeaderString(headerStr);
        for (String header : headers.keySet()) {
//            if (HttpUtils.isValidHeader(header)) {
                conn.setRequestProperty(header, headers.get(header));
//            }
        }
    }

    private String getUrlFromInterest(Interest interest, Integer nameComponentsBeforeUrl) {
        return "http://" + HttpUtils.decodeUrlValue(NdnUtils.getSubname(interest, nameComponentsBeforeUrl).toUri().substring(1));
    }

    private Integer getComponentsBeforeUrl(Interest interest) {
        Integer nameComponentsBeforeUrl;
        if (!hasBody(interest)) {
            nameComponentsBeforeUrl = 6;
        } else {
            nameComponentsBeforeUrl = 8;
        }
        return nameComponentsBeforeUrl;
    }

    private String getMethodFromInterest(Interest interest, Integer nameComponentsBeforeUrl) {
        return NdnUtils.getSubname(interest, nameComponentsBeforeUrl - 2, nameComponentsBeforeUrl - 2).toUri().substring(1);
    }

    private String getHeadersFromInterest(Interest interest, Integer nameComponentsBeforeUrl) {
        return NdnUtils.getSubname(interest, nameComponentsBeforeUrl - 1, nameComponentsBeforeUrl - 1).toUri().substring(1);
    }

    private void sendBodyResponse(Interest interest, Face face, String response) {
        // Make and sign a Data packet.
        Data data = new Data(interest.getName());
        Blob encoding = new Blob(response);
        data.setContent(encoding);

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
            System.out.println("IOException in sending data " + ex.getMessage());
        }
    }

    private void sendResponse(Interest interest, Face face, String response) {
        // Make and sign a Data packet.
        Data data = new Data(interest.getName());

        Blob encoding = new Blob(response);

        if (encoding.size() <= Node.getMaxNdnPacketSize()) {
            data.setContent(encoding);
        } else {
            encoding = new Blob("HTTP/1.1 413 Payload Too Large\n" + "Access-Control-Allow-Origin: * \n"
                + "Access-Control-Allow-Headers: Origin, X-Requested-With, Content-Type, Accept\n"
                + "Cache-Control: no-cache\n\n" + "Failure: Response size too large for ndn packet");
            data.setContent(encoding);
        }

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
            System.out.println("IOException in sending data " + ex.getMessage());
        }
    }

    public void onRegisterFailed(Name prefix) {
        ++responseCount_;
        System.out.println("Register failed for prefix " + prefix.toUri());
    }
}
