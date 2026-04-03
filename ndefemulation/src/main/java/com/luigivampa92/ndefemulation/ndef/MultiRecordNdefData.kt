package com.luigivampa92.ndefemulation.ndef

import android.nfc.NdefRecord

data class MultiRecordNdefData(
    val records: List<NdefRecordData>
) : NdefData() {

    constructor(ndefRecords: Array<NdefRecord>) : this(ndefRecords.map { NdefRecordData(it) })

    init {
        require(records.isNotEmpty()) { "MultiRecordNdefData must contain at least one record" }
    }
}
