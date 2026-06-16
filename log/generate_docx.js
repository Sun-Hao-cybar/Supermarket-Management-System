/**
 * generate_docx.js — 将7天的开发日志 Markdown 文件转换为 DOCX
 *
 * 格式仿照课设报告：
 *   - A4 页面, 2.5cm 页边距
 *   - 标题：黑体 16pt 居中加粗
 *   - 一级标题：黑体 14pt 加粗
 *   - 二级标题：黑体 12pt 加粗
 *   - 正文：宋体 12pt，首行缩进 2 字符
 *   - 表格：三线表
 *   - 图片：内嵌
 *   - 页脚：居中页码
 */

'use strict';

const fs = require('fs');
const path = require('path');

const {
  Document, Packer,
  Paragraph, TextRun,
  Table, TableRow, TableCell,
  Footer,
  PageNumber, AlignmentType, LineRuleType, HeadingLevel,
  BorderStyle, WidthType, ShadingType,
  PageBreak, ImageRun,
} = require('docx');

// ────────────────────────────────────────────────────
// Constants (matching 课设报告 format)
// ────────────────────────────────────────────────────

const PAGE_W = 11906;   // A4 width DXA
const PAGE_H = 16838;   // A4 height DXA
const MARGIN = 1418;    // 2.5 cm
const CONTENT_W = PAGE_W - 2 * MARGIN; // 9070

// Three-line table borders
const THICK = { style: BorderStyle.SINGLE, size: 12, color: '000000' }; // 1.5pt
const THIN  = { style: BorderStyle.SINGLE, size: 6,  color: '000000' }; // 0.75pt
const NO_BORDER = { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' };

// Font constants
const FONT_BODY = { ascii: 'Cambria Math', eastAsia: 'SimSun', hAnsi: 'Cambria Math' };
const FONT_HEI = { ascii: 'Cambria Math', eastAsia: 'SimHei', hAnsi: 'Cambria Math' };

// Paragraph spacing
const BODY_LINE = { line: 240, lineRule: LineRuleType.AUTO }; // single
const HEADING1_LINE = { line: 288, lineRule: LineRuleType.AUTO }; // 1.2x
const HEADING2_LINE = { line: 360, lineRule: LineRuleType.AUTO }; // 1.5x

// ────────────────────────────────────────────────────
// Helper: paragraph builders
// ────────────────────────────────────────────────────

function titlePara(text, size = 32) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 120, after: 120, line: 360, lineRule: LineRuleType.AUTO },
    indent: { firstLine: 0 },
    children: [new TextRun({ text, font: FONT_HEI, size, bold: true })],
  });
}

function subtitlePara(text, size = 28) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 60, after: 60, line: 288, lineRule: LineRuleType.AUTO },
    indent: { firstLine: 0 },
    children: [new TextRun({ text, font: FONT_HEI, size, bold: true })],
  });
}

function centeredPara(text, size = 24, bold = false) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 40, after: 40, ...BODY_LINE },
    indent: { firstLine: 0 },
    children: [new TextRun({ text, font: FONT_BODY, size, bold })],
  });
}

function h1Para(text) {
  return new Paragraph({
    spacing: { before: 240, after: 120, ...HEADING1_LINE },
    indent: { firstLine: 0 },
    children: [new TextRun({ text, font: FONT_HEI, size: 28, bold: true })], // 14pt
  });
}

function h2Para(text) {
  return new Paragraph({
    spacing: { before: 160, after: 80, ...HEADING2_LINE },
    indent: { firstLine: 0 },
    children: [new TextRun({ text, font: FONT_HEI, size: 24, bold: true })], // 12pt
  });
}

function bodyPara(runs) {
  return new Paragraph({
    spacing: { before: 0, after: 0, ...BODY_LINE },
    indent: { firstLine: 480 }, // 2-char indent
    children: runs,
  });
}

function bodyText(text) {
  return bodyPara([new TextRun({ text, font: FONT_BODY, size: 24 })]);
}

