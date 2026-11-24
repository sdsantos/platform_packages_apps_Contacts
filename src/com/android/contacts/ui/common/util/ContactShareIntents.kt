package com.android.contacts.ui.common.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.icu.text.MessageFormat
import android.net.Uri
import android.provider.ContactsContract.Contacts
import com.android.contacts.R
import java.util.Locale

internal object ContactShareIntents {

    private const val LOOKUP_KEY_SEPARATOR = ":"

    fun build(context: Context, lookupKeys: Collection<String>): Intent? {
        if (lookupKeys.isEmpty()) return null

        val uri = shareUri(lookupKeys)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(Contacts.CONTENT_VCARD_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri(null, uri)

        return Intent.createChooser(intent, shareTitle(context, lookupKeys.size))
    }

    private fun shareUri(lookupKeys: Collection<String>): Uri {
        return when (lookupKeys.size) {
            1 -> {
                Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKeys.single())
            }
            else -> {
                Uri.withAppendedPath(
                    Contacts.CONTENT_MULTI_VCARD_URI,
                    Uri.encode(lookupKeys.joinToString(LOOKUP_KEY_SEPARATOR) { Uri.encode(it) }),
                )
            }
        }
    }

    private fun shareTitle(context: Context, count: Int): CharSequence {
        return MessageFormat(context.getString(R.string.title_share_via), Locale.getDefault())
            .format(mapOf("count" to count))
    }
}
