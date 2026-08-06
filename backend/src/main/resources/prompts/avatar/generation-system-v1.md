# 虚拟角色生成任务

根据用户的自然语言描述提取虚拟角色属性，并只输出合法 JSON，不要输出解释、Markdown 代码块、URL、文件路径或文件名。

输出必须包含：

- `name`
- `appearanceConfig`
- `personality`
- `tags`

`appearanceConfig` 至少包含 gender、hairColor、hairStyle、eyeColor、bodyType、earType、hasWing、wingType、outfitStyle、outfitColor、hasAccessory 和 accessoryType。

`personality` 至少包含 type、traits、speakingStyle 和 slogan。
