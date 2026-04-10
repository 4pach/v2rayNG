package com.v2ray.ang.ui.bolt

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.v2ray.ang.R
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.HelperBaseActivity
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BoltVPN main entry point.
 * Replaces v2rayNG's MainActivity as LAUNCHER.
 * Uses v2rayNG engine (V2RayServiceManager, MainViewModel, MmkvManager) without modifying it.
 */
class BoltMainActivity : HelperBaseActivity() {

    val mainViewModel: MainViewModel by viewModels()

    private lateinit var bottomNav: BottomNavigationView

    // Fragment instances — kept alive via hide/show (not replace)
    private var homeFragment: HomeFragment? = null
    private var subscriptionFragment: StubFragment? = null
    private var accountFragment: StubFragment? = null
    private var referralFragment: StubFragment? = null
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bolt_main)

        // ОБЯЗАТЕЛЬНО — инициализация VPN-движка
        mainViewModel.initAssets(assets)
        mainViewModel.startListenBroadcast()

        // Notification permission
        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}

        // Auto-update subscriptions
        lifecycleScope.launch(Dispatchers.IO) {
            val result = mainViewModel.updateConfigViaSubAll()
            withContext(Dispatchers.Main) {
                if (result.configCount > 0) {
                    mainViewModel.reloadServerList()
                }
            }
        }

        setupFragments(savedInstanceState)
        setupBottomNavigation()
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            subscriptionFragment = StubFragment.newInstance(getString(R.string.nav_subscription))
            accountFragment = StubFragment.newInstance(getString(R.string.nav_account))
            referralFragment = StubFragment.newInstance(getString(R.string.nav_referral))

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment!!, "home")
                .add(R.id.fragment_container, subscriptionFragment!!, "subscription").hide(subscriptionFragment!!)
                .add(R.id.fragment_container, accountFragment!!, "account").hide(accountFragment!!)
                .add(R.id.fragment_container, referralFragment!!, "referral").hide(referralFragment!!)
                .commit()

            activeFragment = homeFragment
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
            subscriptionFragment = supportFragmentManager.findFragmentByTag("subscription") as? StubFragment
            accountFragment = supportFragmentManager.findFragmentByTag("account") as? StubFragment
            referralFragment = supportFragmentManager.findFragmentByTag("referral") as? StubFragment
            activeFragment = homeFragment
        }
    }

    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            val target: Fragment? = when (item.itemId) {
                R.id.nav_vpn -> homeFragment
                R.id.nav_subscription -> subscriptionFragment
                R.id.nav_account -> accountFragment
                R.id.nav_referral -> referralFragment
                R.id.nav_more -> {
                    // TODO: BoltSettingsActivity
                    null
                }
                else -> null
            }
            if (target != null && target !== activeFragment) {
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment!!)
                    .show(target)
                    .commit()
                activeFragment = target
            }
            target != null
        }
    }

    fun restartV2Ray() {
        homeFragment?.restartV2Ray()
    }
}
