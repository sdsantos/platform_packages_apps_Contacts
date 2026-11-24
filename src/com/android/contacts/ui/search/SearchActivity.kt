package com.android.contacts.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.contacts.R
import com.android.contacts.activities.RequestPermissionsActivity
import com.android.contacts.ui.core.AppTheme
import com.android.contacts.ui.search.screen.SearchEffectHandlerImpl
import com.android.contacts.ui.search.screen.SearchScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overrideActivityTransition(
            OVERRIDE_TRANSITION_OPEN,
            R.anim.search_fade_in,
            R.anim.search_hold,
        )
        overrideActivityTransition(
            OVERRIDE_TRANSITION_CLOSE,
            R.anim.search_hold,
            R.anim.search_fade_out,
        )

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return
        }

        enableEdgeToEdge()
        val effectHandler = SearchEffectHandlerImpl(activity = this)

        setContent {
            AppTheme {
                SearchScreen(effectHandler = effectHandler)
            }
        }
    }

    companion object {
        internal const val EXTRA_QUERY = "query"
        internal const val EXTRA_REQUEST_FOCUS = "requestFocus"

        @JvmStatic
        internal fun buildIntent(
            context: Context,
            query: String? = null,
            requestFocus: Boolean = query.isNullOrBlank(),
        ): Intent =
            Intent(context, SearchActivity::class.java)
                .putExtra(EXTRA_QUERY, query)
                .putExtra(EXTRA_REQUEST_FOCUS, requestFocus)
    }
}
