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

/**
 * The <code>CompressedRTFInputStream</code> class provides
 * decompression of compressed RTF data.
 *
 * @author Amichai Rothman
 * @since 2009-10-12
 */
public class CompressedRTFInputStream extends InputStream {

    protected static final int
        MAGIC_COMPRESSED = 0x75465a4c,
        MAGIC_UNCOMPRESSED = 0x414c454d,
        DICT_SIZE = 4096,
        DICT_MASK = DICT_SIZE - 1; // for quick modulo operations

    /**
     * Prebuffered bytes used in RTF-compressed format.
     */
    protected static byte[] COMPRESSED_RTF_PREBUF;
    static {
        try {
            String prebuf =
                "{\\rtf1\\ansi\\mac\\deff0\\deftab720{\\fonttbl;}" +
                "{\\f0\\fnil \\froman \\fswiss \\fmodern \\fscript " +
                "\\fdecor MS Sans SerifSymbolArialTimes New RomanCourier" +
                "{\\colortbl\\red0\\green0\\blue0\n\r\\par " +
                "\\pard\\plain\\f0\\fs20\\b\\i\\u\\tab\\tx";
            COMPRESSED_RTF_PREBUF = prebuf.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ignore) {
            // never happens
        }
    }

    /**
     * The lookup table used in the CRC32 calculation
     */
    protected static int[] CRC32_TABLE;
    static {
        CRC32_TABLE = new int[256];
        for (int i = 0; i < 256; i++) {
            int c = i;
            for (int j = 0; j < 8; j++)
                c = ((c & 1) == 1) ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
            CRC32_TABLE[i] = c;
        }
    }

    /**
     * Calculates the CRC32 of the given bytes, continuing a previous calculation.
     * <p>
     * The CRC32 calculation is similar to the standard one as demonstrated
     * in RFC 1952, but with the inversion (before and after the calculation)
     * omitted.
     *
     * @param buf the byte array to calculate CRC32 on
     * @param off the offset of buf at which the CRC32 calculation will start
     * @param len the number of bytes on which to calculate the CRC32
     * @param crc the previous CRC32 value calculated from preceding bytes
     * @return the CRC32 value
     */
    public static int calculateCRC32(byte[] buf, int off, int len, int crc) {
        int end = off + len;
        for (int i = off; i < end; i++)
            crc = CRC32_TABLE[(crc ^ buf[i]) & 0xFF] ^ (crc >>> 8);
        return crc;
    }

    /**
     * Calculates the CRC32 of the given bytes.
     * <p>
     * The CRC32 calculation is similar to the standard one as demonstrated
     * in RFC 1952, but with the inversion (before and after the calculation)
     * omitted.
     *
     * @param buf the byte array to calculate CRC32 on
     * @param off the offset of buf at which the CRC32 calculation will start
     * @param len the number of bytes on which to calculate the CRC32
     * @return the CRC32 value
     */
    public static int calculateCRC32(byte[] buf, int off, int len) {
        return calculateCRC32(buf, off, len, 0);
    }

