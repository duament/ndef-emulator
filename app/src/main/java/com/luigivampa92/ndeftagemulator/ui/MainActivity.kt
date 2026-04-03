package com.luigivampa92.ndeftagemulator.ui

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.luigivampa92.ndefemulation.NdefEmulation
import com.luigivampa92.ndeftagemulator.R

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var emulationSwitch: SwitchCompat
    private lateinit var ndefEmulation: NdefEmulation

    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private var switchChangingProgrammatically = false
    private var isResumed = false
    private var isReaderTabActive = false

    private val emulatorFragment = EmulatorFragment()
    private val readerFragment = ReaderFragment()
    private val tagsFragment = TagsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ndefEmulation = NdefEmulation(this)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags
        )

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        emulationSwitch = findViewById(R.id.emulation_switch)
        emulationSwitch.isChecked = ndefEmulation.currentEmulatedNdefData != null
        emulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (switchChangingProgrammatically) return@setOnCheckedChangeListener
            if (isChecked) {
                emulatorFragment.applyEmulation()
            } else {
                ndefEmulation.currentEmulatedNdefData = null
                emulatorFragment.refreshStatus()
                Toast.makeText(this, "Emulation stopped", Toast.LENGTH_SHORT).show()
            }
        }

        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> emulatorFragment
                1 -> readerFragment
                2 -> tagsFragment
                else -> throw IllegalStateException()
            }
        }
        viewPager.offscreenPageLimit = 2

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                isReaderTabActive = position == 1
                updateNfcForegroundDispatch()
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Emulator"
                1 -> "Reader"
                2 -> "Tags"
                else -> ""
            }
        }.attach()

        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        updateNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun updateNfcForegroundDispatch() {
        if (!isResumed) return
        if (isReaderTabActive) {
            nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } else {
            nfcAdapter?.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED) return

        @Suppress("DEPRECATION")
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        val records = mutableListOf<NdefRecord>()
        rawMessages?.forEach { msg ->
            (msg as? NdefMessage)?.records?.let { records.addAll(it) }
        }
        if (records.isNotEmpty()) {
            viewPager.currentItem = 1
            viewPager.post { readerFragment.displayRecords(records) }
        } else {
            Toast.makeText(this, "No NDEF records found on this tag", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateEmulationSwitch(isOn: Boolean) {
        switchChangingProgrammatically = true
        emulationSwitch.isChecked = isOn
        switchChangingProgrammatically = false
    }
}
