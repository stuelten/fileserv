package de.sty.fileserv.core;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Common constants for WebDAV.
 */
@SuppressWarnings("unused")
public final class WebDavConstants {
    private WebDavConstants() {}

    // --- WebDAV + HTTP verbs/methods ---
    public static final String METHOD_OPTIONS  = "OPTIONS";
    public static final String METHOD_PROPFIND = "PROPFIND";
    public static final String METHOD_MKCOL    = "MKCOL";
    public static final String METHOD_MOVE     = "MOVE";
    public static final String METHOD_COPY     = "COPY";
    public static final String METHOD_LOCK     = "LOCK";
    public static final String METHOD_UNLOCK   = "UNLOCK";
    public static final String METHOD_GET      = "GET";
    public static final String METHOD_HEAD     = "HEAD";
    public static final String METHOD_PUT      = "PUT";
    public static final String METHOD_DELETE   = "DELETE";

    // --- WebDAV + HTTP Headers ---
    public static final String HEADER_DAV           = "DAV";
    public static final String HEADER_DEPTH         = "Depth";
    public static final String HEADER_DESTINATION   = "Destination";
    public static final String HEADER_IF            = "If";
    public static final String HEADER_LOCK_TOKEN    = "Lock-Token";
    public static final String HEADER_TIMEOUT       = "Timeout";
    public static final String HEADER_OVERWRITE     = "Overwrite";
    public static final String HEADER_ALLOW         = "Allow";
    public static final String HEADER_ETAG          = "ETag";
    public static final String HEADER_LAST_MODIFIED = "Last-Modified";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HEADER_CONTENT_TYPE  = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_SERVER         = "Server";

    // --- Auth ---
    public static final String AUTH_PREFIX_BASIC     = "Basic ";

    // --- Microsoft specific ---
    public static final String HEADER_MS_AUTHOR_VIA = "MS-Author-Via";

    // --- Proxy headers ---
    public static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";
    public static final String HEADER_FORWARDED         = "Forwarded";

    // --- Status Codes ---
    public static final int SC_200_OK = HttpServletResponse.SC_OK;                // 200
    public static final int SC_201_CREATED = HttpServletResponse.SC_CREATED;           // 201
    public static final int SC_204_NO_CONTENT = HttpServletResponse.SC_NO_CONTENT;        // 204
    public static final int SC_207_MULTI_STATUS = 207;
    public static final int SC_400_BAD_REQUEST = HttpServletResponse.SC_BAD_REQUEST;       // 400
    public static final int SC_401_UNAUTHORIZED = HttpServletResponse.SC_UNAUTHORIZED;      // 401
    public static final int SC_403_FORBIDDEN = HttpServletResponse.SC_FORBIDDEN;         // 403
    public static final int SC_404_NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;         // 404
    public static final int SC_405_METHOD_NOT_ALLOWED = HttpServletResponse.SC_METHOD_NOT_ALLOWED; // 405
    public static final int SC_409_CONFLICT = HttpServletResponse.SC_CONFLICT;          // 409
    public static final int SC_423_LOCKED = 423;

    // --- Content Types ---
    public static final String CONTENT_TYPE_XML = "application/xml; charset=utf-8";

    // --- Protocols ---
    public static final String PROTOCOL_HTTP_1_1 = "HTTP/1.1";

    // --- WebDAV Values ---
    public static final String DAV_NAMESPACE = "DAV:";
    public static final String INFINITY      = "infinity";
    public static final String TIMEOUT_INFINITE = "Infinite";
    public static final String TIMEOUT_SECOND = "Second-";

}
