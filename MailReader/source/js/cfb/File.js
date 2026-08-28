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
    var CFB;
    (function (CFB) {
        var File = (function (_super) {
            __extends(File, _super);
            function File(data) {
                var _this = _super.call(this, "Root Storage") || this;
                _this.data = new Uint8Array(data);
                _this.header = new CFB.Header(data);
                _this.dir_chain = _this.header.SAT.
                    getChain(_this.header.dir_start);
                var root_dir = _this.parseDirEntry(0);
                _this.ssec_chain = _this.header.SAT.
                    getChain(root_dir.stream_start);
                _this.parseDirectoryTree(root_dir);
                return _this;
            }
            File.prototype.processDirEntry = function (dir) {
                var storage = this.getStorage.apply(this, dir.parents);
                if (storage != undefined) {
                    if (dir.type == 1) {
                        storage.addStorage(new CFB.Storage(dir.name));
                    }
                    else if (dir.type == 2) {
                        var readF = this.readStreamFactory(dir);
                        storage.addStream(new CFB.Stream(dir.name, readF));
                    }
                }
            };
            File.prototype.parseDirEntry = function (index, parents) {
                var offset = this.calcIndirectOffset(index, 128, this.dir_chain);
                return new CFB.DirEntry(this.data.buffer, offset, parents);
            };
            File.prototype.parseDirectoryTree = function (root) {
                var stack = [];
                var dir = root;
                for (; dir != undefined; dir = stack.pop()) {
                    if (dir.type != 5) {
                        this.processDirEntry(dir);
                    }
                    for (var _i = 0, _a = ["child", "left", "right"]; _i < _a.length; _i++) {
                        var k = _a[_i];
                        if (dir[k] != undefined) {
                            var count = dir.parents.length;
                            var parents = Array(count);
                            while (count--) {
                                parents[count] = dir.parents[count];
                            }
                            if (k == "child" && dir.type != 5) {
                                parents.push(dir.name);
                            }
                            stack.push(this.parseDirEntry(dir[k], parents));
                        }
                    }
                }
            };
            File.prototype.readSector = function (id) {
                var offset = 512 + this.header.sector_sz * id;
                return this.data.subarray(offset, this.header.sector_sz + offset);
            };
            File.prototype.readShortSector = function (id) {
                var offset = this.calcIndirectOffset(id, this.header.short_sz, this.ssec_chain);
                return this.data.subarray(offset, this.header.short_sz + offset);
            };
            File.prototype.readStreamFactory = function (entry) {
                var _this = this;
                var result = [];
                var short = entry.stream_size < this.header.stream_min;
                var SAT = short ? this.header.shortSAT : this.header.SAT;
                var sectorSize = short ? this.header.short_sz : this.header.sector_sz;
                var readSectorF = short ? this.readShortSector : this.readSector;
                var chain = SAT.getChain(entry.stream_start);
                var data = new Uint8Array(sectorSize * chain.length);
                return function (_size) {
                    if (_size === void 0) { _size = entry.stream_size; }
                    if (result.length != _size) {
                        for (var i = 0; i < chain.length; i++) {
                            data.set(readSectorF.call(_this, chain[i]), sectorSize * i);
                        }
                        result = Array.prototype.slice.call(data, 0, _size);
                    }
                    return result;
                };
            };
            File.prototype.calcIndirectOffset = function (id, size, chain) {
                var num_in_sec = this.header.sector_sz / size;
                var chain_offset = Math.floor(id / num_in_sec);
                if (chain_offset > chain.length) {
                    throw new Error("[CFB::File::calcIndirectOffset] " +
                        "Invalid SAT chain detected. File is probably corrupt.");
                }
                return 512 + (chain[chain_offset] * this.header.sector_sz) + ((id % num_in_sec) * size);
            };
            return File;
        }(CFB.Storage));
        CFB.File = File;
    })(CFB = MailReader.CFB || (MailReader.CFB = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=File.js.map