package com.samyak.simpletube.utils

import android.content.Intent
import android.util.ArrayMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class ActivityLauncherHelper(
    activity: ComponentActivity
) {
    private var consumers = ArrayMap<String, ((ActivityResult) -> Unit)?>()

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getStringExtra("id")
        val consumer = consumers.get(id)
        consumer?.invoke(result)
        consumers.remove(id)
    }

    fun launchActivityForResult(intent: Intent, onResult: (ActivityResult) -> Unit) {
        val id = intent.getStringExtra("filePath")
        if (id != null) {
            consumers.put(id, onResult)
            launcher.launch(intent)
        }
    }
}
