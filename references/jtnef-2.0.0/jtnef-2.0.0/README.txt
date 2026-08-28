

JTNEF - Java TNEF package 2.0.0
===============================

Copyright © 2003-2015 Amichai Rothman



1. What is TNEF?

    Transport-Neutral Encapsulation Format (TNEF) is Microsoft's non-standard
    format for encapsulating mail which has any non-plain-text content or
    properties (such as rich text, embedded OLE objects, voting buttons, and
    sometimes just attachments). Whether or not a given message is encoded
    using TNEF is determined by the Outlook default settings, per-recipient
    setting, Exchange Server settings, and message type and content.

    Once a TNEF message is used, the entire message, including all the
    original attachments and properties, is encapsulated in a single
    attachment of mime type "application/ms-tnef" added to the message to be
    sent over the Internet. This attachment is usually named "WINMAIL.DAT",
    and when sent to any non-MS mail client, is useless, and makes access to
    the original message attachments impossible.


2. What is the Java TNEF package?

    The Java TNEF package is an open-source implementation of a TNEF message
    handler, which can be used as a command-line utility or integrated into
    Java-based mail applications to extract the original message content.

    It has been in use in many production systems around the world for over a
    decade, including products by small start-ups, large open-source service
    providers, and well-known multinational corporations.


3. How do I use the Java TNEF package?

    The Java TNEF package is written in pure Java, runs on JDK 1.5 or later,
    and requires no special installation - just add the jar file to your
    classpath.

    It is also available on Maven Central at the artifact coordinates
    net.freeutils:jtnef:2.0.0.

    If you are an end user getting strange attachments named "WINMAIL.DAT" or
    "ATT00001.DAT", instead of other expected attachments, you can simply run
    the net.freeutils.tnef.TNEF class from the command line to extract the
    original attachments from such a TNEF file.

    If you are a Java developer working on a mail client or server, and need
    to handle TNEF attachments (because whether you like it or not, they're
    out there in real-world messages), you have several choices:

        1.  Low-level: you can use the TNEFInputStream class to read TNEF
            attributes, which are the basic unit in a TNEF stream, and do with
            them as you please.

        2.  Middle-level: the net.freeutils.tnef package gives you access to
            the entire TNEF content through simple Java objects representing
            the underlying TNEF data structures. You can use these classes to
            access all TNEF attributes and MAPI properties that were sent with
            the message.

            For example, you can choose to implement voting buttons or receipt
            notifications in your Java application by finding and interpreting
            the appropriate MAPI properties. This requires knowledge of the
            MAPI properties and their meaning.

        3.  High-level: The net.freeutils.tnef.TNEF class is a simple example
            of using these middle-level classes to display the message
            properties and extract the attachments. You can use it directly
            from your application, or just browse the source code for an
            example of how to do things yourself.

            The net.freeutils.tnef.mime package gives you high-level access to
            the TNEF message using the JavaMail API. The TNEFMime class is a
            simple example of using these classes, and allows you to extract a
            TNEF attachment from a mime message (which can then be processed
            using the TNEF or TNEFMime classes) or to convert a TNEF attachment
            or a message containing a TNEF attachment to an equivalent standard
            mime message with the original header fields and attachments,
            including read receipt notification conversion and contact to vCard
            conversion, etc.

            The net.freeutils.tnef.msg package contains a .msg file parser.
            The Msg class uses the Apache POI library and provides a simple API
            for parsing a .msg file into a net.freeutils.tnef.Message instance
            for access to its MAPI properties and attachments.


4. License

    The Java TNEF package is provided under the GNU General Public License
    agreement. Please read the full license agreement in the included
    LICENSE.gpl.txt file.

    For non-GPL commercial licensing please contact the address below.


5. Contact

    Please write to support@freeutils.net with any bugs, suggestions, fixes,
    contributions, or just to drop a good word and let me know you've found
    this package useful and you'd like it to keep being maintained.

    Updates and additional info can be found at
    http://www.freeutils.net/source/jtnef/
