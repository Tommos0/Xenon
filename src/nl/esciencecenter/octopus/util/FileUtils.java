package nl.esciencecenter.octopus.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.exceptions.DirectoryNotEmptyException;
import nl.esciencecenter.octopus.exceptions.FileAlreadyExistsException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.exceptions.UnsupportedOperationException;
import nl.esciencecenter.octopus.files.CopyOption;
import nl.esciencecenter.octopus.files.FileAttributes;
import nl.esciencecenter.octopus.files.OpenOption;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.PathAttributesPair;

/**
 * Some additional functionality build on top of the standard API
 * 
 * @author Niels Drost
 * 
 */
public class FileUtils {

    public static final int BUFFER_SIZE = 10240;

    /**
     * Copies all bytes from an input stream to a file.
     * 
     * @throws OctopusException
     *             if an I/O error occurs when reading or writing
     * @throws FileAlreadyExistsException
     *             if the target file exists but cannot be replaced because the {@code REPLACE_EXISTING} option is not specified
     *             <i>(optional specific exception)</i>
     * @throws DirectoryNotEmptyException
     *             the {@code REPLACE_EXISTING} option is specified but the file cannot be replaced because it is a non-empty
     *             directory <i>(optional specific exception)</i> *
     * @throws UnsupportedOperationException
     *             if {@code options} contains a copy option that is not supported
     */
    public static long copy(Octopus octopus, InputStream in, AbsolutePath target, CopyOption... options) throws OctopusException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalBytes = 0;

        OpenOption openOption = OpenOption.CREATE_NEW;

        for (CopyOption copyOption : options) {
            if (copyOption != CopyOption.REPLACE_EXISTING) {
                throw new UnsupportedOperationException("FileUtils", "unsupported copy option " + copyOption + " for " + target);
            }
            openOption = OpenOption.TRUNCATE_EXISTING;
        }

