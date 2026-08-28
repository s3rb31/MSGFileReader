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

import static net.freeutils.tnef.mime.TNEFMime.addTextPart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.freeutils.tnef.*;
import net.freeutils.tnef.Message;

/**
 * The <code>MessageConverter</code> class converts a regular message
 * from a TNEF message into a standard MIME message. It is also used
 * as the default converter for unknown message types.
 *
 * @author Amichai Rothman
 * @since 2003-04-29
 */
public class MessageConverter extends Converter {

    @Override
    public boolean canConvert(Message message) {
        return true; // default converter for all message types
    }

    @Override
    public TNEFMimeMessage convert(Message message, TNEFMimeMessage mime)
            throws IOException, MessagingException {

        // add TNEF attributes
        mime.setTNEFAttributes(message.getAttributes());
        // translate TNEF attributes to mime
        // original headers
        addHeaders(message, mime);
        // from
        Attr attr = message.getAttribute(Attr.attFrom);
        if (attr != null) {
            net.freeutils.tnef.Address address = (net.freeutils.tnef.Address)attr.getValue();
            mime.setFrom(new InternetAddress(address.getAddress(), address.getDisplayName()));
        }
        // date sent
        attr = message.getAttribute(Attr.attDateSent);
        if (attr != null)
            mime.setSentDate((Date)attr.getValue());
        // recipients
        addRecipients(message, mime);
        // subject
        attr = message.getAttribute(Attr.attSubject);
        if (attr != null)
            mime.setSubject((String)attr.getValue());
        // body
        Multipart mp = new MimeMultipart();
        addBody(message, mp);
        // attachments and nested messages
        addAttachments(message, mime, mp);
        // finish up
        if (mp.getCount() > 0)
            mime.setContent(mp);
        else
            mime.setContent("", "text/plain; charset=utf-8"); // because an empty multipart is illegal
        return mime;
    }

    protected void addHeaders(Message message, TNEFMimeMessage mime)
            throws IOException, MessagingException {
        MAPIProps props = message.getMAPIProps();
        if (props != null) {
            String headers = (String)props.getPropValue(MAPIProp.PR_TRANSPORT_MESSAGE_HEADERS);
            if (headers != null) {
                InternetHeaders ih = new InternetHeaders(
                    new ByteArrayInputStream(headers.getBytes("ISO8859_1")));
                Enumeration e = ih.getAllHeaders();
                while (e.hasMoreElements()) {
                    Header h = (Header)e.nextElement();
                    mime.addHeader(h.getName(), h.getValue());
                }
            }
        }
    }

    protected void addRecipients(Message message, TNEFMimeMessage mime)
            throws MessagingException, IOException {
        Attr attr;
        attr = message.getAttribute(Attr.attRecipTable);
        if (attr != null) {
            // remove previously set recipients (from original headers)
            mime.removeHeader("To");
            mime.removeHeader("Cc");
            mime.removeHeader("Bcc");
            // add recipients
            for (MAPIProps recipient : (MAPIProps[])attr.getValue()) {
                String name = (String)recipient.getPropValue(MAPIProp.PR_DISPLAY_NAME);
                String address = (String)recipient.getPropValue(MAPIProp.PR_EMAIL_ADDRESS);
                InternetAddress internetAddress = new InternetAddress(address, name);
                int type = (Integer)recipient.getPropValue(MAPIProp.PR_RECIPIENT_TYPE);

                javax.mail.Message.RecipientType recipientType;
                switch (type) {
                    case MAPIProp.MAPI_TO: recipientType = javax.mail.Message.RecipientType.TO; break;
                    case MAPIProp.MAPI_CC: recipientType = javax.mail.Message.RecipientType.CC; break;
                    case MAPIProp.MAPI_BCC: recipientType = javax.mail.Message.RecipientType.BCC; break;
                    default: throw new IllegalArgumentException("invalid PR_RECIPIENT_TYPE: " + type);
                }
                mime.addRecipient(recipientType, internetAddress);
            }
        }
    }

    protected Multipart addBody(Message message, Multipart mp)
            throws IOException, MessagingException {
        Attr attr;
        attr = message.getAttribute(Attr.attBody);
        if (attr != null) {
            String text = (String)attr.getValue();
            addTextPart(mp, text, "text/plain");
        }

        MAPIProps props = message.getMAPIProps();
        if (props != null) {
            // compressed RTF body
            RawInputStream ris = (RawInputStream)props.getPropValue(MAPIProp.PR_RTF_COMPRESSED);
            if (ris != null) {
                try {
                    // we use readSafely with a CRTF stream rather than the faster array-based
                    // CompressedRTFInputStream.decompressRTF to prevent OOME/DoS
                    // due to maliciously crafted uncompressed length field
                    CompressedRTFInputStream rtfStream = new CompressedRTFInputStream(ris);
                    byte[] rtfBytes = TNEFUtils.readSafely(
                            rtfStream, rtfStream.getUncompressedSize(), 1024 * 1024);
                    addTextPart(mp, new String(rtfBytes), "text/rtf");
                } finally {
                    ris.close();
                }
            } else {
                // HTML body (either PR_HTML or PR_BODY_HTML - both have the
                // same ID, but one is a string and one is a byte array)
                Object html = props.getPropValue(MAPIProp.PR_HTML);
                if (html != null) {
                    String text;
                    if (html instanceof RawInputStream) {
                        ris = (RawInputStream)html;
                        try {
                            text = new String(ris.toByteArray(), "UTF-8");
                        } finally {
                            ris.close();
                        }
                    } else {
                        text = (String)html;
                    }
                    addTextPart(mp, text, "text/html");
                }
            }
        }
        return mp;
    }

    protected void addAttachments(Message message, TNEFMimeMessage mime, Multipart mp)
            throws MessagingException, IOException {
        for (Attachment attachment : message.getAttachments()) {
            TNEFMimeBodyPart part = new TNEFMimeBodyPart();
            if (attachment.getNestedMessage() == null) {
                // add TNEF attributes
                part.setTNEFAttributes(attachment.getAttributes());
                // translate TNEF attributes to Mime
                String filename = attachment.getFilename();
                if (filename != null)
                    part.setFileName(filename);
                String mimeType = null;
                if (attachment.getMAPIProps() != null)
                    mimeType = (String)attachment.getMAPIProps().getPropValue(MAPIProp.PR_ATTACH_MIME_TAG);
                if (mimeType == null && filename != null)
                    mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(filename);
                if (mimeType == null)
                    mimeType = "application/octet-stream";
                DataSource ds = new RawDataSource(attachment.getRawData(), mimeType, filename);
                part.setDataHandler(new DataHandler(ds));
                mp.addBodyPart(part);
            } else { // nested message
                MimeMessage nested = TNEFMime.convert(mime.getSession(), attachment.getNestedMessage());
                part.setDataHandler(new DataHandler(nested, "message/rfc822"));
                mp.addBodyPart(part);
            }
        }
    }

}
