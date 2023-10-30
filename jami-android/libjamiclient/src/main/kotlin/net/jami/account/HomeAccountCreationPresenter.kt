/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.account

import net.jami.mvp.RootPresenter
import javax.inject.Inject

class HomeAccountCreationPresenter @Inject constructor() : RootPresenter<HomeAccountCreationView>() {
    fun clickOnCreateAccount() {
        view?.goToAccountCreation()
    }

    fun clickOnLinkAccount() {
        view?.goToAccountLink()
    }

    fun clickOnConnectAccount() {
        view?.goToAccountConnect()
    }

    fun clickOnCreateSIPAccount() {
        view?.goToSIPAccountCreation()
    }
}