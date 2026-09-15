import { copyFile, mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..");
const tsLibDir = path.join(root, "node_modules", "typescript", "lib");
const assetsDir = path.join(root, "app", "src", "main", "assets");

await mkdir(assetsDir, { recursive: true });
await copyFile(path.join(tsLibDir, "typescript.js"), path.join(assetsDir, "typescript.js"));

const packageJson = JSON.parse(await readFile(path.join(root, "node_modules", "typescript", "package.json"), "utf8"));
const libFiles = (await readdir(tsLibDir))
  .filter((name) => /^lib\..+\.d\.ts$/.test(name))
  .sort();

const libs = {};
for (const name of libFiles) {
  libs[name] = await readFile(path.join(tsLibDir, name), "utf8");
}

const payload = [
  "window.__TS_LIBS__ = ",
  JSON.stringify(libs),
  ";\nwindow.__TS_VERSION__ = ",
  JSON.stringify(packageJson.version),
  ";\n"
].join("");

await writeFile(path.join(assetsDir, "ts_libs.js"), payload, "utf8");
console.log(`Prepared TypeScript ${packageJson.version} compiler and ${libFiles.length} standard-library definition files.`);
