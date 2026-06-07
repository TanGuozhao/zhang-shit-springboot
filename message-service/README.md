# message-service

`message-service` 是消息能力服务，负责模板、变量、发送、调度、重试、收件箱与消息统计。

## Boundary

- 负责消息模板和变量定义。
- 负责发送、草稿、调度、执行、重试与记录查询。
- 负责收件箱和基础统计能力。
- 不负责统一用户认证入口。
- 不负责跨服务编排与对外网关。
- 不直接依赖其他底层业务服务。

## Start

```powershell
.\mvnw.cmd -pl message-service -am spring-boot:run
```

- 默认端口：`8082`

## Example Requests

发送消息：

```bash
curl -X POST http://localhost:8082/api/messages/send ^
  -H "Content-Type: application/json" ^
  -d "{\"templateCode\":\"NOTICE\",\"receivers\":[\"u1001\"],\"variables\":{\"name\":\"demo\"}}"
```

模板预览：

```bash
curl -X POST http://localhost:8082/api/messages/templates/NOTICE/preview ^
  -H "Content-Type: application/json" ^
  -d "{\"variables\":{\"name\":\"demo\"}}"
```

查询收件箱：

```bash
curl http://localhost:8082/api/messages/inbox
```

## Placeholder And Replaceable Parts

- 当前没有真实短信、邮件、站内信供应商接入，属于可复用消息骨架。
- 调度与重试是服务内模板流程，后续可对接 MQ、Quartz、XXL-JOB 或云调度平台。
- 渠道账号和发送执行器后续可按业务场景替换扩展。

## Collaboration Rule

- 如果变更模板、变量、发送 DTO，先更新契约文档再改代码。
- 不要在这里接入用户域权限控制总入口，这部分由 `topbiz-service` 统一承接。
