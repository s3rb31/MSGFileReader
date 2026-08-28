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
import java.util.Arrays;

/**
 * The <code>MAPIProp</code> class encapsulates a MAPI property.
 *
 * @author Amichai Rothman
 * @since 2003-07-25
 */
public class MAPIProp implements Closeable {

    /**
     * MAPI property type constant.
     */
    public static final int
        MV_FLAG             = 0x1000, // OR with type means multiple values
        PT_UNSPECIFIED      = 0x0000, // Unspecified
        PT_NULL             = 0x0001, // null property (no value)
        PT_SHORT            = 0x0002, // short (signed 16 bits)
        PT_INT              = 0x0003, // integer (signed 32 bits)
        PT_FLOAT            = 0x0004, // float (4 bytes)
        PT_DOUBLE           = 0x0005, // double
        PT_CURRENCY         = 0x0006, // currency (64 bits)
        PT_APPTIME          = 0x0007, // application time
        PT_ERROR            = 0x000a, // error (32 bits)
        PT_BOOLEAN          = 0x000b, // boolean (16 bits, non-zero true)
        PT_OBJECT           = 0x000d, // embedded object
        PT_INT8BYTE         = 0x0014, // 8 byte signed int
        PT_STRING           = 0x001e, // string
        PT_UNICODE_STRING   = 0x001f, // unicode-string (null terminated)
        PT_SYSTIME          = 0x0040, // time (64 bits)
        PT_CLSID            = 0x0048, // OLE GUID
        PT_BINARY           = 0x0102; // binary

    /**
     * MAPI recipient type constant.
     */
    public static final int
        MAPI_ORIG       = 0,          // Recipient is message originator
        MAPI_TO         = 1,          // Recipient is a primary recipient
        MAPI_CC         = 2,          // Recipient is a copy recipient
        MAPI_BCC        = 3,          // Recipient is blind copy recipient
        MAPI_P1         = 0x10000000, // Recipient is a P1 resend recipient
        MAPI_SUBMITTED  = 0x80000000; // Recipient is already processed

    /**
     * MAPI IID for properties of type PT_OBJECT which are supported by TNEF.
     */
    public static final GUID
        IID_ISTORAGE    = new GUID("0000000b-0000-0000-c000-000000000046"),
        IID_ISTREAM     = new GUID("0000000c-0000-0000-c000-000000000046"),
        IID_IMESSAGE    = new GUID("00020307-0000-0000-c000-000000000046");

    /**
     * MAPI property set GUID constant for named properties.
     */
    public static final GUID
        PS_MAPI                     = new GUID("00020328-0000-0000-c000-000000000046"),
        PS_PUBLIC_STRINGS           = new GUID("00020329-0000-0000-c000-000000000046"),
        PS_INTERNET_HEADERS         = new GUID("00020386-0000-0000-c000-000000000046"),
        PSETID_Appointment          = new GUID("00062002-0000-0000-c000-000000000046"),
        PSETID_Task                 = new GUID("00062003-0000-0000-c000-000000000046"),
        PSETID_Address              = new GUID("00062004-0000-0000-c000-000000000046"), // IPM.Contact
        PSETID_Common               = new GUID("00062008-0000-0000-c000-000000000046"),
        PSETID_Log                  = new GUID("0006200a-0000-0000-c000-000000000046"), // IPM.Activity
        PSETID_Note                 = new GUID("0006200e-0000-0000-c000-000000000046"), // IPM.StickyNote
        PSETID_Sharing              = new GUID("00062040-0000-0000-c000-000000000046"),
        PSETID_PostRss              = new GUID("00062041-0000-0000-c000-000000000046"),
        PSETID_CalendarAssistant    = new GUID("11000e07-b51b-40d6-af21-caa85edab1d0"),
        PSETID_XmlExtractedEntities = new GUID("23239608-685d-4732-9c55-4c95cb4e8e33"),
        PSETID_Messaging            = new GUID("41f28f13-83f4-4114-a584-eedb5a6b0bff"),
        PSETID_UnifiedMessaging     = new GUID("4442858e-a9e3-4e80-b900-317a210cc15b"),
        PSETID_Meeting              = new GUID("6ed8da90-450b-101b-98da-00aa003f1305"),
        PSETID_AirSync              = new GUID("71035549-0739-4dcb-9163-00f0580dbbdf"),
        PSETID_Attachment           = new GUID("96357f7f-59e1-47d0-99a7-46515c183b54");

    int type;
    int id;
    MAPIPropName name;
    MAPIValue[] values;

    /**
     * Constructs a MAPIProp using the given TNEF stream.
     *
     * @param data the TNEF stream containing the property data
     * @throws IOException if an I/O error occurs
     */
    public MAPIProp(RawInputStream data) throws IOException {
        type = data.readU16();
        boolean isMultiValue = (type & MV_FLAG) != 0;
        type &= ~MV_FLAG; // remove MV_FLAG
        int typeSize = MAPIProp.getTypeSize(type);
        if (typeSize < 0)
            isMultiValue = true; // all variable-length types are same as MV
        id = data.readU16();

        // handle named properties, which include the name before the value(s)
        if (id >= 0x8000 && id <= 0xFFFE)
            name = new MAPIPropName(data);

        // handle multivalue properties
        int valueCount = isMultiValue ? (int)data.readU32() : 1;
        // validate size before memory allocation to prevent OOME/DoS
        // if we have above 1024 values, require at least 1 actual byte per value
        if (valueCount > 1024 && valueCount > data.available())
            throw new IOException("count is too large: " + valueCount);

        // get the value(s)
        values = new MAPIValue[valueCount];
        for (int i = 0; i < valueCount; i++) {
            int length = typeSize >= 0 ? typeSize : (int)data.readU32(); // value length
            values[i] = new MAPIValue(type, data, length);
            data.skip(-length & 3); // pad to 4-byte boundary
        }
    }

    /**
     * Constructs a MAPIProp containing the specified values.
     *
     * @param type the property type (from PT_* constants)
     * @param id the property ID (from PR_*constants)
     * @param values the value(s) of this property
     */
    public MAPIProp(int type, int id, MAPIValue... values) {
        this.type = type;
        this.id = id;
        this.values = values != null ? values : new MAPIValue[0];
    }

    /**
     * Gets the MAPIProp type.
     *
     * @return the MAPIProp type
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the MAPIProp ID.
     *
     * @return the MAPIProp ID
     */
    public int getID() {
        return id;
    }

    /**
     * Gets the MAPIProp name.
     *
     * @return the MAPIProp name
     */
    public MAPIPropName getName() {
        return name;
    }

    /**
     * Sets the MAPIProp name.
     *
     * @param name the MAPIProp name
     */
    public void setName(MAPIPropName name) {
        this.name = name;
    }

    /**
     * Gets the number of MAPIProp values.
     *
     * @return the number of MAPIProp values
     */
    public int getLength() {
        return values.length;
    }

    /**
     * Gets the MAPIProp values.
     *
     * @return the MAPIProp values
     */
    public MAPIValue[] getValues() {
        return values;
    }

    /**
     * Gets the first MAPIProp value.
     * This is a convenience method for single-value properties.
     *
     * @return the first MAPIProp value
     * @throws IOException if an I/O error occurs
     */
    public Object getValue() throws IOException {
        return (values.length > 0 && values[0] != null) ? values[0].getValue() : null;
    }

    /**
     * Closes all underlying resources used by this object.
     * After invoking this method, it should no longer be accessed.
     *
     * @throws IOException if an error occurs
     */
    public void close() throws IOException {
        TNEFUtils.closeAll(values);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("MAPIProp:").append(" type=")
         .append(TNEFUtils.getConstName(getClass(), "PT_", type));
        if (name == null) {
            s.append(" id=").append(TNEFUtils.getConstName(getClass(), "PR_", id));
        } else {
            s.append(" name=").append(name);
            s.append(" id=0x").append(TNEFUtils.toHexString(id, 4).toLowerCase()); // temp id of name
        }
        if (getLength() == 0)
            s.append(" value=").append((Object)null);
        else if (getLength() == 1)
            s.append(" value=").append(values[0]);
        else
            s.append(" values=").append(Arrays.toString(values));
        return s.toString();
    }

