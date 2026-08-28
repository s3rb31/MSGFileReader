namespace MailReader 
{
    export namespace MSG
    {
        // Note: since we can't subclass the 
        // native Map<K,V> composition is faster
        // (instead of subclassing Util.CustomMap)

        export class PropertyNameMap
        {
            private static type_set =
            {
                nameid: 'uint32',
                guididx: 'uint16',
                propidx: 'uint16'
            };

            private map: Map<number | string, number>;

            private static STORE_NAME = "__nameid_version1.0";
            private static ENTRY_STREAM = "__substg1.0_00030102";
            private static STRING_STREAM = "__substg1.0_00040102";

            constructor(root: CFB.Storage)
            {
                this.map = new Map<number | string, number>();
                const storage = root.getStorage(PropertyNameMap.STORE_NAME);

                if (storage != undefined)
                {
                    const entries = storage.getStream(PropertyNameMap.ENTRY_STREAM);
                    const strings = storage.getStream(PropertyNameMap.STRING_STREAM);

                    if (entries != undefined && strings != undefined)
                    {
                        this.parseEntries(
                            new Uint8Array(entries.read()).buffer,
                            new Uint8Array(strings.read()).buffer);
                    }
                }
            }

            public get(key: number | string)
            {
                return this.map.get(key);
            }

            private parseName(propidx: number, strings: ArrayBuffer)
            {
                const view = new DataView(strings, propidx);
                const length = view.getUint32(0, true);

                if (length > 0)
                {
                    const data: number[] = [];

                    for (let j = 0; j < length; j++)
                    {
                        data.push(view.getUint8(4 + j));
                    }

                    if (data.length > 0)
                    {
                        return Util.decodeString(1200, data);
                    }
                }

                return undefined;
            }

            private parseEntries(entries: ArrayBuffer, strings: ArrayBuffer)
            {
                if (entries.byteLength > 0)
                {
                    const entry_num =
                        entries.byteLength / 8;

                    for (let i = 0; i < entry_num; i++)
                    {
                        const entry = Util.Struct.parse(
                            entries, PropertyNameMap.type_set, i * 8)

                        if (entry.propidx == i)
                        {
                            let name_id = entry.nameid;

                            if ((entry.guididx & 1) == 1)
                            {
                                const name = this.parseName(name_id, strings);

                                if (name != undefined)
                                {
                                    name_id = name;
                                }
                            }

                            this.map.set(name_id, 0x8000 + entry.propidx)
                        }
                    }
                }
            }
        }
    }
}
