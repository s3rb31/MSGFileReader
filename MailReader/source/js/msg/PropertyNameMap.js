var MailReader;
(function (MailReader) {
    var MSG;
    (function (MSG) {
        var PropertyNameMap = (function () {
            function PropertyNameMap(root) {
                this.map = new Map();
                var storage = root.getStorage(PropertyNameMap.STORE_NAME);
                if (storage != undefined) {
                    var entries = storage.getStream(PropertyNameMap.ENTRY_STREAM);
                    var strings = storage.getStream(PropertyNameMap.STRING_STREAM);
                    if (entries != undefined && strings != undefined) {
                        this.parseEntries(new Uint8Array(entries.read()).buffer, new Uint8Array(strings.read()).buffer);
                    }
                }
            }
            PropertyNameMap.prototype.get = function (key) {
                return this.map.get(key);
            };
            PropertyNameMap.prototype.parseName = function (propidx, strings) {
                var view = new DataView(strings, propidx);
                var length = view.getUint32(0, true);
                if (length > 0) {
                    var data = [];
                    for (var j = 0; j < length; j++) {
                        data.push(view.getUint8(4 + j));
                    }
                    if (data.length > 0) {
                        return MailReader.Util.decodeString(1200, data);
                    }
                }
                return undefined;
            };
            PropertyNameMap.prototype.parseEntries = function (entries, strings) {
                if (entries.byteLength > 0) {
                    var entry_num = entries.byteLength / 8;
                    for (var i = 0; i < entry_num; i++) {
                        var entry = MailReader.Util.Struct.parse(entries, PropertyNameMap.type_set, i * 8);
                        if (entry.propidx == i) {
                            var name_id = entry.nameid;
                            if ((entry.guididx & 1) == 1) {
                                var name_1 = this.parseName(name_id, strings);
                                if (name_1 != undefined) {
                                    name_id = name_1;
                                }
                            }
                            this.map.set(name_id, 0x8000 + entry.propidx);
                        }
                    }
                }
            };
            return PropertyNameMap;
        }());
        PropertyNameMap.type_set = {
            nameid: 'uint32',
            guididx: 'uint16',
            propidx: 'uint16'
        };
        PropertyNameMap.STORE_NAME = "__nameid_version1.0";
        PropertyNameMap.ENTRY_STREAM = "__substg1.0_00030102";
        PropertyNameMap.STRING_STREAM = "__substg1.0_00040102";
        MSG.PropertyNameMap = PropertyNameMap;
    })(MSG = MailReader.MSG || (MailReader.MSG = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=PropertyNameMap.js.map