package com.example.nfctag.setting.transferkey.vehicle

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class SampleFragmentPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    private val PAGE_COUNT = 2
    private val tabTitles = arrayOf("NFC Transfer", "Vehicle OEM Server Transfer")
    override fun getCount(): Int {
        return PAGE_COUNT
    }

    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            NFCTransferKeyFragment.newInstance()
        } else {
            VehicleOEMTransferKeyFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabTitles[position]
    }
}