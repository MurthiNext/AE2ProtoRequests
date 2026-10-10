# src/requester — ME 红石请求器独立源码集

本目录是“ME 红石请求器”功能的独立源码集，单独编译后并入主 jar。

## 许可范围

- `java/com/murthinext/ae2pr/requester/` 下的**派生代码**沿用了
  [ME Requester](https://github.com/AlmostReliable/merequester)（LGPL-3.0），
  每个文件头部带 `SPDX-License-Identifier: LGPL-3.0-only`；
  沿用范围与修改说明见仓库根 `licenses/ME-Requester-NOTICE.txt`，
  许可证全文见 `licenses/LGPL-3.0.txt`。
- `java/com/murthinext/ae2pr/requester/setup/` 下的注册/初始化代码为本项目原创（MIT）。

## 结构

| 目录 | 内容 |
| --- | --- |
| `requester/` | 方块、方块实体、菜单、数据模型（Requests 等）与工具 |
| `requester/abstraction/` | 菜单抽象层 |
| `requester/platform/`、`requester/network/`、`requester/status/` | 平台/网络/状态支持 |
| `requester/client/` | 客户端界面与控件（仅客户端加载） |
| `requester/compat/` | 与 ME Requester 的兼容 mixin（soft-dep） |
| `requester/mixin/` | 被本功能使用的 Mixin 访问器 |
| `requester/setup/` | 注册与初始化胶水（原创） |
| `resources/` | 本源码集的 mixin 配置 |

## 构建

`build.gradle` 中定义了 `requester` source set：编译依赖主源码集，
产物（含 mixin 配置与 refmap）随主 jar 一并分发；许可证文件经
`processResources` 注入 `META-INF/licenses/`。
