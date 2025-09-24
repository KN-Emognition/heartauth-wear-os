package knemognition.heartauth.mobile

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.lifecycle.lifecycleScope
import com.samsung.android.heartauth.R
import knemognition.heartauth.mobile.data.store.TriggerStore
import knemognition.heartauth.mobile.ui.screens.WaitingScreen
import knemognition.heartauth.mobile.utils.PermissionHelper.isGranted
import knemognition.heartauth.mobile.utils.PermissionHelper.resolveHealthPermission
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val neededPermission = resolveHealthPermission(this)
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(this, getString(R.string.NoPermission), Toast.LENGTH_LONG).show()
            }
        }
        if (!isGranted(this, neededPermission)) {
            permissionLauncher.launch(
                if (neededPermission == getString(R.string.additionalHealthDataPermission)) neededPermission
                else Manifest.permission.BODY_SENSORS
            )
        }
        setContent {
            MaterialTheme {
                WaitingScreen()
            }
        }

    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val saved = TriggerStore.load(applicationContext)
            if (saved != null) {
                if (saved.isExpired()) {
                    TriggerStore.clear(applicationContext)
                } else {
                    startActivity(
                        Intent(this@MainActivity, MeasurementActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .putExtras(saved.args.toBundle())
                    )
                }
            }
        }
    }

}
