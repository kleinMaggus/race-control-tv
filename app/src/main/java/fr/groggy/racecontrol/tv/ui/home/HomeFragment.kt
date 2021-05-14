package fr.groggy.racecontrol.tv.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import fr.groggy.racecontrol.tv.R
import fr.groggy.racecontrol.tv.f1tv.Archive
import fr.groggy.racecontrol.tv.ui.season.archive.SeasonArchiveActivity
import fr.groggy.racecontrol.tv.ui.season.browse.Season
import fr.groggy.racecontrol.tv.ui.season.browse.SeasonBrowseActivity
import fr.groggy.racecontrol.tv.ui.season.browse.Session
import fr.groggy.racecontrol.tv.ui.session.SessionCardPresenter
import org.threeten.bp.Year

@Keep
@AndroidEntryPoint
class HomeFragment : RowsSupportFragment(), OnItemViewClickedListener {

    private val listRowPresenter = ListRowPresenter(FocusHighlight.ZOOM_FACTOR_NONE).apply {
        shadowEnabled = false
        selectEffectEnabled = false
    }
    private val archivesAdapter = ArrayObjectAdapter(listRowPresenter)
    private var imageView: ImageView? = null
    private val currentYear = Year.now().value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUIElements()
        setupEventListeners()
        buildRowsAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            val dimensionPixelSize =
                inflater.context.resources.getDimensionPixelSize(R.dimen.lb_browse_rows_fading_edge)
            val horizontalMargin = -dimensionPixelSize * 2 - 4

            leftMargin = horizontalMargin
            rightMargin = horizontalMargin
        }

        imageView = requireActivity().findViewById(R.id.teaserImage)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView?.requestFocus()
        imageView?.setOnClickListener {
            val activity = SeasonBrowseActivity.intent(requireContext(), Archive(currentYear))
            startActivity(activity)
        }

        val teaserImageText = requireActivity().findViewById<TextView>(R.id.teaserImageText)
        teaserImageText.text = resources.getString(R.string.teaser_image_text, currentYear)
    }

    private fun buildRowsAdapter() {
        val viewModel: HomeViewModel by viewModels()

        lifecycleScope.launchWhenStarted {
            viewModel.getCurrentSeason(Archive(currentYear)).asLiveData()
                .observe(viewLifecycleOwner, ::onUpdatedSeason)
        }
    }

    private fun onUpdatedSeason(season: Season) {
        val viewModel: HomeViewModel by viewModels()
        val event = season.events.filter { it.sessions.isNotEmpty() }[0]
        val listRowAdapter = ArrayObjectAdapter(SessionCardPresenter())
        listRowAdapter.setItems(event.sessions, Session.diffCallback)

        if (archivesAdapter.size() == 0) {
            archivesAdapter.add(ListRow(HeaderItem(event.name + " " + currentYear), listRowAdapter))
            archivesAdapter.add(getArchiveRow(viewModel))
        } else {
            val listRow: ListRow = archivesAdapter.get(0) as ListRow
            (listRow.adapter as ArrayObjectAdapter).setItems(event.sessions, Session.diffCallback)
        }

    }

    private fun getArchiveRow(viewModel: HomeViewModel): ListRow {
        val archives = viewModel.listArchive().subList(1, 6)
            .map { archive -> HomeItem(HomeItemType.ARCHIVE, archive.year.toString()) }

        val listRowAdapter = ArrayObjectAdapter(HomeItemPresenter())
        listRowAdapter.setItems(archives, null)
        listRowAdapter.add(
            HomeItem(
                HomeItemType.ARCHIVE_ALL,
                resources.getString(R.string.home_all)
            )
        )

        return ListRow(HeaderItem(resources.getString(R.string.home_archive)), listRowAdapter)
    }

    private fun setupUIElements() {
        adapter = archivesAdapter
    }

    private fun setupEventListeners() {
        onItemViewClickedListener = this
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        val activity = when ((item as HomeItem).type) {
            HomeItemType.ARCHIVE -> {
                SeasonBrowseActivity.intent(requireContext(), Archive(item.text.toInt()))
            }
            HomeItemType.ARCHIVE_ALL -> {
                SeasonArchiveActivity.intent(requireContext())
            }
        }
        startActivity(activity)
    }
}
