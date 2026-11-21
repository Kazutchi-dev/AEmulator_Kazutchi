package app.aemu.core

import java.io.File

/**
 * Host-side diagnostics for guest processes. Guest services are ordinary host processes:
 * their exit status alone cannot distinguish a host OOM kill from a deliberate signal.
 */
internal object HostProcessDiagnostics {
    private fun read(path: String, limit: Int = 2048): String =
        runCatching { File(path).readText().trim().replace('\n', ';').take(limit) }
            .getOrDefault("unavailable")

    private fun selected(path: String, prefixes: List<String>): String =
        runCatching {
            File(path).useLines { lines ->
                lines.filter { line -> prefixes.any { line.startsWith(it) } }
                    .joinToString(";").ifBlank { "none" }
            }
        }.getOrDefault("unavailable")

    fun process(pid: Int): String {
        val base = "/proc/$pid"
        val stat = read("$base/stat")
        val end = stat.lastIndexOf(") ")
        val fields = if (end >= 0) stat.substring(end + 2).trim().split(Regex("\\s+")) else emptyList()
        val status = selected("$base/status", listOf("Uid:", "VmRSS:", "VmHWM:", "Threads:"))
        return "pid=$pid state=${fields.getOrNull(0) ?: "?"}" +
            " ppid=${fields.getOrNull(1) ?: "?"} pgrp=${fields.getOrNull(2) ?: "?"}" +
            " sid=${fields.getOrNull(3) ?: "?"}" +
            " oom_score=${read("$base/oom_score", 64)}" +
            " oom_score_adj=${read("$base/oom_score_adj", 64)}" +
            " status={$status} cgroup={${read("$base/cgroup", 512)}}"
    }

    fun snapshot(): String {
        val self = android.os.Process.myPid()
        val cgroupRaw = runCatching { File("/proc/self/cgroup").readText().trim() }.getOrDefault("unavailable")
        val cgroup = cgroupRaw.replace('\n', ';').take(1024)
        val v2Path = cgroupRaw.lineSequence().firstOrNull { it.startsWith("0::") }
            ?.substringAfter("0::")?.trim()?.takeIf { it.startsWith("/") }
        val memoryFiles = if (v2Path != null) {
            val dir = File("/sys/fs/cgroup", v2Path.removePrefix("/"))
            listOf("memory.current", "memory.max", "memory.events", "memory.pressure")
                .joinToString(";") { name -> "$name=${read(File(dir, name).absolutePath, 512)}" }
        } else "v2-memory-files=unavailable"
        val mem = selected("/proc/meminfo", listOf("MemTotal:", "MemAvailable:", "MemFree:", "SwapTotal:", "SwapFree:"))
        val vm = selected("/proc/vmstat", listOf("oom_kill ", "pswpout ", "pgmajfault ", "allocstall "))
        return "self={${process(self)}} meminfo={$mem} vmstat={$vm}" +
            " psi={${read("/proc/pressure/memory", 512)}}" +
            " cgroup={$cgroup} memory={$memoryFiles}"
    }
}
