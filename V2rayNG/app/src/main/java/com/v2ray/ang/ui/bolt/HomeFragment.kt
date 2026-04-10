package com.v2ray.ang.ui.bolt

import android.net.VpnService
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.V2RayServiceManager
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BoltVPN Home screen (VPN tab).
 * Uses v2rayNG engine via MainViewModel — no v2rayNG UI code.
 */
class HomeFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()

    // Views (bound manually since we use custom layout, not ViewBinding for v2rayNG's layout)
    private lateinit var btnConnect: FrameLayout
    private lateinit var connectInner: FrameLayout
    private lateinit var ivPower: ImageView
    private lateinit var tvConnectLabel: TextView
    private lateinit var layoutServer: FrameLayout
    private lateinit var ivServerFlag: ImageView
    private lateinit var tvServerName: TextView
    private lateinit var tvServerPing: TextView
    private lateinit var viewStatusDot: View
    private lateinit var tvStatus: TextView
    private lateinit var tvSpeedDown: TextView
    private lateinit var tvSpeedUp: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvTrafficDown: TextView
    private lateinit var tvTrafficUp: TextView
    private lateinit var ivLock: ImageView
    private lateinit var tvIpStatus: TextView
    private lateinit var layoutEncryption: LinearLayout
    private lateinit var tvEncryption: TextView

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            startV2Ray()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupListeners()
        observeViewModel()
        mainViewModel.reloadServerList()
    }

    private fun bindViews(view: View) {
        btnConnect = view.findViewById(R.id.btn_connect)
        connectInner = view.findViewById(R.id.connect_inner)
        ivPower = view.findViewById(R.id.iv_power)
        tvConnectLabel = view.findViewById(R.id.tv_connect_label)
        layoutServer = view.findViewById(R.id.layout_server)
        ivServerFlag = view.findViewById(R.id.iv_server_flag)
        tvServerName = view.findViewById(R.id.tv_server_name)
        tvServerPing = view.findViewById(R.id.tv_server_ping)
        viewStatusDot = view.findViewById(R.id.view_status_dot)
        tvStatus = view.findViewById(R.id.tv_status)
        tvSpeedDown = view.findViewById(R.id.tv_speed_down)
        tvSpeedUp = view.findViewById(R.id.tv_speed_up)
        tvTimer = view.findViewById(R.id.tv_timer)
        tvTrafficDown = view.findViewById(R.id.tv_traffic_down)
        tvTrafficUp = view.findViewById(R.id.tv_traffic_up)
        ivLock = view.findViewById(R.id.iv_lock)
        tvIpStatus = view.findViewById(R.id.tv_ip_status)
        layoutEncryption = view.findViewById(R.id.layout_encryption)
        tvEncryption = view.findViewById(R.id.tv_encryption)
    }

    private fun setupListeners() {
        btnConnect.setOnClickListener { handleConnectAction() }
        layoutServer.setOnClickListener {
            ServerSelectSheet().show(childFragmentManager, "server_select")
        }
    }

    private fun observeViewModel() {
        mainViewModel.isRunning.observe(viewLifecycleOwner) { isRunning ->
            applyRunningState(isRunning)
        }
        mainViewModel.updateListAction.observe(viewLifecycleOwner) {
            updateSelectedServerDisplay()
        }
    }

    private fun handleConnectAction() {
        if (mainViewModel.isRunning.value == true) {
            V2RayServiceManager.stopVService(requireContext())
        } else if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(requireContext())
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
            // No server selected — open server selector
            ServerSelectSheet().show(childFragmentManager, "server_select")
            return
        }
        V2RayServiceManager.startVService(requireContext())
    }

    fun restartV2Ray() {
        if (mainViewModel.isRunning.value == true) {
            V2RayServiceManager.stopVService(requireContext())
        }
        lifecycleScope.launch {
            delay(500)
            startV2Ray()
        }
    }

    private fun applyRunningState(isRunning: Boolean) {
        val ctx = context ?: return

        if (isRunning) {
            // Connected — neon glow
            btnConnect.setBackgroundResource(R.drawable.bolt_connect_button_connected)
            connectInner.setBackgroundResource(R.drawable.bolt_connect_inner)
            ivPower.setColorFilter(ContextCompat.getColor(ctx, R.color.bolt_neon))
            tvConnectLabel.text = getString(R.string.bolt_disconnect)
            tvConnectLabel.setTextColor(ContextCompat.getColor(ctx, R.color.bolt_neon))

            viewStatusDot.setBackgroundResource(R.drawable.bolt_status_dot_green)
            tvStatus.text = getString(R.string.bolt_status_connected)
            tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.bolt_green))

            ivLock.setImageResource(R.drawable.ic_lock_closed)
            tvIpStatus.text = getString(R.string.bolt_ip_hidden) + " • " + getString(R.string.bolt_traffic_encrypted)
            tvIpStatus.setTextColor(0x9000ff88.toInt())
            layoutEncryption.isVisible = true
        } else {
            // Disconnected — muted
            btnConnect.setBackgroundResource(R.drawable.bolt_connect_button_bg)
            connectInner.setBackgroundResource(R.drawable.bolt_connect_inner_off)
            ivPower.setColorFilter(0x8000d4ff.toInt())
            tvConnectLabel.text = getString(R.string.bolt_connect)
            tvConnectLabel.setTextColor(0x8000d4ff.toInt())

            viewStatusDot.setBackgroundResource(R.drawable.bolt_status_dot_gray)
            tvStatus.text = getString(R.string.bolt_status_disconnected)
            tvStatus.setTextColor(0xFF7a9aaa.toInt())

            ivLock.setImageResource(R.drawable.ic_lock_open)
            tvIpStatus.text = getString(R.string.bolt_traffic_unprotected)
            tvIpStatus.setTextColor(0x90ff4466.toInt())
            layoutEncryption.isVisible = false

            tvSpeedDown.text = "— Мб/с"
            tvSpeedUp.text = "— Мб/с"
            tvTimer.text = "00:00"
            tvTrafficDown.text = "0 MB"
            tvTrafficUp.text = "0 MB"
        }
    }

    fun updateSelectedServerDisplay() {
        val selectedGuid = MmkvManager.getSelectServer() ?: return
        val server = mainViewModel.serversCache.firstOrNull { it.guid == selectedGuid } ?: return

        tvServerName.text = server.profile.remarks.ifEmpty { getString(R.string.bolt_select_server) }

        val aff = MmkvManager.decodeServerAffiliationInfo(selectedGuid)
        val pingStr = aff?.getTestDelayString().orEmpty()
        tvServerPing.text = pingStr.ifEmpty { "" }
        tvServerPing.isVisible = pingStr.isNotEmpty()

        // Flag via Glide
        val flagRes = getCountryFlagRes(server.profile.remarks)
        if (flagRes != 0) {
            com.bumptech.glide.Glide.with(this).asGif().load(flagRes).into(ivServerFlag)
        }
    }

    private fun getCountryFlagRes(remarks: String): Int {
        val lower = remarks.lowercase()
        return when {
            lower.contains("us") || lower.contains("сша") || lower.contains("нью-йорк") -> R.raw.flag_us
            lower.contains("nl") || lower.contains("нидерланд") || lower.contains("амстердам") -> R.raw.flag_nl
            lower.contains("de") || lower.contains("герман") || lower.contains("франкфурт") -> R.raw.flag_de
            lower.contains("fi") || lower.contains("финлянд") || lower.contains("хельсинки") -> R.raw.flag_fi
            else -> 0
        }
    }

    override fun onResume() {
        super.onResume()
        updateSelectedServerDisplay()
    }
}
