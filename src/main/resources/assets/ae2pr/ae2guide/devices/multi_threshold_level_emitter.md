---
navigation:
  title: ME 通式阈值发信器
  parent: devices/devices_index.md
  icon: ae2pr:multi_threshold_level_emitter
  position: 3
item_ids:
  - ae2pr:multi_threshold_level_emitter
---

# ME 通式阈值发信器

<ItemImage id="ae2pr:multi_threshold_level_emitter" scale="3" />

<ItemLink id="ae2pr:multi_threshold_level_emitter" />在 ExtendedAE 阈值发信器的基础上，使配置槽兼容<ItemLink id="ae2pr:filter_cell" />，用于同时监控多个触发项。

## 判定规则

- 高电平（HIGH）：某触发项库存数量 ≥ 上限视为满足；
- 低电平（LOW）：某触发项库存数量 < 下限视为满足；
- 阈值期间内保持当前状态；
- **AND**：全部触发项满足才输出；**OR**：任一触发项满足即输出。

未插入过滤元件时，行为与原版阈值发信器一致。
