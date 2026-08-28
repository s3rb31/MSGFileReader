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

import static net.freeutils.tnef.msg.Properties.processProperties;
import java.io.*;
import java.util.*;
import net.freeutils.tnef.*;
import org.apache.poi.poifs.filesystem.*;

/**
 * The <code>Msg</code> class provides access to the contents
 * of ".msg" files in which Outlook saves messages.
 *
 * @author Amichai Rothman
 * @since 2007-06-16
 */
public class Msg {

    /**
     * Prints all entries in a directory (recursively) to standard output.
     * This is useful for debugging purposes.
     *
     * @param dir the directory to print
     * @param linePrefix a prefix to print at the beginning of every line
     * @throws IOException if an error occurs
     */
    public static void printDirectory(DirectoryEntry dir, String linePrefix) throws IOException {
        for (Entry entry : dir) {
            String name = entry.getName();
            if (entry instanceof DirectoryEntry) {
                System.out.println(linePrefix + name + "/");
                printDirectory((DirectoryEntry)entry, linePrefix + "  ");
            } else if (entry instanceof DocumentEntry) {
                System.out.println(linePrefix + name + "  " + toRawInputStream((DocumentEntry)entry));
            } else {
                System.out.println(linePrefix + name + " (UNKNOWN entry type)");
            }
        }
    }

    /**
     * Processes a message read from a stream.
     *
     * @param in the stream containing the message
     * @return the parsed message
     * @throws IOException if an error occurs
     */
    public static Message processMessage(InputStream in) throws IOException {
        POIFSFileSystem fs = new POIFSFileSystem(in);
        return processMessage(fs.getRoot());
    }

    /**
     * Processes the message at the given directory entry.
     *
     * @param dir the directory entry containing the message
     * @return the parsed message
     * @throws IOException if an error occurs
     */
    public static Message processMessage(DirectoryEntry dir) throws IOException {
        return processMessage(dir, null, true);
    }

    /**
     * Processes the message at the given directory entry.
     *
     * @param dir the directory entry containing the message
     * @param names a map of property name IDs and their string representations
     * @param isRoot specifies whether the message is the root (top-level)
     *        message or a nested one
     * @return the parsed message
     * @throws IOException if an error occurs
     */
    protected static Message processMessage(DirectoryEntry dir,
            Map<Integer, MAPIPropName> names, boolean isRoot) throws IOException {
        Message message = new Message();
        // process prop IDs to names map
        if (isRoot) {
            try {
                names = Properties.processNameIDs(dir);
            } catch (FileNotFoundException ignore) {} // we saw a message with missing mapping entries
        }
        // add properties
        Properties properties = processProperties(dir, isRoot ? 32 : 24, names);
        MAPIProps mapiProps = new MAPIProps(properties.toArray());
        message.addAttribute(new Attr(Attr.LVL_MESSAGE, Attr.atpByte, Attr.attMAPIProps, mapiProps));
        // add recipients
        MAPIProps[] recipients = processRecipients(dir, properties.recipientCount, names);
        message.addAttribute(new Attr(Attr.LVL_MESSAGE, Attr.atpByte, Attr.attRecipTable, recipients));
        // add attachments
        List<Attachment> attachments = processAttachments(dir, properties.attachmentCount, names);
        message.setAttachments(attachments);
        return message;
    }

    /**
     * Processes recipients under the given directory entry.
     *
     * @param dir the directory entry containing the recipients
     * @param recipientCount the number of recipients to process
     * @param names a map of property name IDs and their string representations
     * @return the parsed recipients (as MAPI properties)
     * @throws IOException if an error occurs
     */
    protected static MAPIProps[] processRecipients(DirectoryEntry dir,
                    int recipientCount, Map<Integer, MAPIPropName> names) throws IOException {
        // use list with limited initial size instead of pre-allocating array to prevent OOME/DoS
        // also see https://support.microsoft.com/en-us/kb/952295
        List<MAPIProps> recipients = new ArrayList<MAPIProps>(Math.min(recipientCount, 2048));
        for (int i = 0; i < recipientCount; i++) {
            String entryName = "__recip_version1.0_#" + TNEFUtils.toHexString(i, 8);
            Entry entry = dir.getEntry(entryName);
            Properties properties = processProperties((DirectoryEntry)entry, 8, names);
            recipients.add(new MAPIProps(properties.toArray()));
        }
        return recipients.toArray(new MAPIProps[recipients.size()]);
    }

    /**
     * Processes attachments under the given directory entry.
     *
     * @param dir the directory entry containing the attachments
     * @param attachmentCount the number of attachments to process
     * @param names a map of property name IDs and their string representations
     * @return the parsed attachments
     * @throws IOException if an error occurs
     */
    protected static List<Attachment> processAttachments(DirectoryEntry dir,
                    int attachmentCount, Map<Integer, MAPIPropName> names) throws IOException {
        // use list with limited initial size instead of pre-allocating array to prevent OOME/DoS
        // also see https://support.microsoft.com/en-us/kb/952295
        List<Attachment> attachments = new ArrayList<Attachment>(Math.min(attachmentCount, 2048));
        for (int i = 0; i < attachmentCount; i++) {
            String entryName = "__attach_version1.0_#" + TNEFUtils.toHexString(i, 8);
            Entry entry = dir.getEntry(entryName);
            Attachment attachment = processAttachment((DirectoryEntry)entry, names);
            attachments.add(attachment);
        }
        return attachments;
    }

