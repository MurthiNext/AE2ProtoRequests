---
navigation:
  title: 水晶装配线
  parent: multiblocks/multiblocks_index.md
  icon: crystal_assembly_line
  position: 10
item_ids:
  - ae2pr:crystal_assembly_line
  - ae2pr:crystal_assembly_line_casing
  - ae2pr:crystal_assembly_line_grating
  - ae2pr:crystal_assembly_line_unit
  - ae2pr:crystal_assembly_line_charging_unit
  - ae2pr:crystal_assembly_line_advanced_charging_unit
  - ae2pr:crystal_assembly_line_parallel_unit
  - ae2pr:crystal_assembly_line_speed_unit
---

# 水晶装配线

<BlockImage id="ae2pr:crystal_assembly_line" scale="3" />

水晶装配线是一套十分适合进行精密装配的多方块结构，其结构类似于 GregTech 的装配线。当然，功能也是，它的配方是**有序**的，好消息是：在这里，你不需要研究数据来运行特定的配方。

它可以帮助你统合各种装配配方，以高效并行处理不止于此模组的内容。许多配方需要安装对应的模块才能解锁。

默认并行数为16，结构每拓展一格额外增加8并行。

## 替换模块

<ItemGrid>
  <ItemIcon id="ae2pr:crystal_assembly_line_unit" />
  <ItemIcon id="ae2pr:crystal_assembly_line_charging_unit" />
  <ItemIcon id="ae2pr:crystal_assembly_line_advanced_charging_unit" />
  <ItemIcon id="ae2pr:crystal_assembly_line_parallel_unit" />
  <ItemIcon id="ae2pr:crystal_assembly_line_speed_unit" />
</ItemGrid>

水晶装配线的所有控制外壳都可以替换为升级模块，模块分为配方类和增益类。

高等级的配方控制外壳可以兼容低等级；**同一种配方模块最多安装一个**，初级与高级混装同样视为重复安装，违规会导致结构不成型。
<GameScene zoom="3">
  <ImportStructure src="crystal_assembly_line.nbt" />
</GameScene>