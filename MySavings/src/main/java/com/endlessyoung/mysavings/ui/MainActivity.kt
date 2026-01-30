package com.endlessyoung.mysavings.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.FloatingWindow
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.endlessyoung.mysavings.R
import com.endlessyoung.mysavings.databinding.ActivityMainBinding
import com.endlessyoung.mysavings.domain.model.SavingItem
import com.endlessyoung.mysavings.log.MySavingsLog
import com.google.android.material.appbar.AppBarLayout
import java.math.BigDecimal
import kotlin.getValue

import com.endlessyoung.mysavings.ui.utils.SettingsManager

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val sharedVm: SavingViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private lateinit var controller: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply Theme
        SettingsManager.initTheme(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        controller = WindowCompat.getInsetsController(window, window.decorView)

        setupStatusBar()

        try {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
            val navController = navHostFragment.navController

            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.StatsFragment,
                    R.id.HomeFragment,
                    R.id.MineFragment
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            setupWithNavController(binding.bottomNav, navController)

            // 只在 HomeFragment 显示顶部 AppBar / Toolbar
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination is FloatingWindow) {
                    return@addOnDestinationChangedListener
                }

                if (destination.id == R.id.HomeFragment) {
                    binding.appBar.visibility = View.VISIBLE
                    window.statusBarColor = ContextCompat.getColor(this, R.color.main_blue)
                    controller.isAppearanceLightStatusBars = false

                    // HomeFragment需要有上边距，避免被AppBar遮挡
                    // 使用 AppBarLayout 的高度来动态调整
                    binding.appBar.post {
                        binding.navHostFragmentContentMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            topMargin = binding.appBar.height
                        }
                    }
                } else {
                    binding.appBar.visibility = View.GONE
                    window.statusBarColor = ContextCompat.getColor(this, R.color.bg_page_color)
                    controller.isAppearanceLightStatusBars = true

                    // 其他页面去除上边距，实现全屏
                    binding.navHostFragmentContentMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = 0
                    }
                }
            }
        } catch (e: Exception) {
            MySavingsLog.e(TAG, e.message.toString())
        }
    }

    private fun setupStatusBar() {
        val color = ContextCompat.getColor(this, R.color.main_blue)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = color

        controller.isAppearanceLightStatusBars = false
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}