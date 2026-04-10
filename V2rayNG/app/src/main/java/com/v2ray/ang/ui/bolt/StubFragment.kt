package com.v2ray.ang.ui.bolt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Temporary stub fragment for tabs not yet implemented.
 * Shows tab name centered on dark background.
 */
class StubFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String) = StubFragment().apply {
            arguments = Bundle().apply { putString(ARG_TITLE, title) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(requireContext()).apply {
            text = arguments?.getString(ARG_TITLE) ?: "—"
            textSize = 18f
            setTextColor(0xFFd0dce8.toInt())
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF0a0a1a.toInt())
        }
        return tv
    }
}
