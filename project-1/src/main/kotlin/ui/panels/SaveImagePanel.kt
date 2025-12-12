package org.pdi.ui.panels

import org.pdi.core.AppState
import org.pdi.io.saveImage
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class SaveImagePanel(
    private val appState: AppState,
    private val onSave: () -> Unit
) : JPanel() {
    private val formatComboBox = JComboBox(arrayOf("png", "bmp", "netpbm"))
    private val saveButton = JButton("Save")
    private val fileNameField = JTextField(20)
    private val fileChooser = JFileChooser()

    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.add(JLabel("File Name:"))
        topPanel.add(fileNameField)
        topPanel.add(JLabel("Format:"))
        topPanel.add(formatComboBox)

        add(topPanel, BorderLayout.NORTH)
        add(fileChooser, BorderLayout.CENTER)

        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        bottomPanel.add(saveButton)
        add(bottomPanel, BorderLayout.SOUTH)

        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fileChooser.dialogTitle = "Select a directory"

        saveButton.addActionListener {
            val selectedDirectory = fileChooser.selectedFile
            if (selectedDirectory == null) {
                JOptionPane.showMessageDialog(this, "Please select a directory.", "No Directory Selected", JOptionPane.WARNING_MESSAGE)
                return@addActionListener
            }

            val fileName = fileNameField.text
            if (fileName.isBlank()) {
                JOptionPane.showMessageDialog(this, "Please enter a file name.", "No File Name", JOptionPane.WARNING_MESSAGE)
                return@addActionListener
            }

            val format = formatComboBox.selectedItem as String
            val fullPath = "${selectedDirectory.absolutePath}/$fileName.$format"

            appState.context.currentImage?.let { image ->
                try {
                    saveImage(fullPath, format, image)
                    JOptionPane.showMessageDialog(this, "Image saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
                    onSave()
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(this, "Error saving image: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }
}
