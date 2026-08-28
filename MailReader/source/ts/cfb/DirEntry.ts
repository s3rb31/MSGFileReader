namespace MailReader 
{
    export namespace CFB
    {
        export class DirEntry
        {
            [index: string]: any;

            readonly parents: string[];

            readonly name: string;
            readonly type: number;

            readonly stream_start: number;
            readonly stream_size: number;

            private static type_set = {
                name: ['uint8', 64],
                name_sz: 'uint16',
                type: 'uint8',
                color: 'uint8',
                left: 'int32',
                right: 'int32',
                child: 'int32',
                unused1: ['uint8', 16],
                flags: 'uint32',
                ctime: ['uint8', 8],
                mtime: ['uint8', 8],
                stream_start: 'int32',
                stream_size: 'uint32',
                unused2: 'uint32'
            };

            constructor(data: ArrayBuffer, offset: number, parents: string[] = [])
            {
                let dir = Util.Struct.parse(data, DirEntry.type_set, offset);

                for (let k of ["child", "left", "right"])
                {
                    if (dir[k] == -1) {
                        dir[k] = undefined;
                    }

                    this[k] = dir[k];
                }

                this.parents = parents;

                this.type = dir.type
                this.name = Util.decodeString(1200, dir.name, dir.name_sz - 2);

                this.stream_start = dir.stream_start;
                this.stream_size = dir.stream_size;
            }
        }
    }
}
