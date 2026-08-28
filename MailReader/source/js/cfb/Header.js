var MailReader;
(function (MailReader) {
    var CFB;
    (function (CFB) {
        var SectorAllocationTable = (function () {
            function SectorAllocationTable(table) {
                this.table = table;
            }
            Object.defineProperty(SectorAllocationTable.prototype, "length", {
                get: function () {
                    return this.table.length;
                },
                enumerable: true,
                configurable: true
            });
            SectorAllocationTable.prototype.getChain = function (start) {
                var chain = [start];
                for (var i = this.table[start]; i > -2; i = this.table[i]) {
                    if (i >= this.table.length) {
                        throw new Error("[CFB::SectorAllocationTable::getChain] " +
                            "Invalid SAT chain detected! File is probably corrupt.");
                    }
                    chain.push(i);
                }
                return chain;
            };
            return SectorAllocationTable;
        }());
        CFB.SectorAllocationTable = SectorAllocationTable;
        var Header = (function () {
            function Header(data) {
                var int32a = new Int32Array(data);
                var header = MailReader.Util.Struct.parse(data, Header.type_set);
                this.bom = header.bom;
                this.sig = header.sig;
                this.msat_num = header.msat_num;
                this.msat_start = header.msat_start;
                this.ssat_start = header.ssat_start;
                this.msat_table = header.msat_table;
                this.dir_start = header.dir_start;
                this.stream_min = header.stream_min;
                this.short_sz = Math.pow(2, header.short_sz);
                this.sector_sz = Math.pow(2, header.sector_sz);
                if (this.validate() == false) {
                    throw new Error("[CFB::Header::constructor] Invalid " +
                        "magic signature or BOM detected! Probably not a CFB file!");
                }
                this.SAT = this.parseSAT(int32a);
                this.shortSAT = this.parseShortSAT(int32a);
            }
            Header.prototype.validate = function () {
                var magic_signature = [0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1];
                for (var i = 0; i < this.sig.length; i++) {
                    if (this.sig[i] != magic_signature[i]) {
                        return false;
                    }
                }
                var byte_order_mark = 0xfffe;
                if (this.bom != byte_order_mark) {
                    return false;
                }
                return true;
            };
            Header.prototype.parseSAT = function (data) {
                if (this.msat_start != -2 && this.msat_num) {
                    var sec_id = this.msat_start;
                    for (; sec_id && sec_id > 0; sec_id = this.msat_table.pop()) {
                        var offset = (this.sector_sz * (sec_id + 1)) / 4;
                        var chunk = data.subarray(offset, (offset + this.sector_sz / 4));
                        this.msat_table = this.msat_table.
                            concat(Array.prototype.slice.call(chunk));
                    }
                }
                var sat = [];
                for (var i = 0; this.msat_table[i] > -1; i++) {
                    var offset = (this.sector_sz * (this.msat_table[i] + 1)) / 4;
                    var chunk = data.subarray(offset, (offset + this.sector_sz / 4));
                    sat = sat.concat(Array.prototype.slice.call(chunk));
                }
                return new SectorAllocationTable(sat);
            };
            Header.prototype.parseShortSAT = function (data) {
                var ssat = [];
                var ssat_chain = this.SAT.getChain(this.ssat_start);
                for (var i = 0; i < ssat_chain.length; i++) {
                    var offset = (this.sector_sz * (ssat_chain[i] + 1)) / 4;
                    var chunk = data.subarray(offset, (offset + this.sector_sz / 4));
                    ssat = ssat.concat(Array.prototype.slice.call(chunk));
                }
                return new SectorAllocationTable(ssat);
            };
            return Header;
        }());
        Header.type_set = {
            sig: ['uint8', 8],
            clsid: ['uint8', 16],
            revision: 'uint16',
            version: 'uint16',
            bom: 'uint16',
            sector_sz: 'uint16',
            short_sz: 'uint16',
            unused1: ['uint8', 10],
            sat_num: 'uint32',
            dir_start: 'int32',
            unused2: 'uint32',
            stream_min: 'uint32',
            ssat_start: 'int32',
            ssat_num: 'uint32',
            msat_start: 'int32',
            msat_num: 'uint32',
            msat_table: ['int32', 109]
        };
        CFB.Header = Header;
    })(CFB = MailReader.CFB || (MailReader.CFB = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=Header.js.map