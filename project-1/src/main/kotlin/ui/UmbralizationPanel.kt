package org.pdi.ui

import org.pdi.core.AppState
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class UmbralizationPanel(
    private val appState: AppState,
    private val onApply: () -> Unit
) : JPanel() {
    private val thresholds = mutableListOf(127)
    private val gradientPanel: JPanel
    private var draggedThresholdIndex: Int = -1
    private val HIT_TOLERANCE = 5

    private val thresholdListModel = DefaultListModel<Int>()
    private val thresholdList = JList(thresholdListModel)

    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Gradient Panel (Center)
        gradientPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D

                // Draw grayscale gradient
                for (i in 0 until width) {
                    val gray = (i.toFloat() / (width - 1) * 255).toInt()
                    g2d.color = Color(gray, gray, gray)
                    g2d.drawLine(i, 0, i, height)
                }

                // Draw threshold lines
                for (threshold in thresholds) {
                    val x = (threshold / 255.0 * (width - 1)).toInt()
                    g2d.color = Color.RED
                    g2d.drawLine(x, 0, x, height)
                    // Draw a handle
                    g2d.color = Color.WHITE
                    g2d.fillOval(x - 3, height / 2 - 3, 6, 6)
                    g2d.color = Color.BLACK
                    g2d.drawOval(x - 3, height / 2 - 3, 6, 6)
                }
            }
        }
        gradientPanel.preferredSize = Dimension(256, 50)

        val mouseAdapter = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                var x = e.x

                if (SwingUtilities.isRightMouseButton(e)) {
                    var thresholdIndexToRemove = findNearbyThreshold(x)
                    if (thresholdIndexToRemove != -1) {
                        thresholds.removeAt(thresholdIndexToRemove)
                        refreshUI()
                    }
                    return
                }

                draggedThresholdIndex = findNearbyThreshold(x)
                if (draggedThresholdIndex != -1) {
                    gradientPanel.cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
                    thresholdList.setSelectedValue(thresholds[draggedThresholdIndex], false)
                }

                if (e.clickCount == 2 && draggedThresholdIndex == -1) {
                    var newThreshold = ((x.toDouble() / (gradientPanel.width - 1)) * 255).toInt().coerceIn(0, 255)
                    if (thresholds.none { kotlin.math.abs(it - newThreshold) < HIT_TOLERANCE * 2 }) {
                        thresholds.add(newThreshold)
                        refreshUI()
                        thresholdList.setSelectedValue(newThreshold, false)
                    }
                }
            }

            override fun mouseDragged(e: MouseEvent) {
                if (draggedThresholdIndex != -1) {
                    var newThreshold = ((e.x.toDouble() / (gradientPanel.width - 1)) * 255).toInt()

                    var lowerBound = if (draggedThresholdIndex > 0) thresholds[draggedThresholdIndex - 1] + 1 else 0
                    var upperBound = if (draggedThresholdIndex < thresholds.size - 1) thresholds[draggedThresholdIndex + 1] - 1 else 255

                    var clampedValue = newThreshold.coerceIn(lowerBound, upperBound)

                    if (thresholds[draggedThresholdIndex] != clampedValue) {
                        thresholds[draggedThresholdIndex] = clampedValue
                        refreshUI()
                        thresholdList.setSelectedValue(clampedValue, false)
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (draggedThresholdIndex != -1) {
                    draggedThresholdIndex = -1
                    gradientPanel.cursor = Cursor.getDefaultCursor()
                }
            }

            private fun findNearbyThreshold(x: Int): Int {
                for ((index, threshold) in thresholds.withIndex()) {
                    val thresholdX = (threshold / 255.0 * (gradientPanel.width - 1)).toInt()
                    if (kotlin.math.abs(x - thresholdX) <= HIT_TOLERANCE) {
                        return index
                    }
                }
                return -1
            }
        }

        gradientPanel.addMouseListener(mouseAdapter)
        gradientPanel.addMouseMotionListener(mouseAdapter)

        // Control Panel (East)
        val listScrollPane = JScrollPane(thresholdList)
        listScrollPane.border = BorderFactory.createTitledBorder("Values")

        val addThresholdButton = JButton("Add").apply {
            addActionListener { addNewThresholdInLargestGap() }
        }

        val removeThresholdButton = JButton("Remove").apply {
            addActionListener {
                var selectedValue = thresholdList.selectedValue
                if (selectedValue != null) {
                    thresholds.remove(selectedValue)
                    refreshUI()
                }
            }
        }

        thresholdList.addListSelectionListener {
            removeThresholdButton.isEnabled = thresholdList.selectedValue != null
        }
        removeThresholdButton.isEnabled = false

        val eastButtonPanel = JPanel(GridLayout(2, 1, 5, 5)).apply {
            add(addThresholdButton)
            add(removeThresholdButton)
        }

        val controlPanel = JPanel(BorderLayout(5, 5)).apply {
            add(listScrollPane, BorderLayout.CENTER)
            add(eastButtonPanel, BorderLayout.SOUTH)
        }

        // Apply Button (South)
        val applyButton = JButton("Apply").apply {
            addActionListener {
                appState.applyThresholding(thresholds)
                onApply()
            }
        }
        val southPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
            add(applyButton)
        }

        // Instructions (North)
        val instructions = JLabel("Drag handles, double-click to add, or right-click to remove.", SwingConstants.CENTER)

        val centerPanel = JPanel(BorderLayout()).apply {
            add(instructions, BorderLayout.NORTH)
            add(gradientPanel, BorderLayout.CENTER)
        }

        add(centerPanel, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.EAST)
        add(southPanel, BorderLayout.SOUTH)

        refreshUI() // Initial UI setup
    }

    private fun refreshUI() {
        thresholds.sort()
        var selected = thresholdList.selectedValue
        thresholdListModel.clear()
        thresholds.forEach { thresholdListModel.addElement(it) }
        if (selected != null && thresholds.contains(selected)) {
            thresholdList.setSelectedValue(selected, false)
        }
        gradientPanel.repaint()
    }

    private fun addNewThresholdInLargestGap() {
        if (thresholds.size >= 255) return // Full

        var points = (listOf(0) + thresholds + listOf(255)).sorted()
        var maxGap = 0
        var gapStartIndex = 0

        for (i in 0 until points.size - 1) {
            var gap = points[i + 1] - points[i]
            if (gap > maxGap) {
                maxGap = gap
                gapStartIndex = i
            }
        }

        if (maxGap > 1) {
            var newThreshold = points[gapStartIndex] + maxGap / 2
            if (!thresholds.contains(newThreshold)) {
                thresholds.add(newThreshold)
                refreshUI()
                thresholdList.setSelectedValue(newThreshold, false)
            }
        }
    }
}
