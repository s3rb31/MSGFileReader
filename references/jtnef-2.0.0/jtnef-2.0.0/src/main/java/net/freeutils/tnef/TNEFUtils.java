/*
 *  Copyright © 2003-2015 Amichai Rothman
 *
 *  This file is part of JTNEF - the Java TNEF package.
 *
 *  JTNEF is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  JTNEF is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with JTNEF.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  For additional info see http://www.freeutils.net/source/jtnef/
 */

package net.freeutils.tnef;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.TimeZone;

/**
 * The <code>TNEFUtils</code> class provides utility methods used by the TNEF
 * processing classes.
 *
 * @author Amichai Rothman
 * @since 2003-04-25
 */
public class TNEFUtils {

    /**
     * Returns an unsigned 8-bit value from a byte array.
     *
     * @param buf a byte array from which byte value is taken
     * @param offset the offset within buf from which byte value is taken
     * @return an unsigned 8-bit value as an int
     */
    public static int getU8(byte[] buf, int offset) {
        return buf[offset] & 0xFF;
    }

    /**
     * Returns an unsigned 16-bit value from little-endian ordered bytes.
     *
     * @param b1 first byte value
     * @param b2 second byte value
     * @return an unsigned 16-bit value as an int
     */
    public static int getU16(int b1, int b2) {
        return (b1 & 0xFF) | (b2 & 0xFF) << 8;
    }

    /**
     * Returns an unsigned 16-bit value from little-endian ordered bytes.
     *
     * @param buf a byte array from which byte values are taken
     * @param offset the offset within buf from which byte values are taken
     * @return an unsigned 16-bit value as an int
     */
    public static int getU16(byte[] buf, int offset) {
        return (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8);
    }

    /**
     * Returns an unsigned 32-bit value from little-endian ordered bytes.
     *
     * @param b1 first byte value
     * @param b2 second byte value
     * @param b3 third byte value
     * @param b4 fourth byte value
     * @return an unsigned 32-bit value as a long
     */
    public static long getU32(int b1, int b2, int b3, int b4) {
        return ((b1 & 0xFF) | (b2 & 0xFF) << 8 | (b3 & 0xFF) << 16 | (b4 & 0xFF) << 24) & 0xFFFFFFFFL;
    }

