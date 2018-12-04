package com.asfoundation.wallet.ui.iab;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.appcoins.wallet.billing.util.PayloadHelper;
import com.asf.wallet.R;
import com.asfoundation.wallet.entity.TransactionBuilder;
import com.asfoundation.wallet.navigator.UriNavigator;
import com.asfoundation.wallet.ui.BaseActivity;
import com.jakewharton.rxrelay2.PublishRelay;
import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

import static com.appcoins.wallet.billing.AppcoinsBillingBinder.EXTRA_BDS_IAP;

/**
 * Created by franciscocalado on 20/07/2018.
 */

public class IabActivity extends BaseActivity implements IabView, UriNavigator {

  public static final String RESPONSE_CODE = "RESPONSE_CODE";
  public static final int RESULT_USER_CANCELED = 1;
  public static final String SKU_DETAILS = "sku_details";
  public static final String APP_PACKAGE = "app_package";
  public static final String TRANSACTION_EXTRA = "transaction_extra";
  public static final String PRODUCT_NAME = "product_name";
  public static final String EXTRA_DEVELOPER_PAYLOAD = "developer_payload";
  public static final String TRANSACTION_DATA = "transaction_data";
  public static final String TRANSACTION_HASH = "transaction_hash";
  public static final String TRANSACTION_AMOUNT = "transaction_amount";
  public static final String TRANSACTION_CURRENCY = "transaction_currency";
  public static final String DEVELOPER_PAYLOAD = "developer_payload";
  public static final String FIAT_VALUE = "fiat_value";
  private static final String BDS = "BDS";
  private static final String TAG = IabActivity.class.getSimpleName();
  private static final int WEB_VIEW_REQUEST_CODE = 1234;
  private static final String IS_BDS_EXTRA = "is_bds_extra";
  @Inject InAppPurchaseInteractor inAppPurchaseInteractor;
  private boolean isBackEnable;
  private IabPresenter presenter;
  private Bundle savedInstanceState;
  private Bundle skuDetails;
  private TransactionBuilder transaction;
  private boolean isBds;
  private PublishRelay<Uri> results;
  private String developerPayload;

  public static Intent newIntent(Activity activity, Intent previousIntent,
      TransactionBuilder transaction, Boolean isBds, String developerPayload) {
    Intent intent = new Intent(activity, IabActivity.class);
    intent.setData(previousIntent.getData());
    if (previousIntent.getExtras() != null) {
      intent.putExtras(previousIntent.getExtras());
    }
    intent.putExtra(TRANSACTION_EXTRA, transaction);
    intent.putExtra(IS_BDS_EXTRA, isBds);
    intent.putExtra(DEVELOPER_PAYLOAD, developerPayload);
    intent.putExtra(APP_PACKAGE, transaction.getDomain());
    return intent;
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
    results = PublishRelay.create();
    setContentView(R.layout.activity_iab);
    this.savedInstanceState = savedInstanceState;
    isBds = getIntent().getBooleanExtra(IS_BDS_EXTRA, false);
    developerPayload = getIntent().getStringExtra(DEVELOPER_PAYLOAD);
    transaction = getIntent().getParcelableExtra(TRANSACTION_EXTRA);
    isBackEnable = true;
    presenter = new IabPresenter(this);

    if (savedInstanceState != null) {
      if (savedInstanceState.containsKey(SKU_DETAILS)) {
        skuDetails = savedInstanceState.getBundle(SKU_DETAILS);
      }
    }
    presenter.present(savedInstanceState);
  }

  @Override public void onBackPressed() {
    if (isBackEnable) {
      Bundle bundle = new Bundle();
      bundle.putInt(RESPONSE_CODE, RESULT_USER_CANCELED);
      close(bundle);
      super.onBackPressed();
    }
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    results.accept(Objects.requireNonNull(intent.getData(), "Intent data cannot be null!"));
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBundle(SKU_DETAILS, skuDetails);
  }

  @Override public void finish(Bundle bundle) {
    setResult(Activity.RESULT_OK, new Intent().putExtras(bundle));
    finish();
  }

  @Override public void showError() {
    setResult(Activity.RESULT_CANCELED);
    finish();
  }