    /**
     * Finds a property with the specified ID within given property array.
     *
     * @param props the property array to search
     * @param id the ID of the property to search for
     * @return a property with given ID found in the property array,
     *         or null if no such property exists
     */
    public static MAPIProp findProp(MAPIProp[] props, int id) {
        if (props != null) {
            for (MAPIProp prop : props)
                if (prop.getID() == id)
                    return prop;
        }
        return null;
    }

    /**
     * Finds a property with the specified name within given property array.
     *
     * @param props the property array to search
     * @param name the property name of the property to search for
     * @return a property with given name found in the property array,
     *         or null if no such property exists
     */
    public static MAPIProp findProp(MAPIProp[] props, MAPIPropName name) {
        if (props != null) {
            for (MAPIProp prop : props)
                if (name.equals(prop.getName()))
                    return prop;
        }
        return null;
    }

    /**
     * Returns the size (in bytes) of values of the given property type.
     *
     * @param type the property type (PT_* constant)
     * @return the size (in bytes) of values of the given property type,
     *         or -1 for variable-length types
     * @throws IllegalArgumentException if the given type is unknown or
     *         has the multivalue flag (MV_FLAG) set
     */
    public static int getTypeSize(int type) {
        switch (type) {
            case PT_NULL:
                return 0;
            case PT_BOOLEAN:
            case PT_SHORT:
                return 2;
            case PT_INT:
            case PT_FLOAT:
            case PT_ERROR:
                return 4;
            case PT_DOUBLE:
            case PT_APPTIME:
            case PT_CURRENCY:
            case PT_INT8BYTE:
            case PT_SYSTIME:
                return 8;
            case PT_CLSID:
                return 16;
            case PT_OBJECT:
            case PT_STRING:
            case PT_UNICODE_STRING:
            case PT_BINARY:
                return -1;
            default:
                if ((type & MAPIProp.MV_FLAG) != 0)
                    throw new IllegalArgumentException("remove multivalue flag when calculating size");
                throw new IllegalArgumentException("Unknown MAPI type: 0x" + Integer.toHexString(type));
        }
    }

    /**
     * MAPI property ID constant.
     */
    public static final int

        // 0x0001 - 0x0BFF Message object envelope property

        PR_ACKNOWLEDGEMENT_MODE                         = 0x0001,
        PR_EMS_TEMPLATE_BLOB                            = 0x0001,
        PR_ALTERNATE_RECIPIENT_ALLOWED                  = 0x0002,
        PR_AUTHORIZING_USERS                            = 0x0003,
        PR_AUTO_FORWARD_COMMENT                         = 0x0004,
        PR_EMS_SCRIPT_BLOB                              = 0x0004,
        PR_AUTO_FORWARDED                               = 0x0005,
        PR_CONTENT_CONFIDENTIALITY_ALGORITHM_ID         = 0x0006,
        PR_CONTENT_CORRELATOR                           = 0x0007,
        PR_CONTENT_IDENTIFIER                           = 0x0008,
        PR_CONTENT_LENGTH                               = 0x0009,
        PR_CONTENT_RETURN_REQUESTED                     = 0x000A,
        PR_CONVERSATION_KEY                             = 0x000B,
        PR_CONVERSION_EITS                              = 0x000C,
        PR_CONVERSION_WITH_LOSS_PROHIBITED              = 0x000D,
        PR_CONVERTED_EITS                               = 0x000E,
        PR_DEFERRED_DELIVERY_TIME                       = 0x000F,
        PR_DELIVER_TIME                                 = 0x0010,
        PR_DISCARD_REASON                               = 0x0011,
        PR_DISCLOSURE_OF_RECIPIENTS                     = 0x0012,
        PR_DL_EXPANSION_HISTORY                         = 0x0013,
        PR_DL_EXPANSION_PROHIBITED                      = 0x0014,
        PR_EXPIRY_TIME                                  = 0x0015,
        PR_IMPLICIT_CONVERSION_PROHIBITED               = 0x0016,
        PR_IMPORTANCE                                   = 0x0017,
        PR_IPM_ID                                       = 0x0018,
        PR_LATEST_DELIVERY_TIME                         = 0x0019,
        PR_MESSAGE_CLASS                                = 0x001A,
        PR_MESSAGE_DELIVERY_ID                          = 0x001B,
        PR_MESSAGE_SECURITY_LABEL                       = 0x001E,
        PR_OBSOLETED_IPMS                               = 0x001F,
        PR_ORIGINALLY_INTENDED_RECIPIENT_NAME           = 0x0020,
        PR_ORIGINAL_EITS                                = 0x0021,
        PR_ORIGINATOR_CERTIFICATE                       = 0x0022,
        PR_ORIGINATOR_DELIVERY_REPORT_REQUESTED         = 0x0023,
        PR_ORIGINATOR_RETURN_ADDRESS                    = 0x0024,
        PR_PARENT_KEY                                   = 0x0025,
        PR_PRIORITY                                     = 0x0026,
        PR_ORIGIN_CHECK                                 = 0x0027,
        PR_PROOF_OF_SUBMISSION_REQUESTED                = 0x0028,
        PR_READ_RECEIPT_REQUESTED                       = 0x0029,
        PR_RECEIPT_TIME                                 = 0x002A,
        PR_RECIPIENT_REASSIGNMENT_PROHIBITED            = 0x002B,
        PR_REDIRECTION_HISTORY                          = 0x002C,
        PR_RELATED_IPMS                                 = 0x002D,
        PR_ORIGINAL_SENSITIVITY                         = 0x002E,
        PR_LANGUAGES                                    = 0x002F,
        PR_REPLY_TIME                                   = 0x0030,
        PR_REPORT_TAG                                   = 0x0031,
        PR_REPORT_TIME                                  = 0x0032,
        PR_RETURNED_IPM                                 = 0x0033,
        PR_SECURITY                                     = 0x0034,
        PR_INCOMPLETE_COPY                              = 0x0035,
        PR_SENSITIVITY                                  = 0x0036,
        PR_SUBJECT                                      = 0x0037,
        PR_SUBJECT_IPM                                  = 0x0038,
        PR_CLIENT_SUBMIT_TIME                           = 0x0039,
        PR_REPORT_NAME                                  = 0x003A,
        PR_SENT_REPRESENTING_SEARCH_KEY                 = 0x003B,
        PR_X400_CONTENT_TYPE                            = 0x003C,
        PR_SUBJECT_PREFIX                               = 0x003D,
        PR_NON_RECEIPT_REASON                           = 0x003E,
        PR_RECEIVED_BY_ENTRYID                          = 0x003F,
        PR_RECEIVED_BY_NAME                             = 0x0040,
        PR_SENT_REPRESENTING_ENTRYID                    = 0x0041,
        PR_SENT_REPRESENTING_NAME                       = 0x0042,
        PR_RCVD_REPRESENTING_ENTRYID                    = 0x0043,
        PR_RCVD_REPRESENTING_NAME                       = 0x0044,
        PR_REPORT_ENTRYID                               = 0x0045,
        PR_READ_RECEIPT_ENTRYID                         = 0x0046,
        PR_MESSAGE_SUBMISSION_ID                        = 0x0047,
        PR_MTS_ID                                       = 0x0047,
        PR_MTS_REPORT_ID                                = 0x0047,
        PR_PROVIDER_SUBMIT_TIME                         = 0x0048,
        PR_ORIGINAL_SUBJECT                             = 0x0049,
        PR_DISC_VAL                                     = 0x004A,
        PR_ORIG_MESSAGE_CLASS                           = 0x004B,
        PR_ORIGINAL_AUTHOR_ENTRYID                      = 0x004C,
        PR_ORIGINAL_AUTHOR_NAME                         = 0x004D,
        PR_ORIGINAL_SUBMIT_TIME                         = 0x004E,
        PR_REPLY_RECIPIENT_ENTRIES                      = 0x004F,
        PR_REPLY_RECIPIENT_NAMES                        = 0x0050,
        PR_RECEIVED_BY_SEARCH_KEY                       = 0x0051,
        PR_RCVD_REPRESENTING_SEARCH_KEY                 = 0x0052,
        PR_READ_RECEIPT_SEARCH_KEY                      = 0x0053,
        PR_REPORT_SEARCH_KEY                            = 0x0054,
        PR_ORIGINAL_DELIVERY_TIME                       = 0x0055,
        PR_ORIGINAL_AUTHOR_SEARCH_KEY                   = 0x0056,
        PR_MESSAGE_TO_ME                                = 0x0057,
        PR_MESSAGE_CC_ME                                = 0x0058,
        PR_MESSAGE_RECIP_ME                             = 0x0059,
        PR_ORIGINAL_SENDER_NAME                         = 0x005A,
        PR_ORIGINAL_SENDER_ENTRYID                      = 0x005B,
        PR_ORIGINAL_SENDER_SEARCH_KEY                   = 0x005C,
        PR_ORIGINAL_SENT_REPRESENTING_NAME              = 0x005D,
        PR_ORIGINAL_SENT_REPRESENTING_ENTRYID           = 0x005E,
        PR_ORIGINAL_SENT_REPRESENTING_SEARCH_KEY        = 0x005F,
        PR_START_DATE                                   = 0x0060,
        PR_END_DATE                                     = 0x0061,
        PR_OWNER_APPT_ID                                = 0x0062,
        PR_RESPONSE_REQUESTED                           = 0x0063,
        PR_SENT_REPRESENTING_ADDRTYPE                   = 0x0064,
        PR_SENT_REPRESENTING_EMAIL_ADDRESS              = 0x0065,
        PR_ORIGINAL_SENDER_ADDRTYPE                     = 0x0066,
        PR_ORIGINAL_SENDER_EMAIL_ADDRESS                = 0x0067,
        PR_ORIGINAL_SENT_REPRESENTING_ADDRTYPE          = 0x0068,
        PR_ORIGINAL_SENT_REPRESENTING_EMAIL_ADDRESS     = 0x0069,
        PR_CONVERSATION_TOPIC                           = 0x0070,
        PR_CONVERSATION_INDEX                           = 0x0071,
        PR_ORIGINAL_DISPLAY_BCC                         = 0x0072,
        PR_ORIGINAL_DISPLAY_CC                          = 0x0073,
        PR_ORIGINAL_DISPLAY_TO                          = 0x0074,
        PR_RECEIVED_BY_ADDRTYPE                         = 0x0075,
        PR_RECEIVED_BY_EMAIL_ADDRESS                    = 0x0076,
        PR_RCVD_REPRESENTING_ADDRTYPE                   = 0x0077,
        PR_RCVD_REPRESENTING_EMAIL_ADDRESS              = 0x0078,
        PR_ORIGINAL_AUTHOR_ADDRTYPE                     = 0x0079,
        PR_ORIGINAL_AUTHOR_EMAIL_ADDRESS                = 0x007A,
        PR_ORIGINALLY_INTENDED_RECIP_ADDRTYPE           = 0x007B,
        PR_ORIGINALLY_INTENDED_RECIP_EMAIL_ADDRESS      = 0x007C,
        PR_TRANSPORT_MESSAGE_HEADERS                    = 0x007D,
        PR_DELEGATION                                   = 0x007E,
        PR_TNEF_CORRELATION_KEY                         = 0x007F,
        PR_EMS_AB_ROOM_CAPACITY                         = 0x0807,
        PR_EMS_AB_ROOM_DESCRIPTION                      = 0x0809,

