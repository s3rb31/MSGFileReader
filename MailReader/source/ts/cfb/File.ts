namespace MailReader 
{
    export namespace CFB
    {
        export class File extends Storage
        {
            private header: Header;
            private data: Uint8Array;

            private dir_chain: number[];
            private ssec_chain: number[];

            public constructor(data: ArrayBuffer)
            {
                super("Root Storage");

                // Parse and validate the header

                this.data = new Uint8Array(data);
                this.header = new Header(data);

                // Parse the file contents 

                this.dir_chain = this.header.SAT.
                    getChain(this.header.dir_start);

                let root_dir = this.parseDirEntry(0);

                this.ssec_chain = this.header.SAT.
                    getChain(root_dir.stream_start);

                this.parseDirectoryTree(root_dir);
            }

            private processDirEntry(dir: DirEntry)
            {
                let storage = this.getStorage(...dir.parents);

                if (storage != undefined)
                {
                    if (dir.type == 1)
                    {
                        storage.addStorage(new Storage(dir.name))
                    }
                    else if (dir.type == 2)
                    {
                        let readF = this.readStreamFactory(dir);
                        storage.addStream(new Stream(dir.name, readF));
                    }
                }
            }

            private parseDirEntry(index: number, parents?: string[])
            {
                let offset = this.calcIndirectOffset(index, 128, this.dir_chain);

                return new DirEntry(this.data.buffer, offset, parents);
            }

            private parseDirectoryTree(root: DirEntry)
            {
                // Sadly, because MSG files have arbitrary 
                // size we cannot use recursion here because
                // we might run out of stack space.

                let stack: DirEntry[] = [];
                let dir: DirEntry | undefined = root;

                for (; dir != undefined; dir = stack.pop())
                {
                    if (dir.type != 5) {
                        this.processDirEntry(dir);
                    }

                    for (let k of ["child", "left", "right"])
                    {
                        if (dir[k] != undefined)
                        {
                            // clone parents array 

                            let count = dir.parents.length;
                            let parents: string[] = Array(count);

                            while (count--) {
                                parents[count] = dir.parents[count];
                            }

                            // add current dir as parent if "child" is parsed

                            if (k == "child" && dir.type != 5) {
                                parents.push(dir.name)
                            }

                            // add left, right or child dir entry to stack

                            stack.push(this.parseDirEntry(dir[k], parents));
                        }
                    }
                }
            }

            private readSector(id: number)
            {
                // Because the size is static for all sectors and the sector
                // id specifies the (0-based) index of the sector in the file
                // (after the header), a sector offset can be calculated with
                // the following formula:             
                //
                //  512 (fixed header size) + sector_size * sector_id

                let offset = 512 + this.header.sector_sz * id;

                return this.data.subarray(offset, this.header.sector_sz + offset);
            }

            private readShortSector(id: number)
            {
                // While normal sectors are directly referenced through the SAT,
                // short sectors are contained in their own stream referenced by
                // the root directory entry. 
              
                // To retrieve the absolute offset of a short sector we first 
                // need to find the offset of its containing sector and then add 
                // the relative offset to the short sector to the start of its 
                // containing sector.

                let offset = this.calcIndirectOffset(id, this.header.short_sz, this.ssec_chain);

                return this.data.subarray(offset, this.header.short_sz + offset);
            }

            private readStreamFactory(entry: DirEntry)
            {
                let result: number[] = [];
                let short = entry.stream_size < this.header.stream_min;

                let SAT = short ? this.header.shortSAT : this.header.SAT;
                let sectorSize = short ? this.header.short_sz : this.header.sector_sz;
                let readSectorF = short ? this.readShortSector : this.readSector;

                let chain = SAT.getChain(entry.stream_start);
                let data: any = new Uint8Array(sectorSize * chain.length);

                return (_size: number = entry.stream_size) : number[] =>
                {
                    if (result.length != _size)
                    {
                        for (let i = 0; i < chain.length; i++)
                        {
                            // call 'readF' on correct context 
                            data.set(readSectorF.call(this, chain[i]), sectorSize * i)
                        }

                        result = Array.prototype.slice.call(data, 0, _size);
                    }

                    return result;
                }
            }

            private calcIndirectOffset(id: number, size: number, chain: number[])
            {
                let num_in_sec = this.header.sector_sz / size;
                let chain_offset = Math.floor(id / num_in_sec);

                if (chain_offset > chain.length)
                {
                    throw new Error("[CFB::File::calcIndirectOffset] " +
                        "Invalid SAT chain detected. File is probably corrupt.")
                }

                // (chain_val * sec_sz) = sector offset from header (pos 512)
                // (id % num_in_sec) * size = offset into actual sector

                return 512 + (chain[chain_offset] * this.header.sector_sz) + ((id % num_in_sec) * size);
            }
        }
    }
}
