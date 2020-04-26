package com.ndn.http.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;

/**
 * Class for HTTP request parsing as defined by RFC 2612:
 *
 * Request = Request-Line ; Section 5.1 (( general-header ; Section 4.5 |
 * request-header ; Section 5.3 | entity-header ) CRLF) ; Section 7.1 CRLF [
 * message-body ] ; Section 4.3
 *
 */
public class HttpRequestResponseParser {

    private String _requestLine;
    private String _httpVersion;
    private String _requestMethod;
    private String _requestUrl;
    private String _responseCode;
    private String _responseDescription;
    private final Hashtable<String, String> _requestHeaders;
    private final StringBuffer _messageBody;

    public HttpRequestResponseParser() {
        _requestHeaders = new Hashtable<String, String>();
        _messageBody = new StringBuffer();
    }

    /**
     * Parse and HTTP request.
     *
     * @param request String holding http request.
     * @throws IOException         If an I/O error occurs reading the input stream.
     * @throws HttpFormatException If HTTP Request is malformed
     */
    public void parseRequest(final String request) throws IOException, HttpFormatException {
        final BufferedReader reader = new BufferedReader(new StringReader(request));

        parseRequestLine(reader.readLine()); // Request-Line ; Section 5.1

        String header = reader.readLine();
        while (header != null && header.length() > 0) {
            appendHeaderParameter(header);
            header = reader.readLine();
        }

        String bodyLine = reader.readLine();
        while (bodyLine != null) {
            appendMessageBody(bodyLine);
            bodyLine = reader.readLine();
        }

    }

    /**
     *
     * 5.1 Request-Line The Request-Line begins with a method token, followed by the
     * Request-URI and the protocol version, and ending with CRLF. The elements are
     * separated by SP characters. No CR or LF is allowed except in the final CRLF
     * sequence.
     *
     * @return String with Request-Line
     */
    public String getRequestLine() {
        return _requestLine;
    }

    /**
     * @return String with request URL
     */
    public String getHttpVersion() {
        return _httpVersion;
    }

    /**
     * @return String with request method
     */
    public String getRequestMethod() {
        return _requestMethod;
    }

    /**
     * @return String with request URL
     */
    public String getRequestUrl() {
        return _requestUrl;
    }

    /**
     * @return String with response code
     */
    public String getResponseCode() {
        return _responseCode;
    }

    /**
     * @return String with response description
     */
    public String getResponseDescription() {
        return _responseDescription;
    }

    private void parseRequestLine(final String requestLine) throws HttpFormatException {
        if (requestLine == null || requestLine.length() == 0) {
            throw new HttpFormatException("Invalid Request-Line: " + requestLine);
        }
        _requestLine = requestLine;
        String[] splitLine = requestLine.split("\\s");
        if (splitLine[0].matches("GET|HEAD|POST|PUT|DELETE|CONNECT|OPTIONS|TRACE|PATCH")
                && splitLine[2].indexOf("HTTP/") == 0) {
            _requestMethod = splitLine[0];
            _requestUrl = splitLine[1];
            _httpVersion = splitLine[2];
            // System.out.println("\n\n=== GOT REQUEST ===\n\n");
            // System.out.println(_requestMethod);
            // System.out.println(_requestUrl);
            // System.out.println(_httpVersion);
        }
        if (splitLine[0].indexOf("HTTP/") == 0 && splitLine[1].matches("\\d\\d\\d")) {
            _httpVersion = splitLine[0];
            _responseCode = splitLine[1];
            _responseDescription = "";
            for (int i = 2; i < splitLine.length; i++) {
                _responseDescription += splitLine[i];
            }
            // System.out.println("\n\n=== GOT REPLY ===\n\n");
            // System.out.println(_httpVersion);
            // System.out.println(_responseCode);
            // System.out.println(_responseDescription);
        }
    }

    private void appendHeaderParameter(final String header) throws HttpFormatException {
        final int idx = header.indexOf(":");
        if (idx == -1) {
            throw new HttpFormatException("Invalid Header Parameter: " + header);
        }
        _requestHeaders.put(header.substring(0, idx), header.substring(idx + 1));
    }

    /**
     * The message-body (if any) of an HTTP message is used to carry the entity-body
     * associated with the request or response. The message-body differs from the
     * entity-body only when a transfer-coding has been applied, as indicated by the
     * Transfer-Encoding header field (section 14.41).
     *
     * @return String with message-body
     */
    public String getMessageBody() {
        return _messageBody.toString();
    }

    private void appendMessageBody(final String bodyLine) {
        _messageBody.append(bodyLine).append("\r\n");
    }

    /**
     * For list of available headers refer to sections: 4.5, 5.3, 7.1 of RFC 2616
     *
     * @param headerName Name of header
     * @return String with the value of the header or null if not found.
     */
    public String getHeaderParam(final String headerName) {
        return _requestHeaders.get(headerName);
    }

    /**
     *
     * @return Hashtable<String, String> of parsed headers
     */
    public Hashtable<String, String> getHeaders() {
        return _requestHeaders;
    }

    @Override
    public String toString() {
        return "HttpRequestResponseParser [\n\t_httpVersion=" + _httpVersion + ", \n\t_messagetBody=" + _messageBody
                + ", \n\t_requestHeaders=" + _requestHeaders + ",\n\t _requestLine=" + _requestLine + ", \n\t_requestMethod="
                + _requestMethod + ", \n\t_requestUrl=" + _requestUrl + ", \n\t_responseCode=" + _responseCode
                + ", \n\t_responseDescription=" + _responseDescription + "\n]";
    }
}
