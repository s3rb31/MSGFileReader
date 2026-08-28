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
        var ObjectProperty = (function () {
            function ObjectProperty(storage, codepage) {
                this.cp = codepage;
                this.props = new MSG.ObjPropertyStream(storage);
            }
            ObjectProperty.parse = function (file, type, cp, count) {
                var arr = new Array();
                for (var i = 0; i < count; i++) {
                    var storage = file.getStorage(type.PREFIX + ("00000000" + i).slice(-8));
                    if (storage != undefined) {
                        arr.push(new type(storage, cp));
                    }
                }
                return arr;
            };
            return ObjectProperty;
        }());
        MSG.ObjectProperty = ObjectProperty;
        var RecipientType;
        (function (RecipientType) {
            RecipientType[RecipientType["Sender"] = 0] = "Sender";
            RecipientType[RecipientType["Recipient"] = 1] = "Recipient";
            RecipientType[RecipientType["CCRecipient"] = 2] = "CCRecipient";
            RecipientType[RecipientType["BCCRecipient"] = 3] = "BCCRecipient";
        })(RecipientType = MSG.RecipientType || (MSG.RecipientType = {}));
        var Recipient = (function (_super) {
            __extends(Recipient, _super);
            function Recipient() {
                var _this = _super !== null && _super.apply(this, arguments) || this;
                _this._name = "";
                _this._email = "";
                _this._type = -1;
                _this._addressType = "";
                return _this;
            }
            Object.defineProperty(Recipient.prototype, "nameIsEmail", {
                get: function () {
                    return this.name == this.email;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "type", {
                get: function () {
                    if (this._type == -1) {
                        this._type = this.props.read(MSG.PropTag.PidTagRecipientType);
                    }
                    return this._type;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "addressType", {
                get: function () {
                    if (this._addressType == "") {
                        this._addressType = this.props.readString(this.cp, MSG.PropTag.PidTagAddressType);
                    }
                    return this._addressType;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "name", {
                get: function () {
                    if (this._name == "") {
                        this._name = this.props.readString(this.cp, MSG.PropTag.PidTagDisplayName);
                    }
                    return this._name;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Recipient.prototype, "email", {
                get: function () {
                    if (this._email == "") {
                        var prop = this.addressType == "SMTP" ?
                            MSG.PropTag.PidTagEmailAddress : MSG.PropTag.PidTagSmtpAddress;
                        this._email = this.props.readString(this.cp, prop);
                    }
                    return this._email;
                },
                enumerable: true,
                configurable: true
            });
            return Recipient;
        }(ObjectProperty));
        Recipient.PREFIX = "__recip_version1.0_#";
        MSG.Recipient = Recipient;
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
        var Attachment = (function (_super) {
            __extends(Attachment, _super);
            function Attachment() {
                var _this = _super !== null && _super.apply(this, arguments) || this;
                _this._contentId = "";
                _this._data = [];
                _this._hidden = -1;
                _this._objectType = -1;
                _this._attachMethod = -1;
                _this._displayName = "";
                _this._fileExtension = "";
                _this._mimeTag = "";
                return _this;
            }
            Object.defineProperty(Attachment.prototype, "contentId", {
                get: function () {
                    if (this._contentId == "") {
                        this._contentId = this.props.readString(this.cp, MSG.PropTag.PidTagAttachContentId);
                    }
                    return this._contentId;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "data", {
                get: function () {
                    if (this._data != undefined && this._data.length == 0) {
                        this._data = this.props.read(MSG.PropTag.PidTagAttachDataBinary, MSG.PropType.PT_BINARY);
                        this._data = this._data == undefined ? false : this._data;
                    }
                    return this._data;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "hidden", {
                get: function () {
                    if (this._hidden == -1) {
                        this._hidden = this.props.read(MSG.PropTag.PidTagAttachmentHidden);
                    }
                    return this._hidden;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "objectType", {
                get: function () {
                    if (this._objectType == -1) {
                        this._objectType = this.props.read(MSG.PropTag.PidTagObjectType);
                    }
                    return this._objectType;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "attachMethod", {
                get: function () {
                    if (this._attachMethod == -1) {
                        this._attachMethod = this.props.read(MSG.PropTag.PidTagAttachMethod);
                    }
                    return this._attachMethod;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "displayName", {
                get: function () {
                    if (this._displayName == "") {
                        this._displayName = this.props.readString(this.cp, MSG.PropTag.PidTagDisplayName);
                    }
                    return this._displayName;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "fileExtension", {
                get: function () {
                    if (this._fileExtension == "") {
                        this._fileExtension = this.props.readString(this.cp, MSG.PropTag.PidTagAttachExtension);
                    }
                    return this._fileExtension;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Attachment.prototype, "mimeTag", {
                get: function () {
                    if (this._mimeTag == "") {
                        this._mimeTag = this.props.readString(this.cp, MSG.PropTag.PidTagAttachMimeTag);
                    }
                    return this._mimeTag;
                },
                enumerable: true,
                configurable: true
            });
            return Attachment;
        }(ObjectProperty));
        Attachment.PREFIX = "__attach_version1.0_#";
        MSG.Attachment = Attachment;
    })(MSG = MailReader.MSG || (MailReader.MSG = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=ObjectProperty.js.map