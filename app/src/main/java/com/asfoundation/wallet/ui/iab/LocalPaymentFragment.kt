package com.asfoundation.wallet.ui.iab

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.FontAssetDelegate
import com.airbnb.lottie.TextDelegate
import com.asf.wallet.R
import com.asfoundation.wallet.navigator.UriNavigator
import com.asfoundation.wallet.ui.iab.LocalPaymentView.ViewState
import com.asfoundation.wallet.ui.iab.LocalPaymentView.ViewState.*
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.support.DaggerFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_iab_transaction_completed.view.*
import kotlinx.android.synthetic.main.iab_error_layout.view.*
import kotlinx.android.synthetic.main.local_payment_layout.*
import kotlinx.android.synthetic.main.pending_user_payment_view.*
import kotlinx.android.synthetic.main.pending_user_payment_view.view.*
import kotlinx.android.synthetic.main.support_error_layout.*
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LocalPaymentFragment : DaggerFragment(), LocalPaymentView {

  companion object {

    private const val DOMAIN_KEY = "domain"
    private const val SKU_ID_KEY = "skuId"
    private const val ORIGINAL_AMOUNT_KEY = "original_amount"
    private const val CURRENCY_KEY = "currency"
    private const val PAYMENT_KEY = "payment_name"
    private const val BONUS_KEY = "bonus"
    private const val STATUS_KEY = "status"
    private const val TYPE_KEY = "type"
    private const val DEV_ADDRESS_KEY = "dev_address"
    private const val AMOUNT_KEY = "amount"
    private const val CALLBACK_URL = "CALLBACK_URL"
    private const val ORDER_REFERENCE = "ORDER_REFERENCE"
    private const val PAYLOAD = "PAYLOAD"
    private const val PAYMENT_METHOD_URL = "payment_method_url"
    private const val PAYMENT_METHOD_LABEL = "payment_method_label"
    private const val GAMIFICATION_LEVEL = "gamification_level"
    private const val ANIMATION_STEP_ONE_START_FRAME = 0
    private const val ANIMATION_STEP_TWO_START_FRAME = 80
    private const val MID_ANIMATION_FRAME_INCREMENT = 40
    private const val LAST_ANIMATION_FRAME_INCREMENT = 30
    private const val BUTTON_ANIMATION_START_FRAME = 120
    private const val LAST_ANIMATION_FRAME = 150

    @JvmStatic
    fun newInstance(domain: String, skudId: String?, originalAmount: String?, currency: String?,
                    bonus: String?, selectedPaymentMethod: String, developerAddress: String,
                    type: String, amount: BigDecimal, callbackUrl: String?, orderReference: String?,
                    payload: String?, paymentMethodIconUrl: String,
                    paymentMethodLabel: String, gamificationLevel: Int): LocalPaymentFragment {
      return LocalPaymentFragment().apply {
        arguments = Bundle().apply {
          putString(DOMAIN_KEY, domain)
          putString(SKU_ID_KEY, skudId)
          putString(ORIGINAL_AMOUNT_KEY, originalAmount)
          putString(CURRENCY_KEY, currency)
          putString(BONUS_KEY, bonus)
          putString(PAYMENT_KEY, selectedPaymentMethod)
          putString(DEV_ADDRESS_KEY, developerAddress)
          putString(TYPE_KEY, type)
          putSerializable(AMOUNT_KEY, amount)
          putString(CALLBACK_URL, callbackUrl)
          putString(ORDER_REFERENCE, orderReference)
          putString(PAYLOAD, payload)
          putString(PAYMENT_METHOD_URL, paymentMethodIconUrl)
          putString(PAYMENT_METHOD_LABEL, paymentMethodLabel)
          putInt(GAMIFICATION_LEVEL, gamificationLevel)
        }
      }
    }
  }

  private val domain: String by lazy {
    if (arguments!!.containsKey(DOMAIN_KEY)) {
      arguments!!.getString(DOMAIN_KEY)!!
    } else {
      throw IllegalArgumentException("domain data not found")
    }
  }
  private val skudId: String? by lazy {
    if (arguments!!.containsKey(SKU_ID_KEY)) {
      arguments!!.getString(SKU_ID_KEY)
    } else {
      throw IllegalArgumentException("skuId data not found")
    }
  }
  private val originalAmount: String? by lazy {
    if (arguments!!.containsKey(ORIGINAL_AMOUNT_KEY)) {
      arguments!!.getString(ORIGINAL_AMOUNT_KEY)
    } else {
      throw IllegalArgumentException("original amount data not found")
    }
  }

  private val bonus: String? by lazy {
    if (arguments!!.containsKey(BONUS_KEY)) {
      arguments!!.getString(BONUS_KEY)
    } else {
      throw IllegalArgumentException("bonus amount data not found")
    }
  }

  private val paymentId: String by lazy {
    if (arguments!!.containsKey(PAYMENT_KEY)) {
      arguments!!.getString(PAYMENT_KEY)!!
    } else {
      throw IllegalArgumentException("payment method data not found")
    }
  }

  private val currency: String? by lazy {
    if (arguments!!.containsKey(CURRENCY_KEY)) {
      arguments!!.getString(CURRENCY_KEY)
    } else {
      throw IllegalArgumentException("currency data not found")
    }
  }

  private val developerAddress: String by lazy {
    if (arguments!!.containsKey(DEV_ADDRESS_KEY)) {
      arguments!!.getString(DEV_ADDRESS_KEY)!!
    } else {
      throw IllegalArgumentException("dev address data not found")
    }
  }

  private val type: String by lazy {
    if (arguments!!.containsKey(TYPE_KEY)) {
      arguments!!.getString(TYPE_KEY)!!
    } else {
      throw IllegalArgumentException("type data not found")
    }
  }

  private val amount: BigDecimal by lazy {
    if (arguments!!.containsKey(AMOUNT_KEY)) {
      arguments!!.getSerializable(AMOUNT_KEY) as BigDecimal
    } else {
      throw IllegalArgumentException("amount data not found")
    }
  }

  private val orderReference: String? by lazy {
    if (arguments!!.containsKey(ORDER_REFERENCE)) {
      arguments!!.getString(ORDER_REFERENCE)
    } else {
      throw IllegalArgumentException("dev address data not found")
    }
  }

  private val callbackUrl: String? by lazy {
    if (arguments!!.containsKey(CALLBACK_URL)) {
      arguments!!.getString(CALLBACK_URL)
    } else {
      throw IllegalArgumentException("dev address data not found")
    }
  }

  private val payload: String? by lazy {
    if (arguments!!.containsKey(PAYLOAD)) {
      arguments!!.getString(PAYLOAD)
    } else {
      throw IllegalArgumentException("dev address data not found")
    }
  }

  private val paymentMethodIconUrl: String? by lazy {
    if (arguments!!.containsKey(PAYMENT_METHOD_URL)) {
      arguments!!.getString(PAYMENT_METHOD_URL)
    } else {
      throw IllegalArgumentException("payment method icon url not found")
    }
  }

  private val paymentMethodIconLabel: String? by lazy {
    if (arguments!!.containsKey(PAYMENT_METHOD_LABEL)) {
      arguments!!.getString(PAYMENT_METHOD_LABEL)
    } else {
      throw IllegalArgumentException("payment method icon label not found")
    }
  }

  private val gamificationLevel: Int by lazy {
    if (arguments!!.containsKey(GAMIFICATION_LEVEL)) {
      arguments!!.getInt(GAMIFICATION_LEVEL)
    } else {
      throw IllegalArgumentException("gamification level not found")
    }
  }

  @Inject
  lateinit var localPaymentInteractor: LocalPaymentInteractor

  @Inject
  lateinit var analytics: LocalPaymentAnalytics

  private lateinit var iabView: IabView
  private lateinit var navigator: FragmentNavigator
  private lateinit var localPaymentPresenter: LocalPaymentPresenter
  private lateinit var status: ViewState
  private var minFrame = 0
  private var maxFrame = 40

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navigator = FragmentNavigator(activity as UriNavigator?, iabView)
    status = NONE
    localPaymentPresenter =
        LocalPaymentPresenter(this, originalAmount, currency, domain, skudId,
            paymentId, developerAddress, localPaymentInteractor, navigator, type, amount, analytics,
            savedInstanceState, AndroidSchedulers.mainThread(), Schedulers.io(),
            CompositeDisposable(), callbackUrl, orderReference, payload, context,
            paymentMethodIconUrl, gamificationLevel)
  }


  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putSerializable(STATUS_KEY, status)
    localPaymentPresenter.onSaveInstanceState(outState)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    check(context is IabView) {
      throw IllegalStateException("Local payment fragment must be attached to IAB activity")
    }
    iabView = context
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupUi()

    localPaymentPresenter.present()
  }

  private fun setupUi() {
    if (bonus?.isNotBlank() == true) {
      complete_payment_view.lottie_transaction_success.setAnimation(
          R.raw.top_up_bonus_success_animation)
      setAnimationText()
    } else {
      complete_payment_view.lottie_transaction_success.setAnimation(R.raw.top_up_success_animation)
    }

    iabView.disableBack()
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    if (savedInstanceState?.get(STATUS_KEY) != null) {
      status = savedInstanceState.get(STATUS_KEY) as ViewState
      setViewState(status)
    }
    super.onViewStateRestored(savedInstanceState)
  }

  private fun setViewState(viewState: ViewState) = when (viewState) {
    COMPLETED -> showCompletedPayment()
    PENDING_USER_PAYMENT -> localPaymentPresenter.preparePendingUserPayment()
    // TODO we may need to improve the logic here, to handle different errors
    ERROR -> showError()
    LOADING -> showProcessingLoading()
    else -> {
    }
  }

  private fun setAnimationText() {
    val textDelegate = TextDelegate(complete_payment_view.lottie_transaction_success)
    textDelegate.setText("bonus_value", bonus)
    textDelegate.setText("bonus_received",
        resources.getString(R.string.gamification_purchase_completed_bonus_received))
    complete_payment_view.lottie_transaction_success.setTextDelegate(textDelegate)
    complete_payment_view.lottie_transaction_success.setFontAssetDelegate(object :
        FontAssetDelegate() {
      override fun fetchFont(fontFamily: String?) =
          Typeface.create("sans-serif-medium", Typeface.BOLD)
    })
  }

  override fun onDestroyView() {
    localPaymentPresenter.handleStop()
    super.onDestroyView()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.local_payment_layout, container, false)
  }

  override fun getErrorDismissClick() = RxView.clicks(error_view.error_dismiss)

  override fun getSupportLogoClicks() = RxView.clicks(layout_support_logo)

  override fun getSupportIconClicks() = RxView.clicks(layout_support_icn)

  override fun getGotItClick() = RxView.clicks(got_it_button)

  override fun showWalletValidation(error: Int) = iabView.showWalletValidation(error)

  override fun showProcessingLoading() {
    status = LOADING
    progress_bar.visibility = View.VISIBLE
    error_view.visibility = View.GONE
    pending_user_payment_view.visibility = View.GONE
    pending_user_payment_view.in_progress_animation.cancelAnimation()
    complete_payment_view.lottie_transaction_success.cancelAnimation()
  }

  override fun hideLoading() {
    progress_bar.visibility = View.GONE
    error_view.visibility = View.GONE
    pending_user_payment_view.in_progress_animation.cancelAnimation()
    complete_payment_view.lottie_transaction_success.cancelAnimation()
    pending_user_payment_view.visibility = View.GONE
    complete_payment_view.visibility = View.GONE
  }

  override fun showCompletedPayment() {
    status = COMPLETED
    progress_bar.visibility = View.GONE
    error_view.visibility = View.GONE
    pending_user_payment_view.visibility = View.GONE
    complete_payment_view.visibility = View.VISIBLE
    complete_payment_view.iab_activity_transaction_completed.visibility = View.VISIBLE
    complete_payment_view.lottie_transaction_success.playAnimation()
    pending_user_payment_view.in_progress_animation.cancelAnimation()
  }

  override fun showPendingUserPayment(paymentMethodIcon: Bitmap, applicationIcon: Bitmap) {
    status = PENDING_USER_PAYMENT
    error_view.visibility = View.GONE
    complete_payment_view.visibility = View.GONE
    progress_bar.visibility = View.GONE
    pending_user_payment_view?.visibility = View.VISIBLE

    val placeholder = getString(R.string.async_steps_1_no_notification)
    val stepOneText = String.format(placeholder, paymentMethodIconLabel)

    step_one_desc.text = stepOneText

    pending_user_payment_view?.in_progress_animation?.updateBitmap("image_0", paymentMethodIcon)
    pending_user_payment_view?.in_progress_animation?.updateBitmap("image_1", applicationIcon)

    playAnimation()
  }

  override fun showError(message: Int?) {
    status = ERROR
    error_message.text = getString(R.string.ok)
    error_message.text = getString(message ?: R.string.activity_iab_error_message)
    pending_user_payment_view.visibility = View.GONE
    complete_payment_view.visibility = View.GONE
    pending_user_payment_view.in_progress_animation.cancelAnimation()
    complete_payment_view.lottie_transaction_success.cancelAnimation()
    progress_bar.visibility = View.GONE
    error_view.visibility = View.VISIBLE
  }

  override fun dismissError() {
    status = NONE
    error_view.visibility = View.GONE
    iabView.finishWithError()
  }

  override fun close() {
    status = NONE
    progress_bar.visibility = View.GONE
    error_view.visibility = View.GONE
    pending_user_payment_view.in_progress_animation.cancelAnimation()
    complete_payment_view.lottie_transaction_success.cancelAnimation()
    pending_user_payment_view.visibility = View.GONE
    complete_payment_view.visibility = View.GONE
    iabView.close(Bundle())
  }

  override fun getAnimationDuration() = complete_payment_view.lottie_transaction_success.duration

  override fun popView(bundle: Bundle) {
    bundle.putString(InAppPurchaseInteractor.PRE_SELECTED_PAYMENT_METHOD_KEY,
        paymentId)
    iabView.finish(bundle)
  }

  override fun lockRotation() {
    iabView.lockRotation()
  }

  private fun playAnimation() {
    pending_user_payment_view?.in_progress_animation?.setMinAndMaxFrame(minFrame, maxFrame)
    pending_user_payment_view?.in_progress_animation?.addAnimatorListener(object :
        Animator.AnimatorListener {
      override fun onAnimationRepeat(animation: Animator?) = Unit

      override fun onAnimationEnd(animation: Animator?) {
        if (maxFrame == LAST_ANIMATION_FRAME) {
          pending_user_payment_view?.in_progress_animation?.cancelAnimation()
        }
        if (minFrame == BUTTON_ANIMATION_START_FRAME) {
          minFrame += LAST_ANIMATION_FRAME_INCREMENT
          maxFrame += LAST_ANIMATION_FRAME_INCREMENT
          pending_user_payment_view?.in_progress_animation?.setMinAndMaxFrame(minFrame, maxFrame)
          pending_user_payment_view?.in_progress_animation?.playAnimation()
        } else {
          minFrame += MID_ANIMATION_FRAME_INCREMENT
          maxFrame += MID_ANIMATION_FRAME_INCREMENT
          pending_user_payment_view?.in_progress_animation?.setMinAndMaxFrame(minFrame, maxFrame)
          pending_user_payment_view?.in_progress_animation?.playAnimation()
        }
      }

      override fun onAnimationCancel(animation: Animator?) = Unit

      override fun onAnimationStart(animation: Animator?) {
        when (minFrame) {
          ANIMATION_STEP_ONE_START_FRAME -> {
            animateShow(step_one)
            animateShow(step_one_desc)
          }
          ANIMATION_STEP_TWO_START_FRAME -> {
            animateShow(step_two)
            animateShow(step_two_desc)
          }
          BUTTON_ANIMATION_START_FRAME -> animateButton(got_it_button)
          else -> return
        }
      }
    })
    pending_user_payment_view?.in_progress_animation?.playAnimation()
  }

  private fun animateShow(view: View) {
    view.apply {
      alpha = 0.0f
      visibility = View.VISIBLE
      lockRotation()

      animate()
          .alpha(1f)
          .withEndAction { this.visibility = View.VISIBLE }
          .setDuration(TimeUnit.SECONDS.toMillis(1))
          .setListener(null)
    }
  }

  private fun animateButton(view: View) {
    view.apply {
      alpha = 0.2f

      animate()
          .alpha(1f)
          .withEndAction {
            this.isClickable = true
            iabView.enableBack()
          }
          .setDuration(TimeUnit.SECONDS.toMillis(1))
          .setListener(null)
    }
  }

}
