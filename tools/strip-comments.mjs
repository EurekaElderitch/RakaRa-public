#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";

function strip(source) {
  let out = "", i = 0, prev = "";
  let inStr = false, inTriple = false, inLine = false, inBlock = false;

  while (i < source.length) {
    const c = source[i], n = source[i + 1] || "";

    if (!inLine && !inBlock && c === '"' && source.slice(i, i + 3) === '"""') {
      inTriple = !inTriple; out += '"""'; i += 3; prev = '"'; continue;
    }
    if (inTriple) { out += c; i++; continue; }
    if (!inLine && !inBlock && c === '"' && prev !== "\\") inStr = !inStr;
    if (inStr) { out += c; prev = c; i++; continue; }
    if (!inLine && !inBlock && c === "/" && n === "/") { inLine = true; i += 2; continue; }
    if (inLine) { if (c === "\n") { inLine = false; out += c; } i++; continue; }
    if (!inBlock && c === "/" && n === "*") { inBlock = true; i += 2; continue; }
    if (inBlock) { if (c === "*" && n === "/") { inBlock = false; i += 2; } else i++; continue; }

    out += c; prev = c; i++;
  }
  return out;
}

function walk(dir, cb) {
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    if (fs.statSync(p).isDirectory()) walk(p, cb);
    else if (f.endsWith(".kt")) cb(p);
  }
}

const root = process.cwd();
for (const t of ["app", "engine-manager", "source-api", "engines"]) {
  const dir = path.join(root, t);
  if (!fs.existsSync(dir)) continue;
  walk(dir, (file) => {
    const src = fs.readFileSync(file, "utf8");
    const s = strip(src);
    if (src !== s) { fs.writeFileSync(file, s); console.log(`✔ ${path.relative(root, file)}`); }
  });
}