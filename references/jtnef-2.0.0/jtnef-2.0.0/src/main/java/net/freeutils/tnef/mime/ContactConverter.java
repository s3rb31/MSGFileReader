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

package net.freeutils.tnef.mime;

import static net.freeutils.tnef.mime.ContactConverter.ContactField.*;
import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import net.freeutils.tnef.*;
import net.freeutils.tnef.Message;

/**
 * The <code>ContactConverter</code> class converts contacts from a TNEF
 * message into a standard vCard 3.0 attachment, as per RFC 2426.
 *
 * @author Amichai Rothman
 * @since 2007-04-27
 */
public class ContactConverter extends Converter {

    // the definition of all fields used in conversion from TNEF to vCard
    static ContactField[] contactFields = {
        new ContactField("BEGIN", "VCARD"), // must be first
        new ContactField("VERSION", "3.0"),
        new ContactField("FN", MUST, "%s %s %s %s",
            MAPIProp.PR_GIVEN_NAME,
            MAPIProp.PR_MIDDLE_NAME,
            MAPIProp.PR_SURNAME,
            MAPIProp.PR_GENERATION),
        new ContactField("N", MUST, "%s;%s;%s;%s;%s",
            MAPIProp.PR_SURNAME,
            MAPIProp.PR_GIVEN_NAME,
            MAPIProp.PR_MIDDLE_NAME,
            MAPIProp.PR_DISPLAY_NAME_PREFIX,
            MAPIProp.PR_GENERATION),
        new ContactField("NICKNAME", MAPIProp.PR_NICKNAME),
        //"PHOTO"
        new ContactField("BDAY", NONE, "yyyy-MM-dd'T'HH:mm:ss'Z'", MAPIProp.PR_BIRTHDAY),
        new ContactField("ADR;TYPE=WORK", NONE, "%s;%s;%s;%s;%s;%s;%s",
            null,
            MAPIProp.PR_OFFICE_LOCATION,
            MAPIProp.PR_STREET_ADDRESS,
            MAPIProp.PR_LOCALITY,
            MAPIProp.PR_STATE_OR_PROVINCE,
            MAPIProp.PR_POSTAL_CODE,
            MAPIProp.PR_COUNTRY),
        new ContactField("LABEL;TYPE=WORK", NONE, "%s\r\n%s\r\n%s, %s %s\r\n%s",
            MAPIProp.PR_OFFICE_LOCATION,
            MAPIProp.PR_STREET_ADDRESS,
            MAPIProp.PR_LOCALITY,
            MAPIProp.PR_STATE_OR_PROVINCE,
            MAPIProp.PR_POSTAL_CODE,
            MAPIProp.PR_COUNTRY),
        new ContactField("ADR;TYPE=HOME", NONE, "%s;%s;%s;%s;%s;%s;%s",
            null,
            null,
            MAPIProp.PR_HOME_ADDRESS_STREET,
            MAPIProp.PR_HOME_ADDRESS_CITY,
            MAPIProp.PR_HOME_ADDRESS_STATE_OR_PROVINCE,
            MAPIProp.PR_HOME_ADDRESS_POSTAL_CODE,
            MAPIProp.PR_HOME_ADDRESS_COUNTRY),
        new ContactField("LABEL;TYPE=HOME", NONE, "%s\r\n%s, %s %s\r\n%s",
            MAPIProp.PR_HOME_ADDRESS_STREET,
            MAPIProp.PR_HOME_ADDRESS_CITY,
            MAPIProp.PR_HOME_ADDRESS_STATE_OR_PROVINCE,
            MAPIProp.PR_HOME_ADDRESS_POSTAL_CODE,
            MAPIProp.PR_HOME_ADDRESS_COUNTRY),
        new ContactField("ADR;TYPE=POSTAL", NONE, "%s;%s;%s;%s;%s;%s;%s",
            null,
            null,
            MAPIProp.PR_OTHER_ADDRESS_STREET,
            MAPIProp.PR_OTHER_ADDRESS_CITY,
            MAPIProp.PR_OTHER_ADDRESS_STATE_OR_PROVINCE,
            MAPIProp.PR_OTHER_ADDRESS_POSTAL_CODE,
            MAPIProp.PR_OTHER_ADDRESS_COUNTRY),
        new ContactField("LABEL;TYPE=POSTAL", NONE, "%s\r\n%s, %s %s\r\n%s",
            MAPIProp.PR_OTHER_ADDRESS_STREET,
            MAPIProp.PR_OTHER_ADDRESS_CITY,
            MAPIProp.PR_OTHER_ADDRESS_STATE_OR_PROVINCE,
            MAPIProp.PR_OTHER_ADDRESS_POSTAL_CODE,
            MAPIProp.PR_OTHER_ADDRESS_COUNTRY),
        new ContactField("TEL;TYPE=HOME,VOICE", MAPIProp.PR_HOME_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=HOME", MAPIProp.PR_HOME2_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=HOME,FAX", MAPIProp.PR_HOME_FAX_NUMBER),
        new ContactField("TEL;TYPE=WORK,VOICE", MAPIProp.PR_BUSINESS_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=WORK,VOICE", MAPIProp.PR_BUSINESS2_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=WORK,FAX", MAPIProp.PR_BUSINESS_FAX_NUMBER),
        new ContactField("TEL;TYPE=CELL,VOICE", MAPIProp.PR_MOBILE_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=CAR,VOICE", MAPIProp.PR_CAR_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=PREF,VOICE", MAPIProp.PR_CALLBACK_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=VOICE", MAPIProp.PR_OTHER_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=PAGER,VOICE", MAPIProp.PR_PAGER_TELEPHONE_NUMBER),
        new ContactField("TEL;TYPE=ISDN", MAPIProp.PR_ISDN_NUMBER),
        new ContactField("TEL;TYPE=PREF", MAPIProp.PR_PRIMARY_TELEPHONE_NUMBER),
        new ContactField("EMAIL;TYPE=PREF,INTERNET",
            new MAPIPropName(MAPIProp.PSETID_Address, 0x8083)), // CdoContact_EmailEmailAddress
        new ContactField("EMAIL;TYPE=TLX", MAPIProp.PR_TELEX_NUMBER),
        //"MAILER"
        //"TZ"
        //"GEO"
        new ContactField("TITLE", MAPIProp.PR_TITLE),
        new ContactField("ROLE", MAPIProp.PR_PROFESSION),
        //"LOGO"
        //"AGENT"
        new ContactField("ORG", NONE, "%s;%s",
            MAPIProp.PR_COMPANY_NAME,
            MAPIProp.PR_DEPARTMENT_NAME),
        //"CATEGORIES"
        new ContactField("NOTE", ATTR, null, Attr.attBody),
        //"PRODID"
        //"REV"
        //"SORT-STRING
        //"SOUND"
        //"UID"
        new ContactField("URL;TYPE=WORK", MAPIProp.PR_BUSINESS_HOME_PAGE),
        //"CLASS"
        new ContactField("KEY;TYPE=X509", BINARY | CERT, null,
            MAPIProp.PR_USER_X509_CERTIFICATE),
        new ContactField("END", "VCARD") // must be last
    };