        // 0x0C00 - 0x0DFF Recipient property

        PR_CONTENT_INTEGRITY_CHECK                      = 0x0C00,
        PR_EXPLICIT_CONVERSION                          = 0x0C01,
        PR_IPM_RETURN_REQUESTED                         = 0x0C02,
        PR_MESSAGE_TOKEN                                = 0x0C03,
        PR_NDR_REASON_CODE                              = 0x0C04,
        PR_NDR_DIAG_CODE                                = 0x0C05,
        PR_NON_RECEIPT_NOTIFICATION_REQUESTED           = 0x0C06,
        PR_DELIVERY_POINT                               = 0x0C07,
        PR_ORIGINATOR_NON_DELIVERY_REPORT_REQUESTED     = 0x0C08,
        PR_ORIGINATOR_REQUESTED_ALTERNATE_RECIPIENT     = 0x0C09,
        PR_PHYSICAL_DELIVERY_BUREAU_FAX_DELIVERY        = 0x0C0A,
        PR_PHYSICAL_DELIVERY_MODE                       = 0x0C0B,
        PR_PHYSICAL_DELIVERY_REPORT_REQUEST             = 0x0C0C,
        PR_PHYSICAL_FORWARDING_ADDRESS                  = 0x0C0D,
        PR_PHYSICAL_FORWARDING_ADDRESS_REQUESTED        = 0x0C0E,
        PR_PHYSICAL_FORWARDING_PROHIBITED               = 0x0C0F,
        PR_PHYSICAL_RENDITION_ATTRIBUTES                = 0x0C10,
        PR_PROOF_OF_DELIVERY                            = 0x0C11,
        PR_PROOF_OF_DELIVERY_REQUESTED                  = 0x0C12,
        PR_RECIPIENT_CERTIFICATE                        = 0x0C13,
        PR_RECIPIENT_NUMBER_FOR_ADVICE                  = 0x0C14,
        PR_RECIPIENT_TYPE                               = 0x0C15,
        PR_REGISTERED_MAIL_TYPE                         = 0x0C16,
        PR_REPLY_REQUESTED                              = 0x0C17,
        PR_REQUESTED_DELIVERY_METHOD                    = 0x0C18,
        PR_SENDER_ENTRYID                               = 0x0C19,
        PR_SENDER_NAME                                  = 0x0C1A,
        PR_SUPPLEMENTARY_INFO                           = 0x0C1B,
        PR_TYPE_OF_MTS_USER                             = 0x0C1C,
        PR_SENDER_SEARCH_KEY                            = 0x0C1D,
        PR_SENDER_ADDRTYPE                              = 0x0C1E,
        PR_SENDER_EMAIL_ADDRESS                         = 0x0C1F,
        PR_NDR_STATUS_CODE                              = 0x0C20,
        PR_DSN_REMOTE_MTA                               = 0x0C21,

        // 0x0E00 - 0x0FFF: Non-transmittable Message property

        PR_CURRENT_VERSION                              = 0x0E00,
        PR_DELETE_AFTER_SUBMIT                          = 0x0E01,
        PR_DISPLAY_BCC                                  = 0x0E02,
        PR_DISPLAY_CC                                   = 0x0E03,
        PR_DISPLAY_TO                                   = 0x0E04,
        PR_PARENT_DISPLAY                               = 0x0E05,
        PR_MESSAGE_DELIVERY_TIME                        = 0x0E06,
        PR_MESSAGE_FLAGS                                = 0x0E07,
        PR_MESSAGE_SIZE                                 = 0x0E08,
        PR_MESSAGE_SIZE_EXTENDED                        = 0x0E08,
        PR_PARENT_ENTRYID                               = 0x0E09,
        PR_SENTMAIL_ENTRYID                             = 0x0E0A,
        PR_CORRELATE                                    = 0x0E0C,
        PR_CORRELATE_MTSID                              = 0x0E0D,
        PR_DISCRETE_VALUES                              = 0x0E0E,
        PR_RESPONSIBILITY                               = 0x0E0F,
        PR_SPOOLER_STATUS                               = 0x0E10,
        PR_TRANSPORT_STATUS                             = 0x0E11,
        PR_MESSAGE_RECIPIENTS                           = 0x0E12,
        PR_MESSAGE_ATTACHMENTS                          = 0x0E13,
        PR_SUBMIT_FLAGS                                 = 0x0E14,
        PR_RECIPIENT_STATUS                             = 0x0E15,
        PR_TRANSPORT_KEY                                = 0x0E16,
        PR_MSG_STATUS                                   = 0x0E17,
        PR_MESSAGE_DOWNLOAD_TIME                        = 0x0E18,
        PR_CREATION_VERSION                             = 0x0E19,
        PR_MODIFY_VERSION                               = 0x0E1A,
        PR_HASATTACH                                    = 0x0E1B,
        PR_BODY_CRC                                     = 0x0E1C,
        PR_NORMALIZED_SUBJECT                           = 0x0E1D,
        PR_RTF_IN_SYNC                                  = 0x0E1F,
        PR_ATTACH_SIZE                                  = 0x0E20,
        PR_ATTACH_NUM                                   = 0x0E21,
        PR_PREPROCESS                                   = 0x0E22,
        PR_ORIGINATING_MTA_CERTIFICATE                  = 0x0E25,
        PR_PROOF_OF_SUBMISSION                          = 0x0E26,
        PR_PRIMARY_SEND_ACCT                            = 0x0E28,
        PR_NEXT_SEND_ACCT                               = 0x0E29,
        PR_TODO_ITEM_FLAGS                              = 0x0E2B,
        PR_SWAPPED_TODO_STORE                           = 0x0E2C,
        PR_SWAPPED_TODO_DATA                            = 0x0E2D,
        PR_READ                                         = 0x0E69,
        PR_NT_SECURITY_DESCRIPTOR_AS_XML                = 0x0E6A,
        PR_TRUST_SENDER                                 = 0x0E79,
        PR_EXTENDED_RULE_MSG_ACTIONS                    = 0x0E99,
        PR_EXTENDED_RULE_MSG_CONDITION                  = 0x0E9A,
        PR_EXTENDED_RULE_SIZE_LIMIT                     = 0x0E9B,
        PR_ACCESS                                       = 0x0FF4,
        PR_ROW_TYPE                                     = 0x0FF5,
        PR_INSTANCE_KEY                                 = 0x0FF6,
        PR_ACCESS_LEVEL                                 = 0x0FF7,
        PR_MAPPING_SIGNATURE                            = 0x0FF8,
        PR_RECORD_KEY                                   = 0x0FF9,
        PR_STORE_RECORD_KEY                             = 0x0FFA,
        PR_STORE_ENTRYID                                = 0x0FFB,
        PR_MINI_ICON                                    = 0x0FFC,
        PR_ICON                                         = 0x0FFD,
        PR_OBJECT_TYPE                                  = 0x0FFE,
        PR_ENTRYID                                      = 0x0FFF,
        PR_MEMBER_ENTRYID                               = 0x0FFF,

