namespace MailReader
{
    export namespace MSG
    {
        export enum BodyMethod
        {
            None = -1,
            Plain = 1,
            HTML = 2,
            HTMLRTF = 3,
            RTF = 4
        }

        export enum NativeBody
        {
            Unknown = -1,
            PlainText = 1,
            RichText = 2,
            HTML = 3
        }

        export class Mail extends File
        {
            private body_method: BodyMethod;
            private native_body: NativeBody;

            protected recipients: Recipient[];
            protected attachments: Attachment[];

            constructor(data: ArrayBuffer)
            {
                super(data);

                this.recipients = this.parseObjProps(Recipient, this.recip_count);
                this.attachments = this.parseObjProps(Attachment, this.attach_count);

                this.body_method = BodyMethod.None;
            }

            get bodyMethod()
            {
                return this.body_method;
            }

            get senderAddress()
            {
                if (this.senderAddressType == "EX")
                {
                    return this.read<string>(Property.Tag.PidTagSenderSmtpAddress) ||
                        this.readNamed<string>(0x8580, Property.Type.PT_UNICODE);
                }

                return this.read<string>(Property.Tag.PidTagSenderEmailAddress);
            }

            get subject()
            {
                return this.read<string>(Property.Tag.PidTagSubject);
            }

            get displayTo()
            {
                return this.read<string>(Property.Tag.PidTagDisplayTo);
            }

            get displayCc()
            {
                return this.read<string>(Property.Tag.PidTagDisplayCc);
            }

            get senderName()
            {
                return this.read<string>(Property.Tag.PidTagSenderName);
            }

            get deliveryTime()
            {
                let time = this.read<number>(Property.Tag.PidTagClientSubmitTime);
                return time != undefined ? Util.formatWindowsTime(time) : undefined;
            }

            get senderAddressType()
            {
                return this.read<string>(Property.Tag.PidTagSenderAddressType);
            }

            get body()
            {
                if (this.native_body == NativeBody.HTML) return this.getHTMLBody();
                if (this.native_body == NativeBody.PlainText) return this.getPlaintextBody();

                return this.getHTMLBody() || this.getRTFBody() || this.getPlaintextBody();
            }

            private getRTFBody()
            {
                let data = this.readCached(Property.Tag.PidTagRtfCompressed);

                if (data != undefined)
                {
                    this.body_method = BodyMethod.RTF;

                    // Are we dealing with capsulated HTML?

                    let rtf = (new RTF.LZFUCompression(data)).inflate();

                    if (rtf.match(/\\fromhtml1/))
                    {
                        this.body_method = BodyMethod.HTMLRTF;

                        let html = (new RTF.HTMLCapsulation(rtf)).parse();
                        return '<div>' + this.parseHTMLImages(html) + '</div>';
                    }

                    // Either "\fromtext" or pure RTF at
                    // this point ...
                }

                return undefined;
            }

            private parseHTMLImages(html: string)
            {
                let div = document.createElement("div");
                div.innerHTML = html;

                let imgs = div.getElementsByTagName("img");
                for (let i = 0; i < imgs.length; i++)
                {
                    let cid = imgs[i].src;
                    if (cid.substr(0, 4) == "cid:")
                    {
                        for (let attach of this.attachments)
                        {
                            if (attach.objectType == ObjectType.Attachment)
                            {
                                let data = attach.data;

                                if (attach.contentId == cid.substr(4) &&
                                    data != undefined && data.length > 0)
                                {
                                    imgs[i].src = "data:image/png;base64," +
                                        btoa(Util.decodeAsciiFast(data));
                                }
                            }
                        }
                    }
                }

                return div.innerHTML;
            }

            private getHTMLBody()
            {
                // We need to pass PT_BINARY explicitly because
                // PidTagBodyHtml = PidTagHtml except for the type

                let data: any = this.readCached(
                    Property.Tag.PidTagHtml, Property.Type.PT_BINARY);

                if (data != undefined)
                {
                    this.body_method = BodyMethod.HTML;

                    // The HTML body MUST NOT be UTF16 (MS-OXCMAIL 2.1.3.3.1)
                    // so we parsed it as binary, now we apply the codepage ..

                    if (this.body_cp != undefined)
                    {
                        return this.parseHTMLImages(
                            Util.decodeString(this.body_cp, data));
                    }

                    throw new Error("[MSG::File::getHTMLBody] " +
                        "PidTagInternetCodepage is undefined! Cannot decode body!")
                }

                return undefined;
            }

            private getPlaintextBody()
            {
                let data = this.readCached(Property.Tag.PidTagBody);

                if (data != undefined)
                {
                    this.body_method = BodyMethod.Plain;

                    // One of the following attributes is needed to 
                    // determine the codepage of the plaintext body

                    if ((this.body_cp || this.prop_cp))
                    {
                        let cp = this.is_unicode ? 1200 : this.body_cp;

                        return Util.decodeString(cp, data)
                            .replace(/(?:\r\n|\r|\n)/g, "<br />");
                    }

                    throw new Error("[MSG::File::getPlaintextBody] " +
                        "Body codepage could not be determined! Cannot decode body!")
                }

                return data;
            }
        }
    }
}
