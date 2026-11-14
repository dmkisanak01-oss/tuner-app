package com.example.tuner.system

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

/** Backend Shizuku lewat UserService. VERIFY: API Shizuku 13.x, belum dikompilasi atau diuji di perangkat. */
class ShizukuExecutor(private val context: Context) : PrivilegedExecutor {
    private val _state = MutableStateFlow(BackendState.UNAVAILABLE)
    override val state: StateFlow<BackendState> = _state

    @Volatile private var service: IShellService? = null
    private var bindRequested = false
    private val mutex = Mutex()

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShellService::class.java.name),
    ).daemon(false).processNameSuffix("shell").version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = if (binder != null && binder.pingBinder()) IShellService.Stub.asInterface(binder) else null
            recompute()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bindRequested = false
            recompute()
        }
    }

    private val onBinderReceived = Shizuku.OnBinderReceivedListener { recompute() }
    private val onBinderDead = Shizuku.OnBinderDeadListener {
        service = null
        bindRequested = false
        recompute()
    }
    private val onPermissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> recompute() }

    fun start() {
        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermissionResult)
        recompute()
    }

    fun stop() {
        Shizuku.removeBinderReceivedListener(onBinderReceived)
        Shizuku.removeBinderDeadListener(onBinderDead)
        Shizuku.removeRequestPermissionResultListener(onPermissionResult)
    }

    /** Memunculkan dialog izin Shizuku. Aman dipanggil dari UI. */
    fun requestPermission() {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    override suspend fun refresh() = recompute()

    private fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun recompute() {
        _state.value = when {
            !Shizuku.pingBinder() -> if (isInstalled()) BackendState.NOT_RUNNING else BackendState.UNAVAILABLE
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> BackendState.NEEDS_PERMISSION
            service != null -> BackendState.READY
            else -> {
                if (!bindRequested) {
                    bindRequested = true
                    Shizuku.bindUserService(userServiceArgs, connection)
                }
                BackendState.CONNECTING
            }
        }
    }

    override suspend fun exec(argv: List<String>): ExecResult {
        SafeArgs.check(argv)
        return mutex.withLock {
            val svc = try {
                withTimeout(5_000) { state.first { it == BackendState.READY } }
                service
            } catch (e: Exception) {
                null
            } ?: return@withLock ExecResult(-1, "Shizuku tidak siap", Backend.SHIZUKU)
            withContext(Dispatchers.IO) {
                try {
                    val raw = svc.exec(argv.toTypedArray())
                    val code = raw.substringBefore('\n').toIntOrNull() ?: -1
                    ExecResult(code, raw.substringAfter('\n', ""), Backend.SHIZUKU)
                } catch (e: Exception) {
                    ExecResult(-1, "${e.javaClass.simpleName}: ${e.message}", Backend.SHIZUKU)
                }
            }
        }
    }

    private companion object {
        const val REQUEST_CODE = 1001
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
