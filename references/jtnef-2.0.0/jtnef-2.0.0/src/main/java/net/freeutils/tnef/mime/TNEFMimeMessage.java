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

import java.util.List;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import net.freeutils.tnef.Attr;

/**
 * The <code>TNEFMimeMessage</code> class subclasses MimeMessage with the
 * Added TNEF attributes.
 *
 * @author Amichai Rothman
 * @since 2003-04-27
 */
public class TNEFMimeMessage extends MimeMessage {

    List<Attr> attributes;

    /**
     * Constructs an empty TNEFMimeMessage with default content.
     *
     * @param session the session used to handle the MimeMessage
     */
    public TNEFMimeMessage(Session session) {
        super(session);
    }

    /**
     * Gets the message's TNEF attributes.
     *
     * @return the message's TNEF attributes
     */
    public List<Attr> getTNEFAttributes() {
        return attributes;
    }

    /**
     * Sets the message's TNEF attributes.
     *
     * @param attributes the message's TNEF attributes
     */
    public void setTNEFAttributes(List<Attr> attributes) {
        this.attributes = attributes;
    }

}
