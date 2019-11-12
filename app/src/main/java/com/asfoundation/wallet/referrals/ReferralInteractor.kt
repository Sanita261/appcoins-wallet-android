package com.asfoundation.wallet.referrals

import com.appcoins.wallet.gamification.repository.PromotionsRepository
import com.appcoins.wallet.gamification.repository.entity.ReferralResponse
import com.asf.wallet.R
import com.asfoundation.wallet.interact.EmptyNotification
import com.asfoundation.wallet.interact.FindDefaultWalletInteract
import com.asfoundation.wallet.ui.widget.holder.CardNotificationAction
import com.asfoundation.wallet.util.scaleToString
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.math.BigDecimal

class ReferralInteractor(
    private val preferences: SharedPreferencesReferralLocalData,
    private val defaultWallet: FindDefaultWalletInteract,
    private val promotionsRepository: PromotionsRepository) :
    ReferralInteractorContract {

  override fun hasReferralUpdate(address: String, friendsInvited: Int, isVerified: Boolean,
                                 screen: ReferralsScreen): Single<Boolean> {
    return getReferralInformation(address, screen)
        .map { hasDifferentInformation(friendsInvited.toString() + isVerified, it) }
  }

  override fun retrieveReferral(): Single<ReferralModel> {
    return defaultWallet.find()
        .flatMap { promotionsRepository.getReferralUserStatus(it.address) }
        .map { map(it) }
  }

  private fun map(referralResponse: ReferralResponse): ReferralModel {
    return ReferralModel(referralResponse.completed, referralResponse.link,
        referralResponse.invited, referralResponse.pendingAmount, referralResponse.amount,
        referralResponse.symbol, referralResponse.maxAmount, referralResponse.minAmount,
        referralResponse.available, referralResponse.receivedAmount,
        isRedeemed(referralResponse.userStatus), isAvailable(referralResponse.status))
  }

  override fun saveReferralInformation(numberOfFriends: Int, isVerified: Boolean,
                                       screen: ReferralsScreen): Completable {
    return defaultWallet.find()
        .flatMapCompletable {
          saveReferralInformation(it.address, numberOfFriends, isVerified, screen)
        }
  }

  private fun getReferralInformation(address: String, screen: ReferralsScreen): Single<String> {
    return preferences.getReferralInformation(address, screen.toString())
  }

  private fun saveReferralInformation(address: String, numberOfFriends: Int, isVerified: Boolean,
                                      screen: ReferralsScreen): Completable {
    return preferences.saveReferralInformation(address, numberOfFriends, isVerified,
        screen.toString())
  }

  private fun hasDifferentInformation(newInformation: String, savedInformation: String): Boolean {
    return newInformation != savedInformation
  }

  override fun getPendingBonusNotification(): Maybe<ReferralNotification> {
    return defaultWallet.find()
        .flatMapMaybe { wallet ->
          promotionsRepository.getReferralUserStatus(wallet.address)
              .filter { it.pendingAmount.compareTo(BigDecimal.ZERO) != 0 && isAvailable(it.status) }
              .map {
                ReferralNotification(PENDING_AMOUNT_ID,
                    R.string.referral_notification_bonus_pending_title,
                    R.string.referral_notification_bonus_pending_body,
                    R.drawable.ic_bonus_pending,
                    R.string.gamification_APPCapps_button,
                    CardNotificationAction.DISCOVER,
                    it.pendingAmount,
                    it.symbol)
              }
        }
  }

  override fun getUnwatchedPendingBonusNotification(): Single<CardNotification> {
    return defaultWallet.find()
        .flatMap { wallet ->
          promotionsRepository.getReferralUserStatus(wallet.address)
              .flatMap { userStats ->
                preferences.getPendingAmountNotification(wallet.address)
                    .map {
                      userStats.pendingAmount.compareTo(BigDecimal.ZERO) != 0 &&
                          it != userStats.pendingAmount.scaleToString(
                          2) && isAvailable(userStats.status)
                    }
                    .map { shouldShow ->
                      ReferralNotification(PENDING_AMOUNT_ID,
                          R.string.referral_notification_bonus_pending_title,
                          R.string.referral_notification_bonus_pending_body,
                          R.drawable.ic_bonus_pending,
                          R.string.gamification_APPCapps_button,
                          CardNotificationAction.DISCOVER,
                          userStats.pendingAmount,
                          userStats.symbol).takeIf { shouldShow } ?: EmptyNotification()
                    }
              }
        }
  }

  override fun dismissNotification(referralNotification: ReferralNotification): Completable {
    return defaultWallet.find()
        .flatMapCompletable {
          preferences.savePendingAmountNotification(it.address,
              referralNotification.pendingAmount.scaleToString(2))
        }
  }

  override fun getReferralInfo(): Single<ReferralModel> {
    return promotionsRepository.getReferralInfo()
        .map {
          ReferralModel(it.completed, it.link, it.invited, it.pendingAmount, it.amount, it.symbol,
              it.maxAmount, it.minAmount, it.available, it.receivedAmount,
              isRedeemed(it.userStatus), isAvailable(it.status))
        }
  }

  private fun isRedeemed(userStatus: ReferralResponse.UserStatus?): Boolean {
    return userStatus?.let { it == ReferralResponse.UserStatus.REDEEMED } ?: false
  }

  private fun isAvailable(status: ReferralResponse.Status): Boolean {
    return status == ReferralResponse.Status.ACTIVE
  }

  companion object {
    private const val PENDING_AMOUNT_ID = 1
  }

}
