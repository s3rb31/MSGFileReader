var MailReader;
(function (MailReader) {
    var RTF;
    (function (RTF) {
        var LZFUCompression = (function () {
            function LZFUCompression(data) {
                this.prebuf = [];
                this.magic = 0;
                this.raw_size = 0;
                this.comp_size = 0;
                this.data = new Uint8Array(data);
                var view = new DataView(this.data.buffer);
                this.comp_size = view.getUint32(0, true);
                this.raw_size = view.getUint32(4, true);
                this.magic = view.getUint32(8, true);
                if (!this.validate(data.length)) {
                    throw new Error("[RTF::LZFUCompression] " +
                        "Header is invalid or stream is truncated.");
                }
            }
            LZFUCompression.prototype.validate = function (orig_size) {
                if (this.raw_size == 0)
                    return false;
                if (this.comp_size != orig_size - 4)
                    return false;
                return true;
            };
            LZFUCompression.prototype.inflate = function () {
                var output = [];
                var datapos = 16, outpos = 0;
                if (this.magic == 0x414c454d) {
                    for (var c = 0; c < this.raw_size; c++) {
                        output[c] = this.data[datapos + c];
                    }
                }
                else if (this.magic == 0x75465a4c) {
                    var bufpos = LZFUCompression.PREBUF.length;
                    for (var i = 0; i < bufpos; i++) {
                        this.prebuf[i] = LZFUCompression.PREBUF[i];
                    }
                    while (datapos < this.data.length - 2 && outpos < this.raw_size) {
                        var flags = this.data[datapos++] & 0xFF;
                        for (var x = 0; x < 8 && outpos < this.raw_size; x++) {
                            var isRef = ((flags & 1) == 1);
                            flags >>= 1;
                            if (isRef) {
                                var offset = this.data[datapos++] & 0xFF, length_1 = this.data[datapos++] & 0xFF;
                                offset = (offset << 4) | (length_1 >> 4);
                                length_1 = (length_1 & 0xF) + 2;
                                for (var y = 0; y < length_1 && outpos < this.raw_size; y++) {
                                    output[outpos++] = this.prebuf[offset];
                                    this.prebuf[bufpos++] = this.prebuf[offset++];
                                    bufpos %= 4096;
                                    offset %= 4096;
                                }
                            }
                            else {
                                this.prebuf[bufpos++] = this.data[datapos];
                                output[outpos++] = this.data[datapos++];
                                bufpos %= 4096;
                            }
                        }
                    }
                }
                return MailReader.Util.decodeAsciiFast(output);
            };
            return LZFUCompression;
        }());
        LZFUCompression.PREBUF = ("{\\rtf1\\ansi\\mac\\deff0\\deftab720{\\fonttbl;}" +
            "{\\f0\\fnil \\froman \\fswiss \\fmodern \\fscript " +
            "\\fdecor MS Sans SerifSymbolArialTimes New RomanCourier" +
            "{\\colortbl\\red0\\green0\\blue0\n\r\\par " +
            "\\pard\\plain\\f0\\fs20\\b\\i\\u\\tab\\tx").split("").map(function (c) { return c.charCodeAt(0); });
        RTF.LZFUCompression = LZFUCompression;
    })(RTF = MailReader.RTF || (MailReader.RTF = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=LZFUCompression.js.map