package vn.edu.nhansu.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Toàn bộ thao tác SQLite được đặt ở đây để Activity chỉ xử lý giao diện. */
class DBHelper(context: Context, databaseName: String = DATABASE_NAME) :
    SQLiteOpenHelper(context.applicationContext, databaseName, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE employees (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(code)) > 0),
                full_name TEXT NOT NULL CHECK(length(trim(full_name)) > 0),
                birth_year INTEGER NOT NULL CHECK(birth_year >= 1900),
                gender TEXT NOT NULL CHECK(gender IN ('Nam', 'Nữ', 'Khác')),
                hometown TEXT NOT NULL,
                education TEXT NOT NULL,
                photo_uri TEXT NOT NULL DEFAULT '',
                salary INTEGER NOT NULL CHECK(salary >= 0),
                department TEXT NOT NULL CHECK(department IN ('Hành chính', 'Phần mềm', 'Kỹ thuật', 'Dự án')),
                search_text TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 chưa có migration. Khi tăng version, thêm ALTER TABLE ở đây;
        // không DROP TABLE để tránh mất hồ sơ người dùng.
        error("Chưa có migration từ $oldVersion lên $newVersion")
    }

    fun getEmployees(keyword: String = ""): List<Employee> {
        val key = EmployeeRules.searchKey(keyword)
        // instr + selectionArgs coi %, _, dấu nháy là ký tự thường; không nối SQL.
        val selection = if (key.isEmpty()) null else "instr(search_text, ?) > 0"
        val args = if (key.isEmpty()) null else arrayOf(key)
        return readableDatabase.query(
            TABLE, null, selection, args, null, null, "full_name COLLATE LOCALIZED ASC, _id ASC"
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toEmployee()) } }
    }

    fun getEmployee(id: Long): Employee? = readableDatabase.query(
        TABLE, null, "_id = ?", arrayOf(id.toString()), null, null, null
    ).use { if (it.moveToFirst()) it.toEmployee() else null }

    fun codeExists(code: String, excludedId: Long = 0): Boolean = readableDatabase.query(
        TABLE, arrayOf("_id"), "code = ? COLLATE NOCASE AND _id != ?",
        arrayOf(code.trim(), excludedId.toString()), null, null, null, "1"
    ).use { it.moveToFirst() }

    fun insertEmployee(employee: Employee): Long =
        writableDatabase.insertOrThrow(TABLE, null, employee.toValues())

    fun updateEmployee(employee: Employee): Boolean = writableDatabase.update(
        TABLE, employee.toValues(), "_id = ?", arrayOf(employee.id.toString())
    ) == 1

    fun deleteEmployee(id: Long): Boolean =
        writableDatabase.delete(TABLE, "_id = ?", arrayOf(id.toString())) == 1

    private fun Employee.toValues() = ContentValues().apply {
        put("code", code.trim())
        put("full_name", fullName.trim())
        put("birth_year", birthYear)
        put("gender", gender)
        put("hometown", hometown.trim())
        put("education", education.trim())
        put("photo_uri", photoUri)
        put("salary", salary)
        put("department", department)
        put("search_text", EmployeeRules.searchKey("$code $fullName $hometown $education $department"))
    }

    private fun Cursor.toEmployee() = Employee(
        id = getLong(getColumnIndexOrThrow("_id")),
        code = getString(getColumnIndexOrThrow("code")),
        fullName = getString(getColumnIndexOrThrow("full_name")),
        birthYear = getInt(getColumnIndexOrThrow("birth_year")),
        gender = getString(getColumnIndexOrThrow("gender")),
        hometown = getString(getColumnIndexOrThrow("hometown")),
        education = getString(getColumnIndexOrThrow("education")),
        photoUri = getString(getColumnIndexOrThrow("photo_uri")),
        salary = getLong(getColumnIndexOrThrow("salary")),
        department = getString(getColumnIndexOrThrow("department")),
    )

    companion object {
        const val DATABASE_NAME = "nhansu.db"
        const val DATABASE_VERSION = 1
        private const val TABLE = "employees"
    }
}