    /**
     * Processes an attachment at the given directory entry.
     *
     * @param dir the directory entry containing the attachment
     * @param names a map of property name IDs and their string representations
     * @return the parsed attachment
     * @throws IOException if an error occurs
     */
    protected static Attachment processAttachment(DirectoryEntry dir,
            Map<Integer, MAPIPropName> names) throws IOException {
        Properties properties = processProperties(dir, 8, names);
        Attachment att = new Attachment();
        MAPIProps props = new MAPIProps(properties.toArray());
        att.setMAPIProps(props);
        // process nested object (if exists)
        Integer type = (Integer)props.getPropValue(MAPIProp.PR_ATTACH_METHOD);
        if (type != null) { // we've seen messages with no such property
            if (type == 5) { // nested message
                DirectoryEntry entry = (DirectoryEntry)dir.getEntry("__substg1.0_3701000D");
                att.setNestedMessage(processMessage(entry, names, false));
            } else if (type == 6) { // custom attachment, e.g. embedded object in Rich Text message
                DirectoryEntry entry = (DirectoryEntry)dir.getEntry("__substg1.0_3701000D");
                processEmbeddedObject(entry, att);
            } // otherwise it's just a regular attachment with no nested object
        }
        return att;
    }

    /**
     * Processes a non-message embedded object (a.k.a. custom attachment or OLE object).
     * <p>
     * An attempt is made to extract the underlying object data and set it as the
     * attachment's raw data, however other than a few common formats which are supported,
     * the object data format is application-specific and will simply be ignored.
     *
     * @param entry the entry containing the embedded object
     * @param att the attachment on which to set the extracted data
     * @throws IOException if an error occurs
     */
    protected static void processEmbeddedObject(DirectoryEntry entry, Attachment att) throws IOException {
        RawInputStream data = null;
        if (entry.hasEntry("CONTENTS")) {
            data = toRawInputStream((DocumentEntry)entry.getEntry("CONTENTS"));
        } else if (entry.hasEntry("\u0001Ole10Native")) {
            RawInputStream in = toRawInputStream((DocumentEntry)entry.getEntry("\u0001Ole10Native"));
            try {
                long length = in.readU32();
                RawInputStream potential = new RawInputStream(in, 0, length); // potential raw data
                try {
                    int flags = in.readU16();
                    if (flags == 2) { // there is a header
                        potential.close(); // wrong raw data
                        potential = new RawInputStream(in, 0, length - 2); // potential raw data
                        String label = readString(in, 65536);
                        if (label.length() > 0 && label.charAt(0) >= 0x20) { // there is a full header
                            potential.close(); // wrong raw data
                            String filename = readString(in, 1024);
                            int flags2 = in.readU16();
                            int unknown = in.readU16();
                            length = in.readU32();
                            String command = in.readString((int)length);
                            length = in.readU32();
                            potential = new RawInputStream(in, 0, length); // raw data
                            att.setFilename(filename);
                        }
                    }
                    data = potential;
                } finally {
                    if (data != potential)
                        potential.close();
                }
            } finally {
                in.close();
            }
        }
        att.setRawData(data);
    }

    /**
     * Reads a C-style null-terminated string of unknown length from the raw data stream.
     *
     * @param in the raw data stream
     * @param maxLength the maximum string length
     * @return the read string
     * @throws IOException if an error occurs
     */
    protected static String readString(RawInputStream in, int maxLength) throws IOException {
        byte[] buf = new byte[Math.min(maxLength, in.available())]; // prevent OOME/DoS
        int len = 0;
        int b;
        do {
            b = in.read();
            if (b < 0)
                throw new EOFException("unexpected end of stream");
            buf[len++] = (byte) b;
        } while (b > 0);
        return TNEFUtils.createString(buf, 0, len);
    }

    /**
     * Returns the raw data of a single entry.
     *
     * @param entry an entry
     * @return the entry's data
     * @throws IOException if an error occurs
     */
    protected static RawInputStream toRawInputStream(DocumentEntry entry) throws IOException {
        DocumentInputStream in = new DocumentInputStream(entry);
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
        TNEFUtils.transfer(in, out, -1, true, false);
        return new RawInputStream(out.toByteArray());
    }

    /**
     * Main entry point for command-line utility.
     *
     * @param args the command-line arguments
     * @throws IOException if an error occurs
     */
    public static void main(String[] args) throws IOException {

        if (args.length < 1 || args.length > 2) {
            String usage =
                "Usage: java net.freeutils.tnef.msg.Msg <msgfile> [outputdir]\n\n" +
                "Example: java net.freeutils.tnef.msg.Msg c:\\temp\\saved.msg c:\\temp\\attachments\n";
            System.out.println(usage);
            System.exit(1);
        }

        String filename = args[0];
        String outputdir = args.length < 2 ? "." : args[1];

        Message message = null;
        InputStream in = new FileInputStream(filename);
        try {
            POIFSFileSystem fs = new POIFSFileSystem(in);
            DirectoryEntry root = fs.getRoot();
            //printDirectory(root, ""); // useful for debugging
            message = processMessage(root);
            TNEF.extractContent(message, outputdir);
        } finally {
            TNEFUtils.closeAll(in, message);
        }
    }

}
