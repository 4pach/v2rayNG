package com.v2ray.ang.ui.bolt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.V2RayServiceManager
import com.v2ray.ang.viewmodel.MainViewModel

/**
 * Server selection bottom sheet.
 * Per Pencil specs: cornerRadius=24, fill #0d1530F0, stroke #00d4ff20.
 */
class ServerSelectSheet : BottomSheetDialogFragment() {

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun getTheme(): Int = R.style.BoltBottomSheet

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_server_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_servers)
        val quickConnect = view.findViewById<View>(R.id.btn_quick_connect)
        val selectedGuid = MmkvManager.getSelectServer().orEmpty()

        val adapter = ServerListAdapter(
            servers = mainViewModel.serversCache,
            selectedGuid = selectedGuid,
            onSelect = { guid -> selectServer(guid) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        quickConnect.setOnClickListener { quickConnectBest() }
    }

    private fun selectServer(guid: String) {
        val wasRunning = mainViewModel.isRunning.value == true
        MmkvManager.setSelectServer(guid)

        // Update HomeFragment display
        (parentFragment as? HomeFragment)?.updateSelectedServerDisplay()

        if (wasRunning) {
            (activity as? BoltMainActivity)?.restartV2Ray()
        }
        dismiss()
    }

    private fun quickConnectBest() {
        val best = mainViewModel.serversCache
            .mapNotNull { sc ->
                val delay = MmkvManager.decodeServerAffiliationInfo(sc.guid)?.testDelayMillis ?: 0L
                if (delay > 0) sc.guid to delay else null
            }
            .minByOrNull { it.second }
            ?.first

        if (best != null) {
            selectServer(best)
        } else if (mainViewModel.serversCache.isNotEmpty()) {
            selectServer(mainViewModel.serversCache.first().guid)
        }
    }
}
