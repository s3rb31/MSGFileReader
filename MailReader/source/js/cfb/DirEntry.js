var MailReader;
(function (MailReader) {
    var CFB;
    (function (CFB) {
        var DirEntry = (function () {
            function DirEntry(data, offset, parents) {
                if (parents === void 0) { parents = []; }
                var dir = MailReader.Util.Struct.parse(data, DirEntry.type_set, offset);
                for (var _i = 0, _a = ["child", "left", "right"]; _i < _a.length; _i++) {
                    var k = _a[_i];
                    if (dir[k] == -1) {
                        dir[k] = undefined;
                    }
                    this[k] = dir[k];
                }
                this.parents = parents;
                this.type = dir.type;
                this.name = MailReader.Util.decodeString(1200, dir.name, dir.name_sz - 2);
                this.stream_start = dir.stream_start;
                this.stream_size = dir.stream_size;
            }
            return DirEntry;
        }());
        DirEntry.type_set = {
            name: ['uint8', 64],
            name_sz: 'uint16',
            type: 'uint8',
            color: 'uint8',
            left: 'int32',
            right: 'int32',
            child: 'int32',
            unused1: ['uint8', 16],
            flags: 'uint32',
            ctime: ['uint8', 8],
            mtime: ['uint8', 8],
            stream_start: 'int32',
            stream_size: 'uint32',
            unused2: 'uint32'
        };
        CFB.DirEntry = DirEntry;
    })(CFB = MailReader.CFB || (MailReader.CFB = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=DirEntry.js.map