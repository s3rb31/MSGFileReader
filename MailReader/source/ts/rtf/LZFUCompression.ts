/*
 * Based on java-libpst by Richard Johnson & Orin Eman (C) 2010
 * Translation to ECMAScript done by GFA Syscom (C) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
namespace MailReader 
{
    export namespace RTF
    {
        export class LZFUCompression
        {
            private data: Uint8Array;
            private prebuf: number[] = [];

            private magic: number = 0;
            private raw_size: number = 0;
            private comp_size: number = 0;

            private static PREBUF: number[] = (
                "{\\rtf1\\ansi\\mac\\deff0\\deftab720{\\fonttbl;}" +
                "{\\f0\\fnil \\froman \\fswiss \\fmodern \\fscript " +
                "\\fdecor MS Sans SerifSymbolArialTimes New RomanCourier" +
                "{\\colortbl\\red0\\green0\\blue0\n\r\\par " +
                "\\pard\\plain\\f0\\fs20\\b\\i\\u\\tab\\tx"
            ).split("").map((c: string) => c.charCodeAt(0));

            constructor(data: number[])
            {
                this.data = new Uint8Array(data);
                let view = new DataView(this.data.buffer);

                this.comp_size = view.getUint32(0, true);
                this.raw_size = view.getUint32(4, true);
                this.magic = view.getUint32(8, true);

                if (!this.validate(data.length))
                {
                    throw new Error("[RTF::LZFUCompression] " +
                        "Header is invalid or stream is truncated.")
                }
            }

            private validate(orig_size: number)
            {
                if (this.raw_size == 0)
                    return false;

                if (this.comp_size != orig_size - 4)
                    return false;

                return true;
            }

            public inflate()
            {
                let output: number[] = [];
                let datapos = 16, outpos = 0;

                if (this.magic == 0x414c454d)
                {
                    for (let c = 0; c < this.raw_size; c++) {
                        output[c] = this.data[datapos + c];
                    }
                }
                else if (this.magic == 0x75465a4c)
                {
                    let bufpos = LZFUCompression.PREBUF.length;

                    for (let i = 0; i < bufpos; i++) {
                        this.prebuf[i] = LZFUCompression.PREBUF[i];
                    }

                    while (datapos < this.data.length - 2 && outpos < this.raw_size) 
                    {
                        let flags = this.data[datapos++] & 0xFF;

                        for (let x = 0; x < 8 && outpos < this.raw_size; x++)
                        {
                            let isRef = ((flags & 1) == 1);
                            flags >>= 1;

                            if (isRef) 
                            {
                                let offset: number = this.data[datapos++] & 0xFF,
                                    length: number = this.data[datapos++] & 0xFF;

                                offset = (offset << 4) | (length >> 4);
                                length = (length & 0xF) + 2;

                                for (let y = 0; y < length && outpos < this.raw_size; y++)
                                {
                                    output[outpos++] = this.prebuf[offset];
                                    this.prebuf[bufpos++] = this.prebuf[offset++];

                                    bufpos %= 4096;
                                    offset %= 4096;
                                }
                            }
                            else
                            {
                                this.prebuf[bufpos++] = this.data[datapos];
                                output[outpos++] = this.data[datapos++];

                                bufpos %= 4096;
                            }
                        }
                    }
                }

                return Util.decodeAsciiFast(output);
            }
        }
    }
}