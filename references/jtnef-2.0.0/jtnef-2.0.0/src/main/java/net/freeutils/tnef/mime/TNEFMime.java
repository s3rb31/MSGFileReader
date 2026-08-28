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

import java.io.*;
import java.util.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import net.freeutils.tnef.*;
import net.freeutils.tnef.Message;

/**
 * The <code>TNEFMime</code> class provides high-level utility methods to
 * access TNEF streams and extract their contents, using the JavaMail API.
 *
 * Note: This class is experimental and is intended to show possible uses
 * of the Java TNEF package.
 *
 * @author Amichai Rothman
 * @since 2003-04-29
 */
public class TNEFMime {

    // the converters used when converting messages (in order of attempted conversion)
    static Converter[] converters =
        { new ContactConverter(), new ReadReceiptConverter(), new MessageConverter() };

    /**
     * Sets the converters to use when attempting to convert messages.
     * The converters are attempted in order, until one is successful.
     *
     * @param converters the converters to use
     */
    public static void setConverters(Converter... converters) {
        TNEFMime.converters = converters;
    }

    /**
     * Extracts a TNEF attachment from a specified MIME file.
     *
     * @param mimeFilename the filename of a file containing a MIME message
     * @param tnefFilename the filename of a file to which the TNEF attachment
     *        extracted from the MIME message should be written
     * @return true if a TNEF attachment was extracted, false otherwise
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static boolean extractTNEF(String mimeFilename, String tnefFilename)
            throws IOException, MessagingException {
        Properties props = new Properties();
        Session session = Session.getInstance(props, null);
        FileInputStream fis = new FileInputStream(mimeFilename);
        try {
            return extractTNEF(new MimeMessage(session, fis), tnefFilename);
        } finally {
            fis.close();
        }
    }

    /**
     * Extracts a TNEF attachment from a specified message Part (recursively).
     *
     * @param part a message Part which may contain a TNEF attachment or
     *        additional parts
     * @param tnefFilename the filename of a file to which the TNEF attachment
     *        extracted from the Part should be written
     * @return true if a TNEF attachment was extracted, false otherwise
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static boolean extractTNEF(Part part, String tnefFilename)
            throws IOException, MessagingException {
        boolean extracted = false;
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)part.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++)
                if (extractTNEF(mp.getBodyPart(i), tnefFilename))
                    extracted = true;
        } else if (TNEFUtils.isTNEFMimeType(part.getContentType())) {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(tnefFilename));
            InputStream in = part.getInputStream();
            TNEFUtils.transfer(in, out, -1, true, true);
            extracted = true;
        }
        return extracted;
    }

    /**
     * Adds a text part to the given multipart, with the given text and content type,
     * using the UTF-8 encoding (and utf-8 charset parameter).
     *
     * @param mp the multipart to add the text to
     * @param text the text to add
     * @param contentType the full text mime type
     * @return the newly added part
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static MimeBodyPart addTextPart(Multipart mp, String text, String contentType)
            throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        part.setText(text, "UTF-8");
        part.setHeader("Content-Type", contentType + "; charset=utf-8");
        mp.addBodyPart(part);
        return part;
    }

    /**
     * Constructs a TNEFMimeMessage from the given TNEFInputStream.
     * TNEF Attributes are both added to the message, and translated into
     * Mime fields and structure (where applicable).
     *
     * @param session the session used to handle the MimeMessage
     * @param in the TNEFInputStream containing message to convert
     * @return the converted TNEFMimeMessage
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static TNEFMimeMessage convert(Session session, TNEFInputStream in)
            throws IOException, MessagingException {
        Message message = new Message(in);
        try {
            return convert(session, message);
        } finally {
            message.close();
        }
    }

    /**
     * Constructs a TNEFMimeMessage from the given TNEF Message.
     * TNEF Attributes are both added to the message, and translated into
     * Mime fields and structures (where applicable).
     *
     * @param session the session used to handle the MimeMessage
     * @param message the Message to convert
     * @return the converted TNEFMimeMessage
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static TNEFMimeMessage convert(Session session, net.freeutils.tnef.Message message)
            throws IOException, MessagingException {
        TNEFMimeMessage mime = new TNEFMimeMessage(session);
        for (Converter converter : converters) {
            if (converter.canConvert(message)) {
                mime = converter.convert(message, mime);
                break;
            }
        }
        mime.saveChanges();
        return mime;
    }

    /**
     * Converts TNEF parts within given message to MIME-structured parts (recursively).
     *
     * @param session a Session instance used in creating new Parts
     * @param message the MIME message to convert
     * @return the given message instance, with TNEF attachment parts replaced
     *         by MIME-structured parts
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static MimeMessage convert(Session session, MimeMessage message)
            throws IOException, MessagingException {
        return convert(session, message, true);
    }

    /**
     * Converts TNEF parts within given message to MIME-structured parts (recursively).
     *
     * @param session a Session instance used in creating new Parts
     * @param message the MIME message to convert
     * @param embed if true, the messages obtained from converted
     *        TNEF parts are embedded in the message in place of the TNEF parts.
     *        If false, the first TNEF part encountered is converted to a mime
     *        message and returned.
     * @return the given message instance, with TNEF attachment parts replaced
     *         by MIME-structured parts
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static MimeMessage convert(Session session, MimeMessage message, boolean embed)
            throws IOException, MessagingException {
        message = (MimeMessage)convert(session, (Part)message, embed);
        message.saveChanges();
        return message;
    }

    /**
     * Converts TNEF parts within given part to MIME-structured parts (recursively).
     *
     * @param session a Session instance used in creating new Parts
     * @param part the MIME part to convert
     * @return the original part, with TNEF attachment parts replaced
     *         by MIME-structured parts. If the part itself is a TNEF attachment,
     *         a converted MimeMessage is returned instead.
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static Part convert(Session session, Part part) throws IOException, MessagingException {
        return convert(session, part, true);
    }

    /**
     * Converts TNEF parts within given part to MIME-structured parts (recursively).
     *
     * @param session a Session instance used in creating new Parts
     * @param part the MIME part to convert
     * @param embed if true, the messages obtained from converted
     *        TNEF parts are embedded in the part in place of the TNEF parts.
     *        If false, the first TNEF part encountered is converted to a mime
     *        message and returned.
     * @return the original part, with TNEF attachment parts replaced
     *         by MIME-structured parts. If the part itself is a TNEF attachment,
     *         a converted MimeMessage is returned instead.
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing a mime part
     */
    public static Part convert(Session session, Part part, boolean embed)
            throws IOException, MessagingException {
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)part.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                Part mpPart = mp.getBodyPart(i);
                Part convertedPart = convert(session, mpPart);
                if (mpPart != convertedPart) {
                    if (!embed)
                        return convertedPart;
                    mp.removeBodyPart(i);
                    TNEFMimeBodyPart newPart = new TNEFMimeBodyPart();
                    newPart.setDataHandler(new DataHandler(convertedPart, "message/rfc822"));
                    mp.addBodyPart(newPart, i);
                }
            }
            part.setContent(mp);
        } else if (TNEFUtils.isTNEFMimeType(part.getContentType())) {
            TNEFInputStream in = new TNEFInputStream(part.getInputStream());

            if (part instanceof MimeMessage) {
                // if the root message is the attachment itself, a.k.a Summary TNEF (STNEF)
                MimeMessage mm = (MimeMessage)part;
                MimeMessage converted = convert(session, in);
                // remove the attachment content
                mm.removeHeader("Content-Type");
                mm.removeHeader("Content-Transfer-Encoding");
                mm.removeHeader("Content-Disposition");
                // and add the converted content instead, preserving original headers
                mm.setContent(converted.getContent(), converted.getContentType());
            } else {
                // if it's an attachment within a message
                part = convert(session, in);
            }
        }
        return part;
    }

    /**
     * Main entry point for command-line utility.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {

        String usage =
            "Usage:\n\n" +
            "  java net.freeutils.tnef.mime.TNEFMime -<e|c|w> <infile> <outfile>\n\n" +
            "Options:\n\n" +
            "  e Extract the TNEF attachment from a MIME file.\n" +
            "  c Convert a MIME file containing a TNEF attachment to a MIME file\n" +
            "    with a nested rfc822 message.\n" +
            "  w Convert a TNEF attachment to a MIME file.\n\n" +
            "Examples:\n\n" +
            "  java net.freeutils.tnef.mime.TNEFMime -e c:\\temp\\1.mime c:\\temp\\winmail.dat\n" +
            "  java net.freeutils.tnef.mime.TNEFMime -c c:\\temp\\1.mime c:\\temp\\2.mime\n" +
            "  java net.freeutils.tnef.mime.TNEFMime -w c:\\temp\\winmail.dat c:\\temp\\1.mime\n";

        if (args.length < 3) {
            System.out.println(usage);
            System.exit(1);
        }

        String options = args[0].toLowerCase();
        if (options.startsWith("-") || options.startsWith("/"))
            options = options.substring(1).trim();

        String infile = args[1];
        String outfile = args[2];

        System.out.println("Processing file " + infile);
        Session session = Session.getInstance(new Properties());
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            if ("e".equals(options)) {
                extractTNEF(infile, outfile);
            } else if ("c".equals(options)) {
                in = new FileInputStream(infile);
                MimeMessage mime = new MimeMessage(session, in);
                out = new FileOutputStream(outfile);
                mime = convert(session, mime);
                mime.writeTo(out);
            } else if ("w".equals(options)) {
                in = new FileInputStream(infile);
                TNEFInputStream tin = new TNEFInputStream(in);
                out = new FileOutputStream(outfile);
                MimeMessage mime = convert(session, tin);
                mime.writeTo(out);
            } else {
                System.out.println("\nInvalid option: " + options);
                System.out.println(usage);
                System.exit(1);
            }
            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                TNEFUtils.closeAll(in, out);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}
