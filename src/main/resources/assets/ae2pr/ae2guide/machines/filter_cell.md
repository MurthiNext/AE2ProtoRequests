---
navigation:
  title: 过滤元件
  parent: machines/machines_index.md
  icon: ae2pr:filter_cell
  position: 1
item_ids:
  - ae2pr:filter_cell
  - ae2pr:advanced_filter_cell
---

# 过滤元件

<Row>
<ItemImage id="ae2pr:filter_cell" scale="3" />
<ItemImage id="ae2pr:advanced_filter_cell" scale="3" />
</Row>

<ItemLink id="ae2pr:filter_cell" />需要在<ItemLink id="ae2:cell_workbench" />中配置过滤项（最多 63 项，支持物品/流体等任意 AE 键），可安装模糊卡。

<ItemLink id="ae2pr:advanced_filter_cell" />取消物品配置，并额外支持更高级的标签表达式匹配。

## 标签表达式

文本框内填写的是**物品标签**（item tag）的 id，例如 `minecraft:logs`、`forge:ingots/iron`。多个标签可用下列运算符组合：

- `&`：与，两侧都匹配；
- `|`：或，任一侧匹配；
- `^`：异或，仅一侧匹配；
- `!`：取反；
- `()`：分组，改变优先级。
- `*`：通配，例如 `forge:ores/*`。

例如白名单 `a | (b & c)` 表示"匹配标签 a"或"同时匹配标签 b 与 c"。

## 与发信器配合

将过滤元件插入<ItemLink id="ae2pr:multi_level_emitter" />或<ItemLink id="ae2pr:multi_threshold_level_emitter" />的过滤槽后，发信器会遍历网络中所有通过匹配的物品种类，按各自的 AND/OR 组合模式统计或判定。