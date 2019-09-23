package com.flutterwave.raveandroid.banktransfer;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;

import com.flutterwave.raveandroid.DeviceIdGetter;
import com.flutterwave.raveandroid.FeeCheckRequestBody;
import com.flutterwave.raveandroid.GetEncryptedData;
import com.flutterwave.raveandroid.Meta;
import com.flutterwave.raveandroid.Payload;
import com.flutterwave.raveandroid.PayloadBuilder;
import com.flutterwave.raveandroid.PayloadToJson;
import com.flutterwave.raveandroid.RaveConstants;
import com.flutterwave.raveandroid.RavePayInitializer;
import com.flutterwave.raveandroid.ViewObject;
import com.flutterwave.raveandroid.WhiteBox;
import com.flutterwave.raveandroid.card.ChargeRequestBody;
import com.flutterwave.raveandroid.data.Callbacks;
import com.flutterwave.raveandroid.data.NetworkRequestImpl;
import com.flutterwave.raveandroid.data.RequeryRequestBody;
import com.flutterwave.raveandroid.di.DaggerTestAppComponent;
import com.flutterwave.raveandroid.di.TestAndroidModule;
import com.flutterwave.raveandroid.di.TestAppComponent;
import com.flutterwave.raveandroid.di.TestNetworkModule;
import com.flutterwave.raveandroid.responses.ChargeResponse;
import com.flutterwave.raveandroid.responses.FeeCheckResponse;
import com.flutterwave.raveandroid.responses.RequeryResponse;
import com.flutterwave.raveandroid.responses.SubAccount;
import com.flutterwave.raveandroid.validators.AmountValidator;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BankTransferPresenterTest {

    @Mock
    BankTransferContract.View view;
    @Inject
    Context context;
    @Inject
    AmountValidator amountValidator;
    @Inject
    RavePayInitializer ravePayInitializer;
    @Inject
    DeviceIdGetter deviceIdGetter;
    @Inject
    GetEncryptedData getEncryptedData;
    @Inject
    PayloadToJson payloadToJson;
    @Inject
    NetworkRequestImpl networkRequest;
    @Inject
    Bundle bundle;
    @Mock
    BankTransferPresenter bankTransferPresenterMock;
    @Mock
    PayloadBuilder payloadBuilder;
    @Mock
    RequeryRequestBody requeryRequestBody;
    private BankTransferPresenter bankTransferPresenter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        bankTransferPresenter = new BankTransferPresenter(context, view);

        TestAppComponent component = DaggerTestAppComponent.builder()
                .testAndroidModule(new TestAndroidModule())
                .testNetworkModule(new TestNetworkModule())
                .build();

        component.inject(this);
        component.inject(bankTransferPresenter);
    }

    @Test
    public void fetchFee_onError_showFetchFeeFailedCalled_errorOccurredMessage() {

        bankTransferPresenter.fetchFee(generatePayload());

        ArgumentCaptor<Callbacks.OnGetFeeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnGetFeeRequestComplete.class);
        verify(networkRequest).getFee(any(FeeCheckRequestBody.class), captor.capture());

        captor.getAllValues().get(0).onError(generateRandomString());
        verify(view).showProgressIndicator(false);
        verify(view).showFetchFeeFailed("An error occurred while retrieving transaction fee");

    }

    @Test
    public void fetchFee_onSuccess_displayFeeCalled() {

        bankTransferPresenter.fetchFee(generatePayload());

        ArgumentCaptor<Callbacks.OnGetFeeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnGetFeeRequestComplete.class);
        verify(networkRequest).getFee(any(FeeCheckRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(generateFeeCheckResponse());

        verify(view).displayFee(anyString(), any(Payload.class));

    }

    @Test(expected = Exception.class)
    public void fetchFee_onSuccess_nullException_showFetchFeeFailedCalled() {

        bankTransferPresenter.fetchFee(generatePayload());

        ArgumentCaptor<Callbacks.OnGetFeeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnGetFeeRequestComplete.class);
        verify(networkRequest).getFee(any(FeeCheckRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(new FeeCheckResponse());

        doThrow(new Exception())
                .when(view)
                .displayFee(any(String.class), any(Payload.class));

        verify(view).showFetchFeeFailed("An error occurred while retrieving transaction fee");

    }

    @Test
    public void payWithBankTransfer_chargeCard_onSuccess_onTransferDetailsReceivedCalled() {

        ChargeResponse chargeResponse = generateValidChargeResponse();

        when(payloadToJson.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(getEncryptedData.getEncryptedData(any(String.class), any(String.class)))
                .thenReturn(generateRandomString());

        bankTransferPresenter.payWithBankTransfer(generatePayload(), generateRandomString());
        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);


        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(chargeResponse, any(String.class));

        verify(view).showProgressIndicator(false);
        verify(view).onTransferDetailsReceived(
                chargeResponse.getData().getAmount()
                , chargeResponse.getData().getAccountnumber(),
                chargeResponse.getData().getBankname(),
                chargeResponse.getData().getNote().substring(
                chargeResponse.getData().getNote().indexOf("to ") + 3));
    }

    @Test
    public void payWithBankTransfer_chargeCard_onSuccess_nullResponse_onTransferDetailsReceivedCalled() {

        when(payloadToJson.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(getEncryptedData.getEncryptedData(any(String.class), any(String.class)))
                .thenReturn(generateRandomString());

        bankTransferPresenter.payWithBankTransfer(generatePayload(), generateRandomString());
        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);

        ChargeResponse chargeResponse = new ChargeResponse();

        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(chargeResponse, generateRandomString());

        verify(view).onPaymentError("No response data was returned");
    }

    @Test
    public void payWithBankTransfer_chargeCard_onError_onPaymentErrorCalledWithCorrectParams() {

        when(payloadToJson.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(getEncryptedData.getEncryptedData(any(String.class), any(String.class)))
                .thenReturn(generateRandomString());
        String message = generateRandomString();
        String jsonResponse = generateRandomString();

        bankTransferPresenter.payWithBankTransfer(generatePayload(), generateRandomString());
        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);

        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onError(message, jsonResponse);

        verify(view).showProgressIndicator(false);
        verify(view).onPaymentError(message);
    }

    @Test
    public void startPaymentVerification_requeryTxCalledWithCorrectParams() throws NoSuchFieldException {

        //Arrange
        String txRef = generateRandomString();
        String flwRef = generateRandomString();
        String publicKey = generateRandomString();
        boolean pollingCancelled = generateRandomBoolean();

        WhiteBox.setInternalState(bankTransferPresenterMock, "flwRef", flwRef);
        WhiteBox.setInternalState(bankTransferPresenterMock, "txRef", txRef);
        WhiteBox.setInternalState(bankTransferPresenterMock, "publicKey", publicKey);
        WhiteBox.setInternalState(bankTransferPresenterMock, "pollingCancelled", pollingCancelled);
        WhiteBox.setInternalState(bankTransferPresenterMock, "mView", view);

        doCallRealMethod().when(bankTransferPresenterMock).startPaymentVerification();
        bankTransferPresenterMock.startPaymentVerification();

        long requeryCountdownTime = (long) WhiteBox.getInternalState(bankTransferPresenterMock, "requeryCountdownTime");

        verify(view).showPollingIndicator(true);

        verify(bankTransferPresenterMock)
                .requeryTx(flwRef, txRef,
                        publicKey, true, requeryCountdownTime);
    }

    @Test
    public void cancelPolling_pollingCancelledTrue() {
        bankTransferPresenter.cancelPolling();

        boolean pollingCancelled = (boolean) WhiteBox.getInternalState(bankTransferPresenter, "pollingCancelled");

        assertTrue(pollingCancelled);
    }

    @Test
    public void requeryTx_onSuccess_onRequerySuccessful_onPaymentSuccessful_00_Called() {

        String randomString = generateRandomString();
        long time = System.currentTimeMillis();

        bankTransferPresenter.requeryTx(randomString, randomString, randomString, true, time);
        requeryRequestBody.setFlw_ref(generateRandomString());
        requeryRequestBody.setPBFPubKey(generateRandomString());
        String responseJson = generateRandomString();
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);

        verify(networkRequest).requeryPayWithBankTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(generateRequerySuccessful("00"), responseJson);

        verify(view).showPollingIndicator(false);
        verify(view).onPaymentSuccessful(randomString, randomString, responseJson);

    }

    @Test
    public void requeryTx_onSuccess_onRequerySuccessful_onPaymentSuccessful_01_onPollingTimeoutCalled() {

        String randomString = generateRandomString();
        long time = 400000;
        bankTransferPresenter.requeryTx(randomString, randomString, randomString, false, time);

        String responseJson = generateRandomString();
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);


        verify(networkRequest).requeryPayWithBankTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(generateRequerySuccessful("01"), responseJson);

        verify(view).showPollingIndicator(false);
        verify(view).onPollingTimeout(randomString, randomString, responseJson);

    }

    @Test
    public void requeryTx_onSuccess_onRequerySuccessful_onPaymentSuccessful_01_requeryTxCalled() {

        String randomString = generateRandomString();
        long time = 10000;
        bankTransferPresenter.requeryTx(randomString, randomString, randomString, false, time);

        String responseJson = generateRandomString();
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);


        verify(networkRequest).requeryPayWithBankTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(generateRequerySuccessful("01"), responseJson);

        bankTransferPresenterMock.requeryTx(randomString, randomString, randomString, false, time);
        verify(bankTransferPresenterMock).requeryTx(randomString, randomString, randomString, false, time);

    }


    @Test
    public void requeryTx_onSuccess_onRequerySuccessful_onPaymentSuccessful_01_PollingCancelled() {

        String randomString = generateRandomString();
        long time = System.currentTimeMillis();
        bankTransferPresenter.requeryTx(randomString, randomString, randomString, true, time);

        String responseJson = generateRandomString();
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);


        verify(networkRequest).requeryPayWithBankTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(generateRequerySuccessful("01"), responseJson);

        verify(view).showPollingIndicator(false);
        verify(view).onPollingCanceled(randomString, randomString, responseJson);

    }


    @Test
    public void requeryTx_onSuccess_nullResponse_onPaymentFailedCalled() {

        long time = System.currentTimeMillis();
        RequeryResponse requeryResponse = new RequeryResponse();
        String jsonResponse = generateRandomString();
        bankTransferPresenter.requeryTx(generateRandomString(), generateRandomString(), generateRandomString(), true, time);
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);

        verify(networkRequest).requeryPayWithBankTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(requeryResponse, jsonResponse);

        verify(view).onPaymentFailed(requeryResponse.getStatus(), jsonResponse);

    }


    @Test
    public void requeryTx_onError_onPaymentFailedCalled() {

        long time = System.currentTimeMillis();
        bankTransferPresenter.requeryTx(generateRandomString(), generateRandomString(), generateRandomString(), true, time);
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);

        verify(networkRequest).requeryPayWithBankTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onError(generateRandomString(), generateRandomString());

        verify(view).onPaymentFailed(anyString(), anyString());

    }


    @Test
    public void init_validAmount_onAmountValidatedCalledWithValidAmount() {

        Double amount = generateRandomDouble();
        when(amountValidator.isAmountValid(amount)).thenReturn(true);
        when(ravePayInitializer.getAmount()).thenReturn(amount);

        bankTransferPresenter.init(ravePayInitializer);

        verify(view).onAmountValidationSuccessful(amount.toString());

    }

    @Test
    public void init_inValidAmount_onAmountValidatedCalledWithEmptyAmount() {

        Double amount = generateRandomDouble();
        when(amountValidator.isAmountValid(amount)).thenReturn(false);

        bankTransferPresenter.init(ravePayInitializer);
        verify(view).onAmountValidationFailed();

    }


    @Test
    public void onDataCollected_inValidDataPassed_showFieldErrorCalled() {
        int viewID = generateRandomInt();
        HashMap<String, ViewObject> hashMap = generateViewData(viewID);
        int failedValidations = 1;
        generateViewValidation(failedValidations);

        bankTransferPresenter.onDataCollected(hashMap);

        verify(view, times(failedValidations)).showFieldError(viewID, RaveConstants.validAmountPrompt, hashMap.get(RaveConstants.fieldAmount).getViewType());

    }


    @Test
    public void onDataCollected_validDataPassed_onValidationSuccessfulCalled() {
        //arrange
        int viewID = generateRandomInt();
        HashMap<String, ViewObject> map = generateViewData(viewID);
        int failedValidations = 0;
        generateViewValidation(failedValidations);
        //act
        bankTransferPresenter.onDataCollected(map);
        //assert
        verify(view).onValidationSuccessful(map);

    }


    @Test
    public void processTransaction_displayFeeIsEnabled_progressDialogShown() {
        //arrange
        int viewID = generateRandomInt();
        HashMap<String, ViewObject> data = generateViewData(viewID);
        when(ravePayInitializer.getIsDisplayFee()).thenReturn(true);
        when(deviceIdGetter.getDeviceId()).thenReturn(generateRandomString());
        //act
        bankTransferPresenter.processTransaction(data, ravePayInitializer);
        //assert
        verify(view).showProgressIndicator(true);

    }

    @Test
    public void processTransaction_displayFeeIsEnabled_payWithBankTransferCalled() {
        //arrange
        int viewID = generateRandomInt();
        HashMap<String, ViewObject> data = generateViewData(viewID);
        when(ravePayInitializer.getIsDisplayFee()).thenReturn(true);
        when(deviceIdGetter.getDeviceId()).thenReturn(generateRandomString());
        //act
        bankTransferPresenter.processTransaction(data, ravePayInitializer);
        bankTransferPresenterMock.payWithBankTransfer(generatePayload(), generateRandomString());
        //assert
        verify(view).showProgressIndicator(true);
        verify(bankTransferPresenterMock).payWithBankTransfer(any(Payload.class), any(String.class));

    }

    @Test
    public void processTransaction_payloadBuilderCalled() {
        //arrange
        int viewID = generateRandomInt();
        HashMap<String, ViewObject> data = generateViewData(viewID);
        when(ravePayInitializer.getIsDisplayFee()).thenReturn(true);
        when(deviceIdGetter.getDeviceId()).thenReturn(generateRandomString());
        //act
        bankTransferPresenter.processTransaction(data, ravePayInitializer);
        payloadBuilder.createBankPayload();
        //assert
        verify(view).showProgressIndicator(true);
        verify(payloadBuilder).createBankPayload();

    }

    @Test
    public void processTransaction_displayFeeIsDisabled_chargeAccountCalled() {
        //arrange
        int viewID = generateRandomInt();
        HashMap<String, ViewObject> data = generateViewData(viewID);
        when(ravePayInitializer.getIsDisplayFee()).thenReturn(false);
        when(deviceIdGetter.getDeviceId()).thenReturn(generateRandomString());

        Payload payload = generatePayload();
        //act
        bankTransferPresenter.processTransaction(data, ravePayInitializer);
        //assert
        bankTransferPresenter.payWithBankTransfer(payload, generateRandomString());
    }

    @Test
    public void restoreState_onTransferDetailsReceivedCalled() {

        when(bundle.getString("amount")).thenReturn(generateRandomString());
        when(bundle.getString("benef_name")).thenReturn(generateRandomString());
        when(bundle.getString("bank_name")).thenReturn(generateRandomString());
        when(bundle.getString("account_number")).thenReturn(generateRandomString());

        bankTransferPresenter.restoreState(bundle);
        verify(view).onTransferDetailsReceived(any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    public void getState_hasTransferDetailsFalse_nullReturned() {
        bankTransferPresenter.hasTransferDetails = false;
        Bundle bundle = bankTransferPresenter.getState();

        assertNull(bundle);
    }

    @Test
    public void getState_hasTransferDetailsTrue_nullReturned() {
        bankTransferPresenter.hasTransferDetails = true;
        Bundle bundle = bankTransferPresenter.getState();

        assertNotNull(bundle);
    }

    private Boolean generateRandomBoolean() {
        return new Random().nextBoolean();
    }
    private Double generateRandomDouble() {
        return new Random().nextDouble();
    }

    private HashMap<String, ViewObject> generateViewData(int viewID) {

        HashMap<String, ViewObject> viewData = new HashMap<>();
        viewData.put(fieldAmount, new ViewObject(viewID, generateRandomDouble().toString(), TextInputLayout.class));

        return viewData;
    }

    private void generateViewValidation(int failedValidations) {

        List<Boolean> falses = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            if (i < failedValidations) {
                falses.add(false);
            } else {
                falses.add(true);
            }
        }

        when(amountValidator.isAmountValid(anyString())).thenReturn(falses.get(0));
    }

    private int generateRandomInt() {
        return new Random().nextInt();
    }

    private String generateRandomString() {
        return UUID.randomUUID().toString();
    }

    private Payload generatePayload() {
        List<Meta> metas = new ArrayList<>();
        List<SubAccount> subAccounts = new ArrayList<>();
        return new Payload(generateRandomString(), metas, subAccounts, generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString());
    }

    private RequeryResponse generateRequerySuccessful(String responseCode) {
        RequeryResponse requeryResponse = new RequeryResponse();
        RequeryResponse.Data data = new RequeryResponse.Data();
        data.setChargeResponseCode(responseCode);
        requeryResponse.setData(data);
        return requeryResponse;
    }

    private ChargeResponse generateValidChargeResponse() {
        ChargeResponse chargeResponse = new ChargeResponse();
        chargeResponse.setData(new ChargeResponse.Data());

        chargeResponse.getData().setChargeResponseCode("00");
        chargeResponse.getData().setNote(generateRandomString());
        chargeResponse.getData().setChargedAmount(generateRandomString());
        chargeResponse.getData().setBankName(generateRandomString());
        chargeResponse.getData().setAccountnumber(generateRandomString());
        return chargeResponse;
    }

    private FeeCheckResponse generateFeeCheckResponse() {
        FeeCheckResponse feeCheckResponse = new FeeCheckResponse();
        FeeCheckResponse.Data feeCheckResponseData = new FeeCheckResponse.Data();

        feeCheckResponseData.setCharge_amount(generateRandomString());
        feeCheckResponse.setData(feeCheckResponseData);

        return feeCheckResponse;
    }

    private boolean throwException() {
        throw new NullPointerException();
    }

    Bundle generateBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("amount", "100");
        return bundle;
    }
}