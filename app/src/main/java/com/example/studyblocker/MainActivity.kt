package com.example.studyblocker

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private lateinit var etPassword: EditText
    private lateinit var btnAction: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPassword = findViewById(R.id.etPassword)
        btnAction = findViewById(R.id.btnAction)
        tvStatus = findViewById(R.id.tvStatus)

        updateUiState()

        btnAction.setOnClickListener {
            handleAction()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun updateUiState() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isDeviceOwnerApp(packageName)) {
            tvStatus.text = "⚠️ Device Owner 권한이 필요합니다.\nADB 명령어로 설정해 주세요."
            btnAction.isEnabled = false
            return
        }

        val hasPassword = getSavedPasswordHash() != null
        val isLocked = LockManager.isLocked(this)

        if (!hasPassword) {
            tvStatus.text = "보호용 비밀번호를 설정하세요."
            btnAction.text = "비밀번호 설정 및 잠금 시작"
            btnAction.isEnabled = true
        } else if (isLocked) {
            tvStatus.text = "현재 데이터 및 와이파이가 차단되었습니다."
            btnAction.text = "잠금 해제"
            btnAction.isEnabled = true
        } else {
            tvStatus.text = "잠금이 해제된 상태입니다."
            btnAction.text = "다시 잠금 시작"
            btnAction.isEnabled = true
        }
    }

    private fun handleAction() {
        val input = etPassword.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val savedHash = getSavedPasswordHash()

        if (savedHash == null) {
            savePasswordHash(hashPassword(input))
            LockManager.lockDevice(this)
            Toast.makeText(this, "비밀번호가 설정되었으며 네트워크가 차단되었습니다.", Toast.LENGTH_LONG).show()
            etPassword.text.clear()
            updateUiState()
        } else {
            if (hashPassword(input) == savedHash) {
                if (LockManager.isLocked(this)) {
                    LockManager.unlockDevice(this)
                    Toast.makeText(this, "비밀번호 일치: 잠금이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    LockManager.lockDevice(this)
                    Toast.makeText(this, "네트워크가 다시 차단되었습니다.", Toast.LENGTH_SHORT).show()
                }
                etPassword.text.clear()
                updateUiState()
            } else {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun savePasswordHash(hash: String) {
        getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("pw_hash", hash)
            .apply()
    }

    private fun getSavedPasswordHash(): String? {
        return getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
            .getString("pw_hash", null)
    }
}
