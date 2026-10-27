package vn.edu.nhansu.data

import java.text.Normalizer
import java.time.Year
import java.util.Locale

object EmployeeRules {
    val genders = listOf("Nam", "Nữ", "Khác")
    val departments = listOf("Hành chính", "Phần mềm", "Kỹ thuật", "Dự án")

    /** Chuẩn hóa để tìm kiếm cả tiếng Việt có dấu và không dấu. */
    fun searchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")

    fun validBirthYear(value: Int): Boolean = value in 1900..Year.now().value

    // Lương nhập bằng số nguyên VNĐ, tránh sai số của Float/Double.
    fun parseSalary(value: String): Long? = value.trim()
        .takeIf { it.matches(Regex("[0-9]+")) }
        ?.toLongOrNull()?.takeIf { it >= 0 }
}
