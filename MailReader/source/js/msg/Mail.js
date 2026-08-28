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
        var BodyMethod;
        (function (BodyMethod) {
            BodyMethod[BodyMethod["None"] = -1] = "None";
            BodyMethod[BodyMethod["Plain"] = 1] = "Plain";
            BodyMethod[BodyMethod["HTML"] = 2] = "HTML";
            BodyMethod[BodyMethod["HTMLRTF"] = 3] = "HTMLRTF";
            BodyMethod[BodyMethod["RTF"] = 4] = "RTF";
        })(BodyMethod = MSG.BodyMethod || (MSG.BodyMethod = {}));
        var NativeBody;
        (function (NativeBody) {
            NativeBody[NativeBody["Unknown"] = -1] = "Unknown";
            NativeBody[NativeBody["PlainText"] = 1] = "PlainText";
            NativeBody[NativeBody["RichText"] = 2] = "RichText";
            NativeBody[NativeBody["HTML"] = 3] = "HTML";
        })(NativeBody = MSG.NativeBody || (MSG.NativeBody = {}));
        var Mail = (function (_super) {
            __extends(Mail, _super);
            function Mail(data) {
                var _this = _super.call(this, data) || this;
                _this.recipients = _this.parseObjProps(MSG.Recipient, _this.recip_count);
                _this.attachments = _this.parseObjProps(MSG.Attachment, _this.attach_count);
                _this.body_method = BodyMethod.None;
                return _this;
            }
            Object.defineProperty(Mail.prototype, "bodyMethod", {
                get: function () {
                    return this.body_method;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "senderAddress", {
                get: function () {
                    if (this.senderAddressType == "EX") {
                        return this.read(MSG.Property.Tag.PidTagSenderSmtpAddress) ||
                            this.readNamed(0x8580, MSG.Property.Type.PT_UNICODE);
                    }
                    return this.read(MSG.Property.Tag.PidTagSenderEmailAddress);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "subject", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagSubject);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "displayTo", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagDisplayTo);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "displayCc", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagDisplayCc);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "senderName", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagSenderName);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "deliveryTime", {
                get: function () {
                    var time = this.read(MSG.Property.Tag.PidTagClientSubmitTime);
                    return time != undefined ? MailReader.Util.formatWindowsTime(time) : undefined;
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "senderAddressType", {
                get: function () {
                    return this.read(MSG.Property.Tag.PidTagSenderAddressType);
                },
                enumerable: true,
                configurable: true
            });
            Object.defineProperty(Mail.prototype, "body", {
                get: function () {
                    if (this.native_body == NativeBody.HTML)
                        return this.getHTMLBody();
                    if (this.native_body == NativeBody.PlainText)
                        return this.getPlaintextBody();
                    return this.getHTMLBody() || this.getRTFBody() || this.getPlaintextBody();
                },
                enumerable: true,
                configurable: true
            });
            Mail.prototype.getRTFBody = function () {
                var data = this.readCached(MSG.Property.Tag.PidTagRtfCompressed);
                if (data != undefined) {
                    this.body_method = BodyMethod.RTF;
                    var rtf = (new MailReader.RTF.LZFUCompression(data)).inflate();
                    if (rtf.match(/\\fromhtml1/)) {
                        this.body_method = BodyMethod.HTMLRTF;
                        var html = (new MailReader.RTF.HTMLCapsulation(rtf)).parse();
                        return '<div>' + this.parseHTMLImages(html) + '</div>';
                    }
                }
                return undefined;
            };
            Mail.prototype.parseHTMLImages = function (html) {
                var div = document.createElement("div");
                div.innerHTML = html;
                var imgs = div.getElementsByTagName("img");
                for (var i = 0; i < imgs.length; i++) {
                    var cid = imgs[i].src;
                    if (cid.substr(0, 4) == "cid:") {
                        for (var _i = 0, _a = this.attachments; _i < _a.length; _i++) {
                            var attach = _a[_i];
                            if (attach.objectType == MSG.ObjectType.Attachment) {
                                var data = attach.data;
                                if (attach.contentId == cid.substr(4) &&
                                    data != undefined && data.length > 0) {
                                    imgs[i].src = "data:image/png;base64," +
                                        btoa(MailReader.Util.decodeAsciiFast(data));
                                }
                            }
                        }
                    }
                }
                return div.innerHTML;
            };
            Mail.prototype.getHTMLBody = function () {
                var data = this.readCached(MSG.Property.Tag.PidTagHtml, MSG.Property.Type.PT_BINARY);
                if (data != undefined) {
                    this.body_method = BodyMethod.HTML;
                    if (this.body_cp != undefined) {
                        return this.parseHTMLImages(MailReader.Util.decodeString(this.body_cp, data));
                    }
                    throw new Error("[MSG::File::getHTMLBody] " +
                        "PidTagInternetCodepage is undefined! Cannot decode body!");
                }
                return undefined;
            };
            Mail.prototype.getPlaintextBody = function () {
                var data = this.readCached(MSG.Property.Tag.PidTagBody);
                if (data != undefined) {
                    this.body_method = BodyMethod.Plain;
                    if ((this.body_cp || this.prop_cp)) {
                        var cp = this.is_unicode ? 1200 : this.body_cp;
                        return MailReader.Util.decodeString(cp, data)
                            .replace(/(?:\r\n|\r|\n)/g, "<br />");
                    }
                    throw new Error("[MSG::File::getPlaintextBody] " +
                        "Body codepage could not be determined! Cannot decode body!");
                }
                return data;
            };
            return Mail;
        }(MSG.File));
        MSG.Mail = Mail;
    })(MSG = MailReader.MSG || (MailReader.MSG = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=Mail.js.map