function bodyWithBold(segments) {
  // segments: [{text, bold}, ...]
  const runs = segments.map(s => new TextRun({
    text: s.text,
    font: FONT_BODY,
    size: 24,
    bold: !!s.bold,
  }));
  return bodyPara(runs);
}

function bulletPara(text, indentLevel = 0) {
  const leftIndent = 480 + indentLevel * 480;
  return new Paragraph({
    spacing: { before: 0, after: 0, ...BODY_LINE },
    indent: { left: leftIndent, firstLine: 0 },
    children: [
      new TextRun({ text: '• ', font: FONT_BODY, size: 24 }),
      new TextRun({ text, font: FONT_BODY, size: 24 }),
    ],
  });
}

function dashedPara(content, indentLevel = 0) {
  const leftIndent = 480 + indentLevel * 480;
  let children;
  if (typeof content === 'string') {
    children = [
      new TextRun({ text: '- ', font: FONT_BODY, size: 24 }),
      new TextRun({ text: content, font: FONT_BODY, size: 24 }),
    ];
  } else if (Array.isArray(content)) {
    children = [
      new TextRun({ text: '- ', font: FONT_BODY, size: 24 }),
      ...content,
    ];
  } else {
    children = [new TextRun({ text: '- ', font: FONT_BODY, size: 24 })];
  }
  return new Paragraph({
    spacing: { before: 0, after: 0, ...BODY_LINE },
    indent: { left: leftIndent, firstLine: 0 },
    children,
  });
}

function emptyPara() {
  return new Paragraph({
    spacing: { before: 0, after: 0 },
    children: [],
  });
}

function separatorPara() {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 80, after: 80 },
    indent: { firstLine: 0 },
    children: [new TextRun({ text: '—' .repeat(20), font: FONT_BODY, size: 20, color: '999999' })],
  });
}

function pageBreakPara() {
  return new Paragraph({ children: [new PageBreak()] });
}

// Image paragraph
function imagePara(imagePath, caption, maxWidth = 400) {
  const fullPath = path.resolve(__dirname, imagePath);
  const children = [];

  if (fs.existsSync(fullPath)) {
    const imgBuf = fs.readFileSync(fullPath);
    const ext = path.extname(imagePath).slice(1).toLowerCase();
    const imgType = ext === 'jpg' ? 'jpeg' : ext;
    try {
      children.push(new ImageRun({
        type: imgType,
        data: imgBuf,
        transformation: { width: maxWidth, height: Math.round(maxWidth * 0.7) },
        altText: { title: caption || '', description: caption || '', name: caption || '' },
      }));
    } catch (e) {
      children.push(new TextRun({ text: `[图片: ${caption || imagePath}]`, font: FONT_BODY, size: 20, italics: true, color: '999999' }));
    }
  } else {
    children.push(new TextRun({ text: `[图片缺失: ${caption || imagePath}]`, font: FONT_BODY, size: 20, italics: true, color: '999999' }));
  }

  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 80, after: 40 },
    indent: { firstLine: 0 },
    children,
  });
}

function captionPara(text) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 40, after: 120, ...BODY_LINE },
    indent: { firstLine: 0 },
    children: [new TextRun({ text, font: FONT_BODY, size: 20, bold: true })],
  });
}

