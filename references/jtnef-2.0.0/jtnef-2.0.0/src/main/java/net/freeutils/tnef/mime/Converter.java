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

import java.io.IOException;
import javax.mail.MessagingException;
import net.freeutils.tnef.Attr;
import net.freeutils.tnef.Message;

/**
 * The <code>Converter</code> base class converts different classes of
 * TNEF messages into standard MIME messages.
 *
 * @author Amichai Rothman
 * @since 2015-10-30
 */
public abstract class Converter {

    /**
     * Checks whether the given message is of the specified message class.
     *
     * @param message the message to check
     * @param messageClass the messageClass to check against
     * @return whether the given message is of the specified message class
     */
    protected boolean isMessageClass(Message message, String messageClass) {
        try {
            Attr attr = message.getAttribute(Attr.attMessageClass);
            return attr != null && messageClass.equalsIgnoreCase((String)attr.getValue());
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Checks whether the given message can be converted by this converter.
     *
     * @param message the message to test
     * @return whether the given message can be converted by this converter
     */
    public abstract boolean canConvert(Message message);

    /**
     * Converts a TNEF message into a MIME message.
     *
     * @param message the TNEF Message to convert
     * @param mime the target MIME message into which the content is written
     * @return the converted MIME message
     * @throws IOException if an I/O error occurs
     * @throws MessagingException if an error occurs while accessing the MimeEMessage
     */
    public abstract TNEFMimeMessage convert(Message message, TNEFMimeMessage mime)
            throws IOException, MessagingException;

}
