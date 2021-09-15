/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.account

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.AccountCreationModel
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.services.PreferencesService
import net.jami.utils.Log
import net.jami.utils.StringUtils.isEmpty
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class AccountWizardPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mPreferences: PreferencesService,
    private val mDeviceService: DeviceRuntimeService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<AccountWizardView>() {
    //private boolean mCreationError = false;
    private var mCreatingAccount = false
    private var mAccountType: String? = null
    private var mAccountCreationModel: AccountCreationModel? = null
    private var newAccount: Observable<Account>? = null

    fun init(accountType: String) {
        mAccountType = accountType
        if (AccountConfig.ACCOUNT_TYPE_SIP == mAccountType) {
            view?.goToSipCreation()
        } else {
            view?.goToHomeCreation()
        }
    }

    private fun setProxyDetails(accountCreationModel: AccountCreationModel, details: MutableMap<String, String>) {
        if (accountCreationModel.isPush) {
            details[ConfigKey.PROXY_ENABLED.key()] = AccountConfig.TRUE_STR
            val pushToken = mDeviceService.pushToken
            if (pushToken != null && pushToken.isNotEmpty())
                details[ConfigKey.PROXY_PUSH_TOKEN.key()] = pushToken
        }
    }

    fun initJamiAccountConnect(accountCreationModel: AccountCreationModel, defaultAccountName: String) {
        val newAccount = initRingAccountDetails(defaultAccountName)
            .map<Map<String, String>> { accountDetails ->
                if (!isEmpty(accountCreationModel.managementServer)) {
                    accountDetails[ConfigKey.MANAGER_URI.key()] = accountCreationModel.managementServer!!
                    if (!isEmpty(accountCreationModel.username)) {
                        accountDetails[ConfigKey.MANAGER_USERNAME.key()] = accountCreationModel.username
                    }
                } else if (!isEmpty(accountCreationModel.username)) {
                    accountDetails[ConfigKey.ACCOUNT_USERNAME.key()] = accountCreationModel.username
                }
                if (!isEmpty(accountCreationModel.password)) {
                    accountDetails[ConfigKey.ARCHIVE_PASSWORD.key()] = accountCreationModel.password
                }
                setProxyDetails(accountCreationModel, accountDetails)
                accountDetails
            }
        createAccount(accountCreationModel, newAccount)
    }

    fun initJamiAccountCreation(accountCreationModel: AccountCreationModel, defaultAccountName: String) {
        val newAccount = initRingAccountDetails(defaultAccountName)
            .map<Map<String, String>> { accountDetails ->
                if (!isEmpty(accountCreationModel.username)) {
                    accountDetails[ConfigKey.ACCOUNT_REGISTERED_NAME.key()] = accountCreationModel.username
                }
                if (!isEmpty(accountCreationModel.password)) {
                    accountDetails[ConfigKey.ARCHIVE_PASSWORD.key()] = accountCreationModel.password
                }
                setProxyDetails(accountCreationModel, accountDetails)
                accountDetails
            }
        createAccount(accountCreationModel, newAccount)
    }

    fun initJamiAccountLink(accountCreationModel: AccountCreationModel, defaultAccountName: String) {
        val newAccount = initRingAccountDetails(defaultAccountName)
            .map<Map<String, String>> { accountDetails ->
                val settings = mPreferences.settings
                if (settings.enablePushNotifications) {
                    accountCreationModel.isPush = true
                    setProxyDetails(accountCreationModel, accountDetails)
                }
                if (!isEmpty(accountCreationModel.password)) {
                    accountDetails[ConfigKey.ARCHIVE_PASSWORD.key()] = accountCreationModel.password
                }
                if (accountCreationModel.archive != null) {
                    accountDetails[ConfigKey.ARCHIVE_PATH.key()] = accountCreationModel.archive!!.absolutePath
                } else if (accountCreationModel.pin.isNotEmpty()) {
                    accountDetails[ConfigKey.ARCHIVE_PIN.key()] = accountCreationModel.pin
                }
                accountDetails
            }
        createAccount(accountCreationModel, newAccount)
    }

    private fun createAccount(accountCreationModel: AccountCreationModel, details: Single<Map<String, String>>) {
        mAccountCreationModel = accountCreationModel
        val newAccount = details.flatMapObservable { accountDetails -> createNewAccount(accountCreationModel, accountDetails) }
        accountCreationModel.accountObservable = newAccount
        mCompositeDisposable.add(newAccount
            .observeOn(mUiScheduler)
            .subscribe({ account: Account -> accountCreationModel.newAccount = account })
            { e -> Log.e(TAG, "Can't create account", e) })
        if (accountCreationModel.isLink) {
            view!!.displayProgress(true)
            mCompositeDisposable.add(newAccount
                .filter { a: Account ->
                    val newState = a.registrationState
                    !(newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING))
                }
                .firstOrError()
                .observeOn(mUiScheduler)
                .subscribe({ acc: Account ->
                    accountCreationModel.newAccount = acc
                    val view = view
                    if (view != null) {
                        view.displayProgress(false)
                        val newState = acc.registrationState
                        if (newState.contentEquals(AccountConfig.STATE_ERROR_GENERIC)) {
                            mCreatingAccount = false
                            if (accountCreationModel.archive == null) view.displayCannotBeFoundError() else view.displayGenericError()
                        } else {
                            view.goToProfileCreation(accountCreationModel)
                        }
                    }
                }) {
                    mCreatingAccount = false
                    view!!.displayProgress(false)
                    view!!.displayCannotBeFoundError()
                })
        } else {
            view?.goToProfileCreation(accountCreationModel)
        }
    }

    fun successDialogClosed() {
        view?.finish(true)
    }

    private fun initRingAccountDetails(defaultAccountName: String): Single<HashMap<String, String>> {
        return initAccountDetails().map { accountDetails: HashMap<String, String> ->
            accountDetails[ConfigKey.ACCOUNT_ALIAS.key()] = mAccountService.getNewAccountName(defaultAccountName)
            accountDetails[ConfigKey.ACCOUNT_UPNP_ENABLE.key()] = AccountConfig.TRUE_STR
            accountDetails
        }
    }

    private fun initAccountDetails(): Single<HashMap<String, String>> {
        return if (mAccountType == null) Single.error(IllegalStateException())
        else mAccountService.getAccountTemplate(mAccountType!!)
            .map { accountDetails: HashMap<String, String> ->
                accountDetails[ConfigKey.VIDEO_ENABLED.key()] = true.toString()
                accountDetails[ConfigKey.ACCOUNT_DTMF_TYPE.key()] = "sipinfo"
                accountDetails
            }
    }

    private fun createNewAccount(model: AccountCreationModel, accountDetails: Map<String, String>): Observable<Account> {
        if (mCreatingAccount) {
            return newAccount!!
        }
        mCreatingAccount = true
        //mCreationError = false;
        val account = BehaviorSubject.create<Account>()
        account.filter { a: Account ->
            val newState = a.registrationState
            !(newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING))
        }
            .firstElement()
            .subscribe { a: Account ->
                if (!model.isLink && a.isJami && !isEmpty(model.username))
                    mAccountService.registerName(a, model.password, model.username)
                mAccountService.currentAccount = a
                if (model.isPush) {
                    val settings = mPreferences.settings.copy(enablePushNotifications = true)
                    mPreferences.settings = settings
                }
            }
        mAccountService.addAccount(accountDetails)
            .subscribe(account)
        newAccount = account
        return account
    }

    fun profileCreated(accountCreationModel: AccountCreationModel, saveProfile: Boolean) {
        view!!.blockOrientation()
        view!!.displayProgress(true)
        var newAccount = mAccountCreationModel!!.accountObservable!!.filter { a: Account ->
                val newState = a.registrationState
                !(newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING))
            }
            .firstOrError()
        if (saveProfile) {
            newAccount = newAccount.flatMap { a: Account ->
                view!!.saveProfile(a, accountCreationModel).map { a }
            }
        }
        mCompositeDisposable.add(newAccount
            .observeOn(mUiScheduler)
            .subscribe({ account: Account ->
                mCreatingAccount = false
                val view = view
                if (view != null) {
                    view.displayProgress(false)
                    val newState = account.registrationState
                    Log.w(TAG, "newState $newState")
                    when (newState) {
                        AccountConfig.STATE_ERROR_GENERIC -> view.displayGenericError()
                        AccountConfig.STATE_UNREGISTERED -> { }
                        AccountConfig.STATE_ERROR_NETWORK -> view.displayNetworkError()
                        else -> {}
                    }
                    view.displaySuccessDialog()
                }
            }) { e ->
                Log.e(TAG, "Error creating account", e);
                mCreatingAccount = false
                //mCreationError = true;
                val view = view
                if (view != null) {
                    view.displayGenericError()
                    //view.finish(true)
                }
            })
    }

    fun errorDialogClosed() {
        view!!.goToHomeCreation()
    }

    companion object {
        val TAG = AccountWizardPresenter::class.simpleName!!
    }
}