        try (OutputStream out = octopus.files().newOutputStream(target, openOption)) {
            while (true) {
                int read = in.read(buffer);

                if (read == -1) {
                    out.close();
                    return totalBytes;
                }
                out.write(buffer, 0, read);
                totalBytes += read;
            }
        } catch (IOException e) {
            throw new OctopusException("FileUtils", "failed to copy stream to file", e);
        }
    }

    /**
     * Copies all bytes from a file to an output stream.
     * 
     * @throws OctopusException
     *             if and I/O error occurs while reading or writing
     * 
     */
    public static long copy(Octopus octopus, AbsolutePath source, OutputStream out) throws OctopusException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalBytes = 0;

        try (InputStream in = octopus.files().newInputStream(source)) {
            while (true) {
                int read = in.read(buffer);

                if (read == -1) {
                    in.close();
                    return totalBytes;
                }
                out.write(buffer, 0, read);
                totalBytes += read;
            }
        } catch (IOException e) {
            throw new OctopusException("FileUtils", "failed to copy stream to file", e);
        }
    }

    /**
     * Opens a file for reading, returning a BufferedReader that may be used to read text from the file in an efficient manner.
     * @throws OctopusIOException 
     */
    public static BufferedReader newBufferedReader(Octopus octopus, AbsolutePath path, Charset cs) throws OctopusIOException {
        InputStream in = octopus.files().newInputStream(path);

        return new BufferedReader(new InputStreamReader(in, cs));
    }

    /**
     * Opens or creates a file for writing, returning a BufferedWriter that may be used to write text to the file in an efficient
     * manner.
     */
    public static BufferedWriter newBufferedWriter(Octopus octopus, AbsolutePath path, Charset cs, OpenOption... options)
            throws OctopusIOException {
        OutputStream out = octopus.files().newOutputStream(path, options);

        return new BufferedWriter(new OutputStreamWriter(out, cs));
    }

    /**
     * Read all the bytes from a file.
     */
    public static byte[] readAllBytes(Octopus octopus, AbsolutePath path) throws OctopusException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        copy(octopus, path, out);

        return out.toByteArray();

    }

    /**
     * Read all lines from a file.
     */
    public static List<String> readAllLines(Octopus octopus, AbsolutePath path, Charset cs) throws OctopusIOException {
        ArrayList<String> result = new ArrayList<String>();

        try (BufferedReader reader = newBufferedReader(octopus, path, cs)) {
            while (true) {
                String line = reader.readLine();

                if (line == null) {
                    reader.close();
                    return result;
                }
            }
        } catch (IOException e) {
            throw new OctopusIOException("FileUtils", "failed to read lines", e);
        }

    }

    /**
     * Writes bytes to a file.
     */
    public static AbsolutePath write(Octopus octopus, AbsolutePath path, byte[] bytes, OpenOption... options) throws OctopusIOException {
        try (OutputStream out = octopus.files().newOutputStream(path, options)) {
            out.write(bytes);
        } catch (IOException e) {
            throw new OctopusIOException("FileUtils", "failed to copy stream to file", e);
        }
        return path;
    }

    /**
     * Write lines of text to a file.
     * 
     */
    public static AbsolutePath write(Octopus octopus, AbsolutePath path, Iterable<? extends CharSequence> lines, Charset cs,
            OpenOption... options) throws OctopusIOException {
        try (BufferedWriter writer = newBufferedWriter(octopus, path, cs, options)) {
            for (CharSequence line : lines) {
                writer.write(line.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new OctopusIOException("FileUtils", "failed to write lines", e);
        }
        return path;
    }

    /**
     * Walks a file tree.
     */
    public static AbsolutePath walkFileTree(Octopus octopus, AbsolutePath start, FileVisitor visitor) throws OctopusIOException {
        return walkFileTree(octopus, start, false, Integer.MAX_VALUE, visitor);
    }

    /**
     * Walks a file tree.
     */
    public static AbsolutePath walkFileTree(Octopus octopus, AbsolutePath start, boolean followLinks, int maxDepth, FileVisitor visitor)
            throws OctopusIOException {
        FileAttributes attributes = octopus.files().getAttributes(start);

        walk(octopus, start, attributes, followLinks, maxDepth, visitor);

        return start;
    }

    // Walk a file tree.
    private static FileVisitResult walk(Octopus octopus, AbsolutePath path, FileAttributes attributes, boolean followLinks, int maxDepth,
            FileVisitor visitor) throws OctopusIOException {
        FileVisitResult visitResult;
        OctopusIOException exception = null;

        try {
            if (attributes.isDirectory() && maxDepth > 0) {
                visitResult = visitor.preVisitDirectory(path, attributes, octopus);
                if (visitResult == FileVisitResult.CONTINUE) {
                    try {
                        for (PathAttributesPair attributesEntry : octopus.files().newAttributesDirectoryStream(path)) {
                            // recursion step
                            FileVisitResult result =
                                    walk(octopus, attributesEntry.path(), attributesEntry.attributes(), followLinks,
                                            maxDepth - 1, visitor);

                            if (result == FileVisitResult.SKIP_SIBLINGS) {
                                // stop handling entries in this directory`
                                break;
                            } else if (result == FileVisitResult.TERMINATE) {
                                return FileVisitResult.TERMINATE;
                            }
                        }
                    } catch (OctopusIOException e) {
                        exception = e;
                    }
                    return visitor.postVisitDirectory(path, exception, octopus);
                } else if (visitResult == FileVisitResult.SKIP_SIBLINGS) {
                    // skip entries, skip post-visit, skip siblings
                    return FileVisitResult.SKIP_SIBLINGS;
                } else if (visitResult == FileVisitResult.SKIP_SUBTREE) {
                    // skip visiting entries
                    visitor.postVisitDirectory(path, exception, octopus);
                    return FileVisitResult.CONTINUE;
                } else {
                    // TERMINATE
                    return FileVisitResult.TERMINATE;
                }
            } else if (attributes.isSymbolicLink()) {
                if (followLinks) {
                    AbsolutePath target = octopus.files().readSymbolicLink(path);
                    return walk(octopus, target, octopus.files().getAttributes(target), followLinks, maxDepth - 1, visitor);
                } else {
                    // visit the link itself
                    return visitor.visitFile(path, attributes, octopus);
                }
            } else {
                return visitor.visitFile(path, attributes, octopus);
            }
        } catch (OctopusIOException e) {
            return visitor.visitFileFailed(path, e, octopus);
        }
    }

    public static AbsolutePath recursiveCopy(Octopus octopus, AbsolutePath source, AbsolutePath target, CopyOption... options) throws OctopusIOException {
        // MUST ALSO HANDLE DIRECT FILE COPIES!    	
        // FIXME!!!
        throw new OctopusIOException("FileUtils", "NOT IMPLEMENTED!");
    }

    public static void recursiveDelete(Octopus octopus, AbsolutePath path) {
        // TODO Auto-generated method stub
    }

    public static void recursiveWipe(Octopus octopus, AbsolutePath path) {
        // TODO Auto-generated method stub
    }
}
