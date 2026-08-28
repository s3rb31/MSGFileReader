namespace MailReader 
{
    export namespace RTF
    {
        enum TrType
        {
            Byte, Unicode, Fragment
        }

        interface Keyword
        {
            name: string;
            param?: number;
        }

        interface TrValue
        {
            type: TrType;
            value: number | string;
        }

        interface Font
        {
            fcharset: number;
            cpg: number;

            [key: string]: number;
        }

        interface DestState
        {
            ucn: number;
            fonttbl: boolean;
            htmlrtf: boolean;
            htmltag: boolean;

            prev?: DestState
        }

        export class HTMLCapsulation
        {
            private data: string;

            private output: string;
            private position: number;

            private ansicpg: number;
            private activecpg: number;

            private skipchars: number;
            private skipdest: number;

            private dstate: DestState;

            private fonttbl: Font[];
            private textrun: TrValue[];

            private static htmlf_map:
                { [key: string]: string; } =
            {
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

            private static fcs_map:
                { [key: number]: number; } =
            {
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

            constructor(data: string)
            {
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
                }

                this.fonttbl = [];
                this.textrun = [];
            }

            public parse() // load
            {
                this.parseFile(); 

                return this.output;
            }

            private pushDestState()
            {
                // Save current dstate

                let new_state = {
                    prev: this.dstate.prev,
                    ucn: this.dstate.ucn,
                    fonttbl: this.dstate.fonttbl,
                    htmlrtf: this.dstate.htmlrtf,
                    htmltag: this.dstate.htmltag
                }

                this.dstate.prev = new_state;
            }

            private popDestState()
            {
                // Restore previous dstate

                if (this.dstate.prev)
                {
                    this.dstate = this.dstate.prev;
                }
            }

            private eof()
            {
                // Has the end of file been reached?

                return this.position >= this.data.length
            }

            private readChar()
            {
                //return String.fromCharCode(
                //    this.data.charCodeAt(this.position++));

                return this.data[this.position++];
            }
        
            private isDigit(chr: string)
            {
                // Returns true if char is one of [0-9]

                let v = chr.charCodeAt(0);

                return (v >= 48 && v <= 57); 
            }

            private isAlpha(chr: string)
            {
                // Returns true if char is one of [a-zA-Z]

                let v = chr.charCodeAt(0);

                return (v >= 97 && v <= 122) || (v >= 65 && v <= 90);
            }

            private appendToTR(value: number | string, type: TrType)
            {
                var value = value;

                if (type == TrType.Fragment)
                {
                    value = HTMLCapsulation.htmlf_map[value];
                }

                if (value != undefined)
                {
                    this.textrun.push({value: value, type: type});
                }
            }

            private decodeUTF16(u1: number, u2?: number)
            {
                if (u2 != undefined &&
                    u1 >= 0xD800 && u1 <= 0xDBFF)
                {
                    if (u2 >= 0xDC00 && u2 <= 0xDFFF) {
                        return String.fromCharCode(u1, u2);
                    }

                    return ""; // ignore invalid codepoint
                }

                return String.fromCharCode(u1);
            }

            private decodeTextRun()
            {
                let bytes = [];
                let output = "";

                for (let i = 0; i < this.textrun.length; i++)
                {
                    let entry = this.textrun[i];

                    if (entry.type != TrType.Byte)
                    {
                        output += cptable.utils.decode(this.activecpg, bytes);

                        if (entry.type == TrType.Fragment)
                        {
                            output += <string>entry.value;
                        }
                        else if (entry.type == TrType.Unicode)
                        {
                            let u2 = undefined;
                            let u1 = <number>entry.value;

                            if (this.textrun[i + 1] != null && 
                                this.textrun[i + 1].type == TrType.Unicode)
                            {
                                u2 = <number>this.textrun[++i].value;
                            }

                            output += this.decodeUTF16(u1, u2);
                        }

                        bytes = [];
                        continue;
                    }

                    bytes.push(<number>entry.value);
                }

                if (bytes.length > 0) {
                    output += cptable.utils.decode(this.activecpg, bytes);
                }

                this.textrun = [];

                return output;
            }

            private parseNextKeyword(): Keyword
            {
                let char = this.readChar();

                if (this.isAlpha(char))
                {
                    // Parse the keyword

                    let keyword = "";

                    while (this.isAlpha(char))
                    {
                        keyword += char;
                        char = this.readChar();
                    }

                    // Check keyword length

                    if (keyword.length > 32)
                    {
                        throw new Error("[RTF::HTMLCapsulation] " +
                            "Keyword '" + keyword + "' too long.")
                    }

                    // Parse the existing (?) argument 
                    // (number; may be negative)

                    let number = "";

                    if (char == "-")
                    {
                        number += "-";
                        char = this.readChar();
                    }

                    let param: number | undefined;

                    // Do we really have a number?

                    if (this.isDigit(char))
                    {
                        while (this.isDigit(char))
                        {
                            number += char;
                            char = this.readChar();
                        }

                        // Check parameter length

                        if (number.length > 20)
                        {
                            throw new Error("[RTF::HTMLCapsulation] " +
                                "Param for keyword '" + keyword + "' too long.")
                        }

                        // Convert text to real number

                        let num = parseInt(number, 10);
                        param = isNaN(num) ? undefined : num;
                    }

                    // We've intentionally read one byte too much. 
                    // Decrement current position, if the current
                    // char is something else than a whitespace.

                    if (this.position > 0 && char != " ") {
                        this.position--;
                    }

                    return { name: keyword, param: param };
                }

                // According to the doc (MSFT-RTF), any non alpha-
                // numeric token is exactly one char in length

                return { name: char };
            }

            private processKeyword(kw: Keyword)
            {
                let param = kw.param;
                let keyword = kw.name;

                switch (keyword)
                {
                    default:
                    {
                        // Parse CONTENT HTML fragments

                        if (this.dstate.htmltag == true)
                        {
                            if (this.skipchars == 0)
                            {
                                this.appendToTR(keyword, TrType.Fragment);
                            }
                            else
                            {
                                this.skipchars--;
                            }
                        }

                        return;
                    }

                    case "colortbl": // colortbl encountered
                    {
                        this.skipdest++; // skip
                        return;
                    }

                    case "fonttbl": // Toggle FONTTBL destination
                    {
                        this.dstate.fonttbl = true;
                        return;
                    }

                    case "htmlrtf": // Toggle HTMLRTF destination 
                    {
                        this.dstate.htmlrtf = !(param == 0);
                        return;
                    }

                    case "ansicpg": // ansi codepage 
                    {
                        if (param != undefined && param >= 0) {
                            this.ansicpg = param;
                        }

                        return;
                    }

                    case "uc": // Skip N characters after next \u
                    {
                        if (param != undefined && param >= 0) {
                            this.dstate.ucn = param;
                        }

                        return;
                    }

                    case "cpg":
                    case "fcharset":
                    {
                        if (this.dstate.fonttbl == true && param != null)
                        {
                            let idx = this.fonttbl.length - 1;
                            this.fonttbl[idx][keyword] = param;
                        }
                        
                        return;
                    }

                    case "f": // Update current font / fcharset 
                    {
                        if (this.dstate.fonttbl == true)
                        {
                            this.fonttbl.push({ cpg: -1, fcharset: -1 });
                        }
                        else if (param != undefined)
                        {
                            this.activecpg = this.ansicpg;

                            let font = this.fonttbl[param];
                            if (font != undefined)
                            {
                                if (font.cpg != -1)
                                {
                                    this.activecpg = font.cpg;
                                }
                                else if (font.fcharset != -1)
                                {
                                    this.activecpg = HTMLCapsulation.fcs_map[font.fcharset];
                                }
                            }
                        }

                        return;
                    }
                    
                    case "*": // Ignorable destination encountered
                    {
                        this.position++; // skip backslash

                        if (this.parseNextKeyword().name == "htmltag")
                        {
                            this.dstate.htmltag = true; 
                        }
                        else
                        {
                            this.skipdest++; // skip dest if != htmltag
                        }

                        return;
                    }

                    case "'": // Parse the hex symbol
                    {
                        if (this.skipchars == 0)
                        {
                            param = parseInt(this.readChar() + this.readChar(), 16)

                            if (isNaN(param) == false) {
                                this.appendToTR(param, TrType.Byte);
                            }
                        }
                        else
                        {
                            this.skipchars--;
                        }

                        return;
                    }

                    case "u": // Parse the unicode symbol
                    {
                        if (param == undefined) {
                            return;
                        }

                        if (this.skipchars == 0)
                        {
                            param += (param < 0) ? 65536 : 0;

                            if (param >= 0 && param < 65536)
                            {
                                // Check if we have a symbol character
                                // TODO fix symbol font fuck-up

                                //let sym = symbolTable[param.toString(16).substr(2)];

                                //if (sym == undefined) {
                                    //sym = String.fromCharCode(param);
                                    //sym = "&#" + param.toString() + ";"
                                //}

                                //this.appendOutput("&#" + param.toString() + ";");

                                this.skipchars = this.dstate.ucn;
                                this.appendToTR(param, TrType.Unicode);
                            }
                        }
                        else
                        {
                            this.skipchars--;
                        }
                        
                        return;
                    }
                }
            }

            private parseFile()
            {
                // Iterate through the whole file
                // character by character ...
            
                while (!this.eof())
                {
                    let c = this.readChar();

                    // Check if we need to ignore the
                    // symbol (if it's not a group mark)

                    if (c != "{" && c != "}")
                    {
                        // Check if we need to ignore the 
                        // current destination

                        if (this.skipdest > 0) {
                            continue;
                        }

                        // Check if the \htmlrtf control
                        // word currently is enabled

                        if (this.dstate.htmlrtf && c != "\\") {
                            continue;
                        }
                    }

                    // Handle RTF characters

                    switch (c)
                    {
                        // Ignore linebreaks from the RTF document

                        case "\r":
                        case "\n":
                            break;

                        // A RTF keyword was encountered, process it

                        case "\\":
                        {
                            let kw = this.parseNextKeyword();

                            // Ignore any keywords except \f
                            // inside a HTMLRTF destination

                            if (this.dstate.htmlrtf)
                            {
                                if (kw.name != "f" &&
                                    kw.name != "htmlrtf")
                                {
                                    break;
                                }
                            }

                            // Decode current TR before
                            // changing the font and CP 

                            if (kw.name == "f" &&
                                this.dstate.fonttbl == false) 
                            {

                                this.output += this.decodeTextRun();
                            }

                            this.processKeyword(kw);
                            break;
                        }

                        // A RTF group starts here, save state

                        case "{":
                        {
                            this.skipdest += // increment skipdest
                                    (this.skipdest > 0) ? 1 : 0;

                            if (this.skipdest == 0)
                            {
                                this.skipchars = 0;
                                this.pushDestState();
                            }

                            break;
                        }

                        // A RTF group ends here, restore state

                        case "}":
                        {
                            this.skipdest -= // decrement skipdest
                                (this.skipdest > 0) ? 1 : 0;

                            if (this.skipdest == 0)
                            {
                                // capture text for ending group

                                this.output += this.decodeTextRun();

                                this.skipchars = 0;
                                this.popDestState();
                            }

                            break;
                        }

                        // Normal text found, append if neccessary

                        default:
                        {
                            if (this.skipchars == 0)
                            {
                                if (this.dstate.fonttbl == false) {
                                    this.appendToTR(c.charCodeAt(0), TrType.Byte);
                                }

                                break;
                            }

                            this.skipchars--;
                        }
                    }
                }
            }
        }
    }
}