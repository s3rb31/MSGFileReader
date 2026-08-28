namespace MailReader 
{
    export namespace MSG
    {
        abstract class PropertyStream
        {
            protected _codepage: number;
            protected _data: ArrayBuffer;

            private name_map: PropertyNameMap;
            private prop_map: Map<number, Property>;

            private cache: Map<number, number[]>;

            private static NAME = "__properties_version1.0";

            constructor(storage: CFB.Storage, name_map: PropertyNameMap)
            {
                let stream = storage.getStream(PropertyStream.NAME);

                if (stream == undefined)
                {
                    throw new Error("[MSG::PropertyStream::constructor] " +
                        "Property stream is missing in " + storage.name);
                }

                this.name_map = name_map;
                this.prop_map = new Map<number, Property>();

                this.cache = new Map<number, number[]>();
                this._data = new Uint8Array(stream.read()).buffer;
            }

            public readNamed<T>(key: number | string, type?: Property.Type)
            {
                const tag = this.name_map.get(key);

                if (tag != undefined)
                {
                    return this.read<T>(tag, type);
                }
            }

            public readCached(tag: Property.Tag, type?: Property.Type): number[] | undefined
            {
                const id = (tag << 16) +
                    (type || Property.Type.PT_UNSPECIFIED);

                if (this.cache.has(id) == false)
                {
                    const prop = this.prop_map.get(id);

                    if (prop != undefined)
                    {
                        const val = prop.read<number[]>();
                        this.cache.set(id, val);
                    }
                }

                return this.cache.get(id);
            }

            public read<T>(tag: Property.Tag, type?: Property.Type)
            {
                const prop = this.prop_map.get((tag << 16) +
                    (type || Property.Type.PT_UNSPECIFIED));

                if (prop != undefined)
                {
                    if (prop.type == Property.Type.PT_STRING8 ||
                        prop.type == Property.Type.PT_UNICODE)
                    {
                        const data = prop.read<number[]>();

                        if (data != undefined)
                        {
                            let _cp = this._codepage;

                            if (prop.type == Property.Type.PT_UNICODE)
                            {
                                _cp = 1200;
                            }

                            return <T><any>Util.decodeString(_cp, data);
                        }
                    }
                    else
                    {
                        return prop.read<T>();
                    }
                }

                return undefined;
            }

            protected parseEntries(storage: CFB.Storage, header_size: number)
            {
                let count = (this._data.byteLength - header_size) / 16;

                for (let i: number = 0; i < count; i++)
                {
                    let prop = new Property(this._data, storage, i * 16);

                    this.prop_map.set((prop.tag << 16) + prop.type, prop);
                    this.prop_map.set((prop.tag << 16) + Property.Type.PT_UNSPECIFIED, prop);
                }
            }
        }

        export class ObjPropertyStream extends PropertyStream
        {
            // There is nothing in the header of this property stream

            constructor(storage: CFB.Storage, nm: PropertyNameMap, cp: number)
            {
                super(storage, nm);
                this._codepage = cp;

                // Skip first 8 reserved bytes 

                this._data = this._data.slice(8);

                // Parse the entries in the stream

                this.parseEntries(storage, 8);
            }
        }

        export class MsgPropertyStream extends PropertyStream
        {
            readonly next_recip: number;
            readonly next_attach: number;
            readonly recip_count: number;
            readonly attach_count: number;

            private static type_set =
            {
                next_recip: 'uint32',
                next_attach: 'uint32',
                recip_count: 'uint32',
                attach_count: 'uint32'
            };

            set codepage(value: number)
            {
                this._codepage = value;
            }

            constructor(storage: CFB.Storage, nm: PropertyNameMap, embedded: boolean = false)
            {
                super(storage, nm);

                // Skip first 8 reserved bytes 

                let header = Util.Struct.parse(this._data, MsgPropertyStream.type_set, 8);

                this.next_recip = header.next_recip;
                this.next_attach = header.next_attach;
                this.recip_count = header.recip_count;
                this.attach_count = header.attach_count;

                // Skip struct + last 8 reserved bytes if MSG is embedded 

                this._data = this._data.slice(embedded ? 24 : 32);

                // Parse the entries in the stream

                this.parseEntries(storage, embedded ? 24 : 32);
            }
        }
    }
}
