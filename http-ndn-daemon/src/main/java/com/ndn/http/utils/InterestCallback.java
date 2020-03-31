package com.ndn.http.utils;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import net.named_data.jndn.Name.Component;
import net.named_data.jndn.Name;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

public class InterestCallback implements OnData, OnTimeout {
    public int callbackCount_ = 0;
    OutputStream outToClient;
    String serverUrl;
    int port = 0;
    final byte[] reply = new byte[4096];

    public InterestCallback(OutputStream outToClient, String serverUrl, int port) {
        this.outToClient = outToClient;
        this.serverUrl = serverUrl;
        this.port = port;
    }

    public void onData(Interest interest, Data data) {
        try {
            ++callbackCount_;
            // System.out.println("Got data packet with name " + data.getName().toUri());

            String response = buildResponse(interest, data);
            byte[] resp = response.getBytes();

            outToClient.write(resp, 0, resp.length);
            outToClient.flush();
            System.out.println("Sent data packet back to client...\n" + response);
            outToClient.close();
            System.out.println("Closed stream: outToClient");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void onTimeout(Interest interest) {
        ++callbackCount_;
        System.out.println("Time out for interest " + interest.getName().toUri());
    }

    private String buildResponse(Interest interest, Data data) {
        String body = "";
        ByteBuffer content = data.getContent().buf();
        for (int i = content.position(); i < content.limit(); ++i) {
            body += (char) content.get(i);
        }

        String httpVersion = new Name(new Component[]{interest.getName().get(2), interest.getName().get(3)}).toUri();

        return httpVersion + " 200 OK\n" + "Access-Control-Allow-Origin: * \n"
                + "Access-Control-Allow-Headers: Origin, X-Requested-With, Content-Type, Accept\n"
                + "Cache-Control: no-cache\n" + "\n" + body.toString();
    }
}