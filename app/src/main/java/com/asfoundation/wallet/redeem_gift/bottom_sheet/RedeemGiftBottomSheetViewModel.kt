package com.asfoundation.wallet.redeem_gift.bottom_sheet

import com.asfoundation.wallet.base.Async
import com.asfoundation.wallet.base.BaseViewModel
import com.asfoundation.wallet.base.SideEffect
import com.asfoundation.wallet.base.ViewState
import com.asfoundation.wallet.redeem_gift.repository.RedeemCode
import com.asfoundation.wallet.redeem_gift.use_cases.RedeemGiftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class RedeemGiftBottomSheetSideEffect : SideEffect {
  object NavigateBack : RedeemGiftBottomSheetSideEffect()
}

data class RedeemGiftBottomSheetState(val submitRedeemAsync: Async<RedeemCode> = Async.Uninitialized,
                                      val shouldShowDefault: Boolean = false) : ViewState

@HiltViewModel
class RedeemGiftBottomSheetViewModel @Inject constructor(
    private val redeemGiftUseCase: RedeemGiftUseCase
  ) :
  BaseViewModel<RedeemGiftBottomSheetState, RedeemGiftBottomSheetSideEffect>(initialState()) {

  companion object {
    fun initialState(): RedeemGiftBottomSheetState {
      return RedeemGiftBottomSheetState()
    }
  }

  fun submitClick(redeemGiftString: String) {
    redeemGiftUseCase(redeemGiftString)
      .asAsyncToState { async ->
        copy(submitRedeemAsync = async)
      }
      .repeatableScopedSubscribe(RedeemGiftBottomSheetState::submitRedeemAsync.name) { e ->
        e.printStackTrace()
      }
  }

  fun successGotItClick() = sendSideEffect { RedeemGiftBottomSheetSideEffect.NavigateBack }

  fun tryAgainClick() {
    state.submitRedeemAsync.value
  }
}
