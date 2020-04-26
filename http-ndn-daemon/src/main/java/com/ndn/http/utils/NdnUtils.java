package com.ndn.http.utils;

import com.ndn.http.consts.Keys;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.TlvWireFormat;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SafeBag;
import net.named_data.jndn.util.Blob;

import java.util.Random;

public class NdnUtils {

    public static final Name BASE_URI = new Name("/ndn/http-ndn");

    private static final NdnUtils instance_ = new NdnUtils();

    public NdnUtils() {
        Interest.setDefaultCanBePrefix(true);
    }

    public static NdnUtils get() {
        return instance_;
    }

    public static String generateRandomString() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
            .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
            .limit(targetStringLength)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

    public static Name getSubname(Interest interest, Integer start) {
        Name.Component[] urlComps = new Name.Component[interest.getName().size() - start];
        for (int i = start; i < interest.getName().size(); i++) {
            urlComps[i - start] = interest.getName().get(i);
        }
        return new Name(urlComps);
    }

    public static Name getSubname(Interest interest, Integer start, Integer end) {
        // Inclusive end
        end = end + 1 < interest.getName().size() ? end + 1 : interest.getName().size();
        Name.Component[] urlComps = new Name.Component[end - start];
        for (int i = start; i < end; i++) {
            urlComps[i - start] = interest.getName().get(i);
        }
        return new Name(urlComps);
    }

    public static void registerPrefix(Name prefix, boolean single) {
        new Thread(() -> {
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

                RequestHandler handler = RequestHandler.get();
                System.out.println("Register prefix  " + prefix.toUri());
                face.registerPrefix(prefix, handler, handler, TlvWireFormat.get());

                // The main event loop.
                while (!single || handler.responseCount_ < 1) {
                    face.processEvents();
                    // NOTE: We need to sleep for a few milliseconds so we don't use 100% of the
                    // CPU.
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                System.out.println("exception: " + e.getMessage());
            }
        }).start();
    }

    public static void sendInterest(final Interest interest, RequestInterestCallback callback) {
        // Silence the warning from Interest wire encode.
        Interest.setDefaultCanBePrefix(true);

        Face sendFace = new Face();
        // send interest
        System.out.println("Sending interest for: " + interest.getName().toUri());
        try {
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

    public static void handleMultipacketRequest(final Interest interest, RequestInterestCallback callback, String requestBody) {
        // Silence the warning from Interest wire encode.
        Interest.setDefaultCanBePrefix(true);

        Face sendFace = new Face();
        // send interest
        System.out.println("Sending interest for: " + interest.getName().toUri());
        RequestHandler.addInteraction(getSubname(interest,5,5).toUri(), requestBody);
        try {
            // Wait for prefix to register
            Thread.sleep(5);
            sendFace.expressInterest(interest.getName(), callback, callback);

            while (callback.callbackCount_ < 1) {
                sendFace.processEvents();
                // We need to sleep for a few milliseconds so we don't use 100% of
                // the CPU.
            }
        } catch (final Exception e) {
            System.out.println("Failure while sending interest: " + e);
        }
    }
}