  @Override public void close(Bundle data) {
    Intent intent = new Intent();
    if (data != null) {
      intent.putExtras(data);
    }
    setResult(Activity.RESULT_CANCELED, intent);
    finish();
  }

  @Override
  public void navigateToAdyenAuthorization(boolean isBds, String currency, String paymentType) {
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.fragment_container,
            AdyenAuthorizationFragment.newInstance(transaction.getSkuId(), transaction.getType(),
                isBds ? BDS : null, paymentType, transaction.getDomain(),
                getIntent().getDataString(), transaction.amount(), currency))
        .commit();
  }

  @Override public void navigateToWebViewAuthorization(String url) {
    startActivityForResult(WebViewActivity.newIntent(this, url), WEB_VIEW_REQUEST_CODE);
  }

  @Override public void showPaymentMethodsView(String currency) {
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.fragment_container, PaymentMethodsFragment.newInstance(transaction, currency,
            getIntent().getExtras()
                .getString(PRODUCT_NAME), isBds, developerPayload))
        .commit();
  }

  @Override public void showOnChain(BigDecimal amount, boolean isBds) {
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.fragment_container, OnChainBuyFragment.newInstance(createBundle(amount),
              getIntent().getData()
                  .toString(), isBds))
          .commit();
    }
  }

  @Override public void showAdyenPayment(BigDecimal amount, String currency, boolean isBds,
      String paymentType) {
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.fragment_container, ExpressCheckoutBuyFragment.newInstance(
              createBundle(BigDecimal.valueOf(amount.doubleValue()), currency), isBds, paymentType))
          .commit();
    }
  }

  @Override public void showAppcoinsCreditsPayment(BigDecimal amount) {
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.fragment_container,
              AppcoinsRewardsBuyFragment.newInstance(amount, transaction.getDomain(),
                  getIntent().getData()
                      .toString(), getIntent().getExtras()
                      .getString(PRODUCT_NAME, ""), isBds))
          .commit();
    }
  }

  @Override public void showPaymentMethods(
      List<com.asfoundation.wallet.ui.iab.PaymentMethod> paymentMethods) {

  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == WEB_VIEW_REQUEST_CODE) {
      if (resultCode == WebViewActivity.FAIL) {
        finish();
      }
    }
  }

  @NonNull private Bundle createBundle(BigDecimal amount, String currency) {
    Bundle bundle = createBundle(amount);

    bundle.putSerializable(TRANSACTION_CURRENCY, currency);

    return bundle;
  }

  @NonNull private Bundle createBundle(BigDecimal amount) {
    Bundle bundle = new Bundle();
    bundle.putSerializable(TRANSACTION_AMOUNT, amount);
    bundle.putString(APP_PACKAGE, transaction.getDomain());
    bundle.putString(PRODUCT_NAME, getIntent().getExtras()
        .getString(PRODUCT_NAME));
    bundle.putString(TRANSACTION_DATA, getIntent().getDataString());
    String developerPayloadStr = getIntent().getExtras()
        .getString(EXTRA_DEVELOPER_PAYLOAD);
    String developerPayload = "unknown".equals(developerPayloadStr) ? null
        : PayloadHelper.INSTANCE.getPayload(getIntent().getExtras()
            .getString(EXTRA_DEVELOPER_PAYLOAD));
    if (developerPayload != null) {
      bundle.putString(EXTRA_DEVELOPER_PAYLOAD, developerPayload);
    } else {
      bundle.putString(EXTRA_DEVELOPER_PAYLOAD, transaction.getPayload());
    }
    skuDetails = bundle;
    return bundle;
  }

  public String getAppPackage() {
    if (getIntent().hasExtra(APP_PACKAGE)) {
      return getIntent().getStringExtra(APP_PACKAGE);
    }
    throw new IllegalArgumentException("previous app package name not found");
  }

  public boolean isBds() {
    return getIntent().getBooleanExtra(EXTRA_BDS_IAP, false);
  }

  @Override public void navigateToUri(String url) {
    navigateToWebViewAuthorization(url);
  }

  @Override public Observable<Uri> uriResults() {
    return results;
  }
}