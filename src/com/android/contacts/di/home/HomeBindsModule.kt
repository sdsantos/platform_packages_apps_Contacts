package com.android.contacts.di.home

import com.android.contacts.ui.home.mapper.HomeRequestMapper
import com.android.contacts.ui.home.mapper.HomeRequestMapperImpl
import com.android.contacts.ui.home.screen.mapper.HomeUiStateMapper
import com.android.contacts.ui.home.screen.mapper.HomeUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HomeBindsModule {

    @Binds
    @Reusable
    abstract fun bindHomeRequestMapper(
        impl: HomeRequestMapperImpl,
    ): HomeRequestMapper

    @Binds
    @Reusable
    abstract fun bindHomeUiStateMapper(
        impl: HomeUiStateMapperImpl,
    ): HomeUiStateMapper
}
