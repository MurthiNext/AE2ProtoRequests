---
navigation:
  title: 仓室与总线
  parent: multiblocks/multiblocks_index.md
  icon: ae2pr:certus_quartz_crystal_input_hatch
  position: 30
item_ids:
  - ae2pr:me_network_access_hatch
  - ae2pr:certus_quartz_crystal_input_bus
  - ae2pr:certus_quartz_crystal_input_hatch
  - ae2pr:certus_quartz_crystal_output_bus
  - ae2pr:certus_quartz_crystal_output_hatch
  - ae2pr:aev_input_bus
  - ae2pr:aev_input_hatch
  - ae2pr:aev_output_bus
  - ae2pr:aev_output_hatch
  - ae2pr:aev_dual_input_hatch
  - ae2pr:aev_dual_output_hatch
---

# 仓室与总线

<Row>
  <BlockImage id="ae2pr:certus_quartz_crystal_input_bus" scale="3" />
  <BlockImage id="ae2pr:certus_quartz_crystal_input_hatch" scale="3" />
  <BlockImage id="ae2pr:certus_quartz_crystal_output_bus" scale="3" />
  <BlockImage id="ae2pr:certus_quartz_crystal_output_hatch" scale="3" />
</Row>

机器部件（或称仓室）是多方块机器与外界的接口：**总线**搬运物品、**仓**搬运流体、**ME 网络接入仓**连接 ME 网络并为机器提供电力与频道。

把它们放入结构中的对应位置，即可随结构一起成型。

## 总线、仓、总成

| 部件 | 输入 / 输出总线 | 输入 / 输出仓 |
| --- | --- | --- |
| 赛特斯石英水晶系列 | 1 格物品位，单格最高 32K 件 | 1 个流体罐，单罐 16K 桶 |
| §bAEV§r 系列 | 4 格物品位，单格最高 2048 件 | 2 个流体罐，单罐 1024 桶 |

- **输入部件**（输入总线 / 输入仓）：向机器提供原料。
- **输出部件**（输出总线 / 输出仓）：只接受机器产出。

仓室对外暴露标准的物品 / 流体能力，可以使用管道、漏斗等常规物流手段存取。

总成是二者的合成版（只能合成一个系列的仓室），容量与类型也是它们的和，没有折损。

## 自动搬运

每个仓室都有自动搬运的开关，可以主动输入输出，以避免到处拉管道的情况发生。

自动搬运只与**朝向面**的容器交互，可以用扳手旋转部件来改变朝向。

## ME 网络接入仓

<BlockImage id="ae2pr:me_network_access_hatch" scale="3" />

将接入仓安装至成型结构，并从其朝向面连接 ME 线缆，即可让主机接入网络。

主机默认占用 **1 频道**。

实际分配受接入线缆及沿途网络的剩余频道限制：若最多只能分配 6 频道，机器就不可能获得超过 6 频道；频道不足、断线或网络断电时，机器暂停工作，网络恢复后继续。

每台多方块机器最多安装 **1 个**接入仓，多装会导致结构无法成型。
