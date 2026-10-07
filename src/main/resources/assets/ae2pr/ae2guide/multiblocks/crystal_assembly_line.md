---
navigation:
  title: 水晶装配线
  parent: multiblocks/multiblocks_index.md
  icon: crystal_assembly_line
  position: 10
item_ids:
  - ae2pr:crystal_assembly_line
---

# 水晶装配线

<BlockImage id="ae2pr:crystal_assembly_line" scale="3" />

水晶装配线是一套十分适合进行精密装配的多方块结构，其结构类似于 GregTech 的装配线。当然，功能也是，它的配方是**有序**的，好消息是：在这里，你不需要研究数据来运行特定的配方。

它可以帮助你统合各种装配配方，以高效并行处理不止于此模组的内容。不过许多配方需要日后做出更高级的控制外壳才能解锁。

机器会按输入总线与输入仓内现有原料的份数自动并行，默认并行数为16，结构每拓展一格额外增加8并行；能量按实际并行数从 ME 网络一次性扣除。

整台机器最多安装1个能源仓，多余的能源仓会导致结构不成型。

<GameScene zoom="3">
  <ImportStructure src="crystal_assembly_line.nbt" />
</GameScene>