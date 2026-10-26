package ir.hefze.quran.core;

import java.io.*;
import java.nio.file.*;
import java.util.function.BooleanSupplier;

/** Publish a download only after the entire response and the decoder check succeed. */
public final class AudioTransfer {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public interface Progress { void bytes(long received, long total); }
    public interface Validator { void validate(File file) throws IOException; }
    private AudioTransfer() {}

    public static void save(InputStream input, long expected, File target,
                            BooleanSupplier cancelled, Progress progress, Validator validator) throws IOException {
        save(input, expected, target, cancelled, progress, validator, MAX_BYTES);
    }

    /** Larger complete-surah recordings may use a separate, explicit limit. */
    public static void save(InputStream input, long expected, File target,
                            BooleanSupplier cancelled, Progress progress, Validator validator,
                            long maxBytes) throws IOException {
        if (maxBytes <= 0 || expected > maxBytes) throw new IOException("Audio exceeds size limit");
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create audio directory");
        File part = new File(parent, target.getName() + ".part");
        try {
            long received = 0;
            try (FileOutputStream output = new FileOutputStream(part)) {
                byte[] buffer = new byte[16 * 1024]; int n;
                while ((n = input.read(buffer)) != -1) {
                    if (cancelled.getAsBoolean()) throw new InterruptedIOException("Cancelled");
                    received += n;
                    if (received > maxBytes) throw new IOException("Audio exceeds size limit");
                    output.write(buffer, 0, n); progress.bytes(received, expected);
                }
                if (received == 0 || (expected >= 0 && received != expected)) throw new EOFException("Incomplete audio");
                output.getFD().sync();
            }
            if (cancelled.getAsBoolean()) throw new InterruptedIOException("Cancelled");
            validator.validate(part);
            if (cancelled.getAsBoolean()) throw new InterruptedIOException("Cancelled");
            try { Files.move(part.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(part.toPath()); }
    }
}
