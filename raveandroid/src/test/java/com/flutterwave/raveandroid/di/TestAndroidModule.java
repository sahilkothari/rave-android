package com.flutterwave.raveandroid.di;

import android.content.Context;
import android.test.mock.MockContext;

import com.flutterwave.raveandroid.DeviceIdGetter;
import com.flutterwave.raveandroid.RavePayInitializer;
import com.flutterwave.raveandroid.data.NetworkRequestImpl;
import com.flutterwave.raveandroid.validators.AmountValidator;
import com.flutterwave.raveandroid.validators.CardExpiryValidator;
import com.flutterwave.raveandroid.validators.CardNoValidator;
import com.flutterwave.raveandroid.validators.CvvValidator;
import com.flutterwave.raveandroid.validators.EmailValidator;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class TestAndroidModule {

    @Singleton
    @Provides
    public Context providesContext() {
        return new MockContext();
    }

    @Singleton
    @Provides
    public DeviceIdGetter providesDeviceIdGetter() {
        return Mockito.mock(DeviceIdGetter.class);
    }

    @Singleton
    @Provides
    public AmountValidator providesAmountValidator() {
        return Mockito.mock(AmountValidator.class);
    }

    @Singleton
    @Provides
    public EmailValidator providesEmailValidator() {
        return Mockito.mock(EmailValidator.class);
    }

    @Singleton
    @Provides
    public CvvValidator providesCvvValidator() {
        return Mockito.mock(CvvValidator.class);
    }

    @Singleton
    @Provides
    public CardExpiryValidator providesCardExpiryValidator() {
        return Mockito.mock(CardExpiryValidator.class);
    }

    @Singleton
    @Provides
    public CardNoValidator providesCardNoValidator() {
        return Mockito.mock(CardNoValidator.class);
    }

    @Singleton
    @Provides
    public NetworkRequestImpl providesNetworkRequestImpl() {
        return Mockito.mock(NetworkRequestImpl.class);
    }

    @Singleton
    @Provides
    public RavePayInitializer providesRavePayInitializer() {
        return Mockito.mock(RavePayInitializer.class);
    }
}
