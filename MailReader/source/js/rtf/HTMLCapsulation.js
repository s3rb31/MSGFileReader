var MailReader;
(function (MailReader) {
    var RTF;
    (function (RTF) {
        var TrType;
        (function (TrType) {
            TrType[TrType["Byte"] = 0] = "Byte";
            TrType[TrType["Unicode"] = 1] = "Unicode";
            TrType[TrType["Fragment"] = 2] = "Fragment";
        })(TrType || (TrType = {}));
        var HTMLCapsulation = (function () {
            function HTMLCapsulation(data) {
                this.data = data;
                this.output = "";
                this.position = 0;
                this.ansicpg = 0;
                this.activecpg = 0;
                this.skipchars = 0;
                this.skipdest = 0;
                this.dstate = {
                    ucn: 1,
                    fonttbl: false,
                    htmlrtf: false,
                    htmltag: false
                };
                this.fonttbl = [];
                this.textrun = [];
            }
            HTMLCapsulation.prototype.parse = function () {
                this.parseFile();
                return this.output;
            };
            HTMLCapsulation.prototype.pushDestState = function () {
                var new_state = {
                    prev: this.dstate.prev,
                    ucn: this.dstate.ucn,
                    fonttbl: this.dstate.fonttbl,
                    htmlrtf: this.dstate.htmlrtf,
                    htmltag: this.dstate.htmltag
                };
                this.dstate.prev = new_state;
            };
            HTMLCapsulation.prototype.popDestState = function () {
                if (this.dstate.prev) {
                    this.dstate = this.dstate.prev;
                }
            };
            HTMLCapsulation.prototype.eof = function () {
                return this.position >= this.data.length;
            };
            HTMLCapsulation.prototype.readChar = function () {
                return this.data[this.position++];
            };
            HTMLCapsulation.prototype.isDigit = function (chr) {
                var v = chr.charCodeAt(0);
                return (v >= 48 && v <= 57);
            };
            HTMLCapsulation.prototype.isAlpha = function (chr) {
                var v = chr.charCodeAt(0);
                return (v >= 97 && v <= 122) || (v >= 65 && v <= 90);
            };
            HTMLCapsulation.prototype.appendToTR = function (value, type) {
                var value = value;
                if (type == TrType.Fragment) {
                    value = HTMLCapsulation.htmlf_map[value];
                }
                if (value != undefined) {
                    this.textrun.push({ value: value, type: type });
                }
            };
            HTMLCapsulation.prototype.decodeUTF16 = function (u1, u2) {
                if (u2 != undefined &&
                    u1 >= 0xD800 && u1 <= 0xDBFF) {
                    if (u2 >= 0xDC00 && u2 <= 0xDFFF) {
                        return String.fromCharCode(u1, u2);
                    }
                    return "";
                }
                return String.fromCharCode(u1);
            };
            HTMLCapsulation.prototype.decodeTextRun = function () {
                var bytes = [];
                var output = "";
                for (var i = 0; i < this.textrun.length; i++) {
                    var entry = this.textrun[i];
                    if (entry.type != TrType.Byte) {
                        output += cptable.utils.decode(this.activecpg, bytes);
                        if (entry.type == TrType.Fragment) {
                            output += entry.value;
                        }
                        else if (entry.type == TrType.Unicode) {
                            var u2 = undefined;
                            var u1 = entry.value;
                            if (this.textrun[i + 1] != null &&
                                this.textrun[i + 1].type == TrType.Unicode) {
                                u2 = this.textrun[++i].value;
                            }
                            output += this.decodeUTF16(u1, u2);
                        }
                        bytes = [];
                        continue;
                    }
                    bytes.push(entry.value);
                }
                if (bytes.length > 0) {
                    output += cptable.utils.decode(this.activecpg, bytes);
                }
                this.textrun = [];
                return output;
            };
            HTMLCapsulation.prototype.parseNextKeyword = function () {
                var char = this.readChar();
                if (this.isAlpha(char)) {
                    var keyword = "";
                    while (this.isAlpha(char)) {
                        keyword += char;
                        char = this.readChar();
                    }
                    if (keyword.length > 32) {
                        throw new Error("[RTF::HTMLCapsulation] " +
                            "Keyword '" + keyword + "' too long.");
                    }
                    var number = "";
                    if (char == "-") {
                        number += "-";
                        char = this.readChar();
                    }
                    var param = void 0;
                    if (this.isDigit(char)) {
                        while (this.isDigit(char)) {
                            number += char;
                            char = this.readChar();
                        }
                        if (number.length > 20) {
                            throw new Error("[RTF::HTMLCapsulation] " +
                                "Param for keyword '" + keyword + "' too long.");
                        }
                        var num = parseInt(number, 10);
                        param = isNaN(num) ? undefined : num;
                    }
                    if (this.position > 0 && char != " ") {
                        this.position--;
                    }
                    return { name: keyword, param: param };
                }
                return { name: char };
            };
            HTMLCapsulation.prototype.processKeyword = function (kw) {
                var param = kw.param;
                var keyword = kw.name;
                switch (keyword) {
                    default:
                        {
                            if (this.dstate.htmltag == true) {
                                if (this.skipchars == 0) {
                                    this.appendToTR(keyword, TrType.Fragment);
                                }
                                else {
                                    this.skipchars--;
                                }
                            }
                            return;
                        }
                    case "colortbl":
                        {
                            this.skipdest++;
                            return;
                        }
                    case "fonttbl":
                        {
                            this.dstate.fonttbl = true;
                            return;
                        }
                    case "htmlrtf":
                        {
                            this.dstate.htmlrtf = !(param == 0);
                            return;
                        }
                    case "ansicpg":
                        {
                            if (param != undefined && param >= 0) {
                                this.ansicpg = param;
                            }
                            return;
                        }
                    case "uc":
                        {
                            if (param != undefined && param >= 0) {
                                this.dstate.ucn = param;
                            }
                            return;
                        }
                    case "cpg":
                    case "fcharset":
                        {
                            if (this.dstate.fonttbl == true && param != null) {
                                var idx = this.fonttbl.length - 1;
                                this.fonttbl[idx][keyword] = param;
                            }
                            return;
                        }
                    case "f":
                        {
                            if (this.dstate.fonttbl == true) {
                                this.fonttbl.push({ cpg: -1, fcharset: -1 });
                            }
                            else if (param != undefined) {
                                this.activecpg = this.ansicpg;
                                var font = this.fonttbl[param];
                                if (font != undefined) {
                                    if (font.cpg != -1) {
                                        this.activecpg = font.cpg;
                                    }
                                    else if (font.fcharset != -1) {
                                        this.activecpg = HTMLCapsulation.fcs_map[font.fcharset];
                                    }
                                }
                            }
                            return;
                        }
                    case "*":
                        {
                            this.position++;
                            if (this.parseNextKeyword().name == "htmltag") {
                                this.dstate.htmltag = true;
                            }
                            else {
                                this.skipdest++;
                            }
                            return;
                        }
                    case "'":
                        {
                            if (this.skipchars == 0) {
                                param = parseInt(this.readChar() + this.readChar(), 16);
                                if (isNaN(param) == false) {
                                    this.appendToTR(param, TrType.Byte);
                                }
                            }
                            else {
                                this.skipchars--;
                            }
                            return;
                        }
                    case "u":
                        {
                            if (param == undefined) {
                                return;
                            }
                            if (this.skipchars == 0) {
                                param += (param < 0) ? 65536 : 0;
                                if (param >= 0 && param < 65536) {
                                    this.skipchars = this.dstate.ucn;
                                    this.appendToTR(param, TrType.Unicode);
                                }
                            }
                            else {
                                this.skipchars--;
                            }
                            return;
                        }
                }
            };
            HTMLCapsulation.prototype.parseFile = function () {
                while (!this.eof()) {
                    var c = this.readChar();
                    if (c != "{" && c != "}") {
                        if (this.skipdest > 0) {
                            continue;
                        }
                        if (this.dstate.htmlrtf && c != "\\") {
                            continue;
                        }
                    }
                    switch (c) {
                        case "\r":
                        case "\n":
                            break;
                        case "\\":
                            {
                                var kw = this.parseNextKeyword();
                                if (this.dstate.htmlrtf) {
                                    if (kw.name != "f" &&
                                        kw.name != "htmlrtf") {
                                        break;
                                    }
                                }
                                if (kw.name == "f" &&
                                    this.dstate.fonttbl == false) {
                                    this.output += this.decodeTextRun();
                                }
                                this.processKeyword(kw);
                                break;
                            }
                        case "{":
                            {
                                this.skipdest +=
                                    (this.skipdest > 0) ? 1 : 0;
                                if (this.skipdest == 0) {
                                    this.skipchars = 0;
                                    this.pushDestState();
                                }
                                break;
                            }
                        case "}":
                            {
                                this.skipdest -=
                                    (this.skipdest > 0) ? 1 : 0;
                                if (this.skipdest == 0) {
                                    this.output += this.decodeTextRun();
                                    this.skipchars = 0;
                                    this.popDestState();
                                }
                                break;
                            }
                        default:
                            {
                                if (this.skipchars == 0) {
                                    if (this.dstate.fonttbl == false) {
                                        this.appendToTR(c.charCodeAt(0), TrType.Byte);
                                    }
                                    break;
                                }
                                this.skipchars--;
                            }
                    }
                }
            };
            return HTMLCapsulation;
        }());
        HTMLCapsulation.htmlf_map = {
            "par": "\x0d\x0a",
            "tab": "\x09",
            "{": "\x7b",
            "}": "\x7d",
            "\\": "\x5c",
            "lquote": "&lsquo;",
            "rquote": "&rsquo;",
            "ldblquote": "&ldquo;",
            "rdblquote": "&rdquo;",
            "bullet": "&bull;",
            "endash": "&ndash;",
            "emdash": "&mdash;",
            "~": "&nbsp;",
            "_": "&shy;"
        };
        HTMLCapsulation.fcs_map = {
            0: 1252,
            1: 1252,
            2: 1252,
            77: 10000,
            78: 10001,
            79: 10003,
            80: 10008,
            81: 10002,
            83: 10005,
            84: 10004,
            85: 10006,
            86: 10081,
            87: 10021,
            88: 10029,
            89: 10007,
            128: 932,
            129: 949,
            130: 1361,
            134: 936,
            136: 950,
            161: 1253,
            162: 1254,
            163: 1258,
            177: 1255,
            178: 1256,
            186: 1257,
            204: 1251,
            222: 874,
            238: 1250,
            254: 437,
            255: 850
        };
        RTF.HTMLCapsulation = HTMLCapsulation;
    })(RTF = MailReader.RTF || (MailReader.RTF = {}));
})(MailReader || (MailReader = {}));
//# sourceMappingURL=HTMLCapsulation.js.map