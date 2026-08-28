namespace MailReader 
{
    export namespace MSG 
    {
        export enum RecipientType
        {
            Sender = 0,
            Recipient = 1,
            CCRecipient = 2,
            BCCRecipient = 3
        }

        export enum ObjectType
        {
            Store = 1,
            AddressBook = 2,
            AddressBookContainer = 4,
            Message = 5,
            MailUser = 6,
            Attachment = 7,
            DistributionList = 8
        }

        export enum AttachMethod
        {
            ByValue = 1,
            ByReference = 2,
            ByReferenceOnly = 4,
            EmbeddedMessage = 5,
            Storage = 6, // OLE
            ByWebReference = 7
        }

        export interface IObjProperty<T extends ObjPropertyStream>
        {
            PREFIX: string;
            new(storage: CFB.Storage, nm: PropertyNameMap, codepage: number): T;
        }

        export class Recipient extends ObjPropertyStream
        {
            static readonly PREFIX = "__recip_version1.0_#";

            get nameIsEmail()
            {
                return this.name == this.email;
            }

            get name()
            {
                return this.read<string>(Property.Tag.PidTagDisplayName);
            }

            get type()
            {
                return this.read<RecipientType>(Property.Tag.PidTagRecipientType);
            }

            get addressType()
            {
                return this.read<string>(Property.Tag.PidTagAddressType);
            }

            get email()
            {
                let prop = this.addressType == "SMTP" ?
                    Property.Tag.PidTagEmailAddress : Property.Tag.PidTagSmtpAddress;

                return this.read<string>(prop);
            }
        }

        export class Attachment extends ObjPropertyStream
        {
            static readonly PREFIX = "__attach_version1.0_#";

            get contentId()
            {
                return this.read<string>(Property.Tag.PidTagAttachContentId);
            }

            get hidden()
            {
                return this.read<boolean>(Property.Tag.PidTagAttachmentHidden);
            }

            get objectType()
            {
                return this.read<ObjectType>(Property.Tag.PidTagObjectType);
            }

            get attachMethod()
            {
                return this.read<AttachMethod>(Property.Tag.PidTagAttachMethod);
            }

            get displayName()
            {
                return this.read<string>(Property.Tag.PidTagDisplayName);
            }

            get fileExtension()
            {
                return this.read<string>(Property.Tag.PidTagAttachExtension);
            }

            get mimeTag()
            {
                return this.read<string>(Property.Tag.PidTagAttachMimeTag);
            }

            get data()
            {
                return this.readCached(Property.Tag.PidTagAttachDataBinary, Property.Type.PT_BINARY);
            }
        }
    }
}
