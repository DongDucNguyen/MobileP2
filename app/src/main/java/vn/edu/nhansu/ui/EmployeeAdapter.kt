package vn.edu.nhansu.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import vn.edu.nhansu.R
import vn.edu.nhansu.data.Employee
import vn.edu.nhansu.databinding.ItemEmployeeBinding
import java.text.NumberFormat
import java.util.Locale

class EmployeeAdapter : BaseAdapter() {
    private var employees = emptyList<Employee>()
    private val moneyFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

    fun submitList(items: List<Employee>) {
        employees = items
        notifyDataSetChanged()
    }

    override fun getCount() = employees.size
    override fun getItem(position: Int) = employees[position]
    override fun getItemId(position: Int) = getItem(position).id
    override fun hasStableIds() = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
                it.root.tag = it
            }
        } else convertView.tag as ItemEmployeeBinding
        val employee = getItem(position)
        with(binding) {
            name.text = employee.fullName
            metadata.text = parent.context.getString(R.string.row_metadata, employee.code, employee.birthYear, employee.gender)
            department.text = employee.department
            salary.text = parent.context.getString(R.string.salary_display, moneyFormat.format(employee.salary))
            PhotoLoader.load(photo, employee.photoUri)
        }
        return binding.root
    }
}
