package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Разбор рук", appName)
    
    val bg = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
    org.junit.Assert.assertNotNull(bg)
    val fg = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
    org.junit.Assert.assertNotNull(fg)
  }

  @Test
  fun `launch main activity`() {
    androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        org.junit.Assert.assertNotNull(activity)
      }
    }
  }
}
