package com.example.nfctag.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nfctag.databinding.ActivityTransferKeyBinding
import com.example.nfctag.setting.transferkey.vehicle.SampleFragmentPagerAdapter

class TransferKeyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransferKeyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransferKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewpager.adapter = SampleFragmentPagerAdapter(supportFragmentManager)
        binding.slidingTabs.setupWithViewPager(binding.viewpager)
    }
}