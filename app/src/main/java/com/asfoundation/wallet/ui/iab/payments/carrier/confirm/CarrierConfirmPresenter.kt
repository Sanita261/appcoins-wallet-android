package com.asfoundation.wallet.ui.iab.payments.carrier.confirm

import com.appcoins.wallet.billing.carrierbilling.CarrierPaymentModel
import com.appcoins.wallet.billing.common.response.TransactionStatus
import com.asf.wallet.R
import com.asfoundation.wallet.analytics.FacebookEventLogger
import com.asfoundation.wallet.billing.analytics.BillingAnalytics
import com.asfoundation.wallet.logging.Logger
import com.asfoundation.wallet.ui.iab.payments.carrier.CarrierInteractor
import com.asfoundation.wallet.util.applicationinfo.ApplicationInfoLoader
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import retrofit2.HttpException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class CarrierConfirmPresenter(private val disposables: CompositeDisposable,
                              private val view: CarrierConfirmView,
                              private val data: CarrierConfirmData,
                              private val navigator: CarrierConfirmNavigator,
                              private val interactor: CarrierInteractor,
                              private val billingAnalytics: BillingAnalytics,
                              private val appInfoLoader: ApplicationInfoLoader,
                              private val logger: Logger,
                              private val viewScheduler: Scheduler,
                              private val ioScheduler: Scheduler) {

  companion object {
    private val TAG = CarrierConfirmPresenter::class.java.simpleName
  }

  fun present() {
    initializeView()
    handleBackEvents()
    handleNextButton()
    handleTransactionResult()
  }

  private fun initializeView() {
    disposables.add(
        appInfoLoader.getApplicationInfo(data.domain)
            .observeOn(viewScheduler)
            .doOnSuccess { ai ->
              view.initializeView(ai.appName, ai.icon, data.currency, data.totalFiatAmount,
                  data.totalAppcAmount, data.skuDescription, data.bonusAmount, data.carrierName,
                  data.carrierImage, data.feeFiatAmount)
            }
            .subscribe({}, { e -> e.printStackTrace() })
    )
  }

  private fun handleNextButton() {
    disposables.add(
        view.nextClickEvent()
            .doOnNext { view.setLoading() }
            .flatMap { sendPaymentConfirmationEvent("buy").andThen(Observable.just(Unit)) }
            .observeOn(viewScheduler)
            .doOnNext {
              navigator.navigateToPaymentWebView(data.paymentUrl)
            }
            .retry()
            .subscribe({}, { e -> e.printStackTrace() })
    )
  }


  private fun handleBackEvents() {
    disposables.add(
        view.backEvent()
            .flatMap {
              sendPaymentConfirmationEvent("back")
                  .andThen(Observable.just(Unit))
            }
            .observeOn(viewScheduler)
            .doOnNext { navigator.navigateBack() }
            .retry()
            .subscribe({}, { e -> e.printStackTrace() })
    )
  }

  private fun handleTransactionResult() {
    disposables.add(navigator.uriResults()
        .doOnNext {
          view.disableAllBackEvents()
          view.lockRotation()
          view.setLoading()
        }
        .flatMapSingle { uri ->
          interactor.getFinishedPayment(uri, data.domain)
              .subscribeOn(ioScheduler)
        }
        .flatMap { payment ->
          when {
            isErrorStatus(payment.status) -> {
              val code =
                  if (payment.networkError.code == -1) "ERROR" else payment.networkError.code.toString()
              logger.log(TAG, "Transaction came with error status: ${payment.status}")
              return@flatMap sendPaymentErrorEvent(code,
                  payment.networkError.message)
                  .observeOn(viewScheduler)
                  .doOnComplete {
                    navigator.navigateToError(R.string.activity_iab_error_message, false)
                  }
                  .andThen(Observable.just(Unit))
            }
            payment.status == TransactionStatus.COMPLETED -> {
              return@flatMap sendPaymentSuccessEvents()
                  .observeOn(viewScheduler)
                  .andThen(
                      Completable.fromAction { view.showFinishedTransaction() }
                          .andThen(
                              Completable.timer(view.getFinishedDuration(), TimeUnit.MILLISECONDS))
                          .andThen(finishPayment(payment))
                  )
                  .andThen(Observable.just(Unit))
            }
            else -> Observable.just(Unit)
          }
        }
        .onErrorReturn { e -> handleError(e).andThen(Observable.just(Unit)) }
        .subscribe({}, { e -> e.printStackTrace() }))
  }

  private fun finishPayment(payment: CarrierPaymentModel): Completable {
    return interactor.getTransactionBuilder(data.transactionData)
        .flatMap { transaction ->
          interactor.getCompletePurchaseBundle(transaction.type, transaction.domain,
              transaction.skuId, payment.reference, payment.hash, ioScheduler)
        }
        .observeOn(viewScheduler)
        .doOnSuccess { bundle ->
          navigator.finishPayment(bundle)
        }
        .subscribeOn(ioScheduler)
        .ignoreElement()
  }

  private fun handleError(throwable: Throwable): Completable {
    logger.log(TAG, throwable)
    return if (throwable is HttpException) {
      sendPaymentErrorEvent(throwable.code()
          .toString(), throwable.message())
          .andThen(
              if (throwable.code() == 403) {
                handleFraudFlow()
              } else {
                Completable.complete()
              })
    } else {
      Completable.fromAction {
        navigator.navigateToError(R.string.unknown_error, false)
      }
          .subscribeOn(viewScheduler)
    }

  }

  private fun handleFraudFlow(): Completable {
    return interactor.getWalletStatus()
        .observeOn(viewScheduler)
        .doOnSuccess { walletStatus ->
          if (walletStatus.blocked) {
            if (walletStatus.verified) {
              navigator.navigateToError(
                  R.string.purchase_error_wallet_block_code_403, false)
            } else {
              navigator.navigateToWalletValidation(
                  R.string.purchase_error_wallet_block_code_403)
            }
          } else {
            navigator.navigateToError(R.string.purchase_error_wallet_block_code_403, false)
          }
        }
        .ignoreElement()
  }

  private fun sendPaymentErrorEvent(refusalCode: String?, refusalReason: String?): Completable {
    return interactor.getTransactionBuilder(data.transactionData)
        .doOnSuccess { transaction ->
          billingAnalytics.sendPaymentErrorWithDetailsEvent(data.domain, transaction.skuId,
              transaction.amount()
                  .toString(), BillingAnalytics.PAYMENT_METHOD_CARRIER, transaction.type,
              refusalCode.toString(), refusalReason)
        }
        .ignoreElement()
        .subscribeOn(ioScheduler)
  }

  private fun sendPaymentSuccessEvents(): Completable {
    return interactor.getTransactionBuilder(data.transactionData)
        .flatMap { transaction ->
          interactor.convertToFiat(transaction.amount()
              .toDouble(), FacebookEventLogger.EVENT_REVENUE_CURRENCY)
              .doOnSuccess { fiatValue ->
                billingAnalytics.sendPaymentSuccessEvent(data.domain, transaction.skuId,
                    transaction.amount()
                        .toString(), BillingAnalytics.PAYMENT_METHOD_CARRIER, transaction.type)
                billingAnalytics.sendPaymentEvent(data.domain, transaction.skuId,
                    transaction.amount()
                        .toString(), BillingAnalytics.PAYMENT_METHOD_CARRIER, transaction.type)
                billingAnalytics.sendRevenueEvent(fiatValue.amount.setScale(2, BigDecimal.ROUND_UP)
                    .toString())
              }
        }
        .ignoreElement()
        .subscribeOn(ioScheduler)
  }

  private fun sendPaymentConfirmationEvent(event: String): Completable {
    return interactor.getTransactionBuilder(data.transactionData)
        .doOnSuccess { transaction ->
          billingAnalytics.sendPaymentConfirmationEvent(data.domain, transaction.skuId,
              transaction.amount()
                  .toString(), BillingAnalytics.PAYMENT_METHOD_CARRIER, transaction.type, event)
        }
        .ignoreElement()
        .subscribeOn(ioScheduler)
  }

  private fun isErrorStatus(status: TransactionStatus) =
      status == TransactionStatus.FAILED ||
          status == TransactionStatus.CANCELED ||
          status == TransactionStatus.INVALID_TRANSACTION


  fun stop() = disposables.clear()
}