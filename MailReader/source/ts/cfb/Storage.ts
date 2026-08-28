namespace MailReader 
{
    export namespace CFB
    {
		type ReadStreamF = (size?: number) => number[];

        export class Stream
        {
            constructor(readonly name: string,
                        readonly read: ReadStreamF) { }
        }

        export class Storage
        {
            private streams: Map<string, Stream>;
            private storages: Map<string, Storage>;

            constructor(readonly name: string)
            {
                this.streams = new Map<string, Stream>();
                this.storages = new Map<string, Storage>();
            }

            public addStream(stream: Stream)
            {
                Storage.add(stream, this.streams);
            }

            public addStorage(storage: Storage)
            {
                Storage.add(storage, this.storages);
            }

            public getStream(name: string)
            {
                return Storage.get(name, this.streams);
            }

            public getStorage(...children: string[])
            {
                let storage: Storage | undefined = this;

                if (children != undefined && children.length > 0)
                {
                    let elem = undefined;

                    do
                    {
                        elem = children.shift();

                        if (elem != undefined && storage != undefined) {
                            storage = Storage.get(elem, storage.storages);
                        }
                    }
                    while (elem != undefined)
                }

                return storage;
            }

            private static get<T>(name: string,
                map: Map<string, T>)
            {
                if (name.length > 1) {
                    return map.get(name);
                }

                return undefined;
            }

            private static add(obj: Stream | Storage,
                map: Map<string, Stream | Storage>)
            {
                if (obj != undefined) {
                    map.set(obj.name, obj);
                }
            }
        }
    }
}