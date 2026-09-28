package com.example.studyblocker

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.UserManager

object LockManager {

    fun lockDevice(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = MyDeviceAdminReceiver.getComponentName(context)

        if (dpm.isDeviceOwnerApp(context.packageName)) {
            dpm.setUninstallBlocked(admin, context.packageName, true)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_WIFI)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_NETWORK_RESET)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_TETHERING)
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiManager.isWifiEnabled = false

        val serviceIntent = Intent(context, FlashlightMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        setLockStatus(context, true)
    }

    fun unlockDevice(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = MyDeviceAdminReceiver.getComponentName(context)

        if (dpm.isDeviceOwnerApp(context.packageName)) {
            dpm.setUninstallBlocked(admin, context.packageName, false)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_WIFI)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_NETWORK_RESET)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_TETHERING)
        }

        context.stopService(Intent(context, FlashlightMonitorService::class.java))
        setLockStatus(context, false)
    }

    private fun setLockStatus(context: Context, locked: Boolean) {
        val prefs = context.getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_locked", locked).apply()
    }

    fun isLocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_locked", false)
    }
}
