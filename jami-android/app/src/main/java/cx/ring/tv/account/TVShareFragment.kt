/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package cx.ring.tv.account

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cx.ring.R
import cx.ring.databinding.TvFragShareBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.share.SharePresenter
import net.jami.share.ShareView
import javax.inject.Inject
import androidx.core.graphics.createBitmap

@AndroidEntryPoint
class TVShareFragment : BaseSupportFragment<SharePresenter, ShareView>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = TvFragShareBinding.inflate(inflater, container, false).apply {
        val contactUri = Uri.fromString(arguments?.getString(ARG_CONTACT_URI) ?: "")
        if (contactUri.uri.isEmpty()) return@apply

        presenter.loadQRCodeData(
            contactUri,
            foregroundColor = 0x00000000,
            backgroundColor = -0x1
        ) { qrCodeData ->
            val pad = 56
            val bitmap = createBitmap(qrCodeData.width + 2 * pad, qrCodeData.height + 2 * pad)
            bitmap.setPixels(
                qrCodeData.data, 0, qrCodeData.width,
                pad, pad, qrCodeData.width, qrCodeData.height
            )
            qrImage.setImageBitmap(bitmap)
            shareQrInstruction.setText(R.string.share_message)
            qrImage.visibility = View.VISIBLE
        }

        presenter.loadContact(contactUri) { contact ->
            qrUserPhoto?.setImageDrawable(AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
                .build(requireContext()))
            qrUserPhoto?.visibility = View.VISIBLE
            shareUri?.text = contact.displayUri
            shareUri?.visibility = View.VISIBLE
        }
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val ARG_CONTACT_URI = "contact_uri"

        fun newInstance(contactUri: Uri): TVShareFragment {
            val fragment = TVShareFragment()
            val args = Bundle()
            args.putString(ARG_CONTACT_URI, contactUri.uri)
            fragment.arguments = args
            return fragment
        }
    }
}
