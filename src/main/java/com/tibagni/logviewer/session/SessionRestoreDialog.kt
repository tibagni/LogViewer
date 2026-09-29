package com.tibagni.logviewer.session

import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

class SessionRestoreDialog(owner: JFrame?, private val sessionData: SessionData) : JDialog(owner), ButtonsPane.Listener {
    private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this)
    private val contentPane = JPanel()
    
    var shouldRestore: Boolean = false
        private set

    init {
        buildUi()
        setContentPane(contentPane)
        isModal = true
        title = "Restore Session"
        buttonsPane.setOkText("Restore Session")
        buttonsPane.setCancelText("Discard Session")
        buttonsPane.setDefaultButtonOk()
    }

    private fun buildUi() {
        contentPane.layout = GridBagLayout()
        contentPane.border = BorderFactory.createEmptyBorder(
            UIScaleUtils.dip(15), UIScaleUtils.dip(15),
            UIScaleUtils.dip(15), UIScaleUtils.dip(15)
        )

        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        val dateString = formatter.format(Date(sessionData.timestamp))
        
        infoPanel.add(JLabel("<html><body><h3>Restore Previous Session?</h3></body></html>"))
        infoPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(8))))
        
        if (!sessionData.cleanExit) {
            val crashWarning = JLabel("<html><body style='color:#E53935;'><b>Notice:</b> The previous session did not exit cleanly. You can restore your open workspace below.</body></html>")
            infoPanel.add(crashWarning)
            infoPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(10))))
        }
        
        infoPanel.add(JLabel("<html><b>Last active:</b> $dateString</html>"))
        infoPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(4))))
        infoPanel.add(JLabel("<html><b>Open log files:</b> ${sessionData.logFiles.size}</html>"))
        infoPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(4))))
        infoPanel.add(JLabel("<html><b>Filter groups:</b> ${sessionData.filterFiles.size}</html>"))
        infoPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(12))))

        contentPane.add(
            infoPanel,
            GBConstraintsBuilder()
                .withGridx(0).withGridy(0)
                .withWeightx(1.0).withWeighty(1.0)
                .withFill(GridBagConstraints.BOTH)
                .build()
        )

        contentPane.add(
            buttonsPane,
            GBConstraintsBuilder()
                .withGridx(0).withGridy(1)
                .withWeightx(1.0)
                .withFill(GridBagConstraints.HORIZONTAL)
                .build()
        )
    }

    override fun onOk() {
        shouldRestore = true
        dispose()
    }

    override fun onCancel() {
        shouldRestore = false
        dispose()
    }

    companion object {
        fun showDialog(parent: JFrame?, sessionData: SessionData): Boolean {
            val dialog = SessionRestoreDialog(parent, sessionData)
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.isVisible = true
            return dialog.shouldRestore
        }
    }
}