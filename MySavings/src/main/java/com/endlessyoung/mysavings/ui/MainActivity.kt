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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt

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
    
    // App Lock State
    private var isUserAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prevent screenshots in Recents (Optional, based on user pref or always on)
        // window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
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

            // Destination Listener for Toolbar visibility and Animation
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination is FloatingWindow) {
                    return@addOnDestinationChangedListener
                }
                
                // Toolbar Fade Animation
                if (binding.appBar.visibility == View.VISIBLE && destination.id != R.id.HomeFragment) {
                     binding.appBar.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            binding.appBar.visibility = View.GONE
                            binding.appBar.alpha = 1f
                        }
                        .start()
                } else if (binding.appBar.visibility != View.VISIBLE && destination.id == R.id.HomeFragment) {
                    binding.appBar.alpha = 0f
                    binding.appBar.visibility = View.VISIBLE
                    binding.appBar.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }

                if (destination.id == R.id.HomeFragment) {
                    // binding.appBar.visibility = View.VISIBLE // Handled by animation above
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
                    // binding.appBar.visibility = View.GONE // Handled by animation above
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

    override fun onResume() {
        super.onResume()
        checkAppLock()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            isUserAuthenticated = false
        }
    }

    private fun checkAppLock() {
        if (SettingsManager.isAppLockEnabled(this) && !isUserAuthenticated) {
            // 立即显示遮罩，防止内容泄露
            binding.lockOverlay.visibility = View.VISIBLE
            showBiometricPrompt()
        } else {
            binding.lockOverlay.visibility = View.GONE
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUserAuthenticated = true
                    // 验证成功，隐藏遮罩
                    binding.lockOverlay.visibility = View.GONE
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // 用户取消或验证错误，退出应用
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
                        finish()
                    }
                    // 注意：这里不隐藏遮罩，直到成功或退出
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁 MySavings")
            .setSubtitle("请验证指纹或屏幕锁以继续")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            MySavingsLog.e(TAG, "Biometric auth failed to start: ${e.message}")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}