        // 0x1000 - 0x2FFF: Message content property

        PR_BODY                                         = 0x1000,
        PR_REPORT_TEXT                                  = 0x1001,
        PR_ORIGINATOR_AND_DL_EXPANSION_HISTORY          = 0x1002,
        PR_REPORTING_DL_NAME                            = 0x1003,
        PR_REPORTING_MTA_CERTIFICATE                    = 0x1004,
        PR_RTF_SYNC_BODY_CRC                            = 0x1006,
        PR_RTF_SYNC_BODY_COUNT                          = 0x1007,
        PR_RTF_SYNC_BODY_TAG                            = 0x1008,
        PR_RTF_COMPRESSED                               = 0x1009,
        PR_RTF_SYNC_PREFIX_COUNT                        = 0x1010,
        PR_RTF_SYNC_TRAILING_COUNT                      = 0x1011,
        PR_ORIGINALLY_INTENDED_RECIP_ENTRYID            = 0x1012,
        PR_HTML                                         = 0x1013,
        PR_BODY_HTML                                    = 0x1013,
        PR_BODY_CONTENT_LOCATION                        = 0x1014,
        PR_BODY_CONTENT_ID                              = 0x1015,
        PR_NATIVE_BODY_INFO                             = 0x1016,
        PR_INTERNET_MESSAGE_ID                          = 0x1035,
        PR_INTERNET_REFERENCES                          = 0x1039,
        PR_IN_REPLY_TO_ID                               = 0x1042,
        PR_LIST_HELP                                    = 0x1043,
        PR_LIST_SUBSCRIBE                               = 0x1044,
        PR_LIST_UNSUBSCRIBE                             = 0x1045,
        PR_ICON_INDEX                                   = 0x1080,
        PR_LAST_VERB_EXECUTED                           = 0x1081,
        PR_LAST_VERB_EXECUTION_TIME                     = 0x1082,
        PR_FLAG_STATUS                                  = 0x1090,
        PR_FLAG_COMPLETE_TIME                           = 0x1091,
        PR_FOLLOWUP_ICON                                = 0x1095,
        PR_BLOCK_STATUS                                 = 0x1096,
        PR_CDO_RECURRENCEID                             = 0x10C5,
        PR_OWA_URL                                      = 0x10F1,
        PR_DISABLE_FULL_FIDELITY                        = 0x10F2,
        PR_ATTR_HIDDEN                                  = 0x10F4,
        PR_ATTR_READONLY                                = 0x10F6,
        PR_P1_CONTENT                                   = 0x1100,
        PR_P1_CONTENT_TYPE                              = 0x1101,

        // 0x3000 - 0x33FF: Multi-purpose property that can appear on all or most objects

        PR_ROWID                                        = 0x3000,
        PR_DISPLAY_NAME                                 = 0x3001,
        PR_ADDRTYPE                                     = 0x3002,
        PR_EMAIL_ADDRESS                                = 0x3003,
        PR_COMMENT                                      = 0x3004,
        PR_DEPTH                                        = 0x3005,
        PR_PROVIDER_DISPLAY                             = 0x3006,
        PR_CREATION_TIME                                = 0x3007,
        PR_LAST_MODIFICATION_TIME                       = 0x3008,
        PR_RESOURCE_FLAGS                               = 0x3009,
        PR_PROVIDER_DLL_NAME                            = 0x300A,
        PR_SEARCH_KEY                                   = 0x300B,
        PR_PROVIDER_UID                                 = 0x300C,
        PR_PROVIDER_ORDINAL                             = 0x300D,
        PR_TARGET_ENTRYID                               = 0x3010,
        PR_CONVERSATION_ID                              = 0x3013,
        PR_CONVERSATION_INDEX_TRACKING                  = 0x3016,
        PR_ARCHIVE_TAG                                  = 0x3018,
        PR_POLICY_TAG                                   = 0x3019,
        PR_RETENTION_PERIOD                             = 0x301A,
        PR_START_DATE_ETC                               = 0x301B,
        PR_RETENTION_DATE                               = 0x301C,
        PR_RETENTION_FLAGS                              = 0x301D,
        PR_ARCHIVE_PERIOD                               = 0x301E,
        PR_ARCHIVE_DATE                                 = 0x301F,
        PR_FORM_VERSION                                 = 0x3301,
        PR_FORM_CLSID                                   = 0x3302,
        PR_FORM_CONTACT_NAME                            = 0x3303,
        PR_FORM_CATEGORY                                = 0x3304,
        PR_FORM_CATEGORY_SUB                            = 0x3305,
        PR_FORM_HOST_MAP                                = 0x3306,
        PR_FORM_HIDDEN                                  = 0x3307,
        PR_FORM_DESIGNER_NAME                           = 0x3308,
        PR_FORM_DESIGNER_GUID                           = 0x3309,
        PR_FORM_MESSAGE_BEHAVIOR                        = 0x330A,

        // 0x3400 - 0x35FF: Message store property

        PR_DEFAULT_STORE                                = 0x3400,
        PR_STORE_SUPPORT_MASK                           = 0x340D,
        PR_STORE_STATE                                  = 0x340E,
        PR_IPM_SUBTREE_SEARCH_KEY                       = 0x3410,
        PR_IPM_OUTBOX_SEARCH_KEY                        = 0x3411,
        PR_IPM_WASTEBASKET_SEARCH_KEY                   = 0x3412,
        PR_IPM_SENTMAIL_SEARCH_KEY                      = 0x3413,
        PR_MDB_PROVIDER                                 = 0x3414,
        PR_RECEIVE_FOLDER_SETTINGS                      = 0x3415,
        PR_VALID_FOLDER_MASK                            = 0x35DF,
        PR_IPM_SUBTREE_ENTRYID                          = 0x35E0,
        PR_IPM_OUTBOX_ENTRYID                           = 0x35E2,
        PR_IPM_WASTEBASKET_ENTRYID                      = 0x35E3,
        PR_IPM_SENTMAIL_ENTRYID                         = 0x35E4,
        PR_VIEWS_ENTRYID                                = 0x35E5,
        PR_COMMON_VIEWS_ENTRYID                         = 0x35E6,
        PR_FINDER_ENTRYID                               = 0x35E7,

        // 0x3600 - 0x36FF: Folder and address book container property

