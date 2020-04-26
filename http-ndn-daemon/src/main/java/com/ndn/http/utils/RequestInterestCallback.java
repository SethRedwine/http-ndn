package com.ndn.http.utils;

import java.io.OutputStream;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import java.nio.ByteBuffer;

public class RequestInterestCallback implements OnData, OnTimeout {
    public int callbackCount_ = 0;
    OutputStream outToClient;

    public RequestInterestCallback(OutputStream outToClient) {
        this.outToClient = outToClient;
    }

    public void onData(Interest interest, Data data) {
        try {
            ++callbackCount_;
            // System.out.println("Got data packet with name " + data.getName().toUri());

            String response = "";
            ByteBuffer content = data.getContent().buf();
            for (int i = content.position(); i < content.limit(); ++i) {
                response += (char) content.get(i);
            }
            byte[] resp = response.getBytes();
            outToClient.write(resp, 0, resp.length);
            outToClient.flush();
//            System.out.println("Sent data packet back to client...\n" + response);
            // Wait for flush
            Thread.sleep(5);
            outToClient.close();
//            System.out.println("Closed stream: outToClient");
            RequestHandler.removeInteraction(NdnUtils.getSubname(interest,5,5).toUri());
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void onTimeout(Interest interest) {
        ++callbackCount_;
        System.out.println("Time out for interest " + interest.getName().toUri());
    }
}
