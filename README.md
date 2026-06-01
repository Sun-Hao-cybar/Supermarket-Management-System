项目请求完整流程

1\. 前端/ApiFox 发送 HTTP 请求

&#x20;  ↓

2\. 进入 Controller（@RestController）

&#x20;  接收参数 → 调用 Service

&#x20;  ↓

3\. Service 处理业务

&#x20;  调用 Mapper

&#x20;  ↓

4\. Mapper 接口 → XML 执行 SQL

&#x20;  ↓

5\. 访问 MySQL 数据库

&#x20;  ↓

6\. 数据原路返回

&#x20;  ↓

7\. Controller 返回统一格式 Result