        PR_CONTAINER_FLAGS                              = 0x3600,
        PR_FOLDER_TYPE                                  = 0x3601,
        PR_CONTENT_COUNT                                = 0x3602,
        PR_CONTENT_UNREAD                               = 0x3603,
        PR_CREATE_TEMPLATES                             = 0x3604,
        PR_DETAILS_TABLE                                = 0x3605,
        PR_SEARCH                                       = 0x3607,
        PR_SELECTABLE                                   = 0x3609,
        PR_SUBFOLDERS                                   = 0x360A,
        PR_STATUS                                       = 0x360B,
        PR_ANR                                          = 0x360C,
        PR_CONTENTS_SORT_ORDER                          = 0x360D,
        PR_CONTAINER_HIERARCHY                          = 0x360E,
        PR_CONTAINER_CONTENTS                           = 0x360F,
        PR_FOLDER_ASSOCIATED_CONTENTS                   = 0x3610,
        PR_DEF_CREATE_DL                                = 0x3611,
        PR_DEF_CREATE_MAILUSER                          = 0x3612,
        PR_CONTAINER_CLASS                              = 0x3613,
        PR_CONTAINER_MODIFY_VERSION                     = 0x3614,
        PR_AB_PROVIDER_ID                               = 0x3615,
        PR_DEFAULT_VIEW_ENTRYID                         = 0x3616,
        PR_ASSOC_CONTENT_COUNT                          = 0x3617,
        PR_IPM_APPOINTMENT_ENTRYID                      = 0x36D0,
        PR_IPM_CONTACT_ENTRYID                          = 0x36D1,
        PR_IPM_JOURNAL_ENTRYID                          = 0x36D2,
        PR_IPM_NOTE_ENTRYID                             = 0x36D3,
        PR_IPM_TASK_ENTRYID                             = 0x36D4,
        PR_REM_ONLINE_ENTRYID                           = 0x36D5,
        PR_IPM_DRAFTS_ENTRYID                           = 0x36D7,
        PR_ADDITIONAL_REN_ENTRYIDS                      = 0x36D8,
        PR_ADDITIONAL_REN_ENTRYIDS_EX                   = 0x36D9,
        PR_EXTENDED_FOLDER_FLAGS                        = 0x36DA,
        PR_ORDINAL_MOST                                 = 0x36E2,
        PR_FREEBUSY_ENTRYIDS                            = 0x36E4,
        PR_DEF_POST_MSGCLASS                            = 0x36E5,

        // 0x3700 - 0x38FF: Attachment property

        PR_ATTACHMENT_X400_PARAMETERS                   = 0x3700,
        PR_ATTACH_DATA_OBJ                              = 0x3701,
        PR_ATTACH_DATA_BIN                              = 0x3701,
        PR_ATTACH_ENCODING                              = 0x3702,
        PR_ATTACH_EXTENSION                             = 0x3703,
        PR_ATTACH_FILENAME                              = 0x3704,
        PR_ATTACH_METHOD                                = 0x3705,
        PR_ATTACH_LONG_FILENAME                         = 0x3707,
        PR_ATTACH_PATHNAME                              = 0x3708,
        PR_ATTACH_RENDERING                             = 0x3709,
        PR_ATTACH_TAG                                   = 0x370A,
        PR_RENDERING_POSITION                           = 0x370B,
        PR_ATTACH_TRANSPORT_NAME                        = 0x370C,
        PR_ATTACH_LONG_PATHNAME                         = 0x370D,
        PR_ATTACH_MIME_TAG                              = 0x370E,
        PR_ATTACH_ADDITIONAL_INFO                       = 0x370F,
        PR_ATTACH_CONTENT_BASE                          = 0x3711,
        PR_ATTACH_CONTENT_ID                            = 0x3712,
        PR_ATTACH_CONTENT_LOCATION                      = 0x3713,
        PR_ATTACH_FLAGS                                 = 0x3714,
        PR_ATTACH_PAYLOAD_PROV_GUID_STR                 = 0x3719,
        PR_ATTACH_PAYLOAD_CLASS                         = 0x371A,

       // 0x3900 - 0x39FF: Address Book object property

        PR_DISPLAY_TYPE                                 = 0x3900,
        PR_TEMPLATEID                                   = 0x3902,
        PR_PRIMARY_CAPABILITY                           = 0x3904,
        PR_DISPLAY_TYPE_EX                              = 0x3905,
        PR_SMTP_ADDRESS                                 = 0x39FE,
        PR_7BIT_DISPLAY_NAME                            = 0x39FF,
        PR_EMS_AB_DISPLAY_NAME_PRINTABLE                = 0x39FF,

        // 0x3A00 - 0x3BFF: Mail user object property

        PR_ACCOUNT                                      = 0x3A00,
        PR_ALTERNATE_RECIPIENT                          = 0x3A01,
        PR_CALLBACK_TELEPHONE_NUMBER                    = 0x3A02,
        PR_CONVERSION_PROHIBITED                        = 0x3A03,
        PR_DISCLOSE_RECIPIENTS                          = 0x3A04,
        PR_GENERATION                                   = 0x3A05,
        PR_GIVEN_NAME                                   = 0x3A06,
        PR_GOVERNMENT_ID_NUMBER                         = 0x3A07,
        PR_BUSINESS_TELEPHONE_NUMBER                    = 0x3A08,
        PR_OFFICE_TELEPHONE_NUMBER                      = 0x3A08,
        PR_HOME_TELEPHONE_NUMBER                        = 0x3A09,
        PR_INITIALS                                     = 0x3A0A,
        PR_KEYWORD                                      = 0x3A0B,
        PR_LANGUAGE                                     = 0x3A0C,
        PR_LOCATION                                     = 0x3A0D,
        PR_MAIL_PERMISSION                              = 0x3A0E,
        PR_MHS_COMMON_NAME                              = 0x3A0F,
        PR_ORGANIZATIONAL_ID_NUMBER                     = 0x3A10,
        PR_SURNAME                                      = 0x3A11,
        PR_ORIGINAL_ENTRYID                             = 0x3A12,
        PR_ORIGINAL_DISPLAY_NAME                        = 0x3A13,
        PR_ORIGINAL_SEARCH_KEY                          = 0x3A14,
        PR_POSTAL_ADDRESS                               = 0x3A15,
        PR_COMPANY_NAME                                 = 0x3A16,
        PR_TITLE                                        = 0x3A17,
        PR_DEPARTMENT_NAME                              = 0x3A18,
        PR_OFFICE_LOCATION                              = 0x3A19,
        PR_PRIMARY_TELEPHONE_NUMBER                     = 0x3A1A,
        PR_BUSINESS2_TELEPHONE_NUMBER                   = 0x3A1B,
        PR_OFFICE2_TELEPHONE_NUMBER                     = 0x3A1B,
        PR_MOBILE_TELEPHONE_NUMBER                      = 0x3A1C,
        PR_CELLULAR_TELEPHONE_NUMBER                    = 0x3A1C,
        PR_RADIO_TELEPHONE_NUMBER                       = 0x3A1D,
        PR_CAR_TELEPHONE_NUMBER                         = 0x3A1E,
        PR_OTHER_TELEPHONE_NUMBER                       = 0x3A1F,
        PR_TRANSMITABLE_DISPLAY_NAME                    = 0x3A20,
        PR_PAGER_TELEPHONE_NUMBER                       = 0x3A21,
        PR_BEEPER_TELEPHONE_NUMBER                      = 0x3A21,
        PR_USER_CERTIFICATE                             = 0x3A22,
        PR_PRIMARY_FAX_NUMBER                           = 0x3A23,
        PR_BUSINESS_FAX_NUMBER                          = 0x3A24,
        PR_HOME_FAX_NUMBER                              = 0x3A25,
        PR_COUNTRY                                      = 0x3A26,
        PR_BUSINESS_ADDRESS_COUNTRY                     = 0x3A26,
        PR_LOCALITY                                     = 0x3A27,
        PR_BUSINESS_ADDRESS_LOCALITY                    = 0x3A27,
        PR_STATE_OR_PROVINCE                            = 0x3A28,
        PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE           = 0x3A28,
        PR_STREET_ADDRESS                               = 0x3A29,
        PR_BUSINESS_ADDRESS_STREET                      = 0x3A29,
        PR_POSTAL_CODE                                  = 0x3A2A,
        PR_BUSINESS_ADDRESS_POSTAL_CODE                 = 0x3A2A,
        PR_POST_OFFICE_BOX                              = 0x3A2B,
        PR_BUSINESS_ADDRESS_POST_OFFICE_BOX             = 0x3A2B,
        PR_TELEX_NUMBER                                 = 0x3A2C,
        PR_ISDN_NUMBER                                  = 0x3A2D,
        PR_ASSISTANT_TELEPHONE_NUMBER                   = 0x3A2E,
        PR_HOME2_TELEPHONE_NUMBER                       = 0x3A2F,
        PR_ASSISTANT                                    = 0x3A30,
        PR_SEND_RICH_INFO                               = 0x3A40,
        PR_WEDDING_ANNIVERSARY                          = 0x3A41,
        PR_BIRTHDAY                                     = 0x3A42,
        PR_HOBBIES                                      = 0x3A43,
        PR_MIDDLE_NAME                                  = 0x3A44,
        PR_DISPLAY_NAME_PREFIX                          = 0x3A45,
        PR_PROFESSION                                   = 0x3A46,
        PR_REFERRED_BY_NAME                             = 0x3A47,
        PR_SPOUSE_NAME                                  = 0x3A48,
        PR_COMPUTER_NETWORK_NAME                        = 0x3A49,
        PR_CUSTOMER_ID                                  = 0x3A4A,
        PR_TTYTDD_PHONE_NUMBER                          = 0x3A4B,
        PR_FTP_SITE                                     = 0x3A4C,
        PR_GENDER                                       = 0x3A4D,
        PR_MANAGER_NAME                                 = 0x3A4E,
        PR_NICKNAME                                     = 0x3A4F,
        PR_PERSONAL_HOME_PAGE                           = 0x3A50,
        PR_BUSINESS_HOME_PAGE                           = 0x3A51,
        PR_CONTACT_VERSION                              = 0x3A52,
        PR_CONTACT_ENTRYIDS                             = 0x3A53,
        PR_CONTACT_ADDRTYPES                            = 0x3A54,
        PR_CONTACT_DEFAULT_ADDRESS_INDEX                = 0x3A55,
        PR_CONTACT_EMAIL_ADDRESSES                      = 0x3A56,
        PR_COMPANY_MAIN_PHONE_NUMBER                    = 0x3A57,
        PR_CHILDRENS_NAMES                              = 0x3A58,
        PR_HOME_ADDRESS_CITY                            = 0x3A59,
        PR_HOME_ADDRESS_COUNTRY                         = 0x3A5A,
        PR_HOME_ADDRESS_POSTAL_CODE                     = 0x3A5B,
        PR_HOME_ADDRESS_STATE_OR_PROVINCE               = 0x3A5C,
        PR_HOME_ADDRESS_STREET                          = 0x3A5D,
        PR_HOME_ADDRESS_POST_OFFICE_BOX                 = 0x3A5E,
        PR_OTHER_ADDRESS_CITY                           = 0x3A5F,
        PR_OTHER_ADDRESS_COUNTRY                        = 0x3A60,
        PR_OTHER_ADDRESS_POSTAL_CODE                    = 0x3A61,
        PR_OTHER_ADDRESS_STATE_OR_PROVINCE              = 0x3A62,
        PR_OTHER_ADDRESS_STREET                         = 0x3A63,
        PR_OTHER_ADDRESS_POST_OFFICE_BOX                = 0x3A64,
        PR_USER_X509_CERTIFICATE                        = 0x3A70,
        PR_SEND_INTERNET_ENCODING                       = 0x3A71,

