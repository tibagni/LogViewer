package com.tibagni.logviewer.filter

import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.HintTextField
import com.tibagni.logviewer.view.whenTextChanges
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class FiltersSearchPanel(
    private val onSearchQueryChanged: (String) -> Unit,
    private val onCloseRequested: () -> Unit
) : JPanel() {

    val searchField = HintTextField("Search filters...")
    val clearBtn = FlatButton("Clear")
    val closeBtn = FlatButton(StringUtils.DELETE)
    val matchesLabel = JLabel("")

    var currentQuery: String
        get() = searchField.text
        set(value) {
            searchField.text = value
        }

    init {
        layout = GridBagLayout()
        border = BorderFactory.createEmptyBorder(
            UIScaleUtils.dip(2), UIScaleUtils.dip(5),
            UIScaleUtils.dip(4), UIScaleUtils.dip(5)
        )

        closeBtn.toolTipText = "Close search (Escape)"
        clearBtn.toolTipText = "Clear search"

        searchField.whenTextChanges {
            onSearchQueryChanged(searchField.text)
        }

        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    closeSearch()
                }
            }
        })

        clearBtn.addActionListener {
            searchField.text = ""
            searchField.requestFocusInWindow()
        }

        closeBtn.addActionListener {
            closeSearch()
        }

        add(
            searchField,
            GBConstraintsBuilder()
                .withGridx(0).withGridy(0)
                .withWeightx(1.0)
                .withFill(GridBagConstraints.HORIZONTAL)
                .build()
        )

        add(
            clearBtn,
            GBConstraintsBuilder()
                .withGridx(1).withGridy(0)
                .withAnchor(GridBagConstraints.EAST)
                .build()
        )

        add(
            closeBtn,
            GBConstraintsBuilder()
                .withGridx(2).withGridy(0)
                .withAnchor(GridBagConstraints.EAST)
                .build()
        )

        matchesLabel.border = BorderFactory.createEmptyBorder(UIScaleUtils.dip(2), 0, 0, 0)
        add(
            matchesLabel,
            GBConstraintsBuilder()
                .withGridx(0).withGridy(1)
                .withGridWidth(3)
                .withWeightx(1.0)
                .withAnchor(GridBagConstraints.WEST)
                .build()
        )
    }

    fun updateMatchesCount(matches: Int, total: Int, query: String) {
        if (query.isEmpty()) {
            matchesLabel.text = ""
            matchesLabel.isVisible = false
        } else {
            matchesLabel.text = "$matches of $total filter(s) match"
            matchesLabel.isVisible = true
        }
    }

    fun focusSearch() {
        searchField.requestFocusInWindow()
        searchField.selectAll()
    }

    fun closeSearch() {
        searchField.text = ""
        onCloseRequested()
    }
}