// Three-line table
function threeLineTable(headers, rows, colWidths) {
  const sum = colWidths.reduce((a, b) => a + b, 0);
  const scale = CONTENT_W / sum;

  const scaled = colWidths.map(w => Math.round(w * scale));

  const headerCells = headers.map((h, i) =>
    new TableCell({
      width: { size: scaled[i], type: WidthType.DXA },
      borders: { top: THICK, bottom: THIN, left: NO_BORDER, right: NO_BORDER },
      shading: { fill: 'FFFFFF', type: ShadingType.CLEAR },
      margins: { top: 80, bottom: 80, left: 120, right: 120 },
      children: [new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 0, ...BODY_LINE },
        indent: { firstLine: 0 },
        children: [new TextRun({ text: h, font: FONT_HEI, size: 22, bold: true })],
      })],
    })
  );

  const dataRows = rows.map((row, ri) => {
    const isLast = ri === rows.length - 1;
    const cells = row.map((cell, ci) =>
      new TableCell({
        width: { size: scaled[ci], type: WidthType.DXA },
        borders: {
          top: NO_BORDER,
          bottom: isLast ? THICK : NO_BORDER,
          left: NO_BORDER,
          right: NO_BORDER,
        },
        shading: { fill: 'FFFFFF', type: ShadingType.CLEAR },
        margins: { top: 60, bottom: 60, left: 120, right: 120 },
        children: [new Paragraph({
          alignment: ci === 0 ? AlignmentType.CENTER : AlignmentType.LEFT,
          spacing: { before: 0, after: 0, ...BODY_LINE },
          indent: { firstLine: 0 },
          children: [new TextRun({ text: String(cell), font: FONT_BODY, size: 22 })],
        })],
      })
    );
    return new TableRow({ children: cells });
  });

  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: scaled,
    rows: [new TableRow({ children: headerCells }), ...dataRows],
  });
}

// ────────────────────────────────────────────────────
// Code block
// ────────────────────────────────────────────────────

function codePara(text) {
  return new Paragraph({
    spacing: { before: 0, after: 0, line: 200, lineRule: LineRuleType.AUTO },
    indent: { left: 480, firstLine: 0 },
    shading: { fill: 'F5F5F5', type: ShadingType.CLEAR },
    children: [new TextRun({ text, font: { ascii: 'Consolas', hAnsi: 'Consolas' }, size: 20 })],
  });
}

// ────────────────────────────────────────────────────
// Inline markdown parser
// ────────────────────────────────────────────────────

function parseInlineMD(text) {
  if (!text) return [new TextRun({ text: '', font: FONT_BODY, size: 24 })];

  const runs = [];
  // Match: **bold**, `code`, or plain text
  const re = /(\*\*(.+?)\*\*|`([^`]+)`|\[([^\]]+)\]\(([^)]+)\)|[^*`]+)/g;
  let m;
  let lastIdx = 0;

  while ((m = re.exec(text)) !== null) {
    if (m.index > lastIdx) {
      runs.push(new TextRun({ text: text.slice(lastIdx, m.index), font: FONT_BODY, size: 24 }));
    }
    if (m[2] !== undefined) {
      // **bold**
      runs.push(new TextRun({ text: m[2], font: FONT_BODY, size: 24, bold: true }));
    } else if (m[3] !== undefined) {
      // `code`
      runs.push(new TextRun({ text: m[3], font: { ascii: 'Consolas', hAnsi: 'Consolas' }, size: 20 }));
    } else if (m[4] !== undefined) {
      // [text](url) - treat as plain text
      runs.push(new TextRun({ text: m[4], font: FONT_BODY, size: 24 }));
    } else {
      runs.push(new TextRun({ text: m[0], font: FONT_BODY, size: 24 }));
    }
    lastIdx = m.index + m[0].length;
  }

  if (runs.length === 0) {
    runs.push(new TextRun({ text, font: FONT_BODY, size: 24 }));
  }
  return runs;
}

// ────────────────────────────────────────────────────
// Markdown line parser: classify each line
// ────────────────────────────────────────────────────

function classifyLine(line) {
  if (line.startsWith('# ') && !line.startsWith('## ')) return { type: 'title', text: line.slice(2).trim() };
  if (line.startsWith('## ')) return { type: 'h2', text: line.slice(3).trim() };
  if (line.startsWith('### ')) return { type: 'h3', text: line.slice(4).trim() };
  if (line.startsWith('#### ')) return { type: 'h4', text: line.slice(5).trim() };
  if (line.startsWith('---')) return { type: 'separator' };
  if (line.startsWith('- ')) return { type: 'dash', text: line.slice(2).trim() };
  if (line.startsWith('|')) return { type: 'table_row' };
  if (line.startsWith('```')) return { type: 'code_fence' };
  if (line.startsWith('![')) return { type: 'image', text: line };
  if (line.startsWith('> ')) return { type: 'quote', text: line.slice(2).trim() };
  if (line.startsWith('**') && line.includes('：')) {
    // Bold label line like "**题　　目：** 超市进销存管理系统"
    return { type: 'bold_label', text: line };
  }
  return { type: 'text', text: line };
}

