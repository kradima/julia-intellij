package org.ice1000.julia.lang.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.*
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.table.JBTable
import icons.JuliaIcons
import org.ice1000.julia.lang.JuliaBundle
import org.ice1000.julia.lang.printJulia

class JuliaRemovePkgAction(
	private val box: ComboboxWithBrowseButton,
	private val packagesList: JBTable) : AnAction(JuliaIcons.REMOVE_ICON) {
	override fun actionPerformed(e: AnActionEvent) {
		val project = e.project ?: return
		val index = packagesList.selectedRow
		if (index < 0) {
			Messages.showInfoMessage(
				project,
				JuliaBundle.message("julia.messages.package.not-selected"),
				JuliaBundle.message("julia.messages.package.not-selected.title"))
			return
		}
		val removePackageName = packagesList.getValueAt(index, 0).toString()
		ProgressManager.getInstance().run(object :
			Task.Backgroundable(
				project,
				JuliaBundle.message("julia.messages.package.remove", removePackageName),
				true) {
			override fun run(indicator: ProgressIndicator) {
				indicator.text = JuliaBundle.message("julia.messages.package.remove", removePackageName)
				//language=Julia
				printJulia(box.comboBox.selectedItem.toString(), 30_000L, """Pkg.rm("$removePackageName")""")
				Messages.showDialog(
					project,
					JuliaBundle.message("julia.messages.package.removed", removePackageName),
					JuliaBundle.message("julia.messages.package.success"),
					arrayOf(JuliaBundle.message("julia.yes")),
					0,
					JuliaIcons.JOJO_ICON)
			}
		})
	}
}

class JuliaAddPkgAction(private val box: ComboboxWithBrowseButton) : AnAction(JuliaIcons.ADD_ICON) {
	private var out: Pair<List<String>, List<String>>? = null
	private var packageName = ""
	override fun actionPerformed(e: AnActionEvent) {
		val project = e.project ?: return
		Messages.showInputDialog(
			project,
			JuliaBundle.message("julia.messages.package.add"),
			JuliaBundle.message("julia.messages.package.add.title"),
			JuliaIcons.JOJO_ICON,
			"",
			null
		)?.let {
			packageName = it
			ProgressManager.getInstance().run(object :
				Task.Backgroundable(
					project,
					JuliaBundle.message("julia.messages.package.installing", it),
					true) {
				override fun run(indicator: ProgressIndicator) {
					indicator.text = JuliaBundle.message("julia.messages.package.installing", it)
					//language=Julia
					out = printJulia(
						box.comboBox.selectedItem.toString(), 50_000L, """Pkg.add("$it")""")
				}

				override fun onSuccess() = ApplicationManager.getApplication().invokeLater {
					val (stdout, stderr) = out ?: return@invokeLater
					if (stderr.isNotEmpty()) Messages.showDialog(
						project,
						stderr.joinToString("\n"),
						JuliaBundle.message("julia.messages.package.error.title", packageName),
						arrayOf(JuliaBundle.message("julia.yes")),
						0,
						JuliaIcons.JOJO_ICON)
					if (stdout.isNotEmpty()) Messages.showDialog(
						project,
						stdout.joinToString("\n"),
						JuliaBundle.message("julia.messages.package.installing.title"),
						arrayOf(JuliaBundle.message("julia.yes")),
						0,
						JuliaIcons.JOJO_ICON)
				}
			})
		}
	}
}
