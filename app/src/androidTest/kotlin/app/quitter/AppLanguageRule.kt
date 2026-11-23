package app.quitter

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
    val pkg = instrumentation.targetContext.packageName
    instrumentation.uiAutomation.executeShellCommand("cmd locale set-app-locales $pkg --locales $tags").close()
    instrumentation.waitForIdleSync()
  }
}
