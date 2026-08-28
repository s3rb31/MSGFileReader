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

package net.freeutils.tnef.msg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.Entry;
import net.freeutils.tnef.*;

/**
 * The {@code Properties} class parses property entries within ".msg" files.
 *
 * @author Amichai Rothman
 * @since 2007-06-16
 */
public class Properties {

    protected List<MAPIProp> props;
    protected int recipientCount;
    protected int attachmentCount;

    /**
     * Returns the parsed array of MAPI properties.
     *
     * @return the parsed array of MAPI properties
     */
    public MAPIProp[] toArray() {
        return props.toArray(new MAPIProp[props.size()]);
    }

    /**
     * Processes all properties under the given directory entry.
     *
     * @param dir the directory entry containing the properties
     * @param headerSize the expected header size in bytes (according to entry type)
     * @param names a map of property name IDs and their string representations
     * @return the parsed properties
     * @throws IOException if an error occurs
     */
    protected static Properties processProperties(DirectoryEntry dir, int headerSize,
            Map<Integer, MAPIPropName> names) throws IOException {
        Properties properties = new Properties();
        Entry entry = dir.getEntry("__properties_version1.0");

        // read header (all fields are U32 words)
        // - root (top level) message headers include fields 1-8 (32 bytes)
        // - nested messages headers include fields 1-6 (24 bytes)
        // - non-message attachments and recipients headers include fields 1-2 (8 bytes)

        if (headerSize != 8 && headerSize != 24 && headerSize != 32)
            throw new IOException("invalid header length: " + headerSize);

        RawInputStream data = Msg.toRawInputStream((DocumentEntry)entry);
        try {
            if (data.getLength() > 0) { // we saw a message with empty properties entry, so we just ignore it
                data.readU64(); // reserved
                if (headerSize > 8) {
                    int nextRecipientId = (int)data.readU32();
                    int nextAttachmentId = (int)data.readU32();
                    properties.recipientCount = (int)data.readU32();
                    properties.attachmentCount = (int)data.readU32();
                    if (headerSize > 24)
                        data.readU64(); // reserved
                }
            }

            // read MAPI properties list (fixed-length up to 8 bytes)
            properties.props = processPropertyList(data);
            // add all standalone property entries (fixed-length over 8 bytes, variable-length and multi-value)
            properties.props.addAll(processPropertyEntries(dir));
        } finally {
            data.close();
        }
        // translate named properties
        translateNames(properties.props, names);
        return properties;
    }

    /**
     * Processes a list of MAPI properties. The list includes
     * all fixed-length properties up to 8 bytes, and the lengths
     * (not values) of larger, variable-length and multi-value properties.
     * This method only returns properties with values, and skips the others.
     *
     * @param data the raw property list data
     * @return the parsed list of MAPI properties
     * @throws IOException if an error occurs
     */
    protected static List<MAPIProp> processPropertyList(RawInputStream data) throws IOException {
        // process a list of properties, each consisting of 4 U32 words:
        // the first is the MAPI id and type,
        // the second is flags (observed values are 2, 6, 7)
        // and the 3rd and 4th, according to the value type's length:
        //   4: the value and reserved padding value
        //   8: the value
        //   >8 and variable length: the length and a reserved padding value
        //      (the value itself is in a separate document entry with corresponding property ID)
        List<MAPIProp> props = new ArrayList<MAPIProp>();
        while (data.available() > 0) {
            int type = data.readU16();
            type = type & ~MAPIProp.MV_FLAG; // remove MV_FLAG
            int id = data.readU16();
            data.readU32(); // flags
            int typeSize = MAPIProp.getTypeSize(type);
            if (typeSize >= 0 && typeSize <= 8) {
                MAPIValue val = new MAPIValue(type, data, typeSize);
                props.add(new MAPIProp(type, id, val));
                data.skip(-typeSize & 7); // skip reserved bytes (pad to 8 bytes)
            } else {
                // the value itself is in a separate document entry, so we just skip here
                data.readU32(); // value length (plus null terminator for strings, -1 for embedded objects)
                data.readU32(); // reserved (flags)
            }
        }
        return props;
    }

    /**
     * Processes MAPI property entries under the given directory entry.
     *
     * @param dir the directory entry containing the properties
     * @return the parsed properties
     * @throws IOException if an error occurs
     */
    protected static List<MAPIProp> processPropertyEntries(DirectoryEntry dir) throws IOException {
        List<MAPIProp> props = new ArrayList<MAPIProp>();
        for (Entry entry : dir)
            if (entry instanceof DocumentEntry // exclude embedded objects
                    && entry.getName().length() == 20 // exclude variable-length multivalue values
                    && entry.getName().toLowerCase().startsWith("__substg1.0_"))
                props.add(processProperty((DocumentEntry)entry));
        return props;
    }

