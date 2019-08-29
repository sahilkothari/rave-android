package com.flutterwave.raveandroid.card;

import android.content.Context;
import android.support.design.widget.TextInputLayout;

import com.flutterwave.raveandroid.DeviceIdGetter;
import com.flutterwave.raveandroid.RavePayInitializer;
import com.flutterwave.raveandroid.ViewObject;
import com.flutterwave.raveandroid.data.NetworkRequestImpl;
import com.flutterwave.raveandroid.di.DaggerTestAppComponent;
import com.flutterwave.raveandroid.di.TestAndroidModule;
import com.flutterwave.raveandroid.di.TestNetworkModule;
import com.flutterwave.raveandroid.validators.AmountValidator;
import com.flutterwave.raveandroid.validators.CardExpiryValidator;
import com.flutterwave.raveandroid.validators.CardNoValidator;
import com.flutterwave.raveandroid.validators.CvvValidator;
import com.flutterwave.raveandroid.validators.EmailValidator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;

import static com.flutterwave.raveandroid.RaveConstants.fieldAmount;
import static com.flutterwave.raveandroid.RaveConstants.fieldCardExpiry;
import static com.flutterwave.raveandroid.RaveConstants.fieldCvv;
import static com.flutterwave.raveandroid.RaveConstants.fieldEmail;
import static com.flutterwave.raveandroid.RaveConstants.fieldcardNoStripped;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CardPresenterTest {

    CardPresenter presenter;
    @Inject
    Context contextMock;
    @Mock
    CardContract.View viewMock;
    @Inject
    AmountValidator amountValidator;
    @Inject
    CvvValidator cvvValidator;
    @Inject
    EmailValidator emailValidator;
    @Inject
    CardExpiryValidator cardExpiryValidator;
    @Inject
    CardNoValidator cardNoValidator;
    @Inject
    NetworkRequestImpl networkRequest;
    @Inject
    RavePayInitializer ravePayInitializer;
    @Inject
    DeviceIdGetter deviceIdGetter;

    @Before
    public void setUp() {

        DaggerTestAppComponent.builder()
                .testAndroidModule(new TestAndroidModule())
                .testNetworkModule(new TestNetworkModule())
                .build().inject(this);

        MockitoAnnotations.initMocks(this);
        presenter = new CardPresenter(contextMock, viewMock);
        presenter.amountValidator = amountValidator;
        presenter.cvvValidator = cvvValidator;
        presenter.emailValidator = emailValidator;
        presenter.cardExpiryValidator = cardExpiryValidator;
        presenter.cardNoValidator = cardNoValidator;
        presenter.networkRequest = networkRequest;
        presenter.deviceIdGetter = deviceIdGetter;


    }

    @Test
    public void onDataCollected_invalidDataPassed_showFieldErrorCalled() {
        //Arrange
        HashMap<String, ViewObject> invalidData = generateViewData();
        int noOfFailedValidations = 3;
        generateFailedViewValidation(noOfFailedValidations);
        //Act
        presenter.onDataCollected(invalidData);
        //Assert
        verify(viewMock, times(noOfFailedValidations)).showFieldError(anyInt(), anyString(), any(Class.class));
    }

    @Test
    public void onDataCollected_validDataPassed_onValidationSuccessful() {
        //Arrange
        HashMap<String, ViewObject> data = generateViewData();
        int noOfFailedValidations = 0;
        generateFailedViewValidation(noOfFailedValidations);
        //Act
        presenter.onDataCollected(data);
        //Assert
        verify(viewMock).onValidationSuccessful(any());
    }

    @Test
    public void processTransaction_feeDisplayFlagEnabled_displaysGetFeeLoadingDialog_callsGetFee() {

        when(deviceIdGetter.getDeviceImei()).thenReturn(generateRandomString());
        when(ravePayInitializer.getIsDisplayFee()).thenReturn(true);
        ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
        HashMap<String, ViewObject> data = generateViewData();

        presenter.processTransaction(data, ravePayInitializer);

        verify(viewMock).showProgressIndicator(booleanArgumentCaptor.capture());
        assertEquals(booleanArgumentCaptor.getAllValues().get(0), true);
        verify(networkRequest).getFee(any(), any());
    }

    @Test
    public void processTransaction_feeDisplayFlagDisabled_chargeCardCalled() {

        when(deviceIdGetter.getDeviceImei()).thenReturn(generateRandomString());
        when(ravePayInitializer.getIsDisplayFee()).thenReturn(false);
        HashMap<String, ViewObject> data = generateViewData();

        presenter.processTransaction(data, ravePayInitializer);
        verify(networkRequest).chargeCard(any(), any());
    }

    private void generateFailedViewValidation(int noOfFailedValidations) {

        List<Boolean> falses = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            if (i < noOfFailedValidations) {
                falses.add(false);
            } else {
                falses.add(true);
            }
        }

        when(amountValidator.isAmountValid(anyString())).thenReturn(falses.get(0));
        when(cvvValidator.isCvvValid(anyString())).thenReturn(falses.get(1));
        when(emailValidator.isEmailValid(anyString())).thenReturn(falses.get(2));
        when(cardExpiryValidator.isCardExpiryValid(anyString())).thenReturn(falses.get(3));
        when(cardNoValidator.isCardNoStrippedValid(anyString())).thenReturn(falses.get(4));
    }

    private HashMap<String, ViewObject> generateViewData() {
        HashMap<String, ViewObject> map = new HashMap<>();
        map.put(fieldAmount, new ViewObject(generateRandomInt(), generateRandomInt() + "", TextInputLayout.class));
        map.put(fieldcardNoStripped, new ViewObject(generateRandomInt(), generateRandomString(), TextInputLayout.class));
        map.put(fieldCvv, new ViewObject(generateRandomInt(), generateRandomString(), TextInputLayout.class));
        map.put(fieldCardExpiry, new ViewObject(generateRandomInt(), generateRandomString(), TextInputLayout.class));
        map.put(fieldEmail, new ViewObject(generateRandomInt(), generateRandomString(), TextInputLayout.class));
        return map;
    }

    private int generateRandomInt() {
        return new Random().nextInt();
    }

    private String generateRandomString() {
        return UUID.randomUUID().toString();
    }
}