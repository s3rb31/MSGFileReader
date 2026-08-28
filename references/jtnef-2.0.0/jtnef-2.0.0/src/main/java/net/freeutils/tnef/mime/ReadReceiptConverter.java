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
import java.text.DateFormat;
import java.util.Date;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;

import net.freeutils.tnef.*;
import net.freeutils.tnef.Message;

/**
 * The <code>ReadReceiptConverter</code> class converts a read receipt
 * from a TNEF message into a standard RFC 2298 notification message.
 *
 * @author Amichai Rothman
 * @since 2007-04-27
 */
public class ReadReceiptConverter extends Converter {

    @Override
    public boolean canConvert(Message message) {
        return isMessageClass(message, "IPM.Microsoft Mail.Read Receipt");
    }

    @Override
    public TNEFMimeMessage convert(Message message, TNEFMimeMessage mime)
            throws IOException, MessagingException {
        // get required data from message
        MAPIProps props = message.getMAPIProps();
        String recipient = (String)props.getPropValue(MAPIProp.PR_ORIGINAL_DISPLAY_TO);
        String subject = (String)props.getPropValue(MAPIProp.PR_CONVERSATION_TOPIC);
        Date sentDate = (Date)props.getPropValue(MAPIProp.PR_ORIGINAL_SUBMIT_TIME);
        Date readDate = (Date)props.getPropValue(MAPIProp.PR_REPORT_TIME);
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        // create multipart
        MimeMultipart mp = new MimeMultipart("report; report-type=disposition-notification");

        // add text part
        StringBuilder text = new StringBuilder();
        text.append("Your message\r\n");
        text.append("\r\n      To:\t");
        if (recipient != null)
            text.append(recipient);
        text.append("\r\n      Subject:\t");
        if (subject != null)
            text.append(subject);
        text.append("\r\n      Sent:\t");
        if (sentDate != null)
            text.append(format.format(sentDate));
        if (readDate != null)
            text.append("\r\n\r\nwas read on ").append(format.format(readDate)).append('.');
        text.append("\r\n");
        TNEFMime.addTextPart(mp, text.toString(), "text/plain");

        // add notification part
        text.setLength(0);
        text.append("Original-Recipient: rfc822;").append(recipient).append("\r\n")
            .append("Final-Recipient: rfc822;").append(recipient).append("\r\n")
            .append("Disposition: manual-action/MDN-sent-manually; displayed").append("\r\n");
        TNEFMime.addTextPart(mp, text.toString(), "message/disposition-notification");

        mime.setContent(mp);
        return mime;
    }

}