        // 0x3C00 - 0x3CFF: Distribution list property
        // 0x3D00 - 0x3DFF: Profile section property

        PR_STORE_PROVIDERS                              = 0x3D00,
        PR_AB_PROVIDERS                                 = 0x3D01,
        PR_TRANSPORT_PROVIDERS                          = 0x3D02,
        PR_DEFAULT_PROFILE                              = 0x3D04,
        PR_AB_SEARCH_PATH                               = 0x3D05,
        PR_AB_DEFAULT_DIR                               = 0x3D06,
        PR_AB_DEFAULT_PAB                               = 0x3D07,
        PR_FILTERING_HOOKS                              = 0x3D08,
        PR_SERVICE_NAME                                 = 0x3D09,
        PR_SERVICE_DLL_NAME                             = 0x3D0A,
        PR_SERVICE_ENTRY_NAME                           = 0x3D0B,
        PR_SERVICE_UID                                  = 0x3D0C,
        PR_SERVICE_EXTRA_UIDS                           = 0x3D0D,
        PR_SERVICES                                     = 0x3D0E,
        PR_SERVICE_SUPPORT_FILES                        = 0x3D0F,
        PR_SERVICE_DELETE_FILES                         = 0x3D10,
        PR_AB_SEARCH_PATH_UPDATE                        = 0x3D11,
        PR_PROFILE_NAME                                 = 0x3D12,

        // 0x3E00 - 0x3EFF: Status object property

        PR_IDENTITY_DISPLAY                             = 0x3E00,
        PR_IDENTITY_ENTRYID                             = 0x3E01,
        PR_RESOURCE_METHODS                             = 0x3E02,
        PR_RESOURCE_TYPE                                = 0x3E03,
        PR_STATUS_CODE                                  = 0x3E04,
        PR_IDENTITY_SEARCH_KEY                          = 0x3E05,
        PR_OWN_STORE_ENTRYID                            = 0x3E06,
        PR_RESOURCE_PATH                                = 0x3E07,
        PR_STATUS_STRING                                = 0x3E08,
        PR_X400_DEFERRED_DELIVERY_CANCEL                = 0x3E09,
        PR_HEADER_FOLDER_ENTRYID                        = 0x3E0A,
        PR_REMOTE_PROGRESS                              = 0x3E0B,
        PR_REMOTE_PROGRESS_TEXT                         = 0x3E0C,
        PR_REMOTE_VALIDATE_OK                           = 0x3E0D,
        PR_CONTROL_FLAGS                                = 0x3F00,
        PR_CONTROL_STRUCTURE                            = 0x3F01,
        PR_CONTROL_TYPE                                 = 0x3F02,
        PR_DELTAX                                       = 0x3F03,
        PR_DELTAY                                       = 0x3F04,
        PR_XPOS                                         = 0x3F05,
        PR_YPOS                                         = 0x3F06,
        PR_CONTROL_ID                                   = 0x3F07,
        PR_INITIAL_DETAILS_PANE                         = 0x3F08,
        PR_PREVIEW_UNREAD                               = 0x3FD8,
        PR_PREVIEW                                      = 0x3FD9,
        PR_ABSTRACT                                     = 0x3FDA,
        PR_DL_REPORT_FLAGS                              = 0x3FDB,
        PR_BILATERAL_INFO                               = 0x3FDC,
        PR_MSG_BODY_ID                                  = 0x3FDD,
        PR_INTERNET_CPID                                = 0x3FDE,
        PR_AUTO_RESPONSE_SUPPRESS                       = 0x3FDF,
        PR_ACL_TABLE                                    = 0x3FE0,
        PR_ACL_DATA                                     = 0x3FE0,
        PR_RULES_TABLE                                  = 0x3FE1,
        PR_RULES_DATA                                   = 0x3FE1,
        PR_FOLDER_DESIGN_FLAGS                          = 0x3FE2,
        PR_DELEGATED_BY_RULE                            = 0x3FE3,
        PR_DESIGN_IN_PROGRESS                           = 0x3FE4,
        PR_SECURE_ORIGINATION                           = 0x3FE5,
        PR_PUBLISH_IN_ADDRESS_BOOK                      = 0x3FE6,
        PR_RESOLVE_METHOD                               = 0x3FE7,
        PR_ADDRESS_BOOK_DISPLAY_NAME                    = 0x3FE8,
        PR_EFORMS_LOCALE_ID                             = 0x3FE9,
        PR_HAS_DAMS                                     = 0x3FEA,
        PR_DEFERRED_SEND_NUMBER                         = 0x3FEB,
        PR_DEFERRED_SEND_UNITS                          = 0x3FEC,
        PR_EXPIRY_NUMBER                                = 0x3FED,
        PR_EXPIRY_UNITS                                 = 0x3FEE,
        PR_DEFERRED_SEND_TIME                           = 0x3FEF,
        PR_CONFLICT_ENTRYID                             = 0x3FF0,
        PR_MESSAGE_LOCALE_ID                            = 0x3FF1,
        PR_RULE_TRIGGER_HISTORY                         = 0x3FF2,
        PR_MOVE_TO_STORE_ENTRYID                        = 0x3FF3,
        PR_MOVE_TO_FOLDER_ENTRYID                       = 0x3FF4,
        PR_STORAGE_QUOTA_LIMIT                          = 0x3FF5,
        PR_EXCESS_STORAGE_USED                          = 0x3FF6,
        PR_SVR_GENERATING_QUOTA_MSG                     = 0x3FF7,
        PR_CREATOR_NAME                                 = 0x3FF8,
        PR_CREATOR_ENTRYID                              = 0x3FF9,
        PR_LAST_MODIFIER_NAME                           = 0x3FFA,
        PR_LAST_MODIFIER_ENTRYID                        = 0x3FFB,
        PR_REPLY_RECIPIENT_SMTP_PROXIES                 = 0x3FFC,
        PR_MESSAGE_CODEPAGE                             = 0x3FFD,
        PR_EXTENDED_ACL_DATA                            = 0x3FFE,