    /**
     * Processes an individual MAPI property entry (which may be mutli-valued).
     *
     * @param entry the entry containing the property
     * @return the parsed MAPI property
     * @throws IOException if an error occurs
     */
    protected static MAPIProp processProperty(DocumentEntry entry) throws IOException {
        // parse tag
        String name = entry.getName();
        int id = Integer.parseInt(name.substring(12, 16), 16);
        int type = Integer.parseInt(name.substring(16, 20), 16);
        boolean isMultivalue = (type & MAPIProp.MV_FLAG) != 0;
        type = type & ~MAPIProp.MV_FLAG; // remove MV_FLAG
        // read values
        MAPIValue[] vals;
        RawInputStream data = Msg.toRawInputStream(entry);
        try {
            if (!isMultivalue) {
                vals = new MAPIValue[] { new MAPIValue(type, data, (int)data.getLength()) };
            } else {
                // - fixed-length multiple values appear as array of values
                // - variable-length multiple values appear as array of lengths of values,
                //   which are themselves stored in separate entries
                int typeSize = MAPIProp.getTypeSize(type);
                List<MAPIValue> list = new ArrayList<MAPIValue>();
                while (data.available() > 0) { // iterate over array items
                    if (typeSize > -1) { // fixed-size value
                        list.add(new MAPIValue(type, data, typeSize));
                    } else { // variable-length value
                        long length = data.readU32(); // value length (we don't use it, to prevent OOME/DoS)
                        if (type == MAPIProp.PT_BINARY)
                            data.readU32(); // reserved
                        // get value from separate entry
                        String valEntryName = entry.getName() + "-" + TNEFUtils.toHexString(list.size(), 8);
                        DocumentEntry valEntry = (DocumentEntry)entry.getParent().getEntry(valEntryName);
                        RawInputStream valData = Msg.toRawInputStream(valEntry);
                        try {
                            list.add(new MAPIValue(type, valData, (int)valData.getLength()));
                        } finally {
                            valData.close();
                        }
                    }
                }
                vals = list.toArray(new MAPIValue[list.size()]);
            }
        } finally {
            data.close();
        }
        return new MAPIProp(type, id, vals);
    }

    /**
     * Processes property name-ID mappings under the given directory entry.
     *
     * @param dir the directory entry containing the name IDs
     * @return a map of property name IDs and their string representations
     * @throws IOException if an error occurs
     */
    protected static Map<Integer, MAPIPropName> processNameIDs(DirectoryEntry dir) throws IOException {
        DirectoryEntry entry = (DirectoryEntry)dir.getEntry("__nameid_version1.0");
        // create guid lookup table
        List<GUID> guids = new ArrayList<GUID>();
        guids.add(null); // indices are 1-based
        guids.add(MAPIProp.PS_MAPI);
        guids.add(MAPIProp.PS_PUBLIC_STRINGS);
        MAPIProp guidsProp = processProperty((DocumentEntry)entry.getEntry("__substg1.0_00020102"));
        RawInputStream guidsData = (RawInputStream)guidsProp.getValue();
        try {
            while (guidsData.available() > 0)
                guids.add(new GUID(guidsData.readBytes(16)));
        } finally {
            guidsData.close();
        }
        // get name strings data
        byte[] nameStrings;
        MAPIProp namesProp = processProperty((DocumentEntry)entry.getEntry("__substg1.0_00040102"));
        RawInputStream namesData = (RawInputStream)namesProp.getValue();
        try {
            nameStrings = namesData.toByteArray();
        } finally {
            namesData.close();
        }
        // create ID to name map
        Map<Integer, MAPIPropName> names = new HashMap<Integer, MAPIPropName>();
        MAPIProp propsProp = processProperty((DocumentEntry)entry.getEntry("__substg1.0_00030102"));
        RawInputStream propsData = (RawInputStream)propsProp.getValue();
        try {
            while (propsData.available() > 0) {
                byte[] item = propsData.readBytes(8);
                int idOrOffset = (int)TNEFUtils.getU32(item, 0);
                int guidIndex = TNEFUtils.getU16(item, 4); // and last bit is string flag
                GUID guid = guids.get(guidIndex >> 1);
                MAPIPropName propName;
                if ((guidIndex & 1) == 1) { // has name string
                    int len = (int)TNEFUtils.getU32(nameStrings, idOrOffset);
                    String nameStr = TNEFUtils.createStringUnicode(nameStrings, idOrOffset + 4, len);
                    propName = new MAPIPropName(guid, nameStr);
                } else { // has id
                    propName = new MAPIPropName(guid, idOrOffset);
                }
                int index = TNEFUtils.getU16(item, 6);
                names.put(0x8000 + index, propName);
            }
        } finally {
            propsData.close();
        }
        return names;
    }

    /**
     * Translates property names from IDs to their respective string names.
     *
     * @param props the properties whose names are to be translated
     * @param names a map of property name IDs and their string representations
     */
    protected static void translateNames(List<MAPIProp> props, Map<Integer, MAPIPropName> names) {
        if (names != null) {
            for (MAPIProp prop : props) {
                MAPIPropName name = names.get(prop.getID());
                if (name != null)
                    prop.setName(name);
            }
        }
    }
}
