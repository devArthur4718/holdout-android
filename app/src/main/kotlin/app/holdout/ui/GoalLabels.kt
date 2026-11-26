package app.holdout.ui

import android.content.res.Resources
import app.holdout.R
import app.holdout.model.GoalLabel
import app.holdout.model.PersonalGoal

/** The one place a goal is named, in the app's language: "1 week", "30 days", "6 months", "1½ years". */
fun Resources.goalLabel(days: Int): String = when (val label = app.holdout.model.goalLabel(days)) {
  is GoalLabel.Weeks -> getQuantityString(R.plurals.weeks, label.count, label.count)
  is GoalLabel.Days -> getQuantityString(R.plurals.days, label.count, label.count)
  is GoalLabel.Months -> getQuantityString(R.plurals.months, label.count, label.count)
  GoalLabel.YearAndAHalf -> getString(R.string.goal_year_and_half)
  is GoalLabel.Years -> getQuantityString(R.plurals.years, label.count, label.count)
}

/** A goal as the arc and cards name it: a personal goal's name when it has one, else its day label. */
fun Resources.goalName(days: Int, personal: PersonalGoal?): String = personal?.name ?: goalLabel(days)
