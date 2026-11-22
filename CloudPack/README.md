# CloudPack

Ресурспак для плагина **TotemRunes / CloudItems** под Minecraft **1.21.11**.

## Что делает

- Все руны получают одну и ту же текстуру `netherite_upgrade_smithing_template.png`.
- Тотем, на который наложена хотя бы одна руна, получает текстуру `totem_of_undying.png`.
- Ванильные шаблоны кузнеца и ванильный тотем **не затрагиваются**: файлы лежат в своём неймспейсе `clouditems`, а не в `minecraft`.
- `pack.png` — иконка ресурспака 128×128 с текстурами руны и тотема, отображается во вкладке ресурспаков.

## Как это работает

Плагин ставит на свои предметы компонент `item_model`:

- руна → `clouditems:rune`
- тотем с рунами → `clouditems:totem`

Ресурспак определяет эти id в `assets/clouditems/items/`, а модели — в `assets/clouditems/models/item/`.
Обычный ванильный тотем и ванильные кузнечные шаблоны компонент `item_model` не получают, поэтому остаются ванильными.

## Установка

1. Соберите плагин: `mvn clean package` (в `target/` появится `CloudItems-1.0.0.jar`).
2. Положите `CloudPack/` в `resourcepacks/` клиента или используйте готовый `CloudPack.zip` (он уже собран в корне проекта) на сервере через `resource-pack` в `server.properties`.
3. Включите ресурспак в клиенте. Без пака кастомные `item_model` будут выглядеть как missing model — это ожидаемо.

## Структура

```
CloudPack/
├─ pack.mcmeta
├─ pack.png
└─ assets/clouditems/
   ├─ items/
   │  ├─ rune.json
   │  └─ totem.json
   ├─ models/item/
   │  ├─ rune.json
   │  └─ totem.json
   └─ textures/item/
      ├─ netherite_upgrade_smithing_template.png
      └─ totem_of_undying.png
```
