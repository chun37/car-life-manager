# CLAUDE.md

このファイルは Claude Code (claude.ai/code) がこのリポジトリで作業する際の指針です。

## ビルド・実行

JDK 17 / Gradle 8.10.2 は `.mise.toml` で固定。`mise trust` 済みなら `eval "$(mise env)"` で PATH が通ります。

```bash
# Debug APK
./gradlew :app:assembleDebug
# Lint
./gradlew :app:lintDebug
# 単一クラスの単体テスト (JVM)
./gradlew :app:testDebugUnitTest --tests "com.chun.carlife.domain.FuelEconomyTest"
```

Android SDK は `~/Android/Sdk` を想定。`local.properties` の `sdk.dir` で上書き可。

## アーキテクチャ

シングルActivity + Jetpack Compose + Navigation Compose。状態管理は Composable + `remember` + Room の Flow を `collectAsState` で素直に流し込むだけで、ViewModel / Hilt は使っていません。意図的に薄く保っています。

- `CarLifeApp` が `AppDatabase` を保持するシングルトンの入口
- `ui/util/AppDb.kt` の `rememberDatabase()` で Composable から DB に到達
- 画面間でユーザーが選択中の車両は `ui/util/SelectedVehicleStore` (プロセス内 `mutableStateOf`) で共有
- 燃費計算は `domain/FuelEconomy.kt`、整備周期判定は `domain/MaintenanceSchedule.kt` に UI 非依存で切り出し。テスト容易性のためにこの分離を維持すること

### データモデル

`Vehicle 1 — N (Refuel | Maintenance | ScheduleOverride)`。外部キーは `onDelete = CASCADE`。
`AppDatabase` は **本物の `Migration` を書く運用**に切り替え済み (`fallbackToDestructiveMigration` は使っていない)。スキーマ変更時は `version` を上げ、対応する `Migration` を `addMigrations(...)` に追加する。既存の `MIGRATION_1_2` (schedule_overrides テーブル追加) と `MIGRATION_2_3` (vehicles.energyKind カラム追加・既存行は `FUEL` で埋まる) が雛形。

### 動力源 (ガソリン / EV) の出し分け

`Vehicle.energyKind: String` (`"FUEL"` / `"ELECTRIC"`) を持ち、UI 側は `domain.EnergyKind` enum + `Vehicle.energy` 拡張で扱う。表示の単位やラベル (給油↔充電 / L↔kWh / 円/L↔円/kWh / km/L↔km/kWh / 満タン↔満充電 / 燃料費↔電気代) は **すべて `ui/util/EnergyLabels.kt` の `EnergyLabels` バンドルと `formatAmount` / `formatEfficiency` に集約**。

- `Refuel` テーブルは EV 車両でも再利用：`liters` を kWh、`pricePerLiter` を 円/kWh として保存し、計算ロジック (`FuelEconomy`) は単位を意識しない
- 新しく単位を出す場所を増やすときは `formatAmount(value, kind)` / `formatEfficiency(value, kind)` を使い、Hard-coded な "L" / "km/L" は入れない
- CSV インポートは「給油量 ↔ 充電量」「単価 ↔ kWh単価」「満タン ↔ 満充電」を **エイリアス** として受け入れる (`RefuelCsvImporter.AMOUNT_ALIASES` 等)

### ホーム画面ウィジェット → 給油追加

- `widget/RefuelShortcutWidget.kt` に 1x1 / 2x1 の 2 種類の `AppWidgetProvider` を登録
- ウィジェットタップで `MainActivity` を `ACTION_ADD_REFUEL` 付き explicit intent で起動
- `MainActivity` は受信した action を `MutableStateFlow` に保持 → `AppRoot(pendingAction = ...)` に流し、`LaunchedEffect` で `navigate("refuelAdd/0")` する
- 新しいショートカット系の入口を追加するときも、explicit intent + action 文字列 + AppRoot 側のディスパッチに揃える (deep link XML は使っていない)

### 燃費ロジックの不変条件

`FuelEconomy.computeStats` は給油リストを **古い順** で受け取る前提。リポジトリは新しい順で返すので、画面側で `asReversed()` してから渡している (`RefuelScreen.kt`, `StatsScreen.kt`)。順序を変える場合はこの境界を明示的にハンドリングすること。

満タンフラグの扱い:
- **すべての給油は物理的に満タン入れている前提**。`fullTank=false` は「タンクに残量がある」ではなく
  「**この給油より前の区間に記録忘れがあるかも**」という信頼区間マーカーとして使う
- 区間燃費: `kmpl = (今回ODO − 前回ODO) / 今回の給油量`。先頭給油は基準なので `null`
- `fullTank=false` の行は kmpl を `null` にする (前区間が信頼できないため)。ただしその給油の odometer / liters は正しい想定なので、次の区間の起点には**そのまま**使う
- `summarize` の平均燃費も `fullTank=true` な給油の区間だけを合算 (`Σ距離 / Σ給油量`)

## 守ってほしいこと

- 外部ライブラリは**極力増やさない**。グラフは `Canvas` 自作で済ませているし、依存は Compose / Room / Navigation / Coroutines のみ
- 画面追加時は `AppRoot.kt` の `NavHost` にルートを足し、`tabs` リストに含めるかは設計判断
- ViewModel を導入したくなったら相談してから入れる。現在の薄さは意図したもの
- `compileSdk` / `targetSdk` を上げる際は AGP のバージョンも合わせて確認 (現状 AGP 8.5.2 は compileSdk 34 がテスト上限)

## よくある落とし穴

- `ExposedDropdownMenu` は `ExposedDropdownMenuBoxScope` のメンバー関数。`import` できないので `ExposedDropdownMenuBox` のラムダ内でそのまま呼ぶこと（完全修飾も不可）
- `Modifier.menuAnchor()` は非推奨警告が出るが、Material3 1.3 系では現役。アップグレード時に新シグネチャへ
- Room の DAO は Flow を返す `observe*` と suspend の `get*` / `list*` を分けて命名

## ディレクトリ

```
app/src/main/java/com/chun/carlife/
├── CarLifeApp.kt / MainActivity.kt
├── data/        Entity, DAO, AppDatabase, CsvImport (RefuelCsvImporter)
├── domain/      UI 非依存のロジック (FuelEconomy, MaintenanceSchedule)
├── widget/      ホーム画面ウィジェット (RefuelShortcutWidget 1x1/2x1)
└── ui/
    ├── theme/      Material3 テーマ
    ├── util/       フォーマッタ, VehiclePicker, SelectedVehicleStore, rememberDatabase, SettingsAction
    ├── vehicles/   一覧・編集
    ├── refuel/     一覧・追加 (ウィザード) ・編集
    ├── maintenance/一覧・編集・MaintenancePresets
    ├── stats/      集計・グラフ
    ├── settings/   設定トップ / 一般 / 車両 / 給油 (+CSV インポート) / 整備 / 集計
    └── AppRoot.kt
```

ウィジェット関連リソース:

```
app/src/main/res/
├── layout/widget_refuel_shortcut_{1x1,2x1}.xml
├── xml/refuel_shortcut_widget_info_{1x1,2x1}.xml
└── drawable/ic_widget_refuel.xml, widget_refuel_shortcut_*.xml
```
