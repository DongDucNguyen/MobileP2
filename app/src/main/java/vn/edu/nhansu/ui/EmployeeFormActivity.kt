package vn.edu.nhansu.ui

import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import vn.edu.nhansu.R
import vn.edu.nhansu.data.Employee
import vn.edu.nhansu.data.EmployeeRules
import vn.edu.nhansu.databinding.ActivityEmployeeFormBinding

/** Form dùng chung, AddActivity và DetailActivity là hai Activity riêng. */
abstract class EmployeeFormActivity : BaseActivity() {
    protected abstract val isEditing: Boolean
    private lateinit var binding: ActivityEmployeeFormBinding
    private var employeeId = 0L
    private var selectedPhotoUri = ""
    private var saving = false

    private val choosePhoto = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                // Giữ quyền đọc ảnh sau khi đóng/mở ứng dụng, không cần quyền toàn bộ thư viện.
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedPhotoUri = uri.toString()
                showPhoto()
            } catch (_: Exception) {
                toast(R.string.photo_error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        binding.pageTitle.setText(if (isEditing) R.string.detail_title else R.string.add_title)
        binding.pageSubtitle.setText(if (isEditing) R.string.detail_subtitle else R.string.add_subtitle)
        binding.saveButton.setText(if (isEditing) R.string.update else R.string.add)
        binding.secondaryButton.setText(if (isEditing) R.string.cancel else R.string.refresh)
        binding.genderSpinner.adapter = ArrayAdapter(this, R.layout.item_spinner,
            listOf(getString(R.string.gender_prompt)) + EmployeeRules.genders)
        binding.departmentSpinner.adapter = ArrayAdapter(this, R.layout.item_spinner,
            listOf(getString(R.string.department_prompt)) + EmployeeRules.departments)
        with(binding) {
            listOf(codeInput, nameInput, yearInput, hometownInput, educationInput, salaryInput).forEach { field ->
                field.doAfterTextChanged { field.error = null }
            }
        }

        if (isEditing) {
            employeeId = intent.getLongExtra(DetailActivity.EXTRA_EMPLOYEE_ID, 0)
            val employee = try { db.getEmployee(employeeId) } catch (_: SQLiteException) {
                toast(R.string.database_error)
                finish()
                return
            }
            if (employee == null) {
                toast(R.string.employee_missing)
                finish()
                return
            }
            // Android tự khôi phục nội dung các input nếu Activity bị tạo lại.
            if (savedInstanceState == null) populateForm(employee)
        }
        if (savedInstanceState != null) selectedPhotoUri = savedInstanceState.getString(STATE_PHOTO).orEmpty()
        showPhoto()
        binding.backButton.setOnClickListener { finish() }
        binding.choosePhotoButton.setOnClickListener { choosePhoto.launch(arrayOf("image/*")) }
        binding.removePhotoButton.setOnClickListener { selectedPhotoUri = ""; showPhoto() }
        binding.secondaryButton.setOnClickListener {
            if (isEditing) finish() else refreshForm()
        }
        binding.saveButton.setOnClickListener { saveEmployee() }
    }

    private fun populateForm(employee: Employee) = with(binding) {
        codeInput.setText(employee.code)
        nameInput.setText(employee.fullName)
        yearInput.setText(employee.birthYear.toString())
        genderSpinner.setSelection(EmployeeRules.genders.indexOf(employee.gender) + 1)
        hometownInput.setText(employee.hometown)
        educationInput.setText(employee.education)
        salaryInput.setText(employee.salary.toString())
        departmentSpinner.setSelection(EmployeeRules.departments.indexOf(employee.department) + 1)
        selectedPhotoUri = employee.photoUri
    }

    private fun showPhoto() {
        binding.removePhotoButton.visibility = if (selectedPhotoUri.isBlank()) View.GONE else View.VISIBLE
        PhotoLoader.load(binding.photoPreview, selectedPhotoUri) { toast(R.string.photo_error) }
    }

    private fun refreshForm() = with(binding) {
        listOf(codeInput, nameInput, yearInput, hometownInput, educationInput, salaryInput).forEach {
            it.text.clear()
            it.error = null
            it.clearFocus()
        }
        genderSpinner.setSelection(0)
        departmentSpinner.setSelection(0)
        selectedPhotoUri = ""
        showPhoto()
        formScroll.smoothScrollTo(0, 0)
        toast(R.string.refreshed)
    }

    private fun readEmployee(): Employee? = with(binding) {
        val requiredFields = listOf(codeInput, nameInput, yearInput, hometownInput, educationInput, salaryInput)
        requiredFields.forEach { it.error = null }
        val emptyField = requiredFields.firstOrNull { it.text.isNullOrBlank() }
        if (emptyField != null) return invalid(emptyField, R.string.required_error)
        val year = yearInput.text.toString().toIntOrNull()
        if (year == null || !EmployeeRules.validBirthYear(year)) return invalid(yearInput, R.string.year_error)
        val salary = EmployeeRules.parseSalary(salaryInput.text.toString())
            ?: return invalid(salaryInput, R.string.salary_error)
        if (genderSpinner.selectedItemPosition == 0) {
            toast(R.string.gender_error)
            genderSpinner.requestFocus()
            genderSpinner.performClick()
            return null
        }
        if (departmentSpinner.selectedItemPosition == 0) {
            toast(R.string.department_error)
            departmentSpinner.requestFocus()
            departmentSpinner.performClick()
            return null
        }
        Employee(
            id = employeeId,
            code = codeInput.text.toString().trim(),
            fullName = nameInput.text.toString().trim(),
            birthYear = year,
            gender = genderSpinner.selectedItem.toString(),
            hometown = hometownInput.text.toString().trim(),
            education = educationInput.text.toString().trim(),
            photoUri = selectedPhotoUri,
            salary = salary,
            department = departmentSpinner.selectedItem.toString(),
        )
    }

    private fun invalid(field: EditText, message: Int): Employee? {
        field.error = getString(message)
        field.requestFocus()
        return null
    }

    private fun saveEmployee() {
        if (saving) return
        val employee = readEmployee() ?: return
        saving = true
        binding.saveButton.isEnabled = false
        try {
            if (db.codeExists(employee.code, employeeId)) {
                invalid(binding.codeInput, R.string.duplicate_code)
                return
            }
            if (isEditing) {
                if (!db.updateEmployee(employee)) { toast(R.string.employee_missing); return }
            } else {
                db.insertEmployee(employee)
            }
            toast(if (isEditing) R.string.updated else R.string.added)
            setResult(RESULT_OK)
            finish() // Trở lại Main đang có sẵn, không tạo thêm một MainActivity.
        } catch (_: SQLiteConstraintException) {
            invalid(binding.codeInput, R.string.duplicate_code)
        } catch (_: SQLiteException) {
            toast(R.string.database_error)
        } finally {
            saving = false
            binding.saveButton.isEnabled = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_PHOTO, selectedPhotoUri)
        super.onSaveInstanceState(outState)
    }

    companion object { private const val STATE_PHOTO = "selected_photo_uri" }
}
