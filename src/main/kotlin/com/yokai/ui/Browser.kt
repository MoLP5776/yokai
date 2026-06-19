package com.yokai.ui

import java.awt.Desktop
import java.net.URI

fun openInBrowser(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}
