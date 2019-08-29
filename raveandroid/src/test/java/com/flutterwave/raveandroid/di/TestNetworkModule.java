package com.flutterwave.raveandroid.di;

import com.flutterwave.raveandroid.data.ApiService;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;

@Module
public class TestNetworkModule {

    @Singleton
    @Provides
    public ApiService providesApiService() {
        return Mockito.mock(ApiService.class);
    }

    @Singleton
    @Provides
    public Retrofit providesRetrofit() {
        return Mockito.mock(Retrofit.class);
    }
}