// Parse image syntax: ![alt](path) — handles parentheses in filenames
function parseImage(line) {
  const m = line.match(/^!\[([^\]]*)\]\((.+)\)$/);
  if (m) return { alt: m[1], src: m[2] };
  return null;
}

// Parse a markdown table row
function parseTableRow(line) {
  // Split by |, trim each cell
  const cells = line.split('|').map(c => c.trim()).filter(c => c !== '');
  return cells;
}

// ────────────────────────────────────────────────────
// Convert one day's log to DOCX children
// ────────────────────────────────────────────────────

function convertDayLog(mdContent, dayNumber, dateStr) {
  const lines = mdContent.split('\n');
  const children = [];
  let i = 0;

  // Track table parsing state
  let tableRows = [];
  let inTable = false;
  let tableHasSep = false;
  let inCodeBlock = false;
  let codeLines = [];

  // Title page content
  children.push(titlePara('超市管理系统'));
  children.push(subtitlePara(`开发工作日志（第${dayNumber}天）`));
  children.push(emptyPara());
  children.push(centeredPara('项目名称：超市进销存管理系统', 24, false));
  children.push(centeredPara(`开发日期：${dateStr}`, 24, false));
  children.push(centeredPara('开发人员：孙昊', 24, false));
  children.push(emptyPara());
  children.push(separatorPara());
  children.push(emptyPara());

  // Parse body
  while (i < lines.length) {
    const raw = lines[i];
    const line = raw.trim();
    i++;

    // Handle code blocks
    if (line.startsWith('```')) {
      if (inCodeBlock) {
        // End code block
        codeLines.forEach(cl => children.push(codePara(cl)));
        codeLines = [];
        inCodeBlock = false;
      } else {
        // Start code block
        inCodeBlock = true;
      }
      continue;
    }
    if (inCodeBlock) {
      codeLines.push(raw);
      continue;
    }

    // Handle tables
    if (line.startsWith('|')) {
      const cells = parseTableRow(line);
      // Check if it's a separator row (e.g. |---|---|)
      if (cells.every(c => /^:?-{3,}:?$/.test(c))) {
        tableHasSep = true;
        continue;
      }
      if (!inTable) {
        inTable = true;
        tableRows = [];
        tableHasSep = false;
      }
      tableRows.push(cells);
      continue;
    } else if (inTable) {
      // End table, render it
      if (tableRows.length >= 2) {
        const headers = tableRows[0];
        const data = tableRows.slice(1);
        const n = headers.length;
        const colW = Math.round(CONTENT_W / n);
        const widths = Array(n).fill(colW);
        children.push(emptyPara());
        children.push(threeLineTable(headers, data, widths));
        children.push(emptyPara());
      }
      tableRows = [];
      inTable = false;
    }

    // Handle images
    if (line.startsWith('![')) {
      const img = parseImage(line);
      if (img) {
        // If next line is a caption text (not empty, not markdown heading)
        let caption = img.alt;
        if (i < lines.length && lines[i].trim() && !lines[i].trim().startsWith('#') && !lines[i].trim().startsWith('![')) {
          // don't override alt
        }
        children.push(imagePara(img.src, caption));
        if (caption) {
          children.push(captionPara(caption));
        }
      }
      continue;
    }

    const cls = classifyLine(line);

    // Skip empty lines
    if (!line) {
      children.push(emptyPara());
      continue;
    }

    switch (cls.type) {
      case 'title':
        // Skip the main title, already rendered on title page
        break;

      case 'h2':
        children.push(separatorPara());
        children.push(h1Para(cls.text));
        break;

      case 'h3':
        children.push(emptyPara());
        children.push(h2Para(cls.text));
        break;

      case 'h4':
        children.push(emptyPara());
        children.push(bodyPara([new TextRun({ text: cls.text, font: FONT_HEI, size: 24, bold: true })]));
        break;

      case 'separator':
        // Already handled around h2
        break;

      case 'dash':
        children.push(dashedPara(parseInlineMDOrText(cls.text)));
        break;

      case 'text': {
        // Check if it has inline bold
        if (cls.text.includes('**')) {
          const runs = parseInlineMD(cls.text);
          children.push(bodyPara(runs));
        } else if (cls.text) {
          children.push(bodyText(cls.text));
        }
        break;
      }

      case 'bold_label':
        children.push(bodyPara(parseInlineMD(cls.text)));
        break;

      default:
        if (cls.text) {
          children.push(bodyText(cls.text));
        }
    }
  }

  return children;
}

