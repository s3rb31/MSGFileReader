var MailReader;
(function (MailReader) {
    var CFB;
    (function (CFB) {
        var Stream = (function () {
            function Stream(name, read) {
                this.name = name;
                this.read = read;
            }
            return Stream;
        }());
        CFB.Stream = Stream;
        var Storage = (function () {
            function Storage(name) {
                this.name = name;
                this.streams = new Map();
                this.storages = new Map();
            }
            Storage.prototype.addStream = function (stream) {
                Storage.add(stream, this.streams);
            };
            Storage.prototype.addStorage = function (storage) {
                Storage.add(storage, this.storages);
            };
            Storage.prototype.getStream = function (name) {
                return Storage.get(name, this.streams);
            };
            Storage.prototype.getStorage = function () {
                var children = [];
                for (var _i = 0; _i < arguments.length; _i++) {
                    children[_i] = arguments[_i];
                }
                var storage = this;
                if (children != undefined && children.length > 0) {
                    var elem = undefined;
                    do {
                        elem = children.shift();
                        if (elem != undefined && storage != undefined) {
                            storage = Storage.get(elem, storage.storages);
                        }
                    } while (elem != undefined);
                }
                return storage;
            };
            Storage.get = function (name, map) {
                if (name.length > 1) {
                    return map.get(name);
                }
                return undefined;
            };
            Storage.add = function (obj, map) {
                if (obj != undefined) {
                    map.set(obj.name, obj);
                }
            };
            return Storage;
        }());
        CFB.Storage = Storage;
    })(CFB = MailReader.CFB || (MailReader.CFB = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=Storage.js.map