// D:/IProjects/AndroidStudioProjects/MySavings/MySavings/src/main/java/com/endlessyoung/mysavings/data/local/Converters.kt
package com.endlessyoung.mysavings.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String = value.toString()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = BigDecimal(value)
}
