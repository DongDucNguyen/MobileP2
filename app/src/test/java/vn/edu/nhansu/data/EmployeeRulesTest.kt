package vn.edu.nhansu.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Year

class EmployeeRulesTest {
    @Test fun vietnameseSearchIgnoresAccentsCaseAndExtraSpaces() {
        assertEquals("dong duc nguyen", EmployeeRules.searchKey("  ĐÔNG   ĐỨC NGUYÊN "))
        assertEquals("phan mem", EmployeeRules.searchKey("Phần mềm"))
    }

    @Test fun salaryIsExactAndRejectsInvalidOrOverflowValues() {
        assertEquals(15_000_000L, EmployeeRules.parseSalary("15000000"))
        assertEquals(0L, EmployeeRules.parseSalary("0"))
        assertEquals(Long.MAX_VALUE, EmployeeRules.parseSalary(Long.MAX_VALUE.toString()))
        for (invalid in listOf("", "-1", "1.5", "NaN", "15,000,000", "9223372036854775808")) {
            assertNull(invalid, EmployeeRules.parseSalary(invalid))
        }
    }

    @Test fun birthYearMustBeWithinSupportedRange() {
        assertTrue(EmployeeRules.validBirthYear(1900))
        assertTrue(EmployeeRules.validBirthYear(Year.now().value))
        assertFalse(EmployeeRules.validBirthYear(1899))
        assertFalse(EmployeeRules.validBirthYear(Year.now().value + 1))
    }
}
