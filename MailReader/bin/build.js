"use strict";

const fs = require("fs");
const uglify = require("uglify-js");
const prependFile = require('prepend-file');

// ########################################################################### //

const _uglify = (function (files, _options) {
    var uglified = uglify.minify((function (files) {
        var sources = {};
        for (var file of files)
            sources[file] = fs.readFileSync(file, "utf8");
        return sources;
    })(files), Object.assign({}, _options));
    if (uglified.error != undefined) {
        console.log(uglified.error);
        console.log("ERROR: " + uglified.error.message);
        process.exit(0);
    }
    return uglified;
});

const _uglify_single = (function (path, file_name, _options) {
    _options = Object.assign({}, _options);
    if(_options.sourceMap != undefined) {
        _options.sourceMap.content = fs.readFileSync(path + ".map", "utf8");
        _options.sourceMap.url = file_name + ".map";
        _options.sourceMap.filename = file_name;
    }
    return _uglify([path], _options);
});

// ########################################################################### //

const options = {
    mangle: false,
    compress: {
        passes: 2,
        warnings: true,
		sequences: false
    },
    output: {
        beautify: false
    },
    warnings: true
};

console.log("Minifying ../source/js/* into ../dist/* ...");

var uglified = _uglify([
    "../source/js/Util.js",
    "../source/js/cfb/Header.js",
    "../source/js/cfb/DirEntry.js",
    "../source/js/cfb/Storage.js",
    "../source/js/cfb/File.js",
    "../source/js/rtf/HTMLCapsulation.js",
    "../source/js/msg/File.js",
	"../source/js/msg/Mail.js",
    "../source/js/msg/Property.js",
    "../source/js/msg/PropertyNameMap.js",
    "../source/js/msg/PropertyStream.js",
    "../source/js/msg/ObjProps.js"
], options);

fs.writeFileSync("../dist/mailreader.min.js", uglified.code, "utf8");

console.log("\nWriting file '../dist/mailreader.min.js' ... done!");

// continue minification

var uglified = _uglify_single(
    "../source/js/rtf/LZFUCompression.js", "lzfucomp.min.js", options);

fs.writeFileSync("../dist/lzfucomp.min.js", uglified.code, "utf8");

console.log("Writing file '../dist/lzfucomp.min.js' ... done!");

// prepend copyright notices

console.log("\nPrepend copyright notices to files ... done!\n");

prependFile.sync("../dist/mailreader.min.js", "// -- mailreader.min.js, Copyright (C) 2017 GFA SysCom\n");
prependFile.sync("../dist/lzfucomp.min.js", `/*
 // * Based on java-libpst by Richard Johnson & Orin Eman (C) 2010
 // * Translation to ECMAScript done by GFA Syscom (C) 2017
 // *
 // * Licensed under the Apache License, Version 2.0 (the "License");
 // * you may not use this file except in compliance with the License.
 // * You may obtain a copy of the License at
 // *
 // * http://www.apache.org/licenses/LICENSE-2.0
 // *
 // * Unless required by applicable law or agreed to in writing, software
 // * distributed under the License is distributed on an "AS IS" BASIS,
 // * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // * See the License for the specific language governing permissions and
 // * limitations under the License.
 // *
 // */\n`);
