package com.ndn.http.utils;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

public class HttpUtils {

    /* valid HTTP methods */
    private static final String[] methods = {
        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
    };

    /* Forbidden Headers */
    private static final String[] forbiddenHeaders = {
        "ACCEPT-CHARSET", "ACCEPT-ENCODING", "ACCESS-CONTROL-REQUEST-HEADERS", "ACCESS-CONTROL-REQUEST-METHOD",
        "CONNECTION", "CONTENT-LENGTH", "COOKIE", "COOKIE2", "DATE", "DNT", "EXPECT", "FEATURE-POLICY", "HOST",
        "KEEP-ALIVE", "ORIGIN", "PROXY", "SEC", "REFERER", "TE", "TRAILER", "TRANSFER-ENCODING", "UPGRADE", "VIA"
    };

    public static boolean isValidMethod(String method) {
        return Arrays.asList(methods).contains(method.trim().toUpperCase());
    }

    public static boolean isValidHeader(String header) {
        return !Arrays.asList(forbiddenHeaders).contains(header.trim().toUpperCase());
    }

    public static void allowHttpMethods(String... methods) {
        // Adds method to allowed http methods
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);
            methodsField.setAccessible(true);
            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);
            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void allowHttpsMethods(String... methods) {
        // Adds method to allowed http methods
        try {
            Field methodsField = HttpsURLConnection.class.getDeclaredField("methods");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);
            methodsField.setAccessible(true);
            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);
            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String buildResponse(Interest interest, Data data) {
        String body = "";
        ByteBuffer content = data.getContent().buf();
        for (int i = content.position(); i < content.limit(); ++i) {
            body += (char) content.get(i);
        }
        String httpVersion = new Name(new Component[]{interest.getName().get(2), interest.getName().get(3)}).toUri().substring(1);
        String status = "200 OK";

        if (body.equalsIgnoreCase("Failure: Response size too large for ndn packet")) {
            status = "413 Payload Too Large";
        }
        return httpVersion + " "+ status + "\n" + "Access-Control-Allow-Origin: * \n"
                + "Access-Control-Allow-Headers: Origin, X-Requested-With, Content-Type, Accept\n"
                + "Cache-Control: no-cache\n" + "\n" + body;
    }

    public static String buildResponse(Interest interest, String body) {
        String httpVersion = new Name(new Component[]{interest.getName().get(2), interest.getName().get(3)}).toUri().substring(1);
        String status = "200 OK";

        if (body.equalsIgnoreCase("Failure: Response size too large for ndn packet")) {
            status = "413 Payload Too Large";
        }
        return httpVersion + " "+ status + "\n" + "Access-Control-Allow-Origin: * \n"
            + "Access-Control-Allow-Headers: Origin, X-Requested-With, Content-Type, Accept\n"
            + "Cache-Control: no-cache\n" + "\n" + body;
    }

    public static String buildResponse(Interest interest, HttpResponse<String> response) {
        String httpVersion = new Name(new Component[]{interest.getName().get(2), interest.getName().get(3)}).toUri().substring(1);
        String status = Integer.toString(response.statusCode());
        String body = response.body();

        return httpVersion + " "+ status + "\n" + "Access-Control-Allow-Origin: * \n"
                + "Access-Control-Allow-Headers: Origin, X-Requested-With, Content-Type, Accept\n"
                + "Cache-Control: no-cache\n" + "\n" + body;
    }

    public static String decodeUrlValue(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    public static String encodeUrlValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    public static String buildHeaderString(Hashtable<String, String> params) {
        String paramString = "";
        for ( String param : params.keySet()) {
            // TODO: Figure out how to handle restricted headers
            if (isValidHeader(param)) {
                paramString += param + "=" + params.get(param) + "&";
            }
        }
        // Clean trailing ampersand
        if ((paramString != null) && (paramString.length() > 0)) {
            paramString = paramString.substring(0, paramString.length() - 1);
        }
        return encodeUrlValue(paramString);
    }

    public static Hashtable<String, String> parseHeaderString(String params) {
        String paramString = decodeUrlValue(params);
        String[] paramList = paramString.split("&");
        Hashtable<String, String> paramTable = new Hashtable<>();
        for (String param : paramList) {
            String[] keyValue = param.split("=");
            paramTable.put(keyValue[0], keyValue[1]);
        }
        return paramTable;
    }
}
