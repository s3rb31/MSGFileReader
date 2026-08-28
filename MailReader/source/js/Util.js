var MailReader;
(function (MailReader) {
    var Util;
    (function (Util) {
        var Struct = (function () {
            function Struct() {
            }
            Struct.readValue = function (view, type, lE) {
                switch (type) {
                    case "int8":
                        Struct.offset += 1;
                        return view.getInt8(Struct.offset - 1);
                    case "uint8":
                        Struct.offset += 1;
                        return view.getUint8(Struct.offset - 1);
                    case "int16":
                        Struct.offset += 2;
                        return view.getInt16(Struct.offset - 2, lE);
                    case "uint16":
                        Struct.offset += 2;
                        return view.getUint16(Struct.offset - 2, lE);
                    case "int32":
                        Struct.offset += 4;
                        return view.getInt32(Struct.offset - 4, lE);
                    case "uint32":
                        Struct.offset += 4;
                        return view.getUint32(Struct.offset - 4, lE);
                    case "float32":
                        Struct.offset += 4;
                        return view.getFloat32(Struct.offset - 4, lE);
                    case "float64":
                        Struct.offset += 8;
                        return view.getFloat64(Struct.offset - 4, lE);
                }
            };
            Struct.readArray = function (view, type, length, lE) {
                var array = [];
                for (var i = 0; i < length; i++) {
                    var value = this.readValue(view, type, lE);
                    if (value == undefined) {
                        return undefined;
                    }
                    array.push(value);
                }
                return array;
            };
            Struct.parse = function (data, type_set, offset) {
                if (offset === void 0) { offset = 0; }
                Struct.offset = 0;
                var dv = new DataView(data, offset);
                var struct = {};
                for (var key in type_set) {
                    if (type_set.hasOwnProperty(key)) {
                        var type = type_set[key];
                        if (typeof type == "string") {
                            struct[key] = this.readValue(dv, type, true);
                        }
                        else {
                            struct[key] = this.readArray(dv, type[0], type[1], true);
                        }
                    }
                }
                return struct;
            };
            return Struct;
        }());
        Struct.offset = 0;
        Util.Struct = Struct;
        var CustomMap = (function () {
            function CustomMap() {
                this.keys = [];
                this.values = [];
                this.index = 0;
            }
            Object.defineProperty(CustomMap.prototype, "size", {
                get: function () {
                    return this.keys.length;
                },
                enumerable: true,
                configurable: true
            });
            CustomMap.prototype.get = function (key) {
                return this.has(key) ?
                    this.values[this.index] : undefined;
            };
            CustomMap.prototype.clear = function () {
                this.keys.length = 0;
                this.values.length = 0;
            };
            CustomMap.prototype.set = function (key, value) {
                this.has(key) ?
                    this.values[this.index] = value :
                    this.values[this.keys.push(key) - 1] = value;
                return this;
            };
            CustomMap.prototype.delete = function (key) {
                if (this.has(key)) {
                    this.keys.splice(this.index, 1);
                    this.values.splice(this.index, 1);
                }
                return this.index > -1;
            };
            CustomMap.prototype.has = function (key) {
                var is = function (a, b) { return (a === b) || (a !== a && b !== b); };
                if (key == key && key !== 0) {
                    this.index = this.keys.indexOf(key);
                }
                else {
                    for (this.index = this.keys.length; this.index-- && is(key, this.keys[this.index]);) { }
                }
                return this.index > -1;
            };
            return CustomMap;
        }());
        Util.CustomMap = CustomMap;
        function formatWindowsTime(time) {
            var unix_time = (time / 10000000) - 11644473600;
            var date = new Date(unix_time * 1000);
            return date.toLocaleString(navigator.language, {
                weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
                hour: '2-digit', minute: '2-digit', second: '2-digit', timeZoneName: 'short'
            });
        }
        Util.formatWindowsTime = formatWindowsTime;
        function decodeAsciiFast(data, length) {
            if (length === void 0) { length = data.length; }
            var string = "";
            var slice_size = 512;
            for (var offset = 0; offset < length; offset += slice_size) {
                string += String.fromCharCode.apply(null, data.slice(offset, offset + slice_size));
            }
            return string;
        }
        Util.decodeAsciiFast = decodeAsciiFast;
        function decodeString(cp, data, length) {
            if (typeof data != "number") {
                data = length ? data.slice(0, length) : data;
            }
            return (typeof data != "number") ?
                cptable.utils.decode(cp, data) : cptable[cp].dec[data];
        }
        Util.decodeString = decodeString;
    })(Util = MailReader.Util || (MailReader.Util = {}));
})(MailReader || (MailReader = {}));
if (typeof Map == "undefined") {
    self.Map = MailReader.Util.CustomMap;
}
//# sourceMappingURL=Util.js.map