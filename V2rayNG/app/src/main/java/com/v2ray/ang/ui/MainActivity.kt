package com.v2ray.ang.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityMainBinding
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.V2RayServiceManager
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : HelperBaseActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val mainViewModel: MainViewModel by viewModels()

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startV2Ray()
        }
    }
    private val requestActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (SettingsChangeManager.consumeRestartService() && mainViewModel.isRunning.value == true) {
            restartV2Ray()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // BoltVPN: no toolbar, but init progressBar for showLoading()/hideLoading()
        setupToolbar(null, false)

        // BoltVPN: Connect button → VPN toggle
        binding.btnConnect.setOnClickListener { handleFabAction() }

        // BoltVPN: Server selector → open server list (placeholder for ServerBottomSheet)
        binding.layoutServer.setOnClickListener {
            // TODO: Step 3.5 — open ServerBottomSheet
            toast(R.string.bolt_select_server)
        }

        // BoltVPN: Bottom navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_vpn -> true // already on VPN screen
                R.id.nav_subscription -> {
                    requestActivityLauncher.launch(Intent(this, SubSettingActivity::class.java))
                    true
                }
                R.id.nav_account -> {
                    // TODO: Step 3.6 — Account fragment
                    toast(R.string.nav_account)
                    true
                }
                R.id.nav_referral -> {
                    // TODO: Step 3.6 — Referral fragment
                    toast(R.string.nav_referral)
                    true
                }
                R.id.nav_more -> {
                    requestActivityLauncher.launch(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        setupViewModel()
        mainViewModel.reloadServerList()

        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}

        // Auto-update subscription on first launch
        importConfigViaSub()
    }

    private fun setupViewModel() {
        mainViewModel.updateTestResultAction.observe(this) { setTestState(it) }
        mainViewModel.isRunning.observe(this) { isRunning ->
            applyRunningState(false, isRunning)
        }
        mainViewModel.startListenBroadcast()
        mainViewModel.initAssets(assets)
    }

    private fun handleFabAction() {
        applyRunningState(isLoading = true, isRunning = false)

        if (mainViewModel.isRunning.value == true) {
            V2RayServiceManager.stopVService(this)
        } else if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startV2Ray()
            } else {
                requestVpnPermission.launch(intent)
            }
        } else {
            startV2Ray()
        }
    }

    private fun startV2Ray() {
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            toast(R.string.title_file_chooser)
            return
        }
        V2RayServiceManager.startVService(this)
    }

    fun restartV2Ray() {
        if (mainViewModel.isRunning.value == true) {
            V2RayServiceManager.stopVService(this)
        }
        lifecycleScope.launch {
            delay(500)
            startV2Ray()
        }
    }

    private fun setTestState(content: String?) {
        binding.tvTestState.text = content
    }

    /**
     * BoltVPN: Apply running state to new UI elements
     * Updates: connect button, status dot, status text, IP indicator, encryption
     */
    private fun applyRunningState(isLoading: Boolean, isRunning: Boolean) {
        if (isLoading) {
            // Connecting state
            binding.tvConnectLabel.text = getString(R.string.bolt_connecting)
            binding.tvStatus.text = getString(R.string.bolt_status_connecting)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.bolt_yellow))
            binding.progressBar.isVisible = true
            return
        }

        binding.progressBar.isVisible = false

        if (isRunning) {
            // Connected state — neon glow
            binding.btnConnect.setBackgroundResource(R.drawable.bolt_connect_button_connected)
            binding.ivPower.setColorFilter(ContextCompat.getColor(this, R.color.bolt_neon))
            binding.tvConnectLabel.text = getString(R.string.bolt_disconnect)
            binding.tvConnectLabel.setTextColor(ContextCompat.getColor(this, R.color.bolt_neon))

            binding.viewStatusDot.setBackgroundResource(R.drawable.bolt_status_dot_green)
            binding.tvStatus.text = getString(R.string.bolt_status_connected)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.bolt_green))

            // IP & encryption
            binding.ivLock.setImageResource(R.drawable.ic_lock_closed)
            binding.tvIpStatus.text = getString(R.string.bolt_ip_hidden) + " • " + getString(R.string.bolt_traffic_encrypted)
            binding.tvIpStatus.setTextColor(ContextCompat.getColor(this, R.color.bolt_green))
            binding.layoutEncryption.isVisible = true

            setTestState(getString(R.string.connection_connected))
        } else {
            // Disconnected state — muted
            binding.btnConnect.setBackgroundResource(R.drawable.bolt_connect_button_bg)
            binding.ivPower.setColorFilter(ContextCompat.getColor(this, R.color.bolt_text_dim))
            binding.tvConnectLabel.text = getString(R.string.bolt_connect)
            binding.tvConnectLabel.setTextColor(ContextCompat.getColor(this, R.color.bolt_text_dim))

            binding.viewStatusDot.setBackgroundResource(R.drawable.bolt_status_dot_gray)
            binding.tvStatus.text = getString(R.string.bolt_status_disconnected)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.bolt_text_dim))

            // IP & encryption
            binding.ivLock.setImageResource(R.drawable.ic_lock_open)
            binding.tvIpStatus.text = getString(R.string.bolt_traffic_unprotected)
            binding.tvIpStatus.setTextColor(ContextCompat.getColor(this, R.color.bolt_red))
            binding.layoutEncryption.isVisible = false

            // Reset stats
            binding.tvSpeedDown.text = "— Мб/с"
            binding.tvSpeedUp.text = "— Мб/с"
            binding.tvTimer.text = "00:00"
            binding.tvTrafficDown.text = "0 MB"
            binding.tvTrafficUp.text = "0 MB"

            setTestState(getString(R.string.connection_not_connected))
        }
    }

    // ===== Server display (called from subscription update) =====

    /**
     * Updates the server selector card with the currently selected server info
     */
    private fun updateSelectedServerDisplay() {
        val selectedGuid = MmkvManager.getSelectServer() ?: return
        val server = mainViewModel.serversCache.firstOrNull { it.guid == selectedGuid }
        if (server != null) {
            val remarks = server.profile.remarks.ifEmpty { getString(R.string.bolt_select_server) }
            binding.tvServerName.text = remarks

            // Ping
            val aff = MmkvManager.decodeServerAffiliationInfo(selectedGuid)
            val pingStr = aff?.getTestDelayString().orEmpty()
            if (pingStr.isNotEmpty()) {
                binding.tvServerPing.text = pingStr
                binding.tvServerPing.isVisible = true
            } else {
                binding.tvServerPing.isVisible = false
            }

            // Flag — determine country from remarks
            val flagRes = getCountryFlagRes(remarks)
            if (flagRes != 0) {
                com.bumptech.glide.Glide.with(this)
                    .asGif()
                    .load(flagRes)
                    .into(binding.ivServerFlag)
            }
        }
    }

    /**
     * Maps server remarks to country flag GIF resource in res/raw/
     */
    private fun getCountryFlagRes(remarks: String): Int {
        val lower = remarks.lowercase()
        return when {
            lower.contains("us") || lower.contains("сша") || lower.contains("нью-йорк") || lower.contains("new york") -> R.raw.flag_us
            lower.contains("nl") || lower.contains("нидерланд") || lower.contains("amsterdam") || lower.contains("амстердам") -> R.raw.flag_nl
            lower.contains("de") || lower.contains("герман") || lower.contains("frankfurt") || lower.contains("франкфурт") -> R.raw.flag_de
            lower.contains("fi") || lower.contains("финлянд") || lower.contains("helsinki") || lower.contains("хельсинки") -> R.raw.flag_fi
            else -> 0
        }
    }

    // ===== Subscription import =====

    fun importConfigViaSub(): Boolean {
        showLoading()

        lifecycleScope.launch(Dispatchers.IO) {
            val result = mainViewModel.updateConfigViaSubAll()
            delay(500L)
            launch(Dispatchers.Main) {
                if (result.successCount + result.failureCount + result.skipCount == 0) {
                    toast(R.string.title_update_subscription_no_subscription)
                } else if (result.successCount > 0 && result.failureCount + result.skipCount == 0) {
                    toast(getString(R.string.title_update_config_count, result.configCount))
                } else {
                    toast(
                        getString(
                            R.string.title_update_subscription_result,
                            result.configCount, result.successCount, result.failureCount, result.skipCount
                        )
                    )
                }
                if (result.configCount > 0) {
                    mainViewModel.reloadServerList()
                }
                hideLoading()
                updateSelectedServerDisplay()
            }
        }
        return true
    }

    private fun importBatchConfig(server: String?) {
        showLoading()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val (count, countSub) = AngConfigManager.importBatchConfig(server, mainViewModel.subscriptionId, true)
                delay(500L)
                withContext(Dispatchers.Main) {
                    when {
                        count > 0 -> {
                            toast(getString(R.string.title_import_config_count, count))
                            mainViewModel.reloadServerList()
                        }
                        countSub > 0 -> { /* subscription added */ }
                        else -> toastError(R.string.toast_failure)
                    }
                    hideLoading()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toastError(R.string.toast_failure)
                    hideLoading()
                }
                Log.e(AppConfig.TAG, "Failed to import batch config", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSelectedServerDisplay()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // BoltVPN: no toolbar menu — all navigation via bottom tabs
        return true
    }
}
