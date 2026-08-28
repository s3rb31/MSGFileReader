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
        var PropertyStream = (function () {
            function PropertyStream(storage, name_map) {
                var stream = storage.getStream(PropertyStream.NAME);
                if (stream == undefined) {
                    throw new Error("[MSG::PropertyStream::constructor] " +
                        "Property stream is missing in " + storage.name);
                }
                this.name_map = name_map;
                this.prop_map = new Map();
                this.cache = new Map();
                this._data = new Uint8Array(stream.read()).buffer;
            }
            PropertyStream.prototype.readNamed = function (key, type) {
                var tag = this.name_map.get(key);
                if (tag != undefined) {
                    return this.read(tag, type);
                }
            };
            PropertyStream.prototype.readCached = function (tag, type) {
                var id = (tag << 16) +
                    (type || MSG.Property.Type.PT_UNSPECIFIED);
                if (this.cache.has(id) == false) {
                    var prop = this.prop_map.get(id);
                    if (prop != undefined) {
                        var val = prop.read();
                        this.cache.set(id, val);
                    }
                }
                return this.cache.get(id);
            };
            PropertyStream.prototype.read = function (tag, type) {
                var prop = this.prop_map.get((tag << 16) +
                    (type || MSG.Property.Type.PT_UNSPECIFIED));
                if (prop != undefined) {
                    if (prop.type == MSG.Property.Type.PT_STRING8 ||
                        prop.type == MSG.Property.Type.PT_UNICODE) {
                        var data = prop.read();
                        if (data != undefined) {
                            var _cp = this._codepage;
                            if (prop.type == MSG.Property.Type.PT_UNICODE) {
                                _cp = 1200;
                            }
                            return MailReader.Util.decodeString(_cp, data);
                        }
                    }
                    else {
                        return prop.read();
                    }
                }
                return undefined;
            };
            PropertyStream.prototype.parseEntries = function (storage, header_size) {
                var count = (this._data.byteLength - header_size) / 16;
                for (var i = 0; i < count; i++) {
                    var prop = new MSG.Property(this._data, storage, i * 16);
                    this.prop_map.set((prop.tag << 16) + prop.type, prop);
                    this.prop_map.set((prop.tag << 16) + MSG.Property.Type.PT_UNSPECIFIED, prop);
                }
            };
            return PropertyStream;
        }());
        PropertyStream.NAME = "__properties_version1.0";
        var ObjPropertyStream = (function (_super) {
            __extends(ObjPropertyStream, _super);
            function ObjPropertyStream(storage, nm, cp) {
                var _this = _super.call(this, storage, nm) || this;
                _this._codepage = cp;
                _this._data = _this._data.slice(8);
                _this.parseEntries(storage, 8);
                return _this;
            }
            return ObjPropertyStream;
        }(PropertyStream));
        MSG.ObjPropertyStream = ObjPropertyStream;
        var MsgPropertyStream = (function (_super) {
            __extends(MsgPropertyStream, _super);
            function MsgPropertyStream(storage, nm, embedded) {
                if (embedded === void 0) { embedded = false; }
                var _this = _super.call(this, storage, nm) || this;
                var header = MailReader.Util.Struct.parse(_this._data, MsgPropertyStream.type_set, 8);
                _this.next_recip = header.next_recip;
                _this.next_attach = header.next_attach;
                _this.recip_count = header.recip_count;
                _this.attach_count = header.attach_count;
                _this._data = _this._data.slice(embedded ? 24 : 32);
                _this.parseEntries(storage, embedded ? 24 : 32);
                return _this;
            }
            Object.defineProperty(MsgPropertyStream.prototype, "codepage", {
                set: function (value) {
                    this._codepage = value;
                },
                enumerable: true,
                configurable: true
            });
            return MsgPropertyStream;
        }(PropertyStream));
        MsgPropertyStream.type_set = {
            next_recip: 'uint32',
            next_attach: 'uint32',
            recip_count: 'uint32',
            attach_count: 'uint32'
        };
        MSG.MsgPropertyStream = MsgPropertyStream;
    })(MSG = MailReader.MSG || (MailReader.MSG = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=PropertyStream.js.map