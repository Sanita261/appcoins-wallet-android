package com.asfoundation.wallet.wallet_verification.code

import io.reactivex.Observable

interface WalletVerificationCodeView {

  fun showLoading()

  fun hideLoading()

  fun hideKeyboard()

  fun lockRotation()

  fun unlockRotation()

  fun showSuccess()

  fun showGenericError()

  fun showNetworkError()

  fun showSpecificError(stringRes: Int)

  fun getMaybeLaterClicks(): Observable<Any>

  fun getChangeCardClicks(): Observable<Any>

  fun getConfirmClicks(): Observable<String>

  fun getTryAgainClicks(): Observable<Any>

  fun getSupportClicks(): Observable<Any>

}