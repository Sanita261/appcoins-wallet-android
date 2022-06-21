package com.asfoundation.wallet.my_wallets.main

import android.content.Intent
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.navigation.ActivityNavigatorExtras
import androidx.navigation.NavController
import com.asf.wallet.R
import com.asfoundation.wallet.C
import com.asfoundation.wallet.base.Navigator
import com.asfoundation.wallet.base.navigate
import com.asfoundation.wallet.entity.Wallet
import com.asfoundation.wallet.ui.MyAddressActivity
import com.asfoundation.wallet.ui.transact.TransferActivity
import com.asfoundation.wallet.ui.wallets.WalletBalance
import javax.inject.Inject

class MyWalletsNavigator @Inject constructor(
  private val fragment: Fragment,
  private val navController: NavController
) : Navigator {

  fun navigateToChangeActiveWallet(walletBalance: WalletBalance) {
    navigate(
      navController,
      MyWalletsFragmentDirections.actionNavigateToChangeActiveWallet(walletBalance)
    )
  }

  fun navigateToCreateNewWallet() {
    navigate(
      navController,
      MyWalletsFragmentDirections.actionNavigateToCreateWallet(needsWalletCreation = true)
    )
  }

  fun navigateToTokenInfo(title: String, image: String, description: String, showTopUp: Boolean) {
    navigate(
      navController,
      MyWalletsFragmentDirections.actionNavigateToTokenInfo(title, image, description, showTopUp)
    )
  }

  fun navigateToMore(
    walletAddress: String,
    totalFiatBalance: String,
    appcoinsBalance: String,
    creditsBalance: String,
    ethereumBalance: String,
    showDeleteWallet: Boolean
  ) {
    navigate(
      navController,
      MyWalletsFragmentDirections.actionNavigateToMore(
        walletAddress,
        totalFiatBalance,
        appcoinsBalance,
        creditsBalance,
        ethereumBalance,
        showDeleteWallet
      )
    )
  }

  fun navigateToBalanceDetails(
    totalFiatBalance: String,
    appcoinsBalance: String,
    creditsBalance: String,
    ethereumBalance: String
  ) {
    navigate(
      navController,
      MyWalletsFragmentDirections.actionNavigateToBalanceDetails(
        totalFiatBalance,
        appcoinsBalance,
        creditsBalance,
        ethereumBalance
      )
    )
  }

  fun navigateToSend() {
    val intent = TransferActivity.newIntent(fragment.requireContext())
    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    openIntent(intent)
  }

  fun navigateToReceive(wallet: Wallet) {
    val intent = Intent(fragment.requireContext(), MyAddressActivity::class.java)
    intent.putExtra(C.Key.WALLET, wallet)
    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    openIntent(intent)
  }

  fun navigateToNfts() {
    navigate(navController, MyWalletsFragmentDirections.actionNavigateToNfts())
  }

  fun navigateToVerifyPicker() {
    navigate(navController, MyWalletsFragmentDirections.actionNavigateToVerifyPicker())
  }

  fun navigateToVerifyCreditCard() {
    navigate(navController, MyWalletsFragmentDirections.actionNavigateToVerifyCreditCard(false))
  }

  fun navigateToBackupWallet(walletAddress: String) {
    navigate(navController, MyWalletsFragmentDirections.actionNavigateToBackupWallet(walletAddress))
  }

  fun navigateToQrCode(qrCodeView: View) {
    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
      fragment.requireActivity(),
      Pair(qrCodeView, "qr_code_image")
    )
    val extras = ActivityNavigatorExtras(options)
    navController.navigate(R.id.action_navigate_to_qr_code, null, null, extras)
  }

  private fun openIntent(intent: Intent) = fragment.requireContext()
    .startActivity(intent)
}