        // 0x4000 - 0x57FF: Transport-defined envelope property

        PR_CONTENT_FILTER_SCL                           = 0x4076,
        PR_SENDER_ID_STATUS                             = 0x4079,
        PR_HIER_REV                                     = 0x4082,
        PR_PURPORTED_SENDER_DOMAIN                      = 0x4083,

        // 0x5800 - 0x5FFF: Transport-defined recipient property

        PR_INETMAIL_OVERRIDE_FORMAT                     = 0x5902,
        PR_MSG_EDITOR_FORMAT                            = 0x5909,
        PR_SENT_REPRESENTING_SMTP_ADDRESS               = 0x5D02,
        PR_RECIPIENT_ORDER                              = 0x5FDF,
        PR_RECIPIENT_PROPOSED                           = 0x5FE1,
        PR_RECIPIENT_PROPOSEDSTARTTIME                  = 0x5FE3,
        PR_RECIPIENT_PROPOSEDENDTIME                    = 0x5FE4,
        PR_RECIPIENT_DISPLAY_NAME                       = 0x5FF6,
        PR_RECIPIENT_ENTRYID                            = 0x5FF7,
        PR_RECIPIENT_TRACKSTATUS_TIME                   = 0x5FFB,
        PR_RECIPIENT_FLAGS                              = 0x5FFD,
        PR_RECIPIENT_TRACKSTATUS                        = 0x5FFF,

        // 0x6000 - 0x65FF: User-defined non-transmittable property

        PR_JUNK_INCLUDE_CONTACTS                        = 0x6100,
        PR_JUNK_THRESHOLD                               = 0x6101,
        PR_JUNK_PERMANENTLY_DELETE                      = 0x6102,
        PR_JUNK_ADD_RECIPS_TO_SSL                       = 0x6103,
        PR_JUNK_PHISHING_ENABLE_LINKS                   = 0x6107,
        PR_REPLY_TEMPLATE_ID                            = 0x65C2,
        PR_SOURCE_KEY                                   = 0x65E0,
        PR_PARENT_SOURCE_KEY                            = 0x65E1,
        PR_CHANGE_KEY                                   = 0x65E2,
        PR_PREDECESSOR_CHANGE_LIST                      = 0x65E3,
        PR_RULE_MSG_STATE                               = 0x65E9,
        PR_RULE_MSG_USER_FLAGS                          = 0x65EA,
        PR_RULE_MSG_LEVEL                               = 0x65ED,
        PR_RULE_MSG_PROVIDER_DATA                       = 0x65EE,
        PR_RULE_MSG_SEQUENCE                            = 0x65F3,

        // 0x6600 - 0x67FF: Provider-defined internal non-transmittable property

        PR_USER_ENTRYID                                 = 0x6619,
        PR_MAILBOX_OWNER_ENTRYID                        = 0x661B,
        PR_MAILBOX_OWNER_NAME                           = 0x661C,
        PR_OOF_STATE                                    = 0x661D,
        PR_SPLUS_FREE_BUSY_ENTRYID                      = 0x6622,
        PR_RIGHTS                                       = 0x6639,
        PR_HAS_RULES                                    = 0x663A,
        PR_ADDRESS_BOOK_ENTRYID                         = 0x663B,
        PR_HIERARCHY_CHANGE_NUM                         = 0x663E,
        PR_CLIENT_ACTIONS                               = 0x6645,
        PR_DAM_ORIGINAL_ENTRYID                         = 0x6646,
        PR_DAM_BACK_PATCHED                             = 0x6647,
        PR_RULE_ERROR                                   = 0x6648,
        PR_RULE_ACTION_TYPE                             = 0x6649,
        PR_HAS_NAMED_PROPERTIES                         = 0x664A,
        PR_RULE_ACTION_NUMBER                           = 0x6650,
        PR_RULE_FOLDER_ENTRYID                          = 0x6651,
        PR_PROHIBIT_RECEIVE_QUOTA                       = 0x666A,
        PR_IN_CONFLICT                                  = 0x666C,
        PR_MAX_SUBMIT_MESSAGE_SIZE                      = 0x666D,
        PR_PROHIBIT_SEND_QUOTA                          = 0x666E,
        PR_MEMBER_ID                                    = 0x6671,
        PR_MEMBER_NAME                                  = 0x6672,
        PR_MEMBER_RIGHTS                                = 0x6673,
        PR_RULE_ID                                      = 0x6674,
        PR_RULE_IDS                                     = 0x6675,
        PR_RULE_SEQUENCE                                = 0x6676,
        PR_RULE_STATE                                   = 0x6677,
        PR_RULE_USER_FLAGS                              = 0x6678,
        PR_RULE_CONDITION                               = 0x6679,
        PR_RULE_ACTIONS                                 = 0x6680,
        PR_RULE_PROVIDER                                = 0x6681,
        PR_RULE_NAME                                    = 0x6682,
        PR_RULE_LEVEL                                   = 0x6683,
        PR_RULE_PROVIDER_DATA                           = 0x6684,
        PR_DELETED_ON                                   = 0x668F,
        PR_LOCALE_ID                                    = 0x66A1,
        PR_FOLDER_FLAGS                                 = 0x66A8,
        PR_CODE_PAGE_ID                                 = 0x66C3,
        PR_EMS_AB_MANAGE_DL                             = 0x6704,
        PR_SORT_LOCALE_ID                               = 0x6705,
        PR_LOCAL_COMMIT_TIME                            = 0x6709,
        PR_LOCAL_COMMIT_TIME_MAX                        = 0x670A,
        PR_DELETED_COUNT_TOTAL                          = 0x670B,
        PR_FLAT_URL_NAME                                = 0x670E,
        PR_DAM_ORIG_MSG_SVREID                          = 0x6741,

        // 0x6800 - 0x7BFF: Message class-defined content property

        PR_OAB_NAME                                     = 0x6800,
        PR_OAB_SEQUENCE                                 = 0x6801,
        PR_OAB_CONTAINER_GUID                           = 0x6802,
        PR_OAB_MESSAGE_CLASS                            = 0x6803,
        PR_OAB_DN                                       = 0x6804,
        PR_OAB_TRUNCATED_PROPS                          = 0x6805,
        PR_SCHDINFO_RESOURCE_TYPE                       = 0x6841,
        PR_SCHDINFO_BOSS_WANTS_COPY                     = 0x6842,
        PR_SCHDINFO_DONT_MAIL_DELEGATES                 = 0x6843,
        PR_SCHDINFO_DELEGATE_NAMES                      = 0x6844,
        PR_SCHDINFO_DELEGATE_ENTRYIDS                   = 0x6845,
        PR_GATEWAY_NEEDS_TO_REFRESH                     = 0x6846,
        PR_FREEBUSY_PUBLISH_START                       = 0x6847,
        PR_FREEBUSY_PUBLISH_END                         = 0x6848,
        PR_FREEBUSY_EMA                                 = 0x6849,
        PR_SCHDINFO_BOSS_WANTS_INFO                     = 0x684B,
        PR_SCHDINFO_MONTHS_MERGED                       = 0x684F,
        PR_SCHDINFO_FREEBUSY_MERGED                     = 0x6850,
        PR_SCHDINFO_MONTHS_TENTATIVE                    = 0x6851,
        PR_SCHDINFO_FREEBUSY_TENTATIVE                  = 0x6852,
        PR_SCHDINFO_MONTHS_BUSY                         = 0x6853,
        PR_SCHDINFO_FREEBUSY_BUSY                       = 0x6854,
        PR_SCHDINFO_MONTHS_OOF                          = 0x6855,
        PR_SCHDINFO_FREEBUSY_OOF                        = 0x6856,
        PR_FREEBUSY_RANGE_TIMESTAMP                     = 0x6868,
        PR_FREEBUSY_COUNT_MONTHS                        = 0x6869,
        PR_SCHDINFO_APPT_TOMBSTONE                      = 0x686A,
        PR_DELEGATE_FLAGS                               = 0x686B,
        PR_SCHDINFO_FREEBUSY                            = 0x686C,
        PR_SCHDINFO_AUTO_ACCEPT_APPTS                   = 0x686D,
        PR_SCHDINFO_DISALLOW_RECURRING_APPTS            = 0x686E,
        PR_SCHDINFO_DISALLOW_OVERLAPPING_APPTS          = 0x686F,
        PR_WLINK_CLIENTID                               = 0x6890,
        PR_WLINK_AB_EXSTOREEID                          = 0x6891,
        PR_WLINK_RO_GROUP_TYPE                          = 0x6892,
        PR_VD_BINARY                                    = 0x7001,
        PR_VD_STRINGS                                   = 0x7002,
        PR_VD_NAME                                      = 0x7006,
        PR_VD_VERSION                                   = 0x7007,

