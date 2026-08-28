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

/**
 * The <code>GUID</code> class encapsulates a GUID (Globally Unique IDentifier).
 * GUID instances are immutable.
 *
 * @author Amichai Rothman
 * @since 2007-07-19
 */
public class GUID {

    String guid; // in canonized form

    /**
     * Constructs a GUID with the specified value.
     *
     * @param guid the GUID value as a hex string (with optional canonical dashes)
     * @throws IllegalArgumentException if the given string does not
     *         contain a valid GUID
     */
    public GUID(String guid) {
        this.guid = canonize(guid);
    }

    /**
     * Constructs a GUID with the specified value.
     * The first three data fields are in little-endian order.
     *
     * @param guid the GUID value
     * @throws IllegalArgumentException if the given array does not
     *         contain a valid GUID
     */
    public GUID(byte[] guid) {
        StringBuilder s = new StringBuilder(32);
        for (byte b : reverse(guid))
            TNEFUtils.appendHexString(s, b & 0xFF, 2);
        this.guid = canonize(s.toString());
    }

    /**
     * Canonizes the given GUID string into the canonical format of the form:
     * "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", with all values presented as
     * lowercase hex digits.
     *
     * Valid GUIDs consist of 32 case insensitive hex digits (representing 16 bytes),
     * with or without the separating dashes in the appropriate positions.
     *
     * @param guid the GUID value
     * @return the canonized GUID string
     * @throws IllegalArgumentException if the given string does not
     *         contain a valid GUID
     */
    public static String canonize(String guid) {
        int len = guid.length();
        if (len != 32 && len != 36)
            throw new IllegalArgumentException("invalid GUID: " + guid
                + " (string length must be 32 without dashes, or 36 with dashes)");

        char[] chars = new char[36];
        for (int src = 0, dst = 0; src < len; src++, dst++) {
            char c = guid.charAt(src);
            if (dst == 8 || dst == 13 || dst == 18 || dst == 23) {
                chars[dst++] = '-';
                if (len == 36) {
                    if (c != '-')
                        throw new IllegalArgumentException("invalid GUID: " + guid
                            + " ('-' expected at position " + src + ")");
                    c = guid.charAt(++src);
                }
            }
            if (c >= 'A' && c <= 'F')
                c += 'a' - 'A'; // to lowercase
            else if (c < '0' || c > 'f' || (c > '9' && c < 'a'))
                throw new IllegalArgumentException("invalid GUID: " + guid
                    + " (invalid hex character at position " + src + ": '" + c + "')");
            chars[dst] = c;
        }
        return new String(chars);
    }

    /**
     * Reverses the endianness of the first three data fields of the given GUID.
     *
     * @param guid the GUID value to reverse
     * @return the reversed guid
     */
    public static byte[] reverse(byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentException("invalid GUID (must be exactly 16 bytes long)");
        byte[] reversed = new byte[16];
        reversed[0] = guid[3];
        reversed[1] = guid[2];
        reversed[2] = guid[1];
        reversed[3] = guid[0];
        reversed[4] = guid[5];
        reversed[5] = guid[4];
        reversed[6] = guid[7];
        reversed[7] = guid[6];
        System.arraycopy(guid, 8, reversed, 8, 8);
        return reversed;
    }

    /**
     * Returns the GUID as a 16-byte array.
     * The first three data fields are in little-endian order.
     *
     * @return the GUID as a 16-byte array
     */
    public byte[] toByteArray() {
        byte[] b = new byte[16];
        for (int src = 0, dst = 0; dst < 16; src += 2, dst++) {
            if (src == 8 || src == 13 || src == 18 || src == 23)
                src++;
            b[dst] = (byte)(Integer.parseInt(guid.substring(src, src + 2), 16) & 0xFF);
        }
        return reverse(b);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return guid;
    }

    /**
     * Returns whether this GUID is identical to the given GUID.
     *
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof GUID && guid.equals(((GUID)o).guid);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return guid.hashCode();
    }

}
