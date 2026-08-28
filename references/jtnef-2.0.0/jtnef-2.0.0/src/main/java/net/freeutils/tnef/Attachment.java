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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * The <code>Attachment</code> class encapsulates a TNEF message attachment.
 *
 * @author Amichai Rothman
 * @since 2003-04-25
 */
public class Attachment implements Closeable {

    List<Attr> attributes;
    String filename;
    RawInputStream rawData;
    MAPIProps props;
    Message nestedMessage;

    /**
     * Constructs an empty Attachment.
     */
    public Attachment() {
        attributes = new ArrayList<Attr>();
    }

    /**
     * Gets the Attachment attributes.
     *
     * @return the Attachment attributes
     */
    public List<Attr> getAttributes() {
        return attributes;
    }

    /**
     * Sets the Attachment attributes.
     *
     * @param attributes the Attachment attributes
     */
    public void setAttributes(List<Attr> attributes) {
        this.attributes = attributes;
    }

    /**
     * Gets a specific Attachment attribute.
     *
     * @param id the requested attribute ID
     * @return the requested Attachment attribute, or null if no such
     *         attribute exists
     */
    public Attr getAttribute(int id) {
        return Attr.findAttr(attributes, id);
    }

    /**
     * Gets the Attachment filename.
     * <p>
     * Security note: if this filename is used when writing to disk,
     * it must first be sanitized to prevent writing to unwanted directories,
     * overwriting existing files etc.
     *
     * @return the Attachment filename, or null if none exists
     */
    public String getFilename() {
        if (filename == null) { // don't override previously set filename
            try {
                if (props != null) {
                    // long filename
                    filename = (String)props.getPropValue(MAPIProp.PR_ATTACH_LONG_FILENAME);
                    // or short filename
                    if (filename == null)
                        filename = (String)props.getPropValue(MAPIProp.PR_ATTACH_FILENAME);
                }
                // or transport filename
                if (filename == null) {
                    Attr attr = getAttribute(Attr.attAttachTransportFilename);
                    if (attr != null)
                        filename = (String)attr.getValue();
                }
                // or title (filename)
                if (filename == null) {
                    Attr attr = getAttribute(Attr.attAttachTitle);
                    if (attr != null)
                        filename = (String)attr.getValue();
                }
            } catch (IOException ioe) {
                filename = null;
            }
        }
        return filename;
    }

    /**
     * Sets the Attachment filename.
     *
     * @param filename the Attachment filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Adds an Attachment attribute.
     *
     * @param attr the Attachment attribute to add
     */
    public void addAttribute(Attr attr) {
        attributes.add(attr);
    }

    /**
     * Gets the Attachment raw data.
     *
     * @return the Attachment raw data
     */
    public RawInputStream getRawData() {
        return rawData;
    }

    /**
     * Sets the Attachment raw data.
     *
     * @param rawData the Attachment raw data
     */
    public void setRawData(RawInputStream rawData) {
        if (this.rawData != null) {
            try {
                this.rawData.close();
            } catch (IOException ignore) {}
        }
        this.rawData = rawData;
    }

    /**
     * Gets the Attachment nested message.
     *
     * @return the Attachment nested message
     */
    public Message getNestedMessage() {
        return nestedMessage;
    }

    /**
     * Sets the Attachment nested message.
     *
     * @param nestedMessage the Attachment nested message
     */
    public void setNestedMessage(Message nestedMessage) {
        this.nestedMessage = nestedMessage;
    }

    /**
     * Gets the Attachment MAPI properties.
     *
     * @return the Attachment MAPI properties
     */
    public MAPIProps getMAPIProps() {
        return props;
    }

    /**
     * Closes all underlying resources used by this object.
     * After invoking this method, it should no longer be accessed.
     *
     * @throws IOException if an error occurs
     */
    public void close() throws IOException {
        TNEFUtils.closeAll(attributes);
        TNEFUtils.closeAll(rawData, props, nestedMessage);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Attachment:");
        for (Attr attribute : attributes)
            s.append("\n  ").append(attribute);
        if (rawData != null)
            s.append("\n  data=").append(rawData);
        if (props != null) {
            s.append("\n  props=");
            for (MAPIProp prop : props.getProps())
                s.append("\n    ").append(prop);
        }
        if (nestedMessage != null)
            s.append("\n  Nested Message:").append(nestedMessage);
        return s.toString();
    }

    /**
     * Sets this Attachment's MAPI properties. Special properties will be
     * translated into the Attachment's fields.
     *
     * @param props a collection of properties to set
     * @throws IOException if an I/O error occurs
     */
    public void setMAPIProps(MAPIProps props) throws IOException {
        this.props = props;
        // pick out interesting attributes that should go in Attachment
        if (props != null) {
            // attachment data object
            MAPIProp prop = props.getProp(MAPIProp.PR_ATTACH_DATA_OBJ);
            if (prop != null && prop.getLength() > 0) {
                MAPIValue value = prop.getValues()[0];
                if (value != null) {
                    // set raw data
                    RawInputStream data = value.getRawData();
                    if (prop.getType() == MAPIProp.PT_OBJECT) // not PT_BINARY
                        data.readBytes(16); // remove GUID
                    setRawData(data);
                    // set parsed nested message, if any
                    Object o = value.getValue();
                    try {
                        if (o instanceof TNEFInputStream)
                            setNestedMessage(new Message((TNEFInputStream)o));
                    } finally {
                        if (o instanceof Closeable)
                            ((Closeable)o).close();
                    }
                }
            }
        }
    }

    /**
     * Writes the content of this attachment to a file.
     *
     * @param filename the fully qualified filename to which the attachment
     *        content should be written
     * @throws IOException if an I/O error occurs
     */
    public void writeTo(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        try {
            writeTo(fos);
        } finally {
            fos.close();
        }
    }

    /**
     * Writes the content of this attachment to a stream.
     *
     * @param out the OutputStream to which the attachment
     *        content should be written
     * @throws IOException if an I/O error occurs
     */
    public void writeTo(OutputStream out) throws IOException {
        if (rawData != null)
            TNEFUtils.transfer(new RawInputStream(rawData), out, -1, true, false);
    }

}