        // these conflict with the above, and probably won't be in messages

        //PR_RW_RULES_STREAM                            = 0x6802,
        //PR_WB_SF_LAST_USED                            = 0x6834,
        //PR_WB_SF_EXPIRATION                           = 0x683A,
        //PR_WB_SF_TEMPLATE_ID                          = 0x6841,
        //PR_WB_SF_ID                                   = 0x6842,
        //PR_WB_SF_RECREATE_INFO                        = 0x6844,
        //PR_WB_SF_DEFINITION                           = 0x6845,
        //PR_WB_SF_STORAGE_TYPE                         = 0x6846,
        //PR_WB_SF_TAG                                  = 0x6847,
        //PR_WB_SF_EFP_FLAGS                            = 0x6848,
        //PR_WLINK_TYPE                                 = 0x6849,
        //PR_WLINK_FLAGS                                = 0x684A,
        //PR_WLINK_ORDINAL                              = 0x684B,
        //PR_WLINK_ENTRYID                              = 0x684C,
        //PR_WLINK_RECKEY                               = 0x684D,
        //PR_WLINK_STORE_ENTRYID                        = 0x684E,
        //PR_WLINK_FOLDER_TYPE                          = 0x684F,
        //PR_WLINK_GROUP_CLSID                          = 0x6850,
        //PR_WLINK_GROUP_NAME                           = 0x6851,
        //PR_WLINK_SECTION                              = 0x6852,
        //PR_WLINK_CALENDAR_COLOR                       = 0x6853,
        //PR_WLINK_ABEID                                = 0x6854,

        // 0x7C00 - 0x7FFF: Message class-defined non-transmittable property

        PR_ROAMING_DATATYPES                            = 0x7C06,
        PR_ROAMING_DICTIONARY                           = 0x7C07,
        PR_ROAMING_XMLSTREAM                            = 0x7C08,
        PR_OSC_SYNC_ENABLEDONSERVER                     = 0x7C24,
        PR_PROCESSED                                    = 0x7D01,
        PR_EXCEPTION_REPLACETIME                        = 0x7FF9,
        PR_ATTACHMENT_LINKID                            = 0x7FFA,
        PR_EXCEPTION_STARTTIME                          = 0x7FFB,
        PR_EXCEPTION_ENDTIME                            = 0x7FFC,
        PR_ATTACHMENT_FLAGS                             = 0x7FFD,
        PR_ATTACHMENT_HIDDEN                            = 0x7FFE,
        PR_ATTACHMENT_CONTACTPHOTO                      = 0x7FFF,

        // 0x8000 - 0xFFFF: Reserved for mapping to named properties.
        // The exceptions to this rule are some of the address book tagged properties
        // (those with names beginning with PIDTagAddressBook/PR_EMS_AB).
        // Many are static property ids but are in this range.

        PR_EMS_AB_FOLDER_PATHNAME                       = 0x8004,
        PR_EMS_AB_MANAGER                               = 0x8005,
        PR_EMS_AB_HOME_MDB                              = 0x8006,
        PR_EMS_AB_IS_MEMBER_OF_DL                       = 0x8008,
        PR_EMS_AB_MEMBER                                = 0x8009,
        PR_EMS_AB_OWNER_O                               = 0x800C,
        PR_EMS_AB_REPORTS                               = 0x800E,
        PR_EMS_AB_PROXY_ADDRESSES                       = 0x800F,
        PR_EMS_AB_TARGET_ADDRESS                        = 0x8011,
        PR_EMS_AB_PUBLIC_DELEGATES                      = 0x8015,
        PR_EMS_AB_OWNER_BL_O                            = 0x8024,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_1                 = 0x802D,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_2                 = 0x802E,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_3                 = 0x802F,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_4                 = 0x8030,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_5                 = 0x8031,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_6                 = 0x8032,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_7                 = 0x8033,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_8                 = 0x8034,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_9                 = 0x8035,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_10                = 0x8036,
        PR_EMS_AB_OBJ_DIST_NAME                         = 0x803C,
        PR_EMS_AB_DELIV_CONT_LENGTH                     = 0x806A,
        PR_EMS_AB_DL_MEM_SUBMIT_PERMS_BL_O              = 0x8073,
        PR_EMS_AB_NETWORK_ADDRESS                       = 0x8170,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_11                = 0x8C57,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_12                = 0x8C58,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_13                = 0x8C59,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_14                = 0x8C60,
        PR_EMS_AB_EXTENSION_ATTRIBUTE_15                = 0x8C61,
        PR_EMS_AB_X509_CERT                             = 0x8C6A,
        PR_EMS_AB_OBJECT_GUID                           = 0x8C6D,
        PR_EMS_AB_PHONETIC_GIVEN_NAME                   = 0x8C8E,
        PR_EMS_AB_PHONETIC_SURNAME                      = 0x8C8F,
        PR_EMS_AB_PHONETIC_DEPARTMENT_NAME              = 0x8C90,
        PR_EMS_AB_PHONETIC_COMPANY_NAME                 = 0x8C91,
        PR_EMS_AB_PHONETIC_DISPLAY_NAME                 = 0x8C92,
        PR_EMS_AB_DISPLAY_TYPE_EX                       = 0x8C93,
        PR_EMS_AB_HAB_SHOW_IN_DEPARTMENTS               = 0x8C94,
        PR_EMS_AB_ROOM_CONTAINERS                       = 0x8C96,
        PR_EMS_AB_HAB_DEPARTMENT_MEMBERS                = 0x8C97,
        PR_EMS_AB_HAB_ROOT_DEPARTMENT                   = 0x8C98,
        PR_EMS_AB_HAB_PARENT_DEPARTMENT                 = 0x8C99,
        PR_EMS_AB_HAB_CHILD_DEPARTMENTS                 = 0x8C9A,
        PR_EMS_AB_THUMBNAIL_PHOTO                       = 0x8C9E,
        PR_EMS_AB_HAB_SENIORITY_INDEX                   = 0x8CA0,
        PR_EMS_AB_SENIORITY_INDEX                       = 0x8CA0,
        PR_EMS_AB_ORG_UNIT_ROOT_DN                      = 0x8CA8,
        PR_EMS_AB_ENABLE_MODERATION                     = 0x8CB5,
        PR_EMS_AB_UM_SPOKEN_NAME                        = 0x8CC2,
        PR_EMS_AB_AUTH_ORIG                             = 0x8CD8,
        PR_EMS_AB_UNAUTH_ORIG                           = 0x8CD9,
        PR_EMS_AB_DL_MEM_SUBMIT_PERMS                   = 0x8CDA,
        PR_EMS_AB_DL_MEM_REJECT_PERMS                   = 0x8CDB,
        PR_EMS_AB_HAB_IS_HIERARCHICAL_GROUP             = 0x8CDD,
        PR_EMS_AB_IS_ORGANIZATIONAL                     = 0x8CDD,
        PR_EMS_AB_DL_TOTAL_MEMBER_COUNT                 = 0x8CE2,
        PR_EMS_AB_DL_EXTERNAL_MEMBER_COUNT              = 0x8CE3,
        PR_EMS_AB_IS_MASTER                             = 0xFFFB,
        PR_EMS_AB_PARENT_ENTRYID                        = 0xFFFC,
        PR_EMS_AB_CONTAINERID                           = 0xFFFD;
}
