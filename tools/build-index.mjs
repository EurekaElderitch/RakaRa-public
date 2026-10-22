#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const AdmZip = require("adm-zip");
const root = process.cwd();
const distDir = path.join(root, "dist");

const engines = [];
for (const f of fs.readdirSync(distDir).filter(f => f.endsWith(".rkx"))) {
  const p = path.join(distDir, f);
  const zip = new AdmZip(p);
  const manifestEntry = zip.getEntry("manifest.json");
  if (!manifestEntry) continue;
  const manifest = JSON.parse(manifestEntry.getData().toString());
  engines.push({
    id: manifest.id,
    name: manifest.name,
    version: manifest.version,
    apiVersion: manifest.apiVersion,
    lang: manifest.lang,
    url: f,
    sha256: manifest.sha256,
    size: fs.statSync(p).size,
  });
}

const index = {
  generatedAt: new Date().toISOString(),
  engines,
};
fs.writeFileSync(path.join(distDir, "index.json"), JSON.stringify(index, null, 2));
console.log(`✔ index.json dengan ${engines.length} engine`);