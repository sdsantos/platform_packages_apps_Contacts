package com.android.contacts.di.search

import com.android.contacts.ui.search.screen.mapper.SearchUiStateMapper
import com.android.contacts.ui.search.screen.mapper.SearchUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SearchBindsModule {

    @Binds
    @Reusable
    abstract fun bindSearchUiStateMapper(
        impl: SearchUiStateMapperImpl,
    ): SearchUiStateMapper
}
