package edu.jhuapl.trinity.data.graph.construction.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Deterministic source identity helpers, without a persistent repository. */
public final class SourceDocumentIdentity {
    private SourceDocumentIdentity() { }

    /**
     * Stable for a file reimported at the same normalized absolute path.
     * Clients that support document relocation may pass an enduring logical key
     * (such as a source-system document ID) to documentId(String) instead.
     */
    public static String documentId(File file) {
        if (file == null) throw new IllegalArgumentException("file is required");
        return documentId(file.toPath().toAbsolutePath().normalize().toUri().toString());
    }

    public static String documentId(String logicalSourceKey) {
        if (logicalSourceKey == null || logicalSourceKey.isBlank()) {
            throw new IllegalArgumentException("logicalSourceKey is required");
        }
        return "document:" + sha256("trinity-document-v1\u0000" + logicalSourceKey);
    }

    /** A revision is source-content-derived and deliberately distinct from document identity. */
    public static String revision(File file) throws IOException {
        MessageDigest digest = digest();
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String revision(String sourceText) {
        return sha256(sourceText);
    }

    /** Hash the exact bytes from which a particular file extraction will run. */
    public static String revision(byte[] fileBytes) {
        if (fileBytes == null) throw new IllegalArgumentException("fileBytes required");
        return HexFormat.of().formatHex(digest().digest(fileBytes));
    }

    public static String passageId(String documentId, String revision,
                                   int chunkIndex, String chunkText) {
        return "passage:" + sha256("trinity-passage-v1\u0000" + documentId
            + "\u0000" + revision + "\u0000" + chunkIndex + "\u0000" + chunkText);
    }

    public static String structuralId(String kind, String... parts) {
        StringBuilder key = new StringBuilder("trinity-structure-v1\u0000").append(kind);
        for (String part : parts) key.append('\u0000').append(part);
        return kind.toLowerCase(java.util.Locale.ROOT) + ":" + sha256(key.toString());
    }

    public static String sha256(String text) {
        return HexFormat.of().formatHex(digest().digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