    /**
     * The internal <code>ContactField</code> class represents a mapping of a single
     * field from its TNEF source to its vCard destination, and all methods required
     * to perform the field's conversion: parameters, types, formatting, escaping,
     * encoding, folding, etc, according to the vCard RFC.
     */
    static class ContactField {

        /**
         * ContactField flag constant.
         */
        public static final int
            NONE      = 0,
            MUST      = 1,
            ATTR      = 2,
            BINARY    = 4,
            CERT      = 8;

        String type;    // the vCard prefix string
        int flags;      // flags used to determine the conversion type
        String format;  // formatting string for output (where applicable)
        Object src;     // the corresponding TNEF source object

        /**
         * Constructs a ContactField.
         * <p>
         * The source object determines how the value is extracted from a message:
         * <ul>
         * <li>String - the value is the string literal</li>
         * <li>Integer - the value(s) are taken from the MAPI property with the given ID,
         *     or, if the ATTR flag is specified, from the attribute with the given ID</li>
         * <li>MAPIPropName - the value(s) are taken from the MAPI property with the given name</li>
         * <li>Integer[] - the values are taken from the MAPI properties with the given IDs,
         *     and are then combined using the given format string into a single value</li>
         * </ul>
         *
         * @param type the field type (excluding the encoding parameter)
         * @param flags the conversion flags
         * @param format the format string (null if not applicable)
         * @param src the TNEF source object
         */
        public ContactField(String type, int flags, String format, Object src) {
            this.type = type;
            this.src = src;
            this.flags = flags;
            this.format = format;
            if (isFlag(BINARY))
                this.type += ";ENCODING=b";
        }

        /**
         * Constructs a ContactField.
         *
         * @param type the field type (excluding the encoding parameter)
         * @param flags the conversion flags
         * @param format the format string (null if not applicable)
         * @param src the TNEF source object
         * @see #ContactField(String, int, String, Object)
         */
        public ContactField(String type, int flags, String format, Integer... src) {
            this(type, flags, format, src.length == 1 ? src[0] : (Object)src);
        }

