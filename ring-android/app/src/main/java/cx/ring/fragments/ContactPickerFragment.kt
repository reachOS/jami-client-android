package cx.ring.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragContactPickerBinding
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ContactPickerFragment : BottomSheetDialogFragment() {
    private var binding: FragContactPickerBinding? = null
    private var adapter: SmartListAdapter? = null
    private val mDisposableBag = CompositeDisposable()
    private var mAccountId: String? = null
    private val mCurrentSelection: MutableSet<Contact> = HashSet()

    @JvmField
    @Inject
    var mConversationFacade: ConversationFacade? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setRetainInstance(true);
        //((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bdialog = super.onCreateDialog(savedInstanceState)
        if (bdialog is BottomSheetDialog) {
            val behavior = bdialog.behavior
            behavior.isFitToContents = false
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }
        return bdialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mDisposableBag.add(mConversationFacade!!.contactList
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { conversations: MutableList<ConversationItemViewModel> ->
                if (binding == null) return@subscribe
                adapter?.update(conversations)
            })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragContactPickerBinding.inflate(layoutInflater, container, false)
        adapter = SmartListAdapter(null, object : SmartListListeners {
            override fun onItemClick(item: ConversationItemViewModel) {
                mAccountId = item.accountId
                val checked = !item.isChecked
                item.isChecked = checked
                adapter!!.update(item)
                val remover = Runnable {
                    mCurrentSelection.remove(item.contacts[0].contact)
                    if (mCurrentSelection.isEmpty()) binding!!.createGroupBtn.isEnabled = false
                    item.isChecked = false
                    adapter!!.update(item)
                    val v = binding!!.selectedContacts.findViewWithTag<View>(item)
                    if (v != null) binding!!.selectedContacts.removeView(v)
                }
                if (checked) {
                    if (mCurrentSelection.add(item.contacts[0].contact)) {
                        val chip = Chip(requireContext(), null, R.style.Widget_MaterialComponents_Chip_Entry)
                        chip.text = item.contactName
                        chip.chipIcon = AvatarDrawable.Builder()
                            .withViewModel(item)
                            .withCircleCrop(true)
                            .withCheck(false)
                            .build(binding!!.root.context)
                        chip.isCloseIconVisible = true
                        chip.tag = item
                        chip.setOnCloseIconClickListener { v: View? -> remover.run() }
                        binding!!.selectedContacts.addView(chip)
                    }
                    binding!!.createGroupBtn.isEnabled = true
                } else {
                    remover.run()
                }
            }

            override fun onItemLongClick(item: ConversationItemViewModel) {}
        }, mDisposableBag)
        binding!!.createGroupBtn.setOnClickListener { v: View? ->
            mDisposableBag.add(
                mConversationFacade!!.createConversation(mAccountId!!, mCurrentSelection)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { conversation: Conversation ->
                        (requireActivity() as HomeActivity).startConversation(conversation.accountId, conversation.uri)
                        val dialog = dialog
                        dialog?.cancel()
                    })
        }
        binding!!.contactList.adapter = adapter
        return binding!!.root
    }

    override fun onDestroyView() {
        mDisposableBag.clear()
        binding = null
        adapter = null
        super.onDestroyView()
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    companion object {
        val TAG = ContactPickerFragment::class.java.simpleName
        fun newInstance(): ContactPickerFragment {
            return ContactPickerFragment()
        }
    }
}