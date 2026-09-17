package ss.proximityservice.settings

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.android.support.DaggerAppCompatActivity
import ss.proximityservice.ProximityService
import ss.proximityservice.R
import ss.proximityservice.data.EventObserver
import ss.proximityservice.data.ServiceState
import ss.proximityservice.databinding.ActivitySettingsBinding
import javax.inject.Inject

class SettingsActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SettingsViewModel
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(SettingsViewModel::class.java)

        viewModel.serviceState.observe(this, ::updateConditionCard)
        ServiceState.isRunning.observe(this, viewModel::updateState)
        viewModel.alert.observe(this, EventObserver { dialog -> dialog.show(this) })
        viewModel.operationalModeResId.observe(this) { resId ->
            binding.operationalModeSecondaryText.text = getString(resId)
        }
        viewModel.notificationBehaviorResId.observe(this) { resId ->
            binding.notificationBehaviorSecondaryText.text = getString(resId)
        }
        viewModel.screenOffDelayResId.observe(this) { resId ->
            binding.screenOffDelaySecondaryText.text = getString(resId)
        }
        viewModel.screenOffDelayProgress.observe(this) { progress ->
            if (binding.settingScreenOffDelaySeekbar.progress != progress) {
                binding.settingScreenOffDelaySeekbar.progress = progress
            }
        }

        binding.btnService.setOnClickListener {
            val intent = Intent(this, ProximityService::class.java)
                .setAction(viewModel.getNextIntentAction())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        binding.settingOperationalMode.setOnClickListener { viewModel.operationalModeClick() }
        binding.settingNotificationBehavior.setOnClickListener { viewModel.notificationBehaviorClick() }

        binding.settingScreenOffDelaySeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.screenOffDelayProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                viewModel.screenOffDelayUpdate(seekBar.progress)
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            val color = ContextCompat.getColor(this, R.color.primaryDark)
            setTaskDescription(
                ActivityManager.TaskDescription(getString(R.string.app_name), icon, color)
            )
            icon?.recycle()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_source_licenses -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateConditionCard(serviceState: Boolean) {
        val colorId = if (serviceState) R.color.accent else R.color.primaryLight
        val conditionTextId = if (serviceState) R.string.condition_active else R.string.condition_inactive
        val btnTextId = if (serviceState) R.string.button_off else R.string.button_on
        binding.conditionCard.setBackgroundColor(ContextCompat.getColor(this, colorId))
        binding.tvCondition.text = getString(conditionTextId)
        binding.btnService.text = getString(btnTextId)
    }
}
