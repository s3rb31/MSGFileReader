declare namespace MailReader 
{
    namespace MSG 
    {
        enum BodyMethod
        {
            None = -1,
            Plain = 1,
            HTML = 2,
            HTMLRTF = 3,
            RTF = 4
        }

        enum RecipientType
        {
            Sender = 0,
            Recipient = 1,
            CCRecipient = 2,
            BCCRecipient = 3,
        }

        enum ObjectType
        {
            Store = 1,
            AddressBook = 2,
            AddressBookContainer = 4,
            Message = 5,
            MailUser = 6,
            Attachment = 7,
            DistributionList = 8,
        }

        enum AttachMethod
        {
            ByValue = 1,
            ByReference = 2,
            ByReferenceOnly = 4,
            EmbeddedMessage = 5,
            Storage = 6,
            ByWebReference = 7,
        }

        class Recipient
        {
            static readonly PREFIX: string;

            readonly nameIsEmail: boolean;
            readonly type: RecipientType;
            readonly addressType: string;
            readonly name: string;
            readonly email: string;
        }

        class Attachment
        {
            static readonly PREFIX: string;

            readonly contentId: string;
            readonly data: number[];
            readonly hidden: boolean | undefined;
            readonly objectType: ObjectType;
            readonly attachMethod: AttachMethod;
            readonly displayName: string;
            readonly fileExtension: string;
            readonly mimeTag: string;
        }

        class Mail
		{
            readonly recipients: Recipient[];
            readonly attachments: Attachment[];

            constructor(data: ArrayBuffer);
			
            public parse(): void;

            readonly bodyMethod: BodyMethod;
            readonly subject: string;
            readonly deliveryTime: string;
            readonly creationTime: number;
            readonly displayTo: string;
            readonly displayCc: string;
            readonly displayBcc: string;
            readonly senderName: string;
            readonly senderAddress: string;
            readonly senderAddressType: string;
            readonly body: string;
        }
    }
}
