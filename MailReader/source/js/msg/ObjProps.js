var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var MailReader;
(function (MailReader) {
    var MSG;
    (function (MSG) {
        var RecipientType;
        (function (RecipientType) {
            RecipientType[RecipientType["Sender"] = 0] = "Sender";
            RecipientType[RecipientType["Recipient"] = 1] = "Recipient";
            RecipientType[RecipientType["CCRecipient"] = 2] = "CCRecipient";
            RecipientType[RecipientType["BCCRecipient"] = 3] = "BCCRecipient";
        })(RecipientType = MSG.RecipientType || (MSG.RecipientType = {}));
        var ObjectType;
        (function (ObjectType) {
            ObjectType[ObjectType["Store"] = 1] = "Store";
            ObjectType[ObjectType["AddressBook"] = 2] = "AddressBook";
            ObjectType[ObjectType["AddressBookContainer"] = 4] = "AddressBookContainer";
            ObjectType[ObjectType["Message"] = 5] = "Message";
            ObjectType[ObjectType["MailUser"] = 6] = "MailUser";
            ObjectType[ObjectType["Attachment"] = 7] = "Attachment";
            ObjectType[ObjectType["DistributionList"] = 8] = "DistributionList";
        })(ObjectType = MSG.ObjectType || (MSG.ObjectType = {}));
        var AttachMethod;
        (function (AttachMethod) {
            AttachMethod[AttachMethod["ByValue"] = 1] = "ByValue";
            AttachMethod[AttachMethod["ByReference"] = 2] = "ByReference";
            AttachMethod[AttachMethod["ByReferenceOnly"] = 4] = "ByReferenceOnly";
            AttachMethod[AttachMethod["EmbeddedMessage"] = 5] = "EmbeddedMessage";
            AttachMethod[AttachMethod["Storage"] = 6] = "Storage";
            AttachMethod[AttachMethod["ByWebReference"] = 7] = "ByWebReference";
        })(AttachMethod = MSG.AttachMethod || (MSG.AttachMethod = {}));
        var Recipient = (function (_super) {
            __extends(Recipient, _super);
            function Recipient() {
                return _super !== null && _super.apply(this, arguments) || this;
            }
            Object.defineProperty(Recipient.prototype, "nameIsEmail", {
                get: function () {
                    return this.name == this.email;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "name", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagDisplayName);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "type", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagRecipientType);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "addressType", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagAddressType);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "email", {
                get: function () {
                    var prop = this.addressType == "SMTP" ?
                        MSG.Property.Tag.PidTagEmailAddress : MSG.Property.Tag.PidTagSmtpAddress;
                    return this.read(prop);
                },
                enumerable: true,
                configurable: true
            });
            return Recipient;
        }(MSG.ObjPropertyStream));
        Recipient.PREFIX = "__recip_version1.0_#";
        MSG.Recipient = Recipient;
        var Attachment = (function (_super) {
            __extends(Attachment, _super);
            function Attachment() {
                return _super !== null && _super.apply(this, arguments) || this;
            }
            Object.defineProperty(Attachment.prototype, "contentId", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagAttachContentId);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "hidden", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagAttachmentHidden);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "objectType", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagObjectType);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "attachMethod", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagAttachMethod);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "displayName", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagDisplayName);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "fileExtension", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagAttachExtension);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "mimeTag", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagAttachMimeTag);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "data", {
                get: function () {
                    return this.readCached(MSG.Property.Tag.PidTagAttachDataBinary, MSG.Property.Type.PT_BINARY);
                },
                enumerable: true,
                configurable: true
            });
            return Attachment;
        }(MSG.ObjPropertyStream));
        Attachment.PREFIX = "__attach_version1.0_#";
        MSG.Attachment = Attachment;
    })(MSG = MailReader.MSG || (MailReader.MSG = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=ObjProps.js.map