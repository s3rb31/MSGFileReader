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

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;

/**
 * The <code>MAPIProp</code> class encapsulates the value of a MAPI property.
 *
 * @author Amichai Rothman
 * @since 2003-07-25
 */
public class MAPIValue implements Closeable {

    int type;
    RawInputStream rawData;

    /**
     * Constructs a MAPIValue containing a given value.
     *
     * @param type the value type
     * @param data the TNEF stream containing the value
     * @param length the length of the value data (in bytes)
     * @throws IllegalArgumentException if type is invalid
     * @throws IOException if an I/O error occurs
     */
    public MAPIValue(int type, RawInputStream data, int length) throws IOException {
        if ((type & MAPIProp.MV_FLAG) != 0)
            throw new IllegalArgumentException("multivalue is not allowed in single MAPIValue");
        this.type = type;
        this.rawData = new RawInputStream(data, 0, length);
        data.skip(length);
    }

    /**
     * Gets the MAPIValue type.
     *
     * @return the MAPIValue type
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the MAPIValue data length (in bytes).
     *
     * @return the MAPIValue data length (in bytes)
     */
    public int getLength() {
        return (int)rawData.getLength();
    }

    /**
     * Gets the MAPIValue data.
     *
     * @return the MAPIValue data
     */
    public byte[] getData() {
        try {
            return rawData.toByteArray();
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Gets the MAPIValue raw data.
     *
     * @return the MAPIValue raw data
     * @throws IOException if an I/O error occurs
     */
    public RawInputStream getRawData() throws IOException {
        return new RawInputStream(rawData);
    }

    /**
     * Closes all underlying resources used by this object.
     * After invoking this method, it should no longer be accessed.
     *
     * @throws IOException if an error occurs
     */
    public void close() throws IOException {
        rawData.close();
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        try {
            Object o = getValue();
            try {
                if (o instanceof byte[])
                    o = TNEFUtils.toHexString((byte[])o, 512);
                if (o instanceof Date) // use ISO-8601 format with no timezone
                    o = TNEFUtils.formatDate(((Date)o).getTime(), "yyyy-MM-dd'T'HH:mm:ss");
                return String.valueOf(o);
            } finally {
                if (o instanceof Closeable)
                    ((Closeable)o).close();
            }
        } catch (IOException ioe) {
            return "ERROR: " + ioe;
        }
    }

    /**
     * Returns the value encapsulated by this MAPIValue.
     * The returned Object should be cast into an appropriate class,
     * according to the MAPIValue's type.
     *
     * @return the value encapsulated by this MAPIValue
     * @throws IOException if an I/O error occurs
     */
    public Object getValue() throws IOException {
        boolean closeStream = true;
        RawInputStream ris = new RawInputStream(rawData); // a copy
        try {
            switch (type) {
                case MAPIProp.PT_NULL:
                    return null;

                case MAPIProp.PT_BOOLEAN:
                    return ris.readU16() != 0; // 2 bytes

                case MAPIProp.PT_SHORT:
                    return (short)ris.readU16(); // 2 bytes

                case MAPIProp.PT_INT:
                    return (int)ris.readU32(); // 4 bytes

                case MAPIProp.PT_FLOAT: // 4 bytes
                    return Float.intBitsToFloat((int)ris.readU32()); // 4 bytes

                case MAPIProp.PT_ERROR:
                    return (int)ris.readU32(); // 4 bytes

                case MAPIProp.PT_APPTIME:
                    // double that counts the number of days since 1899-12-30
                    // (fractional part is fraction of day since midnight)
                    double d = Double.longBitsToDouble(ris.readU64()); // 8 bytes
                    long millis = (long)(d * 1000L * 60 * 60 * 24); // days (including fraction) to millis
                    // subtract milliseconds between 1899-12-30 and 1970-01-01
                    millis -= 2209161600000L;
                    // Note: the comment below about timezones (under PT_SYSTIME) applies here too
                    return new Date(millis);

                case MAPIProp.PT_SYSTIME:
                    // 64-bit Windows FILETIME is 100ns since January 1, 1601 (UTC)
                    long time = ris.readU64() / 10000; // convert to milliseconds
                    // subtract milliseconds between 1601-01-01 and 1970-01-01 (including 89 leap year days)
                    time -= 1000L * 60 * 60 * 24 * (365 * 369 + 89);
                    // Note: while most date properties are in UTC, this is not guaranteed and it is
                    // up to whomever set the property to convert local dates to UTC, so the returned
                    // date may represent the time in another timezone in some cases
                    return new Date(time);

                case MAPIProp.PT_DOUBLE:
                    return Double.longBitsToDouble(ris.readU64()); // 8 bytes

                case MAPIProp.PT_CURRENCY:
                case MAPIProp.PT_INT8BYTE:
                    return ris.readU64(); // 8 bytes

                case MAPIProp.PT_CLSID:
                    return ris.toByteArray(); // CLSID - 16 bytes

                case MAPIProp.PT_STRING:
                    return ris.readString((int)ris.getLength()); // variable length string

                case MAPIProp.PT_UNICODE_STRING:
                    return ris.readStringUnicode((int)ris.getLength()); // variable length string

                case MAPIProp.PT_BINARY:
                case MAPIProp.PT_UNSPECIFIED:
                    closeStream = false; // it is returned so it shouldn't be closed
                    return ris; // variable length bytes

                case MAPIProp.PT_OBJECT:
                    closeStream = false; // it is returned so it shouldn't be closed
                    // get object IID
                    GUID iid = new GUID(ris.readBytes(16));
                    return iid.equals(MAPIProp.IID_IMESSAGE) ? new TNEFInputStream(ris) : ris;

                default:
                    throw new IOException("Unknown MAPI type: " + type);
            }
        } finally {
            if (closeStream)
                ris.close();
        }
    }

}
