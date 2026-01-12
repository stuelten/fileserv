package de.sty.fileserv.core;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;

import static de.sty.fileserv.core.WebDavConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * WebDAV servlet implementation. Does the heavy lifting.
 */
public class WebDavServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(WebDavServlet.class);

    /** The parameter used in ServletConfig for {@link #dataDir}. */
    public static final String DATA_DIR = "data";

    /** The directory to serve */
    protected Path dataDir;

    /** Holds all locks in memory. */
    private final LockManager locks = new LockManager();

    protected static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String r = Objects.requireNonNull(config.getInitParameter(DATA_DIR),
                "init-param '" + DATA_DIR + "' required");
        dataDir = Path.of(r).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    // --- Core dispatch ---------------------------------------------------------

    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String method = req.getMethod();
        switch (method) {
            case METHOD_OPTIONS  -> doOptions(req, resp);
            case METHOD_PROPFIND -> doPropFind(req, resp);
            case METHOD_MKCOL    -> doMkCol(req, resp);
            case METHOD_MOVE     -> doMove(req, resp);
            case METHOD_COPY     -> doCopy(req, resp);
            case METHOD_LOCK     -> doLock(req, resp);
            case METHOD_UNLOCK   -> doUnlock(req, resp);
            default -> super.service(req, resp); // GET/HEAD/PUT/DELETE handled by overrides
        }
    }

    @Override protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(SC_200_OK);
        resp.setHeader(HEADER_DAV, "1,2");                 // advertise Class 2
        resp.setHeader(HEADER_MS_AUTHOR_VIA, HEADER_DAV);       // helps some MS clients
        resp.setHeader(HEADER_ALLOW,
                METHOD_OPTIONS + ", " + METHOD_PROPFIND + ", " + METHOD_GET + ", " + METHOD_HEAD + ", " +
                METHOD_PUT + ", " + METHOD_DELETE + ", " + METHOD_MKCOL + ", " + METHOD_MOVE + ", " +
                METHOD_COPY + ", " + METHOD_LOCK + ", " + METHOD_UNLOCK);
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        if (!Files.exists(p)) { resp.sendError(SC_404_NOT_FOUND); return; }
        if (Files.isDirectory(p)) { resp.sendError(SC_405_METHOD_NOT_ALLOWED); return; }

        resp.setStatus(SC_200_OK);
        resp.setHeader(HEADER_ETAG, etag(p));
        resp.setHeader(HEADER_LAST_MODIFIED, HTTP_DATE.format(lastModified(p)));
        resp.setContentLengthLong(Files.size(p));

        try (InputStream in = Files.newInputStream(p); OutputStream out = resp.getOutputStream()) {
            in.transferTo(out);
        }
    }

    @Override protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        if (!Files.exists(p)) { resp.sendError(SC_404_NOT_FOUND); return; }
        if (Files.isDirectory(p)) { resp.sendError(SC_405_METHOD_NOT_ALLOWED); return; }

        resp.setStatus(SC_200_OK);
        resp.setHeader(HEADER_ETAG, etag(p));
        resp.setHeader(HEADER_LAST_MODIFIED, HTTP_DATE.format(lastModified(p)));
        resp.setContentLengthLong(Files.size(p));
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        if (!checkWriteLock(req, resp, p)) return;

        Files.createDirectories(p.getParent());

        boolean existed = Files.exists(p);
        try (InputStream in = req.getInputStream()) {
            Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
        }

        resp.setStatus(existed ? SC_204_NO_CONTENT : SC_201_CREATED);
    }

    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        if (!checkWriteLock(req, resp, p)) return;

        if (!Files.exists(p)) { resp.sendError(SC_404_NOT_FOUND); return; }
        if (Files.isDirectory(p)) {
            // recursive delete
            Files.walkFileTree(p, new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file); return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir); return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.deleteIfExists(p);
        }
        resp.setStatus(SC_204_NO_CONTENT);
    }

    // --- WebDAV methods --------------------------------------------------------

    protected void doMkCol(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        if (!checkWriteLock(req, resp, p)) return;

        if (Files.exists(p)) { resp.sendError(SC_405_METHOD_NOT_ALLOWED); return; }
        Files.createDirectories(p);
        resp.setStatus(SC_201_CREATED);
    }

    protected void doMove(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path src = resolve(req);
        if (!checkWriteLock(req, resp, src)) return;
        Path dst = resolveDestination(req, resp);
        if (dst == null) return;

        Files.createDirectories(dst.getParent());
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        resp.setStatus(SC_201_CREATED);
    }

    protected void doCopy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path src = resolve(req);
        Path dst = resolveDestination(req, resp);
        if (dst == null) return;

        if (Files.isDirectory(src)) {
            Files.walkFileTree(src, new SimpleFileVisitor<>() {
                @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path rel = src.relativize(dir);
                    Files.createDirectories(dst.resolve(rel));
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = src.relativize(file);
                    Files.copy(file, dst.resolve(rel), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
        resp.setStatus(SC_201_CREATED);
    }

    protected void doPropFind(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        if (!Files.exists(p)) { resp.sendError(SC_404_NOT_FOUND); return; }

        int depth = parseDepth(req.getHeader(HEADER_DEPTH)); // 0 or 1 enough for Finder/Explorer listing
        String hrefBase = req.getRequestURL().toString();
        if (!hrefBase.endsWith("/")) {
            // keep consistent; for files it’s ok either way, for collections add slash in href generation
        }

        resp.setStatus(SC_207_MULTI_STATUS);
        resp.setContentType(CONTENT_TYPE_XML);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                .append("<D:multistatus xmlns:D=\"").append(DAV_NAMESPACE).append("\">");

        // self
        xml.append(propResponse(req, p, hrefFor(req, p)));

        if (depth >= 1 && Files.isDirectory(p)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(p)) {
                for (Path child : ds) {
                    xml.append(propResponse(req, child, hrefFor(req, child)));
                }
            }
        }

        xml.append("</D:multistatus>");
        resp.getWriter().write(xml.toString());
    }

    protected void doLock(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        // LOCK is also used to create lock on non-existing resources (lock-null resources).
        // For simplicity: ensure parent exists; file may be created later by PUT.
        Files.createDirectories(p.getParent());

        long timeout = parseTimeoutSeconds(req.getHeader(HEADER_TIMEOUT));
        int depth = parseDepth(req.getHeader(HEADER_DEPTH));

        String owner = parseLockOwner(req); // optional
        var lock = locks.createOrRefreshExclusiveLock(pathKey(p), owner, timeout, depth);

        resp.setStatus(SC_200_OK);
        resp.setHeader(HEADER_DAV, "1,2");
        resp.setHeader(HEADER_LOCK_TOKEN, "<" + lock.token() + ">");
        resp.setContentType(CONTENT_TYPE_XML);

        // Minimal lockdiscovery response
        //noinspection ConcatenationWithEmptyString
        String body = ""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:prop xmlns:D=\"" + DAV_NAMESPACE + "\">"
                + "  <D:lockdiscovery>"
                + "    <D:activelock>"
                + "      <D:locktype><D:write/></D:locktype>"
                + "      <D:lockscope><D:exclusive/></D:lockscope>"
                + "      <D:depth>" + (depth == Integer.MAX_VALUE ? INFINITY : depth) + "</D:depth>"
                + "      <D:timeout>" + TIMEOUT_SECOND + timeout + "</D:timeout>"
                + "      <D:locktoken><D:href>" + lock.token() + "</D:href></D:locktoken>"
                + "    </D:activelock>"
                + "  </D:lockdiscovery>"
                + "</D:prop>";

        resp.getWriter().write(body);
    }

    protected void doUnlock(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path p = resolve(req);
        String token = extractLockToken(req.getHeader(HEADER_LOCK_TOKEN));
        if (token == null) { resp.sendError(SC_400_BAD_REQUEST, "Missing Lock-Token"); return; }

        boolean ok = locks.unlock(token, pathKey(p));
        if (!ok) { resp.sendError(SC_409_CONFLICT, "Lock token mismatch"); return; }

        resp.setStatus(SC_204_NO_CONTENT);
    }

    // --- Helpers ---------------------------------------------------------------

    protected Path resolve(HttpServletRequest req) {
        String raw = Optional.ofNullable(req.getPathInfo()).orElse("/");
        String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        // prevent .. traversal
        Path p = dataDir.resolve(decoded.substring(1)).normalize();
        if (!p.startsWith(dataDir)) throw new IllegalArgumentException("Invalid path");
        return p;
    }

    protected Path resolveDestination(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String dest = req.getHeader(HEADER_DESTINATION);
        if (dest == null) { resp.sendError(SC_400_BAD_REQUEST, "Missing Destination"); return null; }

        String pathPart = dest;

        // If absolute URL, keep only path+query part
        int schemeIdx = dest.indexOf("://");
        if (schemeIdx >= 0) {
            int firstSlash = dest.indexOf('/', schemeIdx + 3);
            pathPart = (firstSlash >= 0) ? dest.substring(firstSlash) : "/";
        }

        // Drop query/fragment if present
        int q = pathPart.indexOf('?');
        if (q >= 0) pathPart = pathPart.substring(0, q);
        int hash = pathPart.indexOf('#');
        if (hash >= 0) pathPart = pathPart.substring(0, hash);

        String decoded = java.net.URLDecoder.decode(pathPart, java.nio.charset.StandardCharsets.UTF_8);

        // If proxy adds a prefix (e.g. /webdav), strip it
        String xfPrefix = req.getHeader("X-Forwarded-Prefix");
        if (xfPrefix != null && !xfPrefix.isBlank()) {
            String prefix = xfPrefix.trim();
            if (!prefix.startsWith("/")) prefix = "/" + prefix;
            if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);

            if (decoded.startsWith(prefix + "/")) {
                decoded = decoded.substring(prefix.length());
            } else if (decoded.equals(prefix)) {
                decoded = "/";
            }
        }

        Path p = dataDir.resolve(decoded.startsWith("/") ? decoded.substring(1) : decoded).normalize();
        if (!p.startsWith(dataDir)) { resp.sendError(403); return null; }
        return p;
    }

    private String hrefFor(HttpServletRequest req, Path p) {
        // build href relative to servlet dataDir
        String ctx = req.getContextPath() == null ? "" : req.getContextPath();
        String base = ctx + "/";
        Path rel = dataDir.relativize(p);
        String href = base + rel.toString().replace(File.separatorChar, '/');
        if (Files.isDirectory(p) && !href.endsWith("/")) href += "/";
        if (!href.startsWith("/")) href = "/" + href;
        return href;
    }

    protected String propResponse(HttpServletRequest req, Path p, String href) throws IOException {
        boolean dir = Files.isDirectory(p);
        long size = dir ? 0 : Files.size(p);

        StringBuilder sb = new StringBuilder();
        // Appends display name to XML response
        sb.append("<D:response>")
                .append("<D:href>").append(escapeXml(href)).append("</D:href>")
                .append("<D:propstat><D:prop>")
                .append("<D:displayname>").append(escapeXml(p.getFileName() == null ? "" : p.getFileName().toString())).append("</D:displayname>")
                .append("<D:getlastmodified>").append(HTTP_DATE.format(lastModified(p))).append("</D:getlastmodified>")
                .append("<D:getetag>").append(escapeXml(etag(p))).append("</D:getetag>")
                .append("<D:resourcetype>").append(dir ? "<D:collection/>" : "").append("</D:resourcetype>")
                .append("<D:getcontentlength>").append(size).append("</D:getcontentlength>");

        // Lock discovery if locked
        locks.getActiveLock(pathKey(p)).ifPresent(l -> {
            sb.append("<D:lockdiscovery><D:activelock>")
                    .append("<D:locktype><D:write/></D:locktype>")
                    .append("<D:lockscope><D:exclusive/></D:lockscope>")
                    .append("<D:depth>").append(l.depth() == Integer.MAX_VALUE ? INFINITY : l.depth()).append("</D:depth>")
                    .append("<D:timeout>").append(TIMEOUT_SECOND).append(Math.max(1, (l.expiresAt().getEpochSecond() - Instant.now().getEpochSecond()))).append("</D:timeout>")
                    .append("<D:locktoken><D:href>").append(escapeXml(l.token())).append("</D:href></D:locktoken>")
                    .append("</D:activelock></D:lockdiscovery>");
        });

        sb.append("</D:prop><D:status>").append(PROTOCOL_HTTP_1_1).append(" ").append(SC_200_OK).append(" OK</D:status></D:propstat>")
                .append("</D:response>");
        return sb.toString();
    }

    protected boolean checkWriteLock(HttpServletRequest req, HttpServletResponse resp, Path p) throws IOException {
        // If the resource is locked, require correct token in If: or Lock-Token:
        var lockOpt = locks.getActiveLock(pathKey(p));
        if (lockOpt.isEmpty()) return true;

        String expected = lockOpt.get().token();
        String ifHeader = req.getHeader(HEADER_IF);
        String lockTokenHeader = extractLockToken(req.getHeader(HEADER_LOCK_TOKEN));

        if (containsToken(ifHeader, expected) || (lockTokenHeader != null && lockTokenHeader.equals(expected))) {
            return true;
        }

        resp.sendError(SC_423_LOCKED, "Locked");
        return false;
    }

    protected static boolean containsToken(String ifHeader, String token) {
        if (ifHeader == null) return false;
        return ifHeader.contains(token);
    }

    protected static String extractLockToken(String headerVal) {
        if (headerVal == null) return null;
        // formats: <opaquelocktoken:...>
        String s = headerVal.trim();
        if (s.startsWith("<") && s.endsWith(">")) s = s.substring(1, s.length() - 1);
        return s;
    }

    protected static int parseDepth(String depth) {
        if (depth == null) return 0;
        String d = depth.trim().toLowerCase(Locale.ROOT);
        if (INFINITY.equals(d)) return Integer.MAX_VALUE;
        try { return Integer.parseInt(d); } catch (Exception e) {
            LOG.info("Ignore exception while parsing Depth header: {}", depth, e);
            return 0;
        }
    }

    protected static long parseTimeoutSeconds(String timeout) {
        if (timeout == null) return 600;
        // formats: Second-600, Infinite
        String t = timeout.trim();
        if (t.equalsIgnoreCase(TIMEOUT_INFINITE)) return 86400;
        int i = t.indexOf(TIMEOUT_SECOND);
        if (i >= 0) {
            try { return Long.parseLong(t.substring(i + TIMEOUT_SECOND.length())); } catch (Exception e) {
                LOG.info("Ignore exception while parsing Timeout header: {}", timeout, e);
            }
        }
        return 600;
    }

    protected static String parseLockOwner(HttpServletRequest req) {
        // Try to parse <D:owner> from LOCK request body (optional)
        if (req.getContentLength() == 0) {
            return "";
        }

        try (InputStream rawIn = req.getInputStream()) {
            if (rawIn == null) return "";

            // Read ahead to see if there is any non-whitespace content
            PushbackInputStream in = new PushbackInputStream(rawIn, 1);
            int b;
            while ((b = in.read()) != -1) {
                if (!Character.isWhitespace(b)) {
                    in.unread(b);
                    break;
                }
            }

            if (b == -1) {
                return ""; // only whitespace or empty
            }

            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var builder = factory.newDocumentBuilder();

            Document doc = builder.parse(in);
            NodeList owners = doc.getElementsByTagNameNS("DAV:", "owner");
            if (owners.getLength() > 0) return owners.item(0).getTextContent();
        } catch (Exception e) {
            LOG.warn("Failed to parse lock owner from request body", e);
        }
        return "";
    }

    protected static Instant lastModified(Path p) throws IOException {
        return Files.getLastModifiedTime(p).toInstant();
    }

    protected static String etag(Path p) throws IOException {
        // cheap ETag: size + mtime
        if (!Files.exists(p) || Files.isDirectory(p)) return "\"dir\"";
        long size = Files.size(p);
        long lm = Files.getLastModifiedTime(p).toMillis();
        return "\"" + size + "-" + lm + "\"";
    }

    protected static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    protected static String pathKey(Path p) {
        // stable lock key for resource
        return p.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
