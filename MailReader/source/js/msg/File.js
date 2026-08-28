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
        var File = (function (_super) {
            __extends(File, _super);
            function File(data) {
                var _this = _super.call(this, data) || this;
                _this.name_map = new MSG.PropertyNameMap(_this);
                _this.props = new MSG.MsgPropertyStream(_this, _this.name_map);
                _this.recip_count = _this.props.recip_count;
                _this.attach_count = _this.props.attach_count;
                _this.setCodepageInfo(_this.props.read(MSG.Property.Tag.PidTagStoreSupportMask));
                return _this;
            }
            File.prototype.read = function (tag, type) {
                return this.props.read(tag, type);
            };
            File.prototype.readCached = function (tag, type) {
                return this.props.readCached(tag, type);
            };
            File.prototype.readNamed = function (key, type) {
                return this.props.readNamed(key, type);
            };
            File.prototype.parseObjProps = function (type, count) {
                var array = new Array();
                for (var i = 0; i < count; i++) {
                    var storage = this.getStorage(type.PREFIX + ("00000000" + i).slice(-8));
                    if (storage != undefined) {
                        array.push(new type(storage, this.name_map, this.prop_cp));
                    }
                }
                return array;
            };
            File.prototype.setCodepageInfo = function (ssm) {
                this.is_unicode = ((ssm || 0) & 0x00040000) != 0;
                var prop_cp = this.props.read(MSG.Property.Tag.PidTagMessageCodepage);
                var body_cp = this.props.read(MSG.Property.Tag.PidTagInternetCodepage);
                if (body_cp == undefined) {
                    throw new Error("[MSG::File::constructor] Property PidTagInternetCodepage is missing!");
                }
                this.body_cp = body_cp;
                this.prop_cp = this.is_unicode ? 1200 : (prop_cp || body_cp);
                this.props.codepage = this.prop_cp;
            };
            return File;
        }(MailReader.CFB.File));
        MSG.File = File;
    })(MSG = MailReader.MSG || (MailReader.MSG = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=File.js.map