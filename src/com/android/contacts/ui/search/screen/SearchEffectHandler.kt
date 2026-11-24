package com.android.contacts.ui.search.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.android.contacts.ui.contactdetails.ContactDetailsActivity
import com.android.contacts.ui.search.screen.model.SearchEffect as Effect

internal interface SearchEffectHandler {
    fun handle(effect: Effect)
}

internal class SearchEffectHandlerImpl(
    private val activity: Activity,
) : SearchEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            Effect.Close -> close()
            is Effect.OpenContact -> openContact(effect.contactUri)
        }
    }

    private fun close() {
        activity.finish()
    }

    private fun openContact(contactUri: Uri) {
        val intent = Intent(activity, ContactDetailsActivity::class.java)
            .setData(contactUri)
        activity.startActivity(intent)
    }
}
