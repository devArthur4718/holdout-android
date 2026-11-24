package app.quitter

import android.content.res.Resources
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Runs the app in [language] whatever the device's language, and puts it back afterwards.
 * Tests look for English text, so they set this before the app's screens start.
 */
class AppLanguageRule(private val language: String = "en") : TestRule {
  override fun apply(base: Statement, description: Description) = object : Statement() {
    override fun evaluate() {
      setAppLanguage(language)
      try {
        base.evaluate()
      } finally {
        setAppLanguage("")
      }
    }
  }

  private fun setAppLanguage(tags: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val app = instrumentation.targetContext.applicationContext
    // Reading the output to the end waits for the command to finish.
    ParcelFileDescriptor.AutoCloseInputStream(
      instrumentation.uiAutomation.executeShellCommand("cmd locale set-app-locales ${app.packageName} --locales $tags"),
    ).use { it.readBytes() }
    // The new language reaches the app a moment later. Wait for it, or a test on a phone set to
    // another language can start with the old one.
    val expected = tags.ifEmpty { Resources.getSystem().configuration.locales[0].language }.substringBefore('-')
    val deadline = SystemClock.uptimeMillis() + 5_000
    while (app.resources.configuration.locales[0].language != expected && SystemClock.uptimeMillis() < deadline) {
      instrumentation.waitForIdleSync()
      SystemClock.sleep(50)
    }
    instrumentation.waitForIdleSync()
  }
}
