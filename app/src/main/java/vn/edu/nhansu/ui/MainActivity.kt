package vn.edu.nhansu.ui

import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import vn.edu.nhansu.R
import vn.edu.nhansu.data.Employee
import vn.edu.nhansu.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val employeeAdapter = EmployeeAdapter()
    private var activeQuery = ""

    private val editEmployee = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Sau Add/Update hiển thị toàn bộ, kể cả hồ sơ vừa đổi tên/phòng ban.
            activeQuery = ""
            binding.searchInput.text.clear()
            loadEmployees()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        activeQuery = savedInstanceState?.getString(STATE_QUERY).orEmpty()
        binding.employeeList.adapter = employeeAdapter
        binding.searchButton.setOnClickListener { search() }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { search(); true } else false
        }
        binding.searchInput.doAfterTextChanged {
            if (it.isNullOrBlank()) {
                activeQuery = ""
                loadEmployees()
            }
        }
        binding.addButton.setOnClickListener {
            editEmployee.launch(Intent(this, AddActivity::class.java))
        }
        binding.employeeList.setOnItemClickListener { _, _, position, _ ->
            editEmployee.launch(Intent(this, DetailActivity::class.java).putExtra(
                DetailActivity.EXTRA_EMPLOYEE_ID, employeeAdapter.getItem(position).id
            ))
        }
        binding.employeeList.setOnItemLongClickListener { _, _, position, _ ->
            confirmDelete(employeeAdapter.getItem(position))
            true // Không phát sinh thêm sự kiện click mở Detail.
        }
    }

    override fun onResume() {
        super.onResume()
        loadEmployees()
    }

    private fun search() {
        activeQuery = binding.searchInput.text.toString().trim()
        loadEmployees()
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
            .hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        binding.searchInput.clearFocus()
    }

    private fun loadEmployees() {
        try {
            val employees = db.getEmployees(activeQuery)
            employeeAdapter.submitList(employees)
            val searching = activeQuery.isNotBlank()
            binding.countText.text = getString(if (searching) R.string.count_search else R.string.count_all, employees.size)
            binding.emptyState.visibility = if (employees.isEmpty()) View.VISIBLE else View.GONE
            binding.emptyTitle.setText(if (searching) R.string.no_results_title else R.string.empty_title)
            binding.emptyMessage.setText(if (searching) R.string.no_results_message else R.string.empty_message)
        } catch (_: SQLiteException) {
            toast(R.string.database_error)
        }
    }

    private fun confirmDelete(employee: Employee) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, employee.fullName, employee.code))
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                try {
                    toast(if (db.deleteEmployee(employee.id)) R.string.deleted else R.string.employee_missing)
                    loadEmployees()
                } catch (_: SQLiteException) {
                    toast(R.string.database_error)
                }
            }.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_QUERY, activeQuery)
        super.onSaveInstanceState(outState)
    }

    companion object { private const val STATE_QUERY = "active_query" }
}
