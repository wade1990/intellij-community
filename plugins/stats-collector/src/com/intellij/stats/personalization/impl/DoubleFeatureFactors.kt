package com.intellij.stats.personalization.impl

import com.intellij.stats.personalization.*
import com.jetbrains.completion.ranker.features.DoubleFeature
import com.jetbrains.completion.ranker.features.impl.FeatureUtils

/**
 * @author Vitaliy.Bibaev
 */
class DoubleFeatureReader(factor: DailyAggregatedDoubleFactor)
    : UserFactorReaderBase(factor) {
    fun calculateAverageValue(): Double? {
        return FactorsUtil.calculateAverageByAllDays(factor)
    }

    fun min(): Double? {
        return factor.aggregateMin()["min"]
    }

    fun max(): Double? {
        return factor.aggregateMax()["max"]
    }

    fun undefinedRatio(): Double? {
        val sums = factor.aggregateSum()
        val total = sums["count"] ?: return null
        if (total == 0.0) return null

        return sums.getOrDefault(FeatureUtils.UNDEFINED, 0.0) / total
    }
}

class DoubleFeatureUpdater(factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun update(value: Any?) {
        if (value == null) {
            factor.incrementOnToday(FeatureUtils.UNDEFINED)
        } else {
            val doubleValue = value.asDouble()
            factor.updateOnDate(DateUtil.today()) {
                FactorsUtil.updateAverageValue(this, doubleValue)
                compute("max", { _, old -> if (old == null) doubleValue else maxOf(old, doubleValue) })
                compute("min", { _, old -> if (old == null) doubleValue else minOf(old, doubleValue) })
            }
        }
    }

    private fun Any.asDouble(): Double {
        if (this is Number) return this.toDouble()
        return this.toString().toDouble()
    }
}

abstract class DoubleFeatureUserFactorBase(prefix: String, feature: DoubleFeature) :
        UserFactorBase<DoubleFeatureReader>("${prefix}DoubleFeature:${feature.name}$",
                UserFactorDescriptions.doubleFeatureDescriptor(feature))

class AverageDoubleFeatureValue(feature: DoubleFeature) : DoubleFeatureUserFactorBase("avg", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.calculateAverageValue()?.toString()
}

class MinDoubleFeatureValue(feature: DoubleFeature) : DoubleFeatureUserFactorBase("min", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.min()?.toString()
}

class MaxDoubleFeatureValue(feature: DoubleFeature) : DoubleFeatureUserFactorBase("max", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.max()?.toString()
}

class UndefinedDoubleFeatureValueRatio(feature: DoubleFeature) : DoubleFeatureUserFactorBase("undefinedRatio", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.undefinedRatio()?.toString()
}