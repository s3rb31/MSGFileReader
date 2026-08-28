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

import java.io.IOException;

/**
 * The <code>MAPIPropName</code> class encapsulates the name of a named MAPI property.
 * A MAPI Property name consists of a GUID, as well as either a 32-bit identifier
 * or a String.
 *
 * @author Amichai Rothman
 * @since 2003-07-25
 */
public class MAPIPropName {

    /**
     * MAPI property name type constant.
     */
    public static final int
        MNID_ID     = 0,
        MNID_STRING = 1;

    GUID guid;
    int type;
    long id;
    String name;
    int rawLength;

    /**
     * Constructs a MAPIPropName using the given TNEF stream.
     *
     * @param data the TNEF stream containing the property name data
     * @throws IOException if an I/O error occurs
     */
    public MAPIPropName(RawInputStream data) throws IOException {
        // get TRP structure values
        long startOffset = data.getPosition();
        guid = new GUID(data.readBytes(16));
        type = (int)data.readU32();
        if (type == MNID_STRING) {
            int length = (int)data.readU32();
            name = data.readStringUnicode(length);
            data.skip(-length & 3); // pad to 4 byte boundary
        } else if (type == MNID_ID) {
            id = data.readU32();
        } else {
            throw new IOException("invalid type: " + type);
        }
        rawLength = (int)(data.getPosition() - startOffset);
    }

    /**
     * Constructs a MAPIPropName containing given values.
     *
     * @param guid the property GUID
     * @param id the property ID
     */
    public MAPIPropName(GUID guid, long id) {
        this.guid = guid;
        this.type = MNID_ID;
        this.id = id;
    }

    /**
     * Constructs a MAPIPropName containing given values
     *
     * @param guid the property GUID
     * @param name the property name
     */
    public MAPIPropName(GUID guid, String name) {
        this.guid = guid;
        this.type = MNID_STRING;
        this.name = name;
    }

    /**
     * Gets the MAPIPropName GUID.
     *
     * @return the MAPIPropName GUID
     */
    public GUID getGUID() {
        return guid;
    }

    /**
     * Gets the MAPIPropName type.
     *
     * @return the MAPIPropName type
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the MAPIPropName ID.
     *
     * @return the MAPIPropName ID
     */
    public long getID() {
        return id;
    }

    /**
     * Gets the MAPIPropName name.
     *
     * @return the MAPIPropName name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the raw data length (in bytes) of this instance.
     *
     * @return the raw data length (in bytes) of this instance
     */
    protected int getRawLength() {
        return rawLength;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[GUID=").append(guid);
        if (type == MNID_STRING)
            s.append(" name=").append(name);
        else
            s.append(" id=0x").append(Long.toHexString(id));
        return s.append(']').toString();
    }

    /**
     * Returns whether this MAPIPropName is identical to the given MAPIPropName.
     *
     * @return <code>true</code> if this object is equal to the given
     *         object; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MAPIPropName))
            return false;
        MAPIPropName p = (MAPIPropName)o;
        return
            type == p.type
            && (type == MNID_ID ? id == p.id : name.equals(p.name))
            && guid.equals(p.guid);
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = result * 37 + type;
        result = result * 37 + guid.hashCode();
        if (type == MNID_ID)
            result = result * 37 + (int)(id ^ (id >>> 32));
        if (type == MNID_STRING && name != null)
            result = result * 37 + name.hashCode();
        return result;
    }

}
