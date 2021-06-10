package eu.kanade.tachiyomi.ui.reader

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderExtractPageTextSheetBinding
import eu.kanade.tachiyomi.ui.main.DeepLinkActivity
import eu.kanade.tachiyomi.ui.main.MainActivity.Companion.INTENT_SEARCH
import eu.kanade.tachiyomi.ui.main.MainActivity.Companion.INTENT_SEARCH_QUERY
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * Sheet to show when a page is long clicked.
 */
class ReaderExtractedTextSheet(
    private val activity: ReaderActivity,
    private val text: String
) : BottomSheetDialog(activity) {

    private val binding = ReaderExtractPageTextSheetBinding.inflate(activity.layoutInflater)
    private val searchTerms = mutableSetOf<String>()

    init {
        setContentView(binding.root)

        binding.extractedPageText.text = text

        binding.extractedPageText.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            val ADD_SEARCH_TERM = 999

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                try {
                    val field = menu!!.javaClass.getDeclaredField("mOptionalIconsVisible")
                    field.isAccessible = true
                    field.setBoolean(menu, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.add(0, ADD_SEARCH_TERM, 0, context.getString(R.string.extract_text_add_search_term))?.setIcon(R.drawable.ic_explore_outline_24dp)
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) { }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    ADD_SEARCH_TERM -> {
                        // ref: https://stackoverflow.com/questions/22832123/get-selected-text-from-textview
                        var min = 0
                        var max: Int = binding.extractedPageText.text.length

                        if (binding.extractedPageText.isFocused) {
                            val selStart: Int = binding.extractedPageText.selectionStart
                            val selEnd: Int = binding.extractedPageText.selectionEnd
                            min = max(0, min(selStart, selEnd))
                            max = max(0, max(selStart, selEnd))
                        }

                        // Perform your definition lookup with the selected text
                        val selectedText = binding.extractedPageText.text.subSequence(min, max).toString()
                        mode?.finish()

                        if (selectedText.isNotBlank()) {
                            searchTerms.add(selectedText)
                            updateSearchButton()
                        }

                        return true
                    }
                }

                return false
            }
        }

        binding.extractedPageText.setOnClickListener {
            val searchQuery = searchTerms.joinToString(
                separator = " ",
                transform = { "\"$it\"" }
            )

            Timber.d("searchQuery: $searchQuery")

            val intent = Intent().apply {
                setClass(activity.applicationContext, DeepLinkActivity::class.java)
                action = INTENT_SEARCH
                putExtra(INTENT_SEARCH_QUERY, searchQuery)
            }

            dismiss()
            activity.startActivity(intent)
        }

        setOnDismissListener {
            searchTerms.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
        if (width > 0) {
            window?.setLayout(width, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    private fun updateSearchButton() {
        when {
            searchTerms.size < 1 -> {
                binding.extractedPageText.visibility = View.GONE
            }
            else -> {
                binding.extractedPageText.text = activity.getString(R.string.extract_text_search_terms, searchTerms.size)
                binding.extractedPageText.visibility = View.VISIBLE
            }
        }
    }
}
