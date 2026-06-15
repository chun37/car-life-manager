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
- 燃費計算は `domain/FuelEconomy.kt` に UI 非依存で切り出し。テスト容易性のためにこの分離を維持すること

### データモデル

`Vehicle 1 — N Refuel / Maintenance`。外部キーは `onDelete = CASCADE`。スキーマ変更時は `AppDatabase.version` を上げ、開発段階を抜けたら `fallbackToDestructiveMigration` を外して Migration を書くこと。

### 燃費ロジックの不変条件

`FuelEconomy.computeStats` は給油リストを **古い順** で受け取る前提。リポジトリは新しい順で返すので、画面側で `asReversed()` してから渡している (`RefuelScreen.kt`, `StatsScreen.kt`)。順序を変える場合はこの境界を明示的にハンドリングすること。

満タンフラグの扱い:
- 満タン → 満タン区間で `区間距離 ÷ 区間内給油量合計` を算出
- 途中の半タン給油は次の満タンまで `litersAccum` に蓄積される
- 各区間の先頭給油 (基準点) と、満タンで閉じきれていない区間は `kmPerLiter = null`

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
├── data/        Entity, DAO, AppDatabase
├── domain/      UI 非依存のロジック (FuelEconomy)
└── ui/
    ├── theme/   Material3 テーマ
    ├── util/    フォーマッタ, VehiclePicker, SelectedVehicleStore, rememberDatabase
    ├── vehicles/, refuel/, maintenance/, stats/
    └── AppRoot.kt
```
