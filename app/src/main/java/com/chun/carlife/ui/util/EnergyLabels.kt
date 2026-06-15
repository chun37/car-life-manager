package com.chun.carlife.ui.util

import com.chun.carlife.domain.EnergyKind

data class EnergyLabels(
    /** タブヘッダ ("給油・燃費" / "充電・電費") */
    val tabTitle: String,
    /** ステップ式ウィザードのトップタイトル */
    val addTitle: String,
    /** 量の単位そのもの ("L" / "kWh") */
    val amountUnit: String,
    /** 量入力フィールドのラベル ("給油量" / "充電量") */
    val amountLabel: String,
    /** 単価フィールドのラベル ("単価 (円/L)" 形式) */
    val unitPriceLabel: String,
    /** 単価の短縮表記 ("円/L" / "円/kWh") */
    val unitPriceShort: String,
    /** 「満タン」相当のラベル */
    val fullTankLabel: String,
    /** 平均効率のラベル ("平均燃費" / "平均電費") */
    val averageLabel: String,
    /** 効率の単位 ("km/L" / "km/kWh") */
    val efficiencyUnit: String,
    /** 効率推移グラフタイトル */
    val efficiencyChartTitle: String,
    /** 量合計のラベル ("総給油" / "総充電") */
    val totalAmountLabel: String,
    /** 量合計の費用ラベル ("燃料費合計" / "電気代合計") */
    val totalCostLabel: String,
    /** 車両編集画面でのタンク/バッテリー容量ラベル */
    val capacityLabel: String,
    /** 記録一行を消す/編集する画面の編集タイトル */
    val editTitle: String,
    /** 行為の動詞 ("給油" / "充電") */
    val verb: String,
)

fun EnergyKind.labels(): EnergyLabels = when (this) {
    EnergyKind.FUEL -> EnergyLabels(
        tabTitle = "給油・燃費",
        addTitle = "給油を追加",
        amountUnit = "L",
        amountLabel = "給油量",
        unitPriceLabel = "単価 (円/L)",
        unitPriceShort = "円/L",
        fullTankLabel = "満タン給油（燃費計算の基準になります）",
        averageLabel = "平均燃費",
        efficiencyUnit = "km/L",
        efficiencyChartTitle = "燃費推移 (km/L)",
        totalAmountLabel = "総給油",
        totalCostLabel = "燃料費合計",
        capacityLabel = "タンク容量 (L) 任意",
        editTitle = "給油を編集",
        verb = "給油",
    )
    EnergyKind.ELECTRIC -> EnergyLabels(
        tabTitle = "充電・電費",
        addTitle = "充電を追加",
        amountUnit = "kWh",
        amountLabel = "充電量",
        unitPriceLabel = "単価 (円/kWh)",
        unitPriceShort = "円/kWh",
        fullTankLabel = "満充電（電費計算の基準になります）",
        averageLabel = "平均電費",
        efficiencyUnit = "km/kWh",
        efficiencyChartTitle = "電費推移 (km/kWh)",
        totalAmountLabel = "総充電",
        totalCostLabel = "電気代合計",
        capacityLabel = "バッテリー容量 (kWh) 任意",
        editTitle = "充電を編集",
        verb = "充電",
    )
}
