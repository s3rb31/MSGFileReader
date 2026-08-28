namespace MailReader 
{
    export namespace MSG
    {
        export class File extends CFB.File
        {
            private props: MsgPropertyStream;
            private name_map: PropertyNameMap;

            protected prop_cp: number;
            protected body_cp: number;

            protected recip_count: number;
            protected attach_count: number;
            protected is_unicode: boolean;

            constructor(data: ArrayBuffer)
            {
                super(data);

                this.name_map = new PropertyNameMap(this);
                this.props = new MsgPropertyStream(this, this.name_map);

                this.recip_count = this.props.recip_count;
                this.attach_count = this.props.attach_count;

                this.setCodepageInfo(this.props.read<number>(
                    Property.Tag.PidTagStoreSupportMask));
            }

            protected read<T>(tag: Property.Tag, type?: Property.Type)
            {
                return this.props.read<T>(tag, type);
            }

            protected readCached(tag: Property.Tag, type?: Property.Type)
            {
                return this.props.readCached(tag, type);
            }

            public readNamed<T>(key: number | string, type?: Property.Type)
            {
                return this.props.readNamed<T>(key, type);
            }

            protected parseObjProps<T extends ObjPropertyStream>
                (type: IObjProperty<T>, count: number)
            {
                const array = new Array<T>();

                for (let i = 0; i < count; i++)
                {
                    let storage = this.getStorage(
                        type.PREFIX + ("00000000" + i).slice(-8));

                    if (storage != undefined)
                    {
                        array.push(new type(storage,
                            this.name_map, this.prop_cp));
                    }
                }

                return array;
            }

            private setCodepageInfo(ssm?: number)
            {
                this.is_unicode = ((ssm || 0) & 0x00040000) != 0; 

                // Fetch codepage information 

                const prop_cp = this.props.read<number>(Property.Tag.PidTagMessageCodepage);
                const body_cp = this.props.read<number>(Property.Tag.PidTagInternetCodepage);

                if (body_cp == undefined)
                {
                    throw new Error("[MSG::File::constructor] Property PidTagInternetCodepage is missing!");
                }

                // Set PidTagMessageCodepage for properties in case they are unicode or
                // use PidTagInternetCodepage as fallback if message codepage is missing

                this.body_cp = body_cp;
                this.prop_cp = this.is_unicode ? 1200 : (prop_cp || body_cp);

                // Now we can set the CP for the properties

                this.props.codepage = this.prop_cp;
            }
        }
    }
}