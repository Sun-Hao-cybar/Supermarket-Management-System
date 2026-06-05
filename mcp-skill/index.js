const { Server } = require("@modelcontextprotocol/sdk/server/index.js");
const { StdioServerTransport } = require("@modelcontextprotocol/sdk/server/stdio.js");
const mysql = require("mysql2/promise");

// 数据库配置（你的supermarket_db信息）
const dbConfig = {
  host: "localhost",
  port: 3306,
  user: "claude_user",
  password: "Sh241612",
  database: "supermarket_db"
};

// 1.初始化MCP服务
const server = new Server({
  name: "supermarket-skill-mcp",
  version: "1.0.0"
});

// ==========注册自定义Skill（MCP工具）==========
// Skill1: query_supplier → 一键查全量供应商
server.tool(
  "query_supplier",
  "【进销存技能】查询supplier全表数据，无需手写SQL",
  {},
  async () => {
    const conn = await mysql.createConnection(dbConfig);
    const [rows] = await conn.query("SELECT * FROM supplier;");
    await conn.end();
    return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
  }
);

// Skill2: query_goods → 查询商品表
server.tool(
  "query_goods",
  "【进销存技能】查询goods商品全表数据",
  {},
  async () => {
    const conn = await mysql.createConnection(dbConfig);
    const [rows] = await conn.query("SELECT * FROM goods;");
    await conn.end();
    return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
  }
);

// Skill3: gen_java_entity → 根据表名生成SpringBoot实体类
server.tool(
  "gen_java_entity",
  "【进销存技能】输入表名自动生成Entity实体代码",
  { tableName: { type: "string", description: "数据库表名，如supplier" } },
  async ({ tableName }) => {
    const conn = await mysql.createConnection(dbConfig);
    const [cols] = await conn.query(`DESC ${tableName}`);
    await conn.end();
    // 自动拼装实体代码
    let code = `@Data\n@TableName("${tableName}")\npublic class ${tableName[0].toUpperCase()+tableName.slice(1)} {\n`;
    cols.forEach(c=>{
      code += `    private String ${c.Field};\n`;
    })
    code += "}";
    return { content: [{ type: "text", text: code }] };
  }
);

// Skill4: save_doc → 追加内容到项目设计文档
const fs = require("fs");
server.tool(
  "save_doc",
  "【进销存技能】把内容写入项目说明文档.md末尾",
  { content: { type: "string", description: "需要写入的文档内容" } },
  async ({ content }) => {
    fs.appendFileSync("../项目设计说明书.md", "\n"+content);
    return { content: [{ type: "text", text: "文档写入完成" }] };
  }
);

// 绑定stdio通信（Claude通过进程调用MCP）
const transport = new StdioServerTransport();
server.connect(transport);