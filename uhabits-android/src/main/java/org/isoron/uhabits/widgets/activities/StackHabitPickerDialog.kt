/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.widgets.activities

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.preferences.WidgetPreferences
import org.isoron.uhabits.widgets.WidgetUpdater

class StackHabitPickerDialog : Activity() {

    private var widgetId = INVALID_APPWIDGET_ID
    private lateinit var widgetPreferences: WidgetPreferences
    private lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = (applicationContext as HabitsApplication).component
        AndroidThemeSwitcher(this, component.preferences).apply()
        val habitList = component.habitList
        widgetPreferences = component.widgetPreferences
        widgetUpdater = component.widgetUpdater

        widgetId = intent.extras?.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID)
            ?: INVALID_APPWIDGET_ID

        // Build habit list in global position order, skipping archived habits
        val habitIds = ArrayList<Long>()
        val habitNames = ArrayList<String>()
        for (h in habitList) {
            if (h.isArchived) continue
            habitIds.add(h.id!!)
            habitNames.add(h.name)
        }

        if (habitIds.isEmpty()) {
            Toast.makeText(this, R.string.no_habits, Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Pre-check habits that were previously configured for this widget
        val previousIds = widgetPreferences.getHabitIdsFromWidgetId(widgetId).toHashSet()
        val checkedItems = BooleanArray(habitIds.size) { habitIds[it] in previousIds }
        val tempChecked = checkedItems.copyOf()

        AlertDialog.Builder(this)
            .setTitle(R.string.select_habits)
            .setMultiChoiceItems(habitNames.toTypedArray(), checkedItems) { _, which, isChecked ->
                tempChecked[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val selectedIds = habitIds.filterIndexed { i, _ -> tempChecked[i] }.toLongArray()
                if (selectedIds.isEmpty()) {
                    Toast.makeText(this, R.string.select_at_least_one_habit, Toast.LENGTH_SHORT)
                        .show()
                    setResult(RESULT_CANCELED)
                } else {
                    confirm(selectedIds)
                }
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setOnCancelListener {
                setResult(RESULT_CANCELED)
                finish()
            }
            .show()
    }

    private fun confirm(selectedIds: LongArray) {
        widgetPreferences.addWidget(widgetId, selectedIds)
        widgetUpdater.updateWidgets()
        setResult(
            RESULT_OK,
            Intent().apply { putExtra(EXTRA_APPWIDGET_ID, widgetId) }
        )
    }
}
