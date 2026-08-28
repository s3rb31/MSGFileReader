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

/**
 * The <code>MAPIProps</code> class encapsulates a collection of MAPI properties.
 *
 * @author Amichai Rothman
 * @since 2003-08-15
 */
public class MAPIProps implements Closeable {

    MAPIProp[] props;
    int rawLength;

    /**
     * Creates MAPIProps using the given TNEF stream.
     *
     * @param data the TNEF stream containing property data
     * @throws IOException if an I/O error occurs
     */
    public MAPIProps(RawInputStream data) throws IOException {
        long startOffset = data.getPosition();
        int count = (int)data.readU32();
        // validate size before memory allocation to prevent OOME/DoS
        if (count * 4 > data.available())
            throw new IOException("count is too large: " + count);
        props = new MAPIProp[count];
        for (int i = 0; i < count; i++)
            props[i] = new MAPIProp(data);
        rawLength = (int)(data.getPosition() - startOffset);
    }

    /**
     * Creates MAPIProps using the given properties.
     *
     * @param props an array of MAPI properties
     */
    public MAPIProps(MAPIProp... props) {
        this.props = props;
    }

    /**
     * Gets all the properties.
     *
     * @return all the properties
     */
    public MAPIProp[] getProps() {
        return props;
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
     * Gets a property with the given ID.
     *
     * @param id the requested property ID
     * @return the requested property, or null if no such property exists
     */
    public MAPIProp getProp(int id) {
        return MAPIProp.findProp(props, id);
    }

    /**
     * Gets a property with the given name.
     *
     * @param name the requested property name
     * @return the requested property, or null if no such property exists
     */
    public MAPIProp getProp(MAPIPropName name) {
        return MAPIProp.findProp(props, name);
    }

    /**
     * Gets the first value of a specific MAPI property, if it exists.
     * This is a convenience method for single-value properties.
     *
     * @param id the ID of the requested property
     * @return the value of the requested property, or null if it does not exist
     * @throws IOException if an I/O error occurs
     */
    public Object getPropValue(int id) throws IOException {
        MAPIProp prop = getProp(id);
        return prop != null ? prop.getValue() : null;
    }

    /**
     * Gets the first value of a specific MAPI property, if it exists.
     * This is a convenience method for single-value properties.
     *
     * @param name the name of the requested property
     * @return the value of the requested property, or null if it does not exist
     * @throws IOException if an I/O error occurs
     */
    public Object getPropValue(MAPIPropName name) throws IOException {
        MAPIProp prop = getProp(name);
        return prop != null ? prop.getValue() : null;
    }

    /**
     * Closes all underlying resources used by this object.
     * After invoking this method, it should no longer be accessed.
     *
     * @throws IOException if an error occurs
     */
    public void close() throws IOException {
        TNEFUtils.closeAll(props);
    }

}
