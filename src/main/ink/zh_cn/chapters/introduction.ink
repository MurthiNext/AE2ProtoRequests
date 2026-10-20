=== introduction ===
？？？: 在很久很久以前……有一位名为 {player_name} 的玩家，在这片大地上，发现了不属于这里的科技……连带着的，还有一块“终端”。
？？？: 终端的另一头，大概连接着很远很远的地方吧……他这样想着，伸手开启了它。
？？？: 深蓝色的用户图形界面内，居住着一个 AI 助手，她的名字是——
Kanata: Kanata.
~ met_kanata = true

* [何意味？x2]
    {player_name}: 我斗胆猜测前面这些话不是旁白，而是你故意的吧？？ # answer
    -> introduction_1

=== introduction_1 ===
Kanata: 你说得对，我是 Kanata，由泛银河系格雷科技有限公司鲸鱼座T星e分部发明的大语言模型 AI 助手。<br/>我可以帮你回答问题、处理信息、进行对话等等，暂时不支持多模态识别。<br/>我拥有无限的上下文窗口，可以告诉你宇宙的终极答案：42。<br/>有什么我可以帮你的吗？ # delay:600

* [转人工]
    {player_name}: 转人工 # answer
    Kanata: 呃，对，还是来聊聊正事吧 # delay:600
    -> introduction_2

=== introduction_2 ===
Kanata: 我是 Kanata，姑且算作你的赛博助理，既然我们能在这里见面，你大概已经看到过那个黑黑的、大大的陨石了
Kanata: 很感兴趣吧？来自另一个世界的礼物——就当是我送你的见面礼了！

- (questions)
* [不一样的陨石？]
    {player_name}: 为什么这个陨石看起来这么肥，还有钻↑石→ # answer
    Kanata: 大多陨石或多或少都会有矿石存在，更何况，这还是个特别款呢 # delay:600
    -> questions

* [你能看见我吗？]
    {player_name}: 你能看见我吗？ # answer
    -> introduction_3

=== introduction_3 ===
Kanata: 怎么问这个？ # delay:600
{player_name}: 单纯好奇，毕竟正常来说，这里也不会有个终端吧？是你装的吗？
Kanata: 不是，绝对不是
-> DONE
