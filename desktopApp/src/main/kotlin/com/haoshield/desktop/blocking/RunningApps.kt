package com.haoshield.desktop.blocking

import java.util.Locale
import java.util.stream.Collectors

/**
 * Processes this app will never end, whatever the blocklist says.
 *
 * The blocklist is a list of names, and names can be typed. Someone browsing what is running and
 * adding things could easily add the wrong one — and ending `csrss.exe` or `lsass.exe` does not
 * distract Windows, it takes it down. So the rule is enforced where the killing happens rather than
 * only where the choosing happens: anything living under the Windows directory is out of reach, and
 * a handful of names are refused outright in case a lookalike runs from elsewhere.
 *
 * This costs nothing real. Nobody sets aside protected time from the Desktop Window Manager.
 */
object ProtectedProcesses {

    private val windowsDirectory: String =
        (System.getenv("SystemRoot") ?: "C:\\Windows").lowercase(Locale.ROOT)

    private val neverEnd: Set<String> = setOf(
        "system", "registry", "idle",
        "smss.exe", "csrss.exe", "wininit.exe", "winlogon.exe", "services.exe",
        "lsass.exe", "svchost.exe", "dwm.exe", "fontdrvhost.exe", "conhost.exe",
        "explorer.exe", "sihost.exe", "ctfmon.exe", "taskhostw.exe", "audiodg.exe",
        "runtimebroker.exe", "shellexperiencehost.exe", "startmenuexperiencehost.exe",
        "searchhost.exe", "logonui.exe", "spoolsv.exe", "wmiprvse.exe",
        // The machine's own defences, which are not ours to switch off.
        "msmpeng.exe", "securityhealthservice.exe", "securityhealthsystray.exe",
        // And this app, which cannot guard anything if it ends itself. Both spellings: the packaged
        // executable takes its name from the installer, and the JVM ones cover running from source.
        "haoshield.exe", "hao shield.exe", "java.exe", "javaw.exe",
    )

    fun isProtected(executableName: String, fullPath: String?): Boolean {
        if (executableName.lowercase(Locale.ROOT) in neverEnd) return true
        val path = fullPath?.lowercase(Locale.ROOT) ?: return false
        return path.startsWith(windowsDirectory)
    }
}

/**
 * The one spelling of a process name that everything agrees on.
 *
 * Windows does not care about case, so `Discord.exe` and `discord.exe` are the same app — but a map
 * of temporary allowances does care. Letting an app through under one spelling and then checking
 * for it under another would close it anyway, a minute after being told not to. Both sides of that
 * conversation go through here.
 */
object ProcessName {
    fun normalise(name: String): String = name.trim().lowercase(Locale.ROOT)
}

/** What is running right now, reduced to the things a person might reasonably set aside. */
object RunningApps {

    fun list(): List<String> =
        ProcessHandle.allProcesses()
            .collect(Collectors.toList())
            .mapNotNull { handle -> handle.info().command().orElse(null) }
            .mapNotNull { path ->
                val name = path.substringAfterLast('\\').substringAfterLast('/')
                if (name.isBlank() || ProtectedProcesses.isProtected(name, path)) null else name
            }
            .distinct()
            .sortedBy { it.lowercase(Locale.ROOT) }
}