        /**
         * Constructs a ContactField with given type and source,
         * and with the NONE flag and a null format string.
         *
         * @param type the field type (excluding the encoding parameter)
         * @param src the TNEF source object
         * @see #ContactField(String, int, String, Object)
         */
        public ContactField(String type, Object src) {
            this(type, NONE, null, src);
        }

        /**
         * Returns the vCard field string corresponding to this contact field,
         * the data itself taken from the given TNEF message.
         * The returned string is properly encoded, escaped, formatted, folded
         * etc. according to the vCard RFC.
         *
         * @param message the TNEF message from which the field data is taken
         * @return the vCard field string with the required contact data
         * @throws IOException if an error occurs while reading from message
         */
        public String getValue(net.freeutils.tnef.Message message) throws IOException {
            String v = null;
            if (src instanceof Integer) {
                int id = (Integer)src;
                if (isFlag(ATTR)) {
                    Attr attr = message.getAttribute(id);
                    if (attr != null)
                        v = toVCardField(toString(attr.getValue()), true);
                } else {
                    v = toVCardFields(message.getMAPIProps().getProp(id));
                }
            } else if (src instanceof MAPIPropName) {
                MAPIPropName propname = (MAPIPropName)src;
                v = toVCardFields(message.getMAPIProps().getProp(propname));
            } else if (src instanceof Integer[]) {
                Integer[] ids = (Integer[])src;
                int len = ids.length;
                String[] vals = new String[len];
                boolean hasValue = false;
                for (int i = 0; i < len; i++) {
                    if (ids[i] != null)
                        vals[i] = escape((String)message.getMAPIProps().getPropValue(ids[i]));
                    if (vals[i] == null)
                        vals[i] = "";
                    else
                        hasValue = true;
                }
                if (hasValue)
                    v = toVCardField(String.format(format, vals), false);
            } else if (src instanceof String) {
                v = toVCardField((String)src, true);
            } else {
                throw new IllegalArgumentException("Invalid source: " + src);
            }
            return v;
        }

        /**
         * Converts a property to zero or more concatenated vCard fields, one per value.
         *
         * @param prop the property to convert
         * @return the concatenated fields, or an empty string if there are no values,
         *         or null if the given property is null
         * @throws IOException
         */
        private String toVCardFields(MAPIProp prop) throws IOException {
            String v = null;
            if (prop != null) {
                v = "";
                for (MAPIValue val : prop.getValues()) {
                    if (val != null) {
                        String vstr = toVCardField(toString(val.getValue()), true);
                        if (vstr != null)
                            v += vstr;
                    }
                }
            }
            return v;
        }

        /**
         * Converts the given value to a fully formatted vCard field
         * which can be appended to the vCard text.
         *
         * @param v the field value
         * @param escape specifies whether the value should be {@link #escape escaped}
         * @return the converted field, or null if the value is empty and the field is optional
         */
        String toVCardField(String v, boolean escape) {
            // escape the value
            if (escape)
                v = escape(v);

            // treat empty strings as no value
            if (v != null && v.trim().length() == 0)
                v = null;

            // force required attributes to be specified
            if (isFlag(MUST) && v == null)
                v = "";

            if (v != null) {
                // escape line breaks within value
                v = TNEFUtils.replace(v, "\r\n", "\\n");
                v = TNEFUtils.replace(v, "\n", "\\n");
                // prepare final value and fold
                v = fold(type + ":" + v) + "\r\n";
            }
            return v;
        }

        /**
         * Returns whether this ContactField contains the given flag.
         *
         * @param flag the flag to check
         * @return true if this ContactField contains the given flag,
         *         false otherwise
         */
        boolean isFlag(int flag) {
            return (flags & flag) != 0;
        }

        /**
         * Returns a string representation of the given value.
         *
         * @param val the value to represent as a string
         * @return a string representation of the given value
         * @throws IOException if an error occurs while processing value
         */
        String toString(Object val) throws IOException {
            String v;
            if (val == null) {
                v = null;
            } else if (val instanceof Date) {
                // format the date according to given format (mapi props should be GMT)
                v = TNEFUtils.formatDate(((Date)val).getTime(), format);
            } else if (val instanceof RawInputStream) {
                RawInputStream ris = (RawInputStream)val;
                v = null;
                try {
                    if (isFlag(CERT)) { // special handling for certificates
                        RawInputStream cert = getTLVProp(ris, 0x0003); // the actual certificate bytes
                        if (cert != null) {
                            try {
                                // make sure it's a full certificate and not just its hash
                                if (cert.available() > 32)
                                    v = toBase64(cert);
                            } finally {
                                cert.close();
                            }
                        }
                    } else { // any other kind of data
                        v = toBase64(ris);
                    }
                } finally {
                    ris.close();
                }
            } else {
                v = val.toString();
            }
            return v;
        }

