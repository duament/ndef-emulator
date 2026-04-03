package com.luigivampa92.ndefemulation.ndef

import android.nfc.NdefMessage
import android.nfc.NdefRecord

internal class MultiRecordNdefMessageBuilder : NdefMessageBuilder {

    override fun build(ndefData: NdefData): NdefMessage? {
        if (ndefData !is MultiRecordNdefData) return null
        if (ndefData.records.isEmpty()) return null
        val ndefRecords = ndefData.records.map { r ->
            NdefRecord(r.tnf, r.type, r.id, r.payload)
        }.toTypedArray()
        return NdefMessage(ndefRecords)
    }
}
