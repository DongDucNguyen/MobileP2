package vn.edu.nhansu.data

/** id là khóa nội bộ, mã nhân sự có thể được chỉnh sửa mà không đổi bản ghi. */
data class Employee(
    val id: Long = 0,
    val code: String,
    val fullName: String,
    val birthYear: Int,
    val gender: String,
    val hometown: String,
    val education: String,
    val photoUri: String = "",
    val salary: Long,
    val department: String,
)
