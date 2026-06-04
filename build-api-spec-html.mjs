import { readFile, writeFile } from "node:fs/promises";
import { marked } from "file:///C:/Users/danie/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/marked/lib/marked.esm.js";

const sourcePath = new URL("./ECONOUP_API_SPEC.md", import.meta.url);
const outputPath = new URL("./ECONOUP_API_SPEC.html", import.meta.url);

const slugCounts = new Map();
const escapeHtml = (value) =>
  value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

const slugify = (value) => {
  const base =
    value
      .toLowerCase()
      .replace(/<[^>]+>/g, "")
      .replace(/[`"'():/]/g, "")
      .replace(/[^\p{L}\p{N}]+/gu, "-")
      .replace(/^-+|-+$/g, "") || "section";
  const count = slugCounts.get(base) ?? 0;
  slugCounts.set(base, count + 1);
  return count === 0 ? base : `${base}-${count + 1}`;
};

const source = await readFile(sourcePath, "utf8");
const tokens = marked.lexer(source, { gfm: true });
const headings = [];

const renderer = new marked.Renderer();
renderer.heading = ({ tokens: headingTokens, depth }) => {
  const text = marked.Parser.parseInline(headingTokens);
  const plain = headingTokens
    .map((token) => token.raw ?? token.text ?? "")
    .join("")
    .replace(/[#*_`]/g, "")
    .trim();
  const id = slugify(plain);

  if (depth <= 3) {
    headings.push({ depth, id, label: plain });
  }

  return `<h${depth} id="${id}"><a class="heading-anchor" href="#${id}" aria-label="${escapeHtml(
    plain,
  )} 섹션 링크">#</a>${text}</h${depth}>`;
};

marked.use({
  gfm: true,
  renderer,
});

const articleHtml = marked.parse(source);
const topLevelSections = headings.filter((heading) => heading.depth === 2);
const endpointCount = [...source.matchAll(/^\| `(GET|POST|PUT|PATCH|DELETE)` \| `/gm)].length;
const screenMapCount = [...source.matchAll(/^\| EC-/gm)].length;

const tableOfContents = headings
  .filter((heading) => heading.depth === 2 || heading.depth === 3)
  .map(
    (heading) =>
      `<a class="toc-link depth-${heading.depth}" href="#${heading.id}" data-search="${escapeHtml(
        heading.label.toLowerCase(),
      )}">${escapeHtml(heading.label)}</a>`,
  )
  .join("");

const sectionChips = topLevelSections
  .slice(4, 21)
  .map(
    (heading) =>
      `<a class="section-chip" href="#${heading.id}">${escapeHtml(
        heading.label.replace(/^\d+\.\s*/, ""),
      )}</a>`,
  )
  .join("");

