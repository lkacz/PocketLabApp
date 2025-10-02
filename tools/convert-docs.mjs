import { readFile, writeFile, mkdir } from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";
import TurndownService from "turndown";
import { gfm } from "turndown-plugin-gfm";
import { JSDOM } from "jsdom";

const currentFile = fileURLToPath(import.meta.url);
const repoRoot = path.resolve(path.dirname(currentFile), "..");

const turndown = new TurndownService({
  headingStyle: "atx",
  bulletListMarker: "-",
  codeBlockStyle: "fenced",
  hr: "* * *",
});
turndown.use(gfm);

const sources = [
  {
    input: path.join(repoRoot, "app", "src", "main", "assets", "manual.html"),
    output: path.join(repoRoot, "OnlineProtocolEditor", "content", "about.md"),
    note: "Converted from app/src/main/assets/manual.html",
  },
  {
    input: path.join(repoRoot, "app", "src", "main", "assets", "about.txt"),
    output: path.join(repoRoot, "OnlineProtocolEditor", "content", "manual.md"),
    note: "Converted from app/src/main/assets/about.txt",
  },
];

async function ensureDir(filePath) {
  const dir = path.dirname(filePath);
  await mkdir(dir, { recursive: true });
}

async function convert() {
  for (const { input, output, note } of sources) {
    const html = await readFile(input, "utf-8");
    const dom = new JSDOM(html);
    const { document } = dom.window;
    document.querySelectorAll("style").forEach((node) => node.remove());
    const container = document.querySelector("body") ?? document.documentElement;
    const innerHtml = container ? container.innerHTML : html;
    const markdown = `<!-- ${note} -->\n\n${turndown
      .turndown(innerHtml)
      .replace(/\n{3,}/g, "\n\n")
      .trim()}\n`;
    await ensureDir(output);
    await writeFile(output, markdown, "utf-8");
    console.log(`Converted ${path.relative(repoRoot, input)} -> ${path.relative(repoRoot, output)}`);
  }
}

convert().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});