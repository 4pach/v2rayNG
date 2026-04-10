package com.v2ray.ang.ui.bolt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.v2ray.ang.R
import com.v2ray.ang.dto.ServersCache
import com.v2ray.ang.handler.MmkvManager

class ServerListAdapter(
    private val servers: List<ServersCache>,
    private val selectedGuid: String,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFlag: ImageView = view.findViewById(R.id.iv_item_flag)
        val tvName: TextView = view.findViewById(R.id.tv_item_name)
        val tvPing: TextView = view.findViewById(R.id.tv_item_ping)
        val ivCheck: ImageView = view.findViewById(R.id.iv_item_check)
        val root: View = view.findViewById(R.id.item_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_server, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sc = servers[position]
        val remarks = sc.profile.remarks

        holder.tvName.text = remarks

        // Ping
        val aff = MmkvManager.decodeServerAffiliationInfo(sc.guid)
        val ping = aff?.getTestDelayString().orEmpty()
        holder.tvPing.text = ping

        // Flag
        val flagRes = getFlagRes(remarks)
        if (flagRes != 0) {
            Glide.with(holder.ivFlag).asGif().load(flagRes).into(holder.ivFlag)
        }

        // Selected check
        val isSelected = sc.guid == selectedGuid
        holder.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        holder.root.setBackgroundResource(
            if (isSelected) R.drawable.bolt_server_item_selected else R.drawable.bolt_server_item_bg
        )

        holder.root.setOnClickListener { onSelect(sc.guid) }
    }

    override fun getItemCount() = servers.size

    private fun getFlagRes(remarks: String): Int {
        val lower = remarks.lowercase()
        return when {
            lower.contains("us") || lower.contains("сша") -> R.raw.flag_us
            lower.contains("nl") || lower.contains("нидерланд") -> R.raw.flag_nl
            lower.contains("de") || lower.contains("герман") -> R.raw.flag_de
            lower.contains("fi") || lower.contains("финлянд") -> R.raw.flag_fi
            else -> 0
        }
    }
}
