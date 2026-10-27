package vn.edu.nhansu.ui

/** Cancel chỉ đóng Activity; chỉ Update mới ghi thay đổi vào SQLite. */
class DetailActivity : EmployeeFormActivity() {
    override val isEditing = true

    companion object { const val EXTRA_EMPLOYEE_ID = "employee_id" }
}
