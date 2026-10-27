package vn.edu.nhansu.demo

import android.app.Activity
import android.app.Instrumentation
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import org.json.JSONObject
import vn.edu.nhansu.data.DBHelper
import vn.edu.nhansu.data.Employee
import java.io.File

/** Nạp thủ công bộ dữ liệu debug; không xóa hay ghi đè hồ sơ người dùng. */
class DemoSeedInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        try {
            val appContext = targetContext
            val manifest = appContext.assets.open("demo/employees.json").bufferedReader().use { it.readText() }
            val records = JSONObject(manifest).getJSONArray("employees")
            check(records.length() == 50) { "Cần đúng 50 hồ sơ mẫu" }
            val photoDirectory = File(appContext.filesDir, "demo_portraits").apply { mkdirs() }
            val employees = (0 until records.length()).map { index ->
                val record = records.getJSONObject(index)
                val assetPath = record.getString("photo_asset")
                val photo = File(photoDirectory, File(assetPath).name)
                appContext.assets.open("demo/$assetPath").use { input ->
                    photo.outputStream().use { output -> input.copyTo(output) }
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(photo.absolutePath, bounds)
                check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Ảnh không đọc được: $assetPath" }
                Employee(
                    code = record.getString("code"),
                    fullName = record.getString("full_name"),
                    birthYear = record.getInt("birth_year"),
                    gender = record.getString("gender"),
                    hometown = record.getString("hometown"),
                    education = record.getString("education"),
                    photoUri = Uri.fromFile(photo).toString(),
                    salary = record.getLong("salary"),
                    department = record.getString("department"),
                )
            }
            DBHelper(appContext).use { helper ->
                val database = helper.writableDatabase
                val before = helper.getEmployees().size
                var inserted = 0
                database.beginTransaction()
                try {
                    employees.forEach { employee ->
                        if (!helper.codeExists(employee.code)) {
                            helper.insertEmployee(employee)
                            inserted++
                        }
                    }
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
                val after = helper.getEmployees()
                check(after.size == before + inserted)
                result.putInt("inserted", inserted)
                result.putInt("skipped_existing", employees.size - inserted)
                result.putInt("total", after.size)
                result.putInt("validated_photos", employees.size)
                result.putString("stream", "\nAdded $inserted employees; total ${after.size}; 50 local portraits verified.\n")
            }
            finish(Activity.RESULT_OK, result)
        } catch (error: Exception) {
            result.putString("stream", "\nDemo import failed: ${error.message}\n")
            finish(Activity.RESULT_CANCELED, result)
        }
    }
}
