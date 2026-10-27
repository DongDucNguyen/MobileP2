package vn.edu.nhansu

import android.content.Context
import android.graphics.Bitmap
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
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.nhansu.data.DBHelper
import vn.edu.nhansu.data.Employee
import vn.edu.nhansu.ui.MainActivity
import java.io.File

@RunWith(AndroidJUnit4::class)
class EmployeeFlowTest {
    private lateinit var db: DBHelper
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val code = "UITEST${System.nanoTime() % 1_000_000}"

    @Before fun setup() {
        db = DBHelper(ApplicationProvider.getApplicationContext<Context>())
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun cleanup() {
        scenario.close()
        db.getEmployees(code).forEach { db.deleteEmployee(it.id) }
        db.close()
    }

    @Test fun addRefreshSearchCancelUpdateAndDelete() {
        onView(withText(R.string.student_identity)).check(matches(isDisplayed()))
        onView(withId(R.id.addButton)).perform(click())
        field(R.id.codeInput, "TEMP")
        onView(withId(R.id.secondaryButton)).perform(click())
        onView(withId(R.id.codeInput)).check(matches(withText("")))
        onView(withId(R.id.saveButton)).perform(click())
        onView(withId(R.id.codeInput)).check(matches(hasErrorText("Vui lòng nhập thông tin này")))
        field(R.id.codeInput, code)
        onView(withId(R.id.codeInput)).check { view, _ ->
            assertNull((view as android.widget.EditText).error)
        }
        field(R.id.nameInput, "Đông Đức Nguyên")
        field(R.id.yearInput, "2000")
        select(R.id.genderSpinner, "Nam")
        field(R.id.hometownInput, "Hà Nội")
        field(R.id.educationInput, "Đại học")
        select(R.id.departmentSpinner, "Phần mềm")
        field(R.id.salaryInput, "15000000")
        onView(withId(R.id.codeInput)).perform(scrollTo(), closeSoftKeyboard())
        onView(withId(R.id.formScroll)).perform(swipeDown(), swipeDown())
        capture("02-add")
        onView(withId(R.id.saveButton)).perform(click())
        assertEquals(1, db.getEmployees(code).size)
        val id = db.getEmployees(code).single().id
        capture("01-main")

        onView(withId(R.id.searchInput)).perform(replaceText(code), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.countText)).check(matches(withText("KẾT QUẢ TÌM KIẾM · 1")))
        row().perform(click())
        capture("03-detail")
        field(R.id.nameInput, "Không lưu")
        onView(withId(R.id.secondaryButton)).perform(click())
        assertEquals("Đông Đức Nguyên", db.getEmployee(id)?.fullName)

        row().perform(click())
        field(R.id.nameInput, "Đã cập nhật")
        field(R.id.salaryInput, "20000000")
        onView(withId(R.id.saveButton)).perform(click())
        assertEquals("Đã cập nhật", db.getEmployee(id)?.fullName)
        assertEquals(20000000L, db.getEmployee(id)?.salary)
        onView(withId(R.id.searchInput)).check(matches(withText("")))

        onView(withId(R.id.searchInput)).perform(replaceText("zz_no_employee_zz"), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.emptyTitle)).check(matches(withText(R.string.no_results_title)))
        onView(withId(R.id.searchInput)).perform(replaceText(""), closeSoftKeyboard())
        row().perform(longClick())
        capture("04-delete")
        onView(withText(R.string.no)).perform(click())
        assertNotNull(db.getEmployee(id))
        row().perform(longClick())
        onView(withText(R.string.yes)).perform(click())
        assertNull(db.getEmployee(id))
    }

    @Test fun searchSurvivesActivityRecreation() {
        // ActivityScenario.recreate trên Main giữ bộ lọc đang áp dụng.
        onView(withId(R.id.searchInput)).perform(replaceText(code), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())
        scenario.recreate()
        onView(withId(R.id.searchInput)).check(matches(withText(code)))
        onView(withId(R.id.countText)).check(matches(withText("KẾT QUẢ TÌM KIẾM · 0")))
    }

    private fun field(id: Int, value: String) {
        onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
    }

    private fun select(id: Int, value: String) {
        onView(withId(id)).perform(scrollTo(), click())
        onData(equalTo(value)).inRoot(isDialog()).perform(click())
    }

    private fun row() = onData(object : BoundedMatcher<Any, Employee>(Employee::class.java) {
        override fun matchesSafely(item: Employee) = item.code == code
        override fun describeTo(description: Description) { description.appendText("employee with code $code") }
    }).inAdapterView(withId(R.id.employeeList))

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Chờ Toast hệ thống biến mất để ảnh QA không che nút ở cuối màn hình.
        android.os.SystemClock.sleep(2500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "qa")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