    /**
     * Returns an unsigned 32-bit value from little-endian ordered bytes.
     *
     * @param buf a byte array from which byte values are taken
     * @param offset the offset within buf from which byte values are taken
     * @return an unsigned 32-bit value as a long
     */
    public static long getU32(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) | (buf[offset + 1] & 0xFF) << 8 |
                (buf[offset + 2] & 0xFF) << 16 | (buf[offset + 3] & 0xFF) << 24) & 0xFFFFFFFFL;
    }

    /**
     * Returns a 64-bit value from little-endian ordered bytes.
     *
     * @param buf a byte array from which byte values are taken
     * @param offset the offset within buf from which byte values are taken
     * @return a 64-bit value as a long
     */
    public static long getU64(byte[] buf, int offset) {
        return (getU32(buf, offset + 4) & 0xFFFFFFFFL) << 32 |
                getU32(buf, offset) & 0xFFFFFFFFL;
    }

    /**
     * Returns a 32-bit value containing a combined attribute type and ID.
     *
     * @param atp the attribute type
     * @param id the attribute ID
     * @return a 32-bit value containing a combined attribute type and ID
     */
    public static int attribute(int atp, int id) {
        return atp << 16 | id;
    }

    /**
     * Returns the ID part of a 32-bit combined attribute type and ID value.
     *
     * @param att the combined attribute type and ID value
     * @return the ID part of a 32-bit combined attribute type and ID value
     */
    public static int attID(int att) {
        return att & 0xFFFF;
    }

    /**
     * Returns the type part of a 32-bit combined attribute type and ID value.
     *
     * @param att the combined attribute type and ID value
     * @return the type part of a 32-bit combined attribute type and ID value
     */
    public static int attType(int att) {
        return att >>> 16;
    }

    /**
     * Returns the checksum of a given byte array.
     *
     * @param data the byte array on which to calculate the checksum
     * @return the checksum of a given byte array
     */
    public static int calculateChecksum(byte[] data) {
        return calculateChecksum(data, 0, data.length);
    }

    /**
     * Returns the checksum of a range of bytes within a given byte array.
     *
     * @param data the byte array on which to calculate the checksum
     * @param offset the offset within the array from which to begin
     * @param length the number of bytes to calculate checksum on
     * @return the checksum of a range of bytes within a given byte array
     */
    public static int calculateChecksum(byte[] data, int offset, int length) {
        // Note: the AND operation prevents sign extension
        // when the byte is widened to an int
        int checksum = 0;
        length += offset; // now marks the end index itself
        for (int i = offset; i < length; i++)
            checksum += data[i] & 0xFF;
        return checksum & 0xFFFF;
    }

    /**
     * Returns the checksum of all the data in a given RawInputStream.
     * The stream's current position is not modified.
     *
     * @param ris the stream from which the data is read
     * @return the checksum of all the data in a given RawInputStream
     * @throws IOException if an I/O error occurs
     */
    public static int calculateChecksum(RawInputStream ris) throws IOException {
        int checksum = 0;
        RawInputStream r = new RawInputStream(ris); // make a copy, original is unmodified
        try {
            int read;
            byte[] buf = new byte[4096];
            while ((read = r.read(buf)) != -1)
                checksum += calculateChecksum(buf, 0, read);
        } finally {
            r.close();
        }
        return checksum & 0xFFFF;
    }

    /**
     * Returns the name of a constant which is defined in given Class,
     * has a name beginning with given prefix, and has given value. If the
     * constant's name cannot be found, the value is returned as a hex String.
     *
     * @param cls the Class containing the constant
     * @param constPrefix the prefix of the constant name (used in grouping constants)
     * @param value the constant's value
     * @return the name of the constant
     */
    public static String getConstName(Class cls, String constPrefix, long value) {
        try {
            for (Field field : cls.getFields()) {
                if (field.getName().startsWith(constPrefix) &&
                        field.getLong(null) == value)
                    return field.getName();
            }
        } catch (IllegalAccessException ignore) {}
        return "0x" + Long.toHexString(value);
    }

    /**
     * Removes all null characters ('\0') from the end of a given String.
     * Useful for converting a C-style null terminated string to a Java String.
     *
     * @param s a String
     * @return a String identical to the given string, with trailing null
     *         characters removed
     */
    public static String removeTerminatingNulls(String s) {
        if (s == null)
            return null;
        int len = s.length();
        while (len > 0 && s.charAt(len - 1) == '\0')
            len--;
        return len == s.length() ? s : s.substring(0, len);
    }

    /**
     * Replaces all occurrences of given substring within string with a replacement
     * string.
     *
     * @param s the string to be modified
     * @param search the substring to search for
     * @param replace the string with which to replace occurrences of the search substring
     * @return a new string consisting of the given string, with all occurrences
     *         of search string replaced by replace string. If given string or
     *         search string are empty or null, the string itself is returned.
     */
    public static String replace(String s, String search, String replace) {
        if (s == null || search == null || search.length() == 0)
            return s;
        if (replace == null)
            replace = "";
        int len = s.length();
        int slen = search.length();
        int rlen = replace.length();
        int ind = 0;
        while (ind < len && (ind = s.indexOf(search, ind)) > -1) {
            s = s.substring(0, ind) + replace + s.substring(ind + slen);
            ind += rlen;
        }
        return s;
    }

    /**
     * Creates a String from a C-style null terminated byte sequence.
     *
     * The null terminated byte sequence is interpreted as 8-bit ISO-8859-1
     * characters (a.k.a. ISO-Latin-1), which is a superset of US-ASCII.
     * This way we don't lose any 8-bit values and remain fully compatible:
     * If the source charset is unknown, a str.getByte("ISO8859_1") will
     * reconstruct the exact original byte sequence which the application
     * can then process in any charset it sees fit.
     *
     * @param bytes a byte array containing a C-style null terminated string
     * @param offset the offset within bytes where the string begins
     * @param length the length of the C-style string in bytes, which may
     *        include any number of terminating null ('\0') characters
     * @return a String containing the C-style string's characters, interpreted
     *         as ISO-8859-1 characters
     */
    public static String createString(byte[] bytes, int offset, int length) {
        try {
            return removeTerminatingNulls(new String(bytes, offset, length, "ISO8859_1"));
        } catch (UnsupportedEncodingException ignore) {}
        return "";
    }

    /**
     * Creates a String from a C-style null terminated Unicode byte sequence.
     *
     * The null terminated byte sequence is interpreted as 16-bit Unicode
     * characters (UTF-16), stored in Little Endian order.
     *
     * @param bytes a byte array containing a C-style null terminated Unicode string
     * @param offset the offset within bytes where the string begins
     * @param length the length of the C-style string in bytes, which may
     *        include any number of terminating null ('\0') characters
     * @return a String containing the C-style string's characters, interpreted
     *         as Unicode (UTF-16 Little Endian) characters
     */
    public static String createStringUnicode(byte[] bytes, int offset, int length) {
        try {
            return removeTerminatingNulls(new String(bytes, offset, length, "UTF-16LE"));
        } catch (UnsupportedEncodingException ignore) {}
        return "";
    }

    /**
     * Appends a hex string representation of the given integer
     * to a StringBuilder.
     * <p>
     * Note that if passing a byte value, it must be ANDed with 0xFF
     * to prevent sign extension during the widening conversion.
     *
     * @param sb the StringBuilder to which the hex string is appended
     * @param i an integer
     * @param len the minimum length of the hex string (it will be padded with
     *        leading zeros to reach this minimum length if necessary)
     * @return the given StringBuilder
     */
    public static StringBuilder appendHexString(StringBuilder sb, int i, int len) {
        String s = Integer.toHexString(i).toUpperCase();
        len -= s.length();
        while (len-- > 0)
            sb.append('0');
        return sb.append(s);
    }

    /**
     * Returns a hex string representation of the given integer.
     * <p>
     * Note that if passing a byte value, it must be ANDed with 0xFF
     * to prevent sign extension during the widening conversion.
     *
     * @param i an integer
     * @param len the minimum length of the hex string (it will be padded with
     *        leading zeros to reach this minimum length if necessary)
     * @return a hex string representation of the integer
     */
    public static String toHexString(int i, int len) {
        return TNEFUtils.appendHexString(new StringBuilder(8), i, len).toString();
    }

    /**
     * Creates a String containing the hexadecimal representation of the given
     * bytes.
     *
     * @param bytes a byte array whose content is to be displayed
     * @return a String containing the hexadecimal representation of the given
     *         bytes
     */
    public static String toHexString(byte[] bytes) {
        return toHexString(bytes, 0, bytes != null ? bytes.length : 0, -1);
    }

    /**
     * Creates a String containing the hexadecimal representation of the given
     * bytes.
     *<p>
     * If {@code max} is non-negative and {@code bytes.length > max}, then the
     * first {@code max} bytes are returned, followed by a human-readable
     * indication that there are {@code bytes.length} total bytes of data
     * including those that are not returned.
     *
     * @param bytes a byte array whose content is to be displayed
     * @param max the maximum number of bytes to be displayed (-1 means no limit)
     * @return a String containing the hexadecimal representation of the given
     *         bytes
     */
    public static String toHexString(byte[] bytes, int max) {
        return toHexString(bytes, 0, bytes != null ? bytes.length : 0, max);
    }

    /**
     * Creates a String containing the hexadecimal representation of the given
     * bytes.
     * <p>
     * If {@code max} is non-negative and {@code len > max}, then the
     * first {@code max} bytes are returned, followed by a human-readable
     * indication that there are {@code len} total bytes of data
     * including those that are not returned.
     * <p>
     * In particular, {@code offset + len} can extend beyond the array boundaries,
     * as long as {@code offset + max} is still within them, resulting in
     * {@code max} bytes returned followed by an indication that there are
     * {@code len} total data bytes (including those that are not returned).
     *
     * @param bytes a byte array whose content is to be displayed
     * @param offset the offset within the byte array to start at
     * @param len the number of bytes
     * @param max the maximum number of bytes to be displayed (-1 means no limit)
     * @return a String containing the hexadecimal representation of the given
     *         bytes
     */
    public static String toHexString(byte[] bytes, int offset, int len, int max) {
        if (bytes == null)
            return "[null]";
        int count = max > -1 && max < len ? max : len;
        StringBuilder s = new StringBuilder(2 * count + 20);
        s.append('[');
        for (int i = 0; i < count; i++)
            appendHexString(s, bytes[offset + i] & 0xFF, 2);
        if (count < len)
            s.append("... (").append(len).append(" bytes)");
        s.append(']');
        return s.toString();
    }

    /**
     * Returns a string representation of the given time
     * in the given format. The timezone is assumed to be
     * GMT (UTC), i.e. no timezone conversion occurs.
     *
     * @param time the time
     * @param format the format string (as specified by {@link SimpleDateFormat}
     * @return a string representation of the given time
     */
    public static String formatDate(long time, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(time);
    }

    /**
     * Checks whether the given string contains a TNEF mime type
     *
     * @param mimeType the mimeType to check
     * @return true if the given string contains a TNEF mime type,
     *         false otherwise
     */
    public static boolean isTNEFMimeType(String mimeType) {
        // note that application/ms-tnefx was also observed in the wild
        return mimeType != null
            && ((mimeType = mimeType.toLowerCase()).startsWith("application/ms-tnef")
                || mimeType.startsWith("application/vnd.ms-tnef"));
    }

    /**
     * Reads bytes from a stream. The number of bytes read is no less
     * than the given minimum and no more than the given maximum.
     *
     * @param in the input stream to read from
     * @param b the byte array to read into
     * @param off the offset within the byte array to start reading into
     * @param min the minimum number of bytes to read
     * @param max the maximum number of bytes to read
     * @return the number of bytes read
     * @throws EOFException if the end of stream has been reached
     *         before the minimum number of bytes have been read
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if min is greater than max
     * @throws IndexOutOfBoundsException if min is greater than the remaining array capacity
     */
    public static int read(InputStream in, byte[] b, int off, int min, int max) throws IOException {
        if (min > max)
            throw new IllegalArgumentException("min is greater than max");
        int remaining = b.length - off;
        if (min > remaining)
            throw new IndexOutOfBoundsException("available array space is smaller than min");
        if (max > remaining)
            max = remaining;
        int total = 0;
        while (total < min) {
            int read = in.read(b, off + total, max - total);
            if (read < 0)
                throw new EOFException("Unexpected end of stream");
            total += read;
        }
        return total;
    }

    /**
     * Reads an array of bytes from a stream by allocating memory
     * incrementally (rather than all at once). This is useful when the
     * number of bytes to read is obtained from an external, potentially
     * untrusted source: by allocating the memory incrementally, it prevents
     * denial of service (DoS) attacks or accidental {@link OutOfMemoryError}s.
     * <p>
     * The initial amount of allocated memory is specified by the caller
     * (a reasonable minimum may be applied). This value should strike a balance
     * between being too small (incurring unnecessary memory allocations and
     * copying) and being too large (potentially allowing an OOME/DoS).
     * After the initial allocation, the memory buffer size is doubled as data
     * is read into it, until all requested bytes are read. This ensures that the
     * allocated memory is at most twice the size of the actually available data.
     *
     * @param in the input stream to read from
     * @param length the number of bytes to read
     * @param initialBufferLength the initial amount of memory to allocate
     * @return the read bytes
     * @throws EOFException if the end of stream has been reached
     *         before the given number of bytes have been read
     * @throws IOException if an I/O error occurs
     * @throws NegativeArraySizeException if length is negative
     */
    public static byte[] readSafely(InputStream in, int length, int initialBufferLength) throws IOException {
        byte[] b = new byte[Math.min(length, Math.max(initialBufferLength, 4096))];
        int remaining = length;
        while (remaining > 0) {
            int block = b.length - (length - remaining);
            remaining -= read(in, b, b.length - block, block, block);
            if (remaining > 0) {
                byte[] temp = new byte[b.length + Math.min(remaining, b.length)]; // at most double the size
                System.arraycopy(b, 0, temp, 0, b.length);
                b = temp;
            }
        }
        return b;
    }

    /**
     * Transfers data from an input stream to an output stream.
     *
     * @param in the input stream to read from
     * @param out the output stream to write to
     * @param maxLength maximum number of bytes to read;
     *        if negative, read until end of stream
     * @param closeIn if true, the input stream is closed by this method
     * @param closeOut if true, the output stream is closed by this method
     * @return the number of bytes transferred
     * @throws IOException if an error occurs
     */
    public static long transfer(InputStream in, OutputStream out, long maxLength,
            boolean closeIn, boolean closeOut) throws IOException {
        long len = maxLength;
        byte[] buf = new byte[4096];
        try {
            while (len != 0) {
                int count = len < 0 || buf.length < len ? buf.length : (int)len;
                count = in.read(buf, 0, count);
                if (count == -1)
                    break;
                out.write(buf, 0, count);
                len -= count;
            }
        } finally {
            if (closeIn)
                in.close();
            if (closeOut)
                out.close();
        }
        return maxLength - len;
    }

    /**
     * Closes all of the given Closeables.
     * All nulls are silently ignored.
     *
     * @param closeables the Closeables to close
     * @param <T> the element type
     * @throws IOException if an error occurs
     */
    public static <T extends Closeable> void closeAll(Collection<T> closeables) throws IOException {
        if (closeables != null && !closeables.isEmpty())
            closeAll(closeables.toArray(new Closeable[closeables.size()]));
    }

    /**
     * Closes all of the given Closeables.
     * All nulls are silently ignored.
     *
     * @param closeables the Closeables to close
     * @param <T> the element type
     * @throws IOException if an error occurs
     */
    public static <T extends Closeable> void closeAll(T... closeables) throws IOException {
        if (closeables != null) {
            Throwable error = null;
            for (Closeable closeable : closeables) {
                try {
                    if (closeable != null)
                        closeable.close();
                } catch (Throwable t) {
                    error = t;
                }
            }
            if (error != null) {
                IOException throwable = new IOException();
                throwable.initCause(error); // no cause in JDK 1.5 IOException constructor
                throw throwable;
            }
        }
    }

}
