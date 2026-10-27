package vn.edu.nhansu

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.nhansu.data.DBHelper
import vn.edu.nhansu.data.Employee

@RunWith(AndroidJUnit4::class)
class DBHelperTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "employee-test.db"
    private lateinit var db: DBHelper

    @Before fun setup() {
        context.deleteDatabase(databaseName)
        db = DBHelper(context, databaseName)
    }

    @After fun cleanup() {
        db.close()
        context.deleteDatabase(databaseName)
    }

    private fun employee(code: String = "NS001") = Employee(
        code = code, fullName = "Đông Đức Nguyên", birthYear = 2000, gender = "Nam",
        hometown = "Hà Nội", education = "Đại học", salary = 15000000, department = "Phần mềm"
    )

    @Test fun crudAndPersistenceKeepAllFields() {
        val original = employee().copy(photoUri = "content://test/photo/1")
        val id = db.insertEmployee(original)
        assertEquals(original.copy(id = id), db.getEmployee(id))
        db.close()
        db = DBHelper(context, databaseName)
        assertEquals(original.copy(id = id), db.getEmployee(id))
        val changed = original.copy(id = id, code = "NS002", fullName = "Tên mới", salary = 22000000, department = "Dự án")
        assertTrue(db.updateEmployee(changed))
        assertEquals(changed, db.getEmployee(id))
        assertTrue(db.getEmployees("dong duc").isEmpty())
        assertEquals(listOf(changed), db.getEmployees("ten moi"))
        assertTrue(db.deleteEmployee(id))
        assertNull(db.getEmployee(id))
        assertFalse(db.deleteEmployee(id))
        assertTrue(db.getEmployees().isEmpty())
    }

    @Test fun searchHandlesAccentsEmptyAndLiteralSqlCharacters() {
        db.insertEmployee(employee())
        db.insertEmployee(employee("NS002").copy(fullName = "Lê An", department = "Kỹ thuật"))
        assertEquals(2, db.getEmployees("   ").size)
        assertEquals(1, db.getEmployees("dong duc nguyen").size)
        assertEquals(1, db.getEmployees("PHẦN MỀM").size)
        assertEquals(1, db.getEmployees("NS001").size)
        assertTrue(db.getEmployees("%_'").isEmpty())
        assertTrue(db.getEmployees("' OR 1=1 --").isEmpty())
    }

    @Test fun duplicateCodesCannotOverwriteAnotherEmployee() {
        val id = db.insertEmployee(employee())
        val secondId = db.insertEmployee(employee("NS002"))
        assertTrue(db.codeExists(" ns001 "))
        assertFalse(db.codeExists("NS001", id))
        assertThrows(SQLiteConstraintException::class.java) { db.insertEmployee(employee("ns001")) }
        assertThrows(SQLiteConstraintException::class.java) { db.updateEmployee(employee("NS001").copy(id = secondId)) }
        assertEquals("NS002", db.getEmployee(secondId)?.code)
        assertEquals(2, db.getEmployees().size)
    }
}
