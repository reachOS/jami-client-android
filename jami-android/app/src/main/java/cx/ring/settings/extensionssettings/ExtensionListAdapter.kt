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
package cx.ring.settings.extensionssettings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.viewholders.ExtensionSettingViewHolder

class ExtensionsListAdapter(private var mList: List<ExtensionDetails>, private val listener: ExtensionListItemListener, private val accountId: String? = "") :
    RecyclerView.Adapter<ExtensionSettingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtensionSettingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.frag_extensions_list_item, parent, false)
        return ExtensionSettingViewHolder(view, listener)
    }

    fun notifyItemChanged(extensionDetails: ExtensionDetails) =
        mList.indexOf(extensionDetails).let { if (it != -1) notifyItemChanged(it) }

    override fun onBindViewHolder(holderSetting: ExtensionSettingViewHolder, position: Int) {
        for (item in mList) {
            item.accountId = accountId
        }
        holderSetting.setDetails(mList[position])
    }

    override fun getItemCount(): Int {
        if (!accountId!!.isEmpty()) {
            var copy: List<ExtensionDetails> = ArrayList()
            for (item in mList) {
                item.accountId = accountId
                if (!item.extensionPreferences.isEmpty())
                    copy += item
            }
            mList = copy
        }
        return mList.size
    }

    fun updateExtensionsList(listExtensions: List<ExtensionDetails>) {
        for (item in listExtensions) {
            item.accountId = accountId
        }
        mList = listExtensions
        notifyDataSetChanged()
    }

    interface ExtensionListItemListener {
        fun onExtensionItemClicked(extensionDetails: ExtensionDetails)
        fun onExtensionEnabled(extensionDetails: ExtensionDetails)
    }

    companion object {
        val TAG = ExtensionsListAdapter::class.simpleName!!
    }
}