const html = `<!doctype html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Econo-up API 명세서</title>
    <style>
      :root {
        color-scheme: light;
        --bg: #f5f7fb;
        --paper: #ffffff;
        --paper-soft: #f8fbff;
        --ink: #172033;
        --muted: #5d687c;
        --line: #d8e0ee;
        --line-strong: #bcc9df;
        --nav: #0f172a;
        --nav-soft: #18243d;
        --brand: #1463ff;
        --brand-soft: #e8f0ff;
        --mint: #0f9f74;
        --mint-soft: #e5fbf3;
        --gold: #b05b00;
        --gold-soft: #fff0d9;
        --violet: #6656d9;
        --violet-soft: #eeeaff;
        --danger: #bc3347;
        --danger-soft: #fff0f2;
        --shadow: 0 18px 50px rgba(21, 36, 65, 0.1);
      }

      * {
        box-sizing: border-box;
      }

      html {
        scroll-behavior: smooth;
      }

      body {
        margin: 0;
        background:
          linear-gradient(180deg, #eef4ff 0, transparent 360px),
          var(--bg);
        color: var(--ink);
        font-family:
          Inter, Pretendard, "Apple SD Gothic Neo", "Noto Sans KR",
          system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        line-height: 1.62;
        letter-spacing: 0;
      }

      a {
        color: inherit;
      }

      .layout {
        display: grid;
        grid-template-columns: 320px minmax(0, 1fr);
        min-height: 100vh;
      }

      .sidebar {
        position: sticky;
        top: 0;
        height: 100vh;
        overflow: auto;
        padding: 28px 22px;
        background: var(--nav);
        color: #e8eefc;
      }

      .brand {
        display: grid;
        gap: 8px;
        padding: 18px;
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-radius: 8px;
        background: linear-gradient(145deg, rgba(20, 99, 255, 0.28), rgba(15, 159, 116, 0.12));
      }

      .eyebrow {
        margin: 0;
        color: #9bb7ff;
        font-size: 12px;
        font-weight: 800;
        text-transform: uppercase;
      }

      .brand h1 {
        margin: 0;
        font-size: 24px;
        line-height: 1.15;
      }

      .brand p {
        margin: 0;
        color: #c3d0ec;
        font-size: 13px;
      }

      .search {
        width: 100%;
        margin: 18px 0 14px;
        padding: 12px 14px;
        border: 1px solid rgba(255, 255, 255, 0.16);
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.1);
        color: #ffffff;
        font: inherit;
      }

      .search::placeholder {
        color: #9caecc;
      }

      .toc {
        display: grid;
        gap: 3px;
        padding-bottom: 30px;
      }

      .toc-link {
        border-radius: 7px;
        color: #d4def3;
        text-decoration: none;
        transition: background 140ms ease, color 140ms ease;
      }

      .toc-link:hover,
      .toc-link.active {
        background: rgba(255, 255, 255, 0.13);
        color: #ffffff;
      }

      .toc-link.depth-2 {
        margin-top: 5px;
        padding: 9px 10px;
        font-size: 14px;
        font-weight: 800;
      }

      .toc-link.depth-3 {
        padding: 6px 10px 6px 22px;
        color: #aebedc;
        font-size: 12px;
      }

      .main {
        min-width: 0;
        padding: 42px clamp(18px, 4vw, 58px) 72px;
      }

      .hero,
      .article {
        max-width: 1180px;
        margin: 0 auto;
      }

      .hero {
        overflow: hidden;
        margin-bottom: 24px;
        padding: clamp(24px, 4vw, 46px);
        border: 1px solid rgba(20, 99, 255, 0.12);
        border-radius: 8px;
        background:
          linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(240, 247, 255, 0.94)),
          var(--paper);
        box-shadow: var(--shadow);
      }

      .hero-grid {
        display: grid;
        grid-template-columns: minmax(0, 1.35fr) minmax(250px, 0.65fr);
        gap: 24px;
        align-items: end;
      }

      .hero h2 {
        margin: 8px 0 12px;
        font-size: clamp(30px, 4vw, 54px);
        line-height: 1.05;
      }

      .hero-copy {
        max-width: 720px;
        margin: 0;
        color: var(--muted);
        font-size: 16px;
      }

      .summary {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
      }

      .metric {
        min-height: 104px;
        padding: 16px;
        border: 1px solid var(--line);
        border-radius: 8px;
        background: var(--paper);
      }

      .metric strong {
        display: block;
        font-size: 30px;
        line-height: 1.1;
      }

      .metric span {
        color: var(--muted);
        font-size: 13px;
        font-weight: 700;
      }

      .chips {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-top: 24px;
      }

      .section-chip {
        border: 1px solid var(--line);
        border-radius: 999px;
        padding: 7px 11px;
        background: var(--paper);
        color: var(--ink);
        font-size: 12px;
        font-weight: 800;
        text-decoration: none;
      }

      .section-chip:hover {
        border-color: var(--brand);
        background: var(--brand-soft);
      }

      .article {
        padding: clamp(20px, 3vw, 44px);
        border: 1px solid var(--line);
        border-radius: 8px;
        background: var(--paper);
        box-shadow: var(--shadow);
      }

      .article > h1:first-child {
        display: none;
      }

      h2 {
        margin-top: 58px;
        padding-top: 14px;
        border-top: 1px solid var(--line);
        font-size: 28px;
        line-height: 1.2;
      }

      .article h2:first-of-type {
        margin-top: 0;
        border-top: 0;
      }

      h3 {
        margin-top: 34px;
        font-size: 21px;
        line-height: 1.3;
      }

      h4 {
        margin-top: 24px;
        color: var(--muted);
        font-size: 15px;
        text-transform: uppercase;
      }

      h2,
      h3,
      h4 {
        scroll-margin-top: 24px;
      }

      .heading-anchor {
        display: inline-block;
        width: 25px;
        margin-left: -25px;
        color: var(--brand);
        opacity: 0;
        text-decoration: none;
      }

      h2:hover .heading-anchor,
      h3:hover .heading-anchor,
      h4:hover .heading-anchor {
        opacity: 1;
      }

      p,
      li {
        color: var(--ink);
      }

      blockquote {
        margin: 22px 0;
        padding: 14px 18px;
        border-left: 4px solid var(--brand);
        border-radius: 0 8px 8px 0;
        background: var(--brand-soft);
        color: var(--muted);
      }

      blockquote p {
        margin: 0;
      }

      table {
        display: block;
        width: 100%;
        overflow-x: auto;
        margin: 18px 0 28px;
        border-collapse: separate;
        border-spacing: 0;
        border: 1px solid var(--line);
        border-radius: 8px;
        background: var(--paper);
      }

      th,
      td {
        min-width: 110px;
        padding: 11px 13px;
        border-bottom: 1px solid var(--line);
        vertical-align: top;
        text-align: left;
        font-size: 13px;
      }

      th {
        position: sticky;
        top: 0;
        background: var(--paper-soft);
        color: var(--muted);
        font-size: 12px;
        font-weight: 900;
        text-transform: uppercase;
      }

      tr:last-child td {
        border-bottom: 0;
      }

      td:first-child {
        font-weight: 800;
      }

      code {
        border: 1px solid #dbe6fb;
        border-radius: 5px;
        padding: 1px 5px;
        background: #f1f6ff;
        color: #0d47bf;
        font-size: 0.93em;
      }

      pre {
        overflow: auto;
        margin: 16px 0 28px;
        border: 1px solid #23304b;
        border-radius: 8px;
        padding: 18px;
        background: #0e1628;
        color: #dfe8ff;
        font-size: 13px;
        line-height: 1.55;
      }

      pre code {
        border: 0;
        padding: 0;
        background: transparent;
        color: inherit;
      }

      ul,
      ol {
        padding-left: 22px;
      }

      hr {
        border: 0;
        border-top: 1px solid var(--line);
      }

      .scope {
        display: inline-flex;
        align-items: center;
        border-radius: 999px;
        padding: 3px 8px;
        font-size: 11px;
        font-weight: 900;
        white-space: nowrap;
      }

      .scope.mvp {
        background: var(--mint-soft);
        color: var(--mint);
      }

      .scope.bm {
        background: var(--gold-soft);
        color: var(--gold);
      }

      .scope.post {
        background: var(--violet-soft);
        color: var(--violet);
      }

      .method {
        display: inline-flex;
        min-width: 62px;
        justify-content: center;
        border-radius: 6px;
        padding: 3px 7px;
        color: white;
        font-size: 11px;
        font-weight: 900;
      }

      .get {
        background: #137c59;
      }

      .post {
        background: #1463ff;
      }

      .put {
        background: #985900;
      }

      .delete {
        background: var(--danger);
      }

      .back-top {
        position: fixed;
        right: 22px;
        bottom: 22px;
        border: 1px solid var(--line-strong);
        border-radius: 8px;
        padding: 10px 12px;
        background: var(--paper);
        box-shadow: var(--shadow);
        color: var(--ink);
        font-weight: 900;
        text-decoration: none;
      }

      @media (max-width: 980px) {
        .layout {
          grid-template-columns: 1fr;
        }

        .sidebar {
          position: relative;
          height: auto;
          padding-bottom: 18px;
        }

        .toc {
          max-height: 320px;
          overflow: auto;
        }

        .main {
          padding-top: 20px;
        }

        .hero-grid {
          grid-template-columns: 1fr;
        }
      }

      @media (max-width: 640px) {
        .summary {
          grid-template-columns: 1fr 1fr;
        }

        .metric {
          min-height: 86px;
        }

        .metric strong {
          font-size: 24px;
        }

        .article {
          padding: 18px 14px;
        }

        th,
        td {
          min-width: 96px;
          padding: 9px;
        }

        .back-top {
          right: 12px;
          bottom: 12px;
        }
      }
    </style>
  </head>
  <body id="top">
    <div class="layout">
      <aside class="sidebar">
        <div class="brand">
          <p class="eyebrow">Econo-up</p>
          <h1>API Spec</h1>
          <p>학습, 복습, 시뮬레이션, BM, 소셜 API를 한눈에 훑는 개발 문서</p>
        </div>
        <input class="search" id="toc-search" type="search" placeholder="목차 검색" aria-label="목차 검색" />
        <nav class="toc" id="toc">${tableOfContents}</nav>
      </aside>
      <main class="main">
        <section class="hero">
          <div class="hero-grid">
            <div>
              <p class="eyebrow">Backend Contract Draft</p>
              <h2>이코노업 API 명세서</h2>
              <p class="hero-copy">
                와이어프레임 기준 전체 API 계약을 정리했습니다. MVP 핵심 흐름과 BM, Post-MVP 범위를 분리해
                화면과 엔드포인트를 빠르게 연결할 수 있습니다.
              </p>
            </div>
            <div class="summary">
              <div class="metric"><strong>${topLevelSections.length}</strong><span>문서 섹션</span></div>
              <div class="metric"><strong>${endpointCount}</strong><span>명시 API 행</span></div>
              <div class="metric"><strong>${screenMapCount}</strong><span>화면 매핑</span></div>
              <div class="metric"><strong>v1</strong><span>Draft</span></div>
            </div>
          </div>
          <div class="chips">${sectionChips}</div>
        </section>
        <article class="article" id="spec">${articleHtml}</article>
      </main>
    </div>
    <a class="back-top" href="#top">Top</a>
    <script>
      const links = [...document.querySelectorAll(".toc-link")];
      const search = document.querySelector("#toc-search");
      const article = document.querySelector("#spec");
      const enhanceCell = (cell) => {
        const text = cell.textContent.trim();
        if (/^(GET|POST|PUT|PATCH|DELETE)$/.test(text)) {
          cell.innerHTML = '<span class="method ' + text.toLowerCase() + '">' + text + '</span>';
        }
        if (/^MVP/.test(text)) {
          cell.innerHTML = '<span class="scope mvp">' + text + '</span>';
        }
        if (/^(BM|Research Optional)/.test(text)) {
          cell.innerHTML = '<span class="scope bm">' + text + '</span>';
        }
        if (/^(Post-MVP|MVP Optional|MVP Recommended)/.test(text)) {
          cell.innerHTML = '<span class="scope post">' + text + '</span>';
        }
      };

      document.querySelectorAll("td").forEach(enhanceCell);
      search.addEventListener("input", () => {
        const query = search.value.trim().toLowerCase();
        links.forEach((link) => {
          link.hidden = query && !link.dataset.search.includes(query);
        });
      });

      const sectionObserver = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            links.forEach((link) => link.classList.toggle("active", link.hash === "#" + entry.target.id));
          });
        },
        { rootMargin: "-18% 0px -72% 0px" },
      );

      article.querySelectorAll("h2, h3").forEach((heading) => sectionObserver.observe(heading));
    </script>
  </body>
</html>
`;

await writeFile(outputPath, html, "utf8");
console.log(`Built ${outputPath.pathname}`);
