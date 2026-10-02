package ir.negahjavan.newscard.util

import java.time.LocalDate

object JalaliDate {
    fun today(): String {
        val d = LocalDate.now()
        val (jy, jm, jd) = gregorianToJalali(d.year, d.monthValue, d.dayOfMonth)
        return "%04d/%02d/%02d".format(jy, jm, jd).toPersianDigits()
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDays = intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334)
        var gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gDays[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return Triple(jy, jm, jd)
    }

    private fun String.toPersianDigits(): String = map {
        if (it in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it
    }.joinToString("")
}