    /**
     * Decompresses compressed-RTF data.
     * <p>
     * This standalone method decompresses the data using a somewhat different
     * algorithm than this stream's algorithm. It trades memory footprint
     * (twice the size of the entire output is kept in memory) for speed
     * (twice as fast as the stream version).
     * <p>
     * It also serves as a concise reference implementation for
     * the decompression algorithm.
     * <p>
     * Note that the memory is allocated according to the uncompressed size
     * declared in the compression header. If the source of the data is
     * potentially untrusted, this value should be validated or limited
     * to prevent a denial of service (DoS) attack or accidental {@link OutOfMemoryError}.
     *
     * @param src the compressed-RTF data bytes
     * @return an array containing the decompressed bytes
     * @throws IllegalArgumentException if src does not contain valid
     *         compressed-RTF bytes
     */
    public static byte[] decompressRTF(byte[] src) {
        byte[] dst; // destination for uncompressed bytes
        int in = 0; // current position in src array
        int out = 0; // current position in dst array

        // get header fields
        if (src == null || src.length < 16)
            throw new IllegalArgumentException("Invalid compressed-RTF header");

        int compressedSize = (int)TNEFUtils.getU32(src, in);
        in += 4;
        int uncompressedSize = (int)TNEFUtils.getU32(src, in);
        in += 4;
        int magic = (int)TNEFUtils.getU32(src, in);
        in += 4;
        // note: CRC must be validated only for compressed data (and includes padding)
        int crc32 = (int)TNEFUtils.getU32(src, in);
        in += 4;

        if (compressedSize != src.length - 4) // check size excluding the size field itself
            throw new IllegalArgumentException("compressed data size mismatch");

        // process the data
        if (magic == MAGIC_UNCOMPRESSED) {
            dst = new byte[uncompressedSize];
            System.arraycopy(src, in, dst, out, uncompressedSize); // just copy it
        } else if (magic == MAGIC_COMPRESSED) {
			
			/***********************************/
			
            if (crc32 != calculateCRC32(src, 16, src.length - 16))
                throw new IllegalArgumentException("compressed-RTF CRC32 failed");
            out = COMPRESSED_RTF_PREBUF.length;
            dst = new byte[out + uncompressedSize];
            System.arraycopy(COMPRESSED_RTF_PREBUF, 0, dst, 0, out);
            int flagCount = 0;
            int flags = 0;
            try {
                while (true) {
                    // each flag byte controls 8 literals/references, 1 per bit
                    // each bit is 1 for reference, 0 for literal
                    flags = ((flagCount++ & 7) == 0) ? src[in++] : flags >> 1;
                    if ((flags & 1) == 0) {
                        dst[out++] = src[in++]; // copy literal
                    } else {
                        // read reference: 12-bit offset (from block start) and 4-bit length
                        int offset = src[in++] & 0xFF;
                        int length = src[in++] & 0xFF;
                        offset = (offset << 4) | (length >>> 4); // the offset from block start
                        length = (length & 0xF) + 2; // the number of bytes to copy
                        // the decompression buffer is supposed to wrap around back
                        // to the beginning when the end is reached. we save the
                        // need for such a buffer by pointing straight into the data
                        // buffer, and simulating this behaviour by modifying the
                        // pointers appropriately.
                        offset = out & ~DICT_MASK | offset; // the absolute offset in array
                        if (offset >= out) {
                            if (offset == out)
                                break; // a self-reference marks the end of data
                            offset -= DICT_SIZE; // take from previous block
                        }
                        // note: can't use System.arraycopy, because the referenced
                        // bytes can cross through the current out position.
                        int end = offset + length;
                        while (offset < end)
                            dst[out++] = dst[offset++];
                    }
                }
            } catch (IndexOutOfBoundsException ioobe) {
                // allow empty source, anything else is invalid
                if (uncompressedSize != 0)
                    throw new IllegalArgumentException("uncompressed data size mismatch");
            }
            // copy it back without the prebuffered data
            src = dst;
            dst = new byte[uncompressedSize];
            System.arraycopy(src, COMPRESSED_RTF_PREBUF.length, dst, 0, uncompressedSize);
			
			/***********************************/
			
        } else { // unknown magic number
            throw new IllegalArgumentException(
                "Unknown compression type (magic number " + magic + ")");
        }

        return dst;
    }

    protected final InputStream in;
    protected final byte[] buf;
    protected final byte[] dict = new byte[DICT_SIZE];
    protected int bufstart;
    protected int bufend;
    protected int crc;
    protected int out;
    protected int dictstart;
    protected int flagCount;
    protected int flags;

    protected int compressedSize, declaredCompressedSize;
    protected int uncompressedSize, declaredUncompressedSize;
    protected int magic;
    protected int crc32;

    /**
     * Constructs a new CompressedRTFInputStream which reads the
     * compressed data from the given underlying stream.
     *
     * @param in the stream containing the compressed data
     * @throws IOException if an error occurs
     */
    public CompressedRTFInputStream(InputStream in) throws IOException {
        this.in = in;
        this.buf = new byte[4096];
        TNEFUtils.read(in, buf, 0, 16, 16); // read header bytes
        init();
    }

    /**
     * Initializes this stream by reading the compressed RTF header data.
     *
     * @throws IOException if an error occurs
     */
    protected void init() throws IOException {
        // get header fields
        // compressed size includes header (except size field itself), so we subtract it
        declaredCompressedSize = compressedSize = (int)TNEFUtils.getU32(buf, 0) - 12;
        declaredUncompressedSize = uncompressedSize = (int)TNEFUtils.getU32(buf, 4);
        magic = (int)TNEFUtils.getU32(buf, 8);
        crc32 = (int)TNEFUtils.getU32(buf, 12);

        if (magic == MAGIC_COMPRESSED) {
            out = COMPRESSED_RTF_PREBUF.length;
            dictstart = out;
            System.arraycopy(COMPRESSED_RTF_PREBUF, 0, dict, 0, out);
        } else if (magic != MAGIC_UNCOMPRESSED) {
            // unknown magic number
            throw new IOException("Unknown compression type (magic number " + magic + ")");
        }
    }

    /**
     * Returns the number of compressed bytes
     * (as declared in the stream header, minus the header itself).
     *
     * @return the number of remaining compressed bytes
     */
    public int getCompressedSize() {
        return declaredCompressedSize;
    }

    /**
     * Returns the number of uncompressed bytes
     * (as declared in the stream header).
     *
     * @return the number of remaining uncompressed bytes
     */
    public int getUncompressedSize() {
        return declaredUncompressedSize;
    }

