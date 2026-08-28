namespace MailReader 
{
    export namespace Util
    {
        export class Struct
        {
            private static offset: number = 0;

            private static readValue(view: DataView, type: string, lE: boolean)
            {
                switch (type)
                {
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
            }

            private static readArray(view: DataView, type: string, length: number, lE: boolean)
            {
                let array = [];

                for (let i = 0; i < length; i++)
                {
                    let value = this.readValue(view, type, lE);

                    if (value == undefined)
                    {
                        return undefined;
                    }

                    array.push(value);
                }

                return array;
            }

            public static parse(data: ArrayBuffer, type_set: { [key: string]: string | any[] }, offset = 0): any
            {
                Struct.offset = 0;

                let dv = new DataView(data, offset);
                let struct: { [key: string]: number | number[] | undefined } = {};

                for (let key in type_set)
                {
                    if (type_set.hasOwnProperty(key))
                    {
                        let type = type_set[key];

                        if (typeof type == "string")
                        {
                            struct[key] = this.readValue(dv, type, true);
                        }
                        else
                        {
                            struct[key] = this.readArray(dv, type[0], type[1], true);
                        }
                    }
                }

                return struct;
            }
        }

        export class CustomMap<K, V> implements Map<K, V>
        {
            private keys: K[] = [];
            private values: V[] = [];
            private index: number = 0;

            get size()
            {
                return this.keys.length;
            }

            public get(key: K): V | undefined
            {
                return this.has(key) ?
                    this.values[this.index] : undefined;
            }

            public clear(): void
            {
                this.keys.length = 0;
                this.values.length = 0;
            }

            public set(key: K, value: V): this
            {
                this.has(key) ?
                    this.values[this.index] = value :
                    this.values[this.keys.push(key) - 1] = value;

                return this;
            }

            public delete(key: K): boolean
            {
                if (this.has(key))
                {
                    this.keys.splice(this.index, 1);
                    this.values.splice(this.index, 1);
                }

                return this.index > -1;
            }

            public has(key: K): key is K
            {
                let is = (a: any, b: any) => (a === b) || (a !== a && b !== b); 

                if (key == key && <any>key !== 0) {
                    this.index = this.keys.indexOf(key);
                } else {
                    for (this.index = this.keys.length;
                         this.index-- && is(key, this.keys[this.index]);) { }
                }

                return this.index > -1;
            }
        }

        export function formatWindowsTime(time: number) 
        {
            let unix_time = (time / 10000000) - 11644473600;
            let date = new Date(unix_time * 1000)

            return date.toLocaleString(navigator.language, {
                weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
                hour: '2-digit', minute: '2-digit', second: '2-digit', timeZoneName: 'short'
            });
        }

        export function decodeAsciiFast(data: number[], length: number = data.length)
        {
            var string = "";
            var slice_size = 512;

            for (let offset = 0; offset < length; offset += slice_size)
            {
                string += String.fromCharCode.apply(null,
                    data.slice(offset, offset + slice_size));
            }

            return string;
        }

        export function decodeString(cp: number, data: number | number[], length?: number)
        {
            if (typeof data != "number") {
                data = length ? data.slice(0, length) : data;
            }

            return (typeof data != "number") ?
                cptable.utils.decode(cp, data) : cptable[cp].dec[data];
        }

        //export function parseUTF16(data: number[], length: number = data.length)
        //{
        //    let string = "";

        //    for (let i = 0; i < length; i += 2)
        //    {
        //        let w1 = (data[i + 1] << 8) + data[i];

        //        // Is this a single code unit char?

        //        if (data[i + 1] < 0xD8 || data[i + 1] >= 0xE0)
        //        {
        //            string += String.fromCharCode(w1);
        //            continue;
        //        }

        //        // It's a double code unit (4 byte) char ..

        //        let w2 = (data[i + 3] << 8) + data[i + 2]; 

        //        // VanillaJS will combine two values to one
        //        // char if they form a valid UTF16 codepoint

        //        string += String.fromCharCode(w1, w2);

        //        i += 2;
        //    }

        //    return string;
        //}

        //function parseUTF8(data: number[], length: number = data.length)
        //{
        //    let string = "";

        //    for (let i = 0; i < length; i++)
        //    {
        //        let w1 = data[i];
        //        let hi_nib = (w1 >> 4);

        //        // http://scripts.sil.org/iws-appendixa
        //        // We save the right-shift for each if,
        //        // by basing all calculations on hi-nibble

        //        if (hi_nib <= 7) // w1 >> 7 == 0
        //        {
        //            string += String.fromCharCode(w1);
        //        }
        //        else if (hi_nib == 12 || hi_nib == 13) // w1 >> 5 == 6
        //        {
        //            let C = (   w1 & 0x1F) << 6 |
        //                (data[++i] & 0x3F);

        //            string += String.fromCharCode(C);
        //        }
        //        else if (hi_nib == 14) // w1 >> 4 == 14
        //        {
        //            let C = (   w1 & 0x0F) << 12 |
        //                (data[++i] & 0x3F) << 6  |
        //                (data[++i] & 0x3F);

        //            string += String.fromCharCode(C);
        //        }
        //        else if (hi_nib == 15) // w1 >> 4 == 15
        //        {
        //            let C = (   w1 & 0x7 ) << 18 |
        //                (data[++i] & 0x3F) << 12 |
        //                (data[++i] & 0x3F) << 6  |
        //                (data[++i] & 0x3F);

        //            // make UTF16 H/L surrogate pair 
        //            // (String.fromCodePoint missing from IE)

        //            C -= 0x10000;

        //            let H = (C >> 0x400) + 0xD800;
        //            let L = (C  % 0x400) + 0xDC00;

        //            string += String.fromCharCode(H, L);
        //        }
        //    }

        //    return string;
        //}
    }
}

if (typeof Map == "undefined") // FIX IE
{
    self.Map = MailReader.Util.CustomMap;
}