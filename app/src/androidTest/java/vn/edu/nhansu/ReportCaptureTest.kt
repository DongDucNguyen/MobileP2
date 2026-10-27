package vn.edu.nhansu

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Description
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.nhansu.data.DBHelper
import vn.edu.nhansu.data.Employee
import vn.edu.nhansu.ui.MainActivity
import java.io.File

/** Chạy riêng khi chụp báo cáo; chỉ tạo/xóa hồ sơ tạm của lượt chạy này. */
@RunWith(AndroidJUnit4::class)
class ReportCaptureTest {
    private val code = "BC${System.nanoTime() % 1_000_000}"
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun captureAllFunctions() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("captureReport") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        DBHelper(context).use { db ->
            val before = db.getEmployees().size
            ActivityScenario.launch(MainActivity::class.java).use {
                try {
                    capture("01-main")
                    search("phan mem")
                    assertTrue(db.getEmployees("phan mem").isNotEmpty())
                    capture("02-search-department")
                    search("khong_ton_tai_311")
                    onView(withId(R.id.emptyTitle)).check(matches(withText(R.string.no_results_title)))
                    capture("03-search-empty")
                    search("")
                    onView(withId(R.id.countText)).check(matches(withText("TẤT CẢ NHÂN SỰ · $before")))

                    onView(withId(R.id.addButton)).perform(click())
                    field(R.id.codeInput, "NHAPTHU")
                    field(R.id.nameInput, "Dữ liệu nhập thử")
                    field(R.id.yearInput, "1998")
                    select(R.id.genderSpinner, "Nam")
                    field(R.id.hometownInput, "Hà Nội")
                    field(R.id.educationInput, "Đại học")
                    select(R.id.departmentSpinner, "Kỹ thuật")
                    field(R.id.salaryInput, "15000000")
                    top()
                    capture("04-before-refresh")
                    onView(withId(R.id.secondaryButton)).perform(click())
                    for (id in listOf(R.id.codeInput, R.id.nameInput, R.id.yearInput, R.id.hometownInput, R.id.educationInput, R.id.salaryInput)) {
                        onView(withId(id)).check(matches(withText("")))
                    }
                    capture("05-after-refresh")
                    onView(withId(R.id.saveButton)).perform(click())
                    onView(withId(R.id.codeInput)).check(matches(hasErrorText("Vui lòng nhập thông tin này")))
                    capture("06-validation")

                    // Chọn ảnh thật qua DocumentsUI từ ảnh minh họa đã đặt trong Download.
                    field(R.id.codeInput, code)
                    top()
                    onView(withId(R.id.choosePhotoButton)).perform(object : androidx.test.espresso.ViewAction {
                        override fun getConstraints() = isDisplayed()
                        override fun getDescription() = "Mở trình chọn ảnh từ nút Chọn ảnh"
                        override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                            check(view.performClick())
                            controller.loopMainThreadUntilIdle()
                        }
                    })
                    SystemClock.sleep(1500)
                    capture("07-photo-picker")
                    chooseDocument("bao-cao-avatar.jpg")
                    onView(withId(R.id.removePhotoButton)).check(matches(isDisplayed()))
                    field(R.id.codeInput, code)
                    field(R.id.nameInput, "Nguyễn Minh Khoa")
                    field(R.id.yearInput, "1998")
                    select(R.id.genderSpinner, "Nam")
                    field(R.id.hometownInput, "Hà Nội")
                    field(R.id.educationInput, "Đại học")
                    select(R.id.departmentSpinner, "Phần mềm")
                    field(R.id.salaryInput, "15000000")
                    capture("09-add-work")
                    top()
                    capture("08-add-personal")
                    onView(withId(R.id.saveButton)).perform(click())
                    assertEquals(before + 1, db.getEmployees().size)
                    val added = db.getEmployees(code).single()
                    assertTrue(added.photoUri.isNotBlank())
                    context.contentResolver.openInputStream(android.net.Uri.parse(added.photoUri))!!.use { input ->
                        assertNotNull(android.graphics.BitmapFactory.decodeStream(input))
                    }
                    search(code)
                    capture("10-added-result")

                    row().perform(click())
                    capture("11-detail")
                    field(R.id.nameInput, "Nội dung chưa lưu")
                    top()
                    capture("12-before-cancel")
                    onView(withId(R.id.secondaryButton)).perform(click())
                    assertEquals("Nguyễn Minh Khoa", db.getEmployee(added.id)?.fullName)
                    capture("13-after-cancel")

                    row().perform(click())
                    select(R.id.departmentSpinner, "Dự án")
                    field(R.id.salaryInput, "22000000")
                    capture("14-before-update")
                    onView(withId(R.id.saveButton)).perform(click())
                    assertEquals(22000000L, db.getEmployee(added.id)?.salary)
                    assertEquals("Dự án", db.getEmployee(added.id)?.department)
                    search(code)
                    capture("15-after-update")

                    row().perform(longClick())
                    capture("16-delete-confirm")
                    onView(withText(R.string.no)).perform(click())
                    assertNotNull(db.getEmployee(added.id))
                    capture("17-delete-no")
                    row().perform(longClick())
                    onView(withText(R.string.yes)).perform(click())
                    assertNull(db.getEmployee(added.id))
                    capture("18-delete-yes")
                    search("")
                    assertEquals(before, db.getEmployees().size)
                    capture("19-final-main")
                } finally {
                    db.getEmployees(code).filter { employee -> employee.code == code }.forEach { employee -> db.deleteEmployee(employee.id) }
                }
            }
        }
    }

    private fun search(value: String) {
        onView(withId(R.id.searchInput)).perform(replaceText(value), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())
    }

    private fun field(id: Int, value: String) {
        onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
    }

    private fun select(id: Int, value: String) {
        onView(withId(id)).perform(scrollTo(), click())
        onData(equalTo(value)).inRoot(isDialog()).perform(click())
    }

    private fun top() {
        onView(withId(R.id.formScroll)).perform(swipeDown(), swipeDown(), closeSoftKeyboard())
        SystemClock.sleep(700)
    }

    private fun row() = onData(object : BoundedMatcher<Any, Employee>(Employee::class.java) {
        override fun matchesSafely(item: Employee) = item.code == code
        override fun describeTo(description: Description) { description.appendText("report employee $code") }
    }).inAdapterView(withId(R.id.employeeList))

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(2600)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "report").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun chooseDocument(filename: String) {
        fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
            if (root == null) return emptyList()
            return listOf(root) + (0 until root.childCount).flatMap { nodes(root.getChild(it)) }
        }
        fun clickNode(node: AccessibilityNodeInfo) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            check(!bounds.isEmpty)
            // DocumentsUI nằm ngoài tiến trình ứng dụng; dùng thao tác input của hệ thống.
            instrumentation.uiAutomation.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}").use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
            SystemClock.sleep(1200)
        }
        for (attempt in 0 until 8) {
            val visible = nodes(instrumentation.uiAutomation.rootInActiveWindow)
            val image = visible.firstOrNull { it.text?.toString()?.contains(filename) == true || it.contentDescription?.toString()?.contains(filename) == true }
            if (image != null) { clickNode(image); return }
            val downloads = visible.firstOrNull { it.text?.toString() in listOf("Downloads", "Tải xuống") }
            if (downloads != null) { clickNode(downloads); continue }
            val menu = visible.firstOrNull { it.contentDescription?.toString() in listOf("Show roots", "Hiển thị thư mục gốc", "Open navigation drawer") }
            if (menu != null) clickNode(menu) else SystemClock.sleep(1000)
        }
        error("Không tìm thấy ảnh trong trình chọn: " + nodes(instrumentation.uiAutomation.rootInActiveWindow).joinToString { "${it.text}/${it.contentDescription}" })
    }
}
