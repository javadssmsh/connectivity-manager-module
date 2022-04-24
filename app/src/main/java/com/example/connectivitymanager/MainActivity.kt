package com.example.connectivitymanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.connectivitymanagermodule.CheckConnectivityModule

class MainActivity : AppCompatActivity() {

    private lateinit var showInternetStateTextView: TextView
    private lateinit var showInternetStateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showInternetStateButton = findViewById(R.id.check_internet_btn)
        showInternetStateTextView = findViewById(R.id.check_internet_tv)

        showInternetStateButton.setOnClickListener {
            showInternetStateTextView.text =
                CheckConnectivityModule.checkHasConnectionAndInternet().toString()
        }

    }
}