        /**
         * Folds the given string according to the vCard RFC.
         *
         * @param v the value to fold
         * @return the folded string
         */
        String fold(String v) {
            int vlen = v.length();
            if (vlen > 75) { // apply folding only if necessary
                StringBuilder folded = new StringBuilder((int)(vlen * (1 + 3.0 / 75)));
                int breakpos = -1;
                int count = 0;
                for (int i = 0; i < vlen; i++, count++) {
                    char ch = v.charAt(i);
                    // mark potential break positions
                    if (Character.isWhitespace(ch) || ch == '\\')
                        breakpos = i;
                    // break the line when we have enough chars on it
                    if (count == 74) {
                        if (breakpos == -1) // if there's no preferred break position,
                            breakpos = i; // force a break at end of line
                        folded.append(v.substring(i - count, breakpos)).append("\r\n ");
                        count = i - breakpos;
                        breakpos = -1;
                    }
                }
                // add the last partial line
                if (count > 0)
                    folded.append(v.substring(vlen - count));
                v = folded.toString();
            }
            return v;
        }

        /**
         * Escapes the given string according to the vCard RFC.
         *
         * @param v the string to escape
         * @return the escaped string
         */
        static String escape(String v) {
            if (v == null)
                return null;
            v = TNEFUtils.replace(v, "\r\n", "\\n");
            v = TNEFUtils.replace(v, "\n", "\\n");
            v = TNEFUtils.replace(v, ",", "\\,");
            v = TNEFUtils.replace(v, ";", "\\;");
            return v;
        }

        /**
         * Encodes the bytes in the given stream as a Base64 string.
         *
         * @param in the stream containing the bytes to encode
         * @return the encoded Base64 string
         * @throws IOException if an I/O error occurs
         */
        static String toBase64(InputStream in) throws IOException {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                OutputStream out = MimeUtility.encode(bout, "base64");
                TNEFUtils.transfer(in, out, -1, false, true);
                String s = bout.toString("US-ASCII");
                return TNEFUtils.replace(s, "\r\n", ""); // remove MimeUtility's folding
            } catch (MessagingException me) {
                throw new IOException(me.toString());
            }
        }

        /**
         * Retrieves the value of the property with given tag
         * from within a stream, which is treated as a
         * tag-length-value list of properties.
         *
         * @param in the RawInputStream containing the tag-length-value data
         * @param tag the tag whose value is requested
         * @return the requested tag's value, or null if not found
         * @throws IOException if an I/O error occurs
         */
        static RawInputStream getTLVProp(RawInputStream in, int tag) throws IOException {
            in = new RawInputStream(in); // make a copy, don't modify original
            try {
                while (in.available() >= 4) {
                    int tg = in.readU16();
                    int len = in.readU16() - 4; // subtract the tag and length fields
                    if (tg == tag)
                        return (RawInputStream)in.newStream(0, len);
                    in.skip(len);
                }
                return null;
            } finally {
                in.close();
            }
        }
    }

    @Override
    public boolean canConvert(Message message) {
        return isMessageClass(message, "IPM.Contact");
    }

    @Override
    public TNEFMimeMessage convert(Message message, TNEFMimeMessage mime)
            throws IOException, MessagingException {
        // convert content
        StringBuilder vcard = new StringBuilder();
        for (ContactField contactField : contactFields) {
            String v = contactField.getValue(message);
            if (v != null)
                vcard.append(v);
        }
        // prepare name
        String name = null;
        Attr attr = message.getAttribute(Attr.attSubject); // should be the contact name
        if (attr != null)
            name = (String)attr.getValue();
        if (name == null || name.length() == 0)
            name = "contact";
        name = '"' + MimeUtility.encodeWord(name + ".vcf", "UTF-8", null) + '"';
        // create attachment
        MimeMultipart mp = new MimeMultipart();
        Part part = TNEFMime.addTextPart(mp, vcard.toString(), "text/x-vcard; name=" + name);
        part.setHeader("Content-Disposition", "attachment; filename=" + name);
        mime.setContent(mp);
        return mime;
    }

}
