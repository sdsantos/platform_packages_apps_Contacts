package com.android.contacts.ui.home.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.contacts.R
import com.android.contacts.ui.home.screen.model.HomeAccountUiState
import com.android.contacts.ui.home.screen.model.HomeAction as Action
import com.android.contacts.ui.home.screen.model.HomeContactsFilter
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun HomeFilterRow(
    accountState: HomeAccountUiState,
    activeFilters: ImmutableSet<HomeContactsFilter>,
    isFiltersExpanded: Boolean,
    onFiltersClick: () -> Unit,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            when (accountState) {
                is HomeAccountUiState.Ready -> {
                    AccountFilterButton(
                        accountState = accountState,
                        onClick = { onAction(Action.AccountsClick) },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                else -> {
                    Spacer(Modifier)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onAction(Action.GroupsClick) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Label,
                        contentDescription = stringResource(R.string.menu_title_groups),
                        modifier = Modifier.size(ActionIconSize),
                    )
                }
                FiltersButton(
                    activeCount = activeFilters.size,
                    isExpanded = isFiltersExpanded,
                    onClick = onFiltersClick,
                )
            }
        }
        AnimatedVisibility(visible = isFiltersExpanded) {
            FilterChips(
                activeFilters = activeFilters,
                onToggle = { onAction(Action.FilterToggled(it)) },
            )
        }
    }
}

private val ActionIconSize = 28.dp

@Composable
private fun FiltersButton(
    activeCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = when (activeCount) {
        0 -> stringResource(R.string.home_filters)
        else -> pluralStringResource(R.plurals.home_filters_active, activeCount, activeCount)
    }
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = when {
                isExpanded -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground
            },
        ),
        modifier = modifier.semantics { contentDescription = description },
    ) {
        BadgedBox(
            badge = {
                if (activeCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(text = activeCount.toString())
                    }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                modifier = Modifier.size(ActionIconSize),
            )
        }
    }
}

@Composable
private fun FilterChips(
    activeFilters: ImmutableSet<HomeContactsFilter>,
    onToggle: (HomeContactsFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        HomeContactsFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter in activeFilters,
                onClick = { onToggle(filter) },
                label = { Text(text = filterLabel(filter)) },
                shape = RoundedCornerShape(8.dp),
                leadingIcon = {
                    Icon(
                        imageVector = filterIcon(filter),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

private fun filterIcon(filter: HomeContactsFilter): ImageVector {
    return when (filter) {
        HomeContactsFilter.PHONE_CONTACTS -> Icons.Outlined.Phone
        HomeContactsFilter.EMAIL_CONTACTS -> Icons.Outlined.Email
    }
}

@Composable
private fun filterLabel(filter: HomeContactsFilter): String {
    return when (filter) {
        HomeContactsFilter.PHONE_CONTACTS -> stringResource(R.string.home_filter_phone_contacts)
        HomeContactsFilter.EMAIL_CONTACTS -> stringResource(R.string.home_filter_email_contacts)
    }
}

@Composable
private fun AccountFilterButton(
    accountState: HomeAccountUiState.Ready,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        accountState.isSelectable -> {
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = modifier,
            ) {
                AccountFilterContent(
                    accountState = accountState,
                    showDropDown = true,
                )
            }
        }
        else -> {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .heightIn(min = ButtonDefaults.MinHeight)
                        .padding(ButtonDefaults.TextButtonContentPadding),
                ) {
                    AccountFilterContent(
                        accountState = accountState,
                        showDropDown = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.AccountFilterContent(
    accountState: HomeAccountUiState.Ready,
    showDropDown: Boolean,
) {
    Icon(
        imageVector = accountIcon(accountState),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(
        text = accountName(accountState),
        maxLines = 1,
        style = MaterialTheme.typography.labelLarge,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
    )
    if (showDropDown) {
        Icon(
            imageVector = Icons.Rounded.ArrowDropDown,
            contentDescription = null,
        )
    }
}

@Composable
private fun accountIcon(accountState: HomeAccountUiState.Ready): ImageVector {
    return when (accountState) {
        HomeAccountUiState.AllAccounts -> {
            Icons.Outlined.PeopleAlt
        }
        is HomeAccountUiState.OneAccount -> {
            when {
                accountState.account.isDeviceAccount -> Icons.Outlined.Smartphone
                else -> Icons.Outlined.Person
            }
        }
    }
}

@Composable
private fun accountName(accountState: HomeAccountUiState.Ready): String {
    return when (accountState) {
        HomeAccountUiState.AllAccounts -> {
            stringResource(R.string.local_search_label)
        }
        is HomeAccountUiState.OneAccount -> {
            accountState.account.name
                ?: accountState.account.type
                ?: stringResource(R.string.account_phone)
        }
    }
}
