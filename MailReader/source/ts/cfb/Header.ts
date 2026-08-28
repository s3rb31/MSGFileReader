namespace MailReader 
{
    export namespace CFB
    {
        export class SectorAllocationTable
        {
            constructor(private table: number[]) { }

            get length()
            {
                return this.table.length;
            }

            public getChain(start: number): number[]
            {
                // A sector id chain is a sequence of entries from the SAT
                // and represents the sectors belonging to a specific stream
                // and their order.

                // Within the SAT, the _position_ of an entry (array index)
                // refers to it's corresponding sector and the _value_ contained
                // at that position refers to the following entry in the chain.
                // A sequence is terminated by the ENDOFCHAIN(-2) value.

                let chain: number[] = [start];

                for (let i = this.table[start]; i > -2; i = this.table[i])
                {
                    if (i >= this.table.length)
                    {
                        throw new Error("[CFB::SectorAllocationTable::getChain] " +
                            "Invalid SAT chain detected! File is probably corrupt.")
                    }

                    chain.push(i);
                }

                return chain;
            }
        }

        export class Header
        {
            private bom: number;
            private sig: number[];

            private msat_num: number;
            private msat_start: number;
            private ssat_start: number;
            private msat_table: number[];

            readonly dir_start: number;
            readonly stream_min: number;

            readonly short_sz: number;
            readonly sector_sz: number;

            readonly SAT: SectorAllocationTable;
            readonly shortSAT: SectorAllocationTable;

            private static type_set =
            {
                sig: ['uint8', 8],
                clsid: ['uint8', 16],
                revision: 'uint16',
                version: 'uint16',
                bom: 'uint16',
                sector_sz: 'uint16',
                short_sz: 'uint16',
                unused1: ['uint8', 10],
                sat_num: 'uint32',
                dir_start: 'int32',
                unused2: 'uint32',
                stream_min: 'uint32',
                ssat_start: 'int32',
                ssat_num: 'uint32',
                msat_start: 'int32',
                msat_num: 'uint32',
                msat_table: ['int32', 109]
            };

            constructor(data: ArrayBuffer)
            {
                let header = Util.Struct.
                    parse(data, Header.type_set);

                this.bom = header.bom;
                this.sig = header.sig;

                this.msat_num = header.msat_num;
                this.msat_start = header.msat_start;
                this.ssat_start = header.ssat_start;
                this.msat_table = header.msat_table;

                this.dir_start = header.dir_start;
                this.stream_min = header.stream_min;

                this.short_sz = Math.pow(2, header.short_sz);
                this.sector_sz = Math.pow(2, header.sector_sz);

                if (this.validate() == false)
                {
                    throw new Error("[CFB::Header::constructor] Invalid " +
                        "magic signature or BOM detected! Probably not a CFB file!");
                }

                let int32a = new Int32Array(data);

                this.SAT = this.parseSAT(int32a);
                this.shortSAT = this.parseShortSAT(int32a);
            }

            private validate()
            {
                // Check the signature of the file

                let magic_signature: number[] =
                    [0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1];

                for (let i = 0; i < this.sig.length; i++)
                {
                    if (this.sig[i] != magic_signature[i]) 
                    {
                        return false;
                    }
                }

                // Check the byte-order mark (Little-Endian)

                let byte_order_mark: number = 0xfffe;

                if (this.bom != byte_order_mark)
                {
                    return false;
                }

                return true;
            }

            private parseSAT(data: Int32Array): SectorAllocationTable
            {
                // The SAT is an array of 32-bit signed integers, each entry 
                // representing a sector id or one of four special values.

                // If the MSAT is larger than 109 sectors, the additional sector
                // ids are stored within additional MSAT sectors. The amount and
                // the first sector id of these additional sectors are stored in 
                // the header. The last sector id in each of the additional MSAT
                // sectors specifies the next addtional MSAT sector.

                if (this.msat_start != -2 && this.msat_num) // -2 = EndOfChain
                {
                    let sec_id: number | undefined = this.msat_start;

                    for (; sec_id && sec_id > 0; sec_id = this.msat_table.pop()) 
                    {
                        let offset = (this.sector_sz * (sec_id + 1)) / 4;
                        let chunk = data.subarray(offset, (offset + this.sector_sz / 4));

                        this.msat_table = this.msat_table.
                            concat(Array.prototype.slice.call(chunk));
                    }
                }

                // Parse SAT from the first 109 MSAT sectors given in header

                let sat: number[] = [];

                for (let i = 0; this.msat_table[i] > -1; i++) 
                {
                    let offset = (this.sector_sz * (this.msat_table[i] + 1)) / 4;
                    let chunk = data.subarray(offset, (offset + this.sector_sz / 4));

                    sat = sat.concat(Array.prototype.slice.call(chunk));
                }

                return new SectorAllocationTable(sat);
            }

            private parseShortSAT(data: Int32Array): SectorAllocationTable
            {        
                // The sectors for the Short-SAT are stored in the SAT, thus it
                // has to be generated after the SAT has been built. The principle 
                // is the same as with the first 109 sectors of the MSAT.

                let ssat: number[] = [];
                let ssat_chain = this.SAT.getChain(this.ssat_start);

                for (let i = 0; i < ssat_chain.length; i++)
                {
                    let offset = (this.sector_sz * (ssat_chain[i] + 1)) / 4;
                    let chunk = data.subarray(offset, (offset + this.sector_sz / 4));

                    ssat = ssat.concat(Array.prototype.slice.call(chunk));
                }

                return new SectorAllocationTable(ssat);
            }
        }
    }
}
