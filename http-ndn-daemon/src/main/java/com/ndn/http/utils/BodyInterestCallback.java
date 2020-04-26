package com.ndn.http.utils;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class BodyInterestCallback implements OnData, OnTimeout {
    public int callbackCount_ = 0;
    private Consumer<String> handleBodyResponse;

    public BodyInterestCallback(Consumer<String> handleBodyResponse) {
        this.handleBodyResponse = handleBodyResponse;
    }

    public void onData(Interest interest, Data data) {
        try {
            ++callbackCount_;
            // System.out.println("Got data packet with name " + data.getName().toUri());\
            String response = "";
            ByteBuffer content = data.getContent().buf();
            for (int i = content.position(); i < content.limit(); ++i) {
                response += (char) content.get(i);
            }
            handleBodyResponse.accept(response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void onTimeout(Interest interest) {
        ++callbackCount_;
        System.out.println("Time out for interest " + interest.getName().toUri());
    }
}
