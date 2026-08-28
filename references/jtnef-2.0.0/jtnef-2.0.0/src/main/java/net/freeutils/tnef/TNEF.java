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

import java.io.File;
import java.io.IOException;

/**
 * The <code>TNEF</code> class provides high-level utility methods to
 * access TNEF streams and extract their contents.
 *
 * Note: This class is experimental and is intended to show possible uses
 * of the Java TNEF package.
 *
 * @author Amichai Rothman
 * @since 2003-04-25
 */
public class TNEF {

    /**
     * Extracts the content from the given TNEFInputStream, saving attachments
     * to the given output directory.
     *
     * @param in the TNEFInputStream containing the content
     * @param outputdir the directory in which attachments should be saved
     * @return the number of attachments that were saved
     * @throws IOException if an I/O error occurs
     */
    public static int extractContent(TNEFInputStream in, String outputdir) throws IOException {
        Message message = new Message(in);
        try {
            return extractContent(message, outputdir);
        } finally {
            message.close();
        }
    }

    /**
     * Extracts the content from the given TNEF Message, saving attachments
     * to the given output directory, and printing attributes and properties
     * to standard output.
     *
     * @param message the Message containing the content
     * @param outputdir the directory in which attachments should be saved
     * @return the number of attachments that were saved
     * @throws IOException if an I/O error occurs
     */
    public static int extractContent(Message message, String outputdir) throws IOException {
        // extract attributes and MAPI properties to standard output
        System.out.println(message);
        // extract attachments to files
        int count = extractAttachments(message, outputdir);
        System.out.println("\nWrote " + count + " attachments.");
        return count;
    }

    /**
     * Extracts the attachments from the given TNEF Message, saving them
     * to the given output directory.
     *
     * @param message the Message containing the content
     * @param outputdir the directory in which attachments should be saved
     * @return the number of attachments that were saved
     * @throws IOException if an I/O error occurs
     */
    public static int extractAttachments(Message message, String outputdir) throws IOException {
        if (!outputdir.endsWith(File.separator))
            outputdir += File.separator;

        // extract attachments to files
        int count = 0;
        for (Attachment attachment : message.getAttachments()) {
            if (attachment.getNestedMessage() != null) { // nested message
                count += extractAttachments(attachment.getNestedMessage(), outputdir);
            } else { // regular attachment
                count++;
                String filename = attachment.getFilename();
                if (filename != null)
                    filename = filename.replaceAll("(.*[/\\\\])*", ""); // remove path
                if (filename == null || filename.replace(".", "").length() == 0)
                    filename = "attachment" + count;
                filename = outputdir + filename;
                System.out.println("\n>>> Writing attachment #" + count + " to " + filename);
                attachment.writeTo(filename);
            }
        }
        return count;
    }

    /**
     * Main entry point for command-line utility.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {

        if (args.length < 1 || args.length > 2) {
            String usage =
                "Usage: java net.freeutils.tnef.TNEF <tneffile> [outputdir]\n\n" +
                "Example: java net.freeutils.tnef.TNEF c:\\temp\\winmail.dat c:\\temp\\attachments\n";
            System.out.println(usage);
            System.exit(1);
        }

        String tneffile = args[0];
        String outputdir = args.length < 2 ? "." : args[1];

        System.out.println("Processing TNEF file " + tneffile + "\n");

        RawInputStream ris = null;
        TNEFInputStream in = null;
        try {
            ris = new RawInputStream(tneffile);
            in = new TNEFInputStream(ris);
            extractContent(in, outputdir);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Operation aborted.");
            System.exit(-1);
        } finally {
            try {
                TNEFUtils.closeAll(in, ris);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        System.out.println("\nFinished processing TNEF file " + tneffile);
    }

}