    /**
     * Reads (and decompresses) more compressed data into the dictionary buffer.
     *
     * @return true if more data was read, false if there is no more data available
     * @throws IOException if an error occurs
     */
    protected boolean moreCompressed() throws IOException {
        if (compressedSize == 0 && bufstart == bufend) {
            if (crc32 != crc) // check crc also for empty compressed data
                throw new IOException("compressed-RTF CRC32 failed");
            return false; // nothing more to read and nothing more in buffer
        }
        while (true) {
            boolean newRun = (flagCount++ & 7) == 0;
            // if buf is empty, read more. The compressed data must end with
            // a (self-)reference, so there must be at least two more bytes
            // remaining at this point, plus a control byte if this is a new run
            int remaining = bufend - bufstart;
            if (remaining < 3) {
                int required = newRun ? 3 : 2;
                if (remaining < required) {
                    if (remaining > 0) {
                        buf[0] = buf[bufstart];
                        if (remaining > 1)
                            buf[1] = buf[bufstart + 1];
                    }
                    int read = TNEFUtils.read(in, buf, remaining,
                        required - remaining, buf.length - remaining);
                    bufstart = 0;
                    bufend = remaining + read;
                    compressedSize -= read;
                    crc = calculateCRC32(buf, remaining, read, crc);
                }
            }
            // each flag byte controls 8 literals/references, 1 per bit
            // each bit is 1 for reference, 0 for literal
            flags = newRun ? buf[bufstart++] : flags >> 1;
            if ((flags & 1) == 0) {
                dict[out++] = buf[bufstart++]; // copy literal
            } else {
                // read reference: 12-bit offset (from block start) and 4-bit length
                int offset = buf[bufstart++] & 0xFF;
                int length = buf[bufstart++] & 0xFF;
                offset = (offset << 4) | (length >>> 4); // the offset from block start
                length = (length & 0xF) + 2; // the number of bytes to copy
                if (offset == out)
                    break; // a self-reference marks the end of data
                // note: can't use System.arraycopy, because the referenced
                // bytes can cross through the current out position
                int end = offset + length;
                while (offset < end)
                    dict[out++ & DICT_MASK] = dict[offset++ & DICT_MASK]; // wrap around
            }
            // wrap dictionary when full
            out &= DICT_MASK;
            if (((dictstart - out) & DICT_MASK) < 17)
                return true; // dict is full - may not be room for another reference
        }
        // verify source size
        if (compressedSize < 0)
            throw new IOException("compressed data size mismatch");
        // consume extra padding (which is included in CRC)
        while (compressedSize > 0) {
            bufend = Math.min(compressedSize, buf.length);
            bufend = TNEFUtils.read(in, buf, 0, bufend, bufend);
            crc = calculateCRC32(buf, 0, bufend, crc);
            compressedSize -= bufend;
        }
        bufstart = bufend = 0; // mark buffer as empty
        // verify CRC
        if (crc32 != crc)
            throw new IOException("compressed-RTF CRC32 failed");
        return out != dictstart;
    }

    /**
     * Reads more uncompressed data into the dictionary buffer.
     *
     * @return true if more data was read, false if there is no more data available
     * @throws IOException if an error occurs
     */
    protected boolean moreUncompressed() throws IOException {
        int read = in.read(dict, 0, Math.min(uncompressedSize, DICT_SIZE));
        if (read <= 0) {
            if (uncompressedSize == 0)
                return false; // reached end
            throw new EOFException("unexpected end of stream");
        }
        out = read & DICT_MASK;
        uncompressedSize -= read;
        return true;
    }

    /**
     * Reads more data into the dictionary buffer.
     *
     * @return true if more data was read, false if there is no more data available
     * @throws IOException if an error occurs
     */
    protected boolean more() throws IOException {
        if (dictstart < 0)
            throw new IOException("stream has been closed");
        return magic == MAGIC_COMPRESSED ? moreCompressed() : moreUncompressed();
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream.
     *
     * @return an estimate of the number of bytes that can be read (or skipped
     *         over) from this input stream without blocking or {@code 0} when
     *         it reaches the end of the input stream
     * @throws IOException if an error occurs
     */
    @Override
    public int available() throws IOException {
        return (out - dictstart) & DICT_MASK;
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @throws IOException if an error occurs
     */
    @Override
    public void close() throws IOException {
        dictstart = out = -1;
        in.close();
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     *         stream is reached
     * @throws IOException if an error occurs
     */
    @Override
    public int read() throws IOException {
        if (dictstart == out && !more())
            return -1;
        int b = dict[dictstart];
        dictstart = (dictstart + 1) & DICT_MASK;
        return b;
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     *
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     *
     * @param b the buffer into which the data is read
     * @param off the start offset in array <code>b</code>
     *        at which the data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of
     *         the stream has been reached
     * @throws IOException If the first byte cannot be read for any reason
     *         other than end of file, or if the input stream has been closed,
     *         or if some other I/O error occurs
     * @throws NullPointerException If <code>b</code> is <code>null</code>
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *         <code>len</code> is negative, or <code>len</code> is greater than
     *         <code>b.length - off</code>
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0)
            return 0;
        if (dictstart == out && !more())
            return -1;
        len = Math.min(len, available());
        if (len == 0) // we just read successfully, so filled entire dict and wrapped around
            len = DICT_SIZE;
        int count = DICT_SIZE - dictstart;
        if (len <= count) {
            System.arraycopy(dict, dictstart, b, off, len);
        } else {
            System.arraycopy(dict, dictstart, b, off, count);
            System.arraycopy(dict, 0, b, off + count, len - count);
        }
        dictstart = (dictstart + len) & DICT_MASK;
        return len;
    }

}