// Helper: parse a dash item that may have inline markdown
function parseInlineMDOrText(text) {
  return parseInlineMD(text);
}

// ────────────────────────────────────────────────────
// Generate DOCX for a single day
// ────────────────────────────────────────────────────

function generateDayDocx(dayNumber, dateStr, mdFile, outputFile) {
  console.log(`Generating Day ${dayNumber}: ${mdFile} → ${outputFile}`);

  const mdContent = fs.readFileSync(mdFile, 'utf-8');
  const children = convertDayLog(mdContent, dayNumber, dateStr);

  const doc = new Document({
    styles: {
      default: {
        document: {
          run: { font: FONT_BODY, size: 24 },
          paragraph: { spacing: { before: 0, after: 0, ...BODY_LINE } },
        },
      },
    },
    sections: [{
      properties: {
        page: {
          size: { width: PAGE_W, height: PAGE_H },
          margin: { top: MARGIN, right: MARGIN, bottom: MARGIN, left: MARGIN },
        },
      },
      footers: {
        default: new Footer({
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              indent: { firstLine: 0 },
              children: [
                new TextRun({ text: '第 ', font: FONT_BODY, size: 20 }),
                new TextRun({ children: [PageNumber.CURRENT], font: FONT_BODY, size: 20 }),
                new TextRun({ text: ' 页', font: FONT_BODY, size: 20 }),
              ],
            }),
          ],
        }),
      },
      children,
    }],
  });

  Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync(outputFile, buffer);
    console.log(`  ✅ Generated: ${outputFile} (${(buffer.length / 1024).toFixed(1)} KB)`);
  }).catch(err => {
    console.error(`  ❌ Error: ${err.message}`);
  });
}

// ────────────────────────────────────────────────────
// Main: Generate all 7 days
// ────────────────────────────────────────────────────

const LOG_DIR = __dirname;
const IMAGES_DIR = path.join(LOG_DIR, 'images');

const days = [
  { num: 1, date: '2026 年 06 月 01 日', file: 'Day1_2026-06-01.md' },
  { num: 2, date: '2026 年 06 月 04 日', file: 'Day2_2026-06-04.md' },
  { num: 3, date: '2026 年 06 月 05 日', file: 'Day3_2026-06-05.md' },
  { num: 4, date: '2026 年 06 月 09 日', file: 'Day4_2026-06-09.md' },
  { num: 5, date: '2026 年 06 月 11 日', file: 'Day5_2026-06-11.md' },
  { num: 6, date: '2026 年 06 月 12 日', file: 'Day6_2026-06-12.md' },
  { num: 7, date: '2026 年 06 月 15 日', file: 'Day7_2026-06-15.md' },
];

console.log('═'.repeat(50));
console.log('  超市管理系统 — 开发工作日志 DOCX 生成器');
console.log('═'.repeat(50));
console.log('');

for (const day of days) {
  const mdFile = path.join(LOG_DIR, day.file);
  const outputFile = path.join(LOG_DIR, day.file.replace('.md', '.docx'));

  if (!fs.existsSync(mdFile)) {
    console.log(`  ⚠️  Skipping Day ${day.num}: ${mdFile} not found`);
    continue;
  }

  generateDayDocx(day.num, day.date, mdFile, outputFile);
}

console.log('');
console.log('═'.repeat(50));
console.log('  全部文档生成完成！请查看 log/ 文件夹');
console.log('═'.repeat(50));
