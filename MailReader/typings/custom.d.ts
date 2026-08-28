interface Window
{
    Map: MapConstructor;
}

declare interface CPTable
{
    [key: number]: {
        dec: { [key: number]: string },
        enc: { [key: string]: number }
    },
    utils: {
        decode: (cp: number, data: number[]) => string,
        encode: (cp: number, data: string, ofmt: string) => any
    }
}

declare interface SymTable
{
    [key: string]: string;
}

declare var cptable: CPTable;
declare var symbolTable: SymTable;


// copied from lib.es6.d.ts

interface Map<K, V>
{
    readonly size: number;

    get(key: K): V | undefined;
    has(key: K): boolean;
    set(key: K, value?: V): this;

    clear(): void;
    delete(key: K): boolean;
}

interface MapConstructor
{
    new (): Map<any, any>;
    new <K, V>(entries?: [K, V][]): Map<K, V>;
    readonly prototype: Map<any, any>;
}

declare var Map: MapConstructor;