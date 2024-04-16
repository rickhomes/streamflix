package com.tanasi.streamflix.adapters.viewholders

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.postDelayed
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.ContentCategorySwiperMobileBinding
import com.tanasi.streamflix.databinding.ContentCategorySwiperTvBinding
import com.tanasi.streamflix.databinding.ItemCategoryMobileBinding
import com.tanasi.streamflix.databinding.ItemCategoryTvBinding
import com.tanasi.streamflix.fragments.home.HomeTvFragment
import com.tanasi.streamflix.fragments.home.HomeTvFragmentDirections
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.Show
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity

class CategoryViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var category: Category

    val childRecyclerView: RecyclerView?
        get() = when (_binding) {
            is ItemCategoryMobileBinding -> _binding.rvCategory
            is ItemCategoryTvBinding -> _binding.hgvCategory
            is ContentCategorySwiperMobileBinding -> _binding.vpCategorySwiper.javaClass
                .getDeclaredField("mRecyclerView").let {
                    it.isAccessible = true
                    it.get(_binding.vpCategorySwiper) as RecyclerView
                }
            else -> null
        }

    fun bind(category: Category) {
        this.category = category

        when (_binding) {
            is ItemCategoryMobileBinding -> displayMobileItem(_binding)
            is ItemCategoryTvBinding -> displayTvItem(_binding)

            is ContentCategorySwiperMobileBinding -> displayMobileSwiper(_binding)
            is ContentCategorySwiperTvBinding -> displayTvSwiper(_binding)
        }
    }


    private fun displayMobileItem(binding: ItemCategoryMobileBinding) {
        binding.tvCategoryTitle.text = category.name

        binding.rvCategory.apply {
            adapter = AppAdapter().apply {
                submitList(category.list)
            }
            if (itemDecorationCount == 0) {
                addItemDecoration(SpacingItemDecoration(category.itemSpacing))
            }
        }
    }

    private fun displayTvItem(binding: ItemCategoryTvBinding) {
        binding.tvCategoryTitle.text = category.name

        binding.hgvCategory.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                submitList(category.list)
            }
            setItemSpacing(category.itemSpacing)
        }
    }


    private fun displayMobileSwiper(binding: ContentCategorySwiperMobileBinding) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(8_000) {
            binding.vpCategorySwiper.currentItem += 1
        }

        binding.vpCategorySwiper.apply {
            adapter = AppAdapter().apply {
                submitList(category.list)
            }
        }

        binding.llDotsIndicator.apply {
            removeAllViews()
            repeat(category.list.size) {
                val view = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(15, 15).apply {
                        setMargins(10, 0, 10, 0)
                    }
                    setBackgroundResource(R.drawable.bg_dot_indicator)
                }
                addView(view)
            }
        }

        binding.vpCategorySwiper.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.llDotsIndicator.children.forEachIndexed { index, view ->
                    view.isSelected = (position == index)
                }
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(8_000) {
                    binding.vpCategorySwiper.currentItem += 1
                }
            }
        })
    }

    private fun displayTvSwiper(binding: ContentCategorySwiperTvBinding) {
        val selected = category.list.getOrNull(category.selectedIndex) as? Show ?: return

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(8_000) {
            category.selectedIndex = (category.selectedIndex + 1) % category.list.size
            if (binding.btnSwiperWatchNow.hasFocus()) {
                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeTvFragment -> when (val it = category.list[category.selectedIndex]) {
                        is Movie -> fragment.updateBackground(it.banner)
                        is TvShow -> fragment.updateBackground(it.banner)
                    }
                }
            }
            bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
        }

        binding.tvSwiperTitle.text = when (selected) {
            is Movie -> selected.title
            is TvShow -> selected.title
        }

        binding.tvSwiperTvShowLastEpisode.apply {
            text = when (selected) {
                is TvShow -> selected.seasons.lastOrNull()?.let { season ->
                    season.episodes.lastOrNull()?.let { episode ->
                        if (season.number != 0) {
                            context.getString(
                                R.string.tv_show_item_season_number_episode_number,
                                season.number,
                                episode.number
                            )
                        } else {
                            context.getString(
                                R.string.tv_show_item_episode_number,
                                episode.number
                            )
                        }
                    }
                } ?: context.getString(R.string.tv_show_item_type)
                else -> context.getString(R.string.movie_item_type)
            }
        }

        binding.tvSwiperQuality.apply {
            text = when (selected) {
                is Movie -> selected.quality
                is TvShow -> selected.quality
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperReleased.apply {
            text = when (selected) {
                is Movie -> selected.released?.format("yyyy")
                is TvShow -> selected.released?.format("yyyy")
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperRating.apply {
            text = when (selected) {
                is Movie -> selected.rating?.let { String.format("%.1f", it) }
                is TvShow -> selected.rating?.let { String.format("%.1f", it) }
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.ivSwiperRatingIcon.visibility = binding.tvSwiperRating.visibility

        binding.tvSwiperOverview.text = when (selected) {
            is Movie -> selected.overview
            is TvShow -> selected.overview
        }

        binding.btnSwiperWatchNow.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    when (val fragment = context.toActivity()?.getCurrentFragment()) {
                        is HomeTvFragment -> when (selected) {
                            is Movie -> fragment.updateBackground(selected.banner)
                            is TvShow -> fragment.updateBackground(selected.banner)
                        }
                    }
                }
            }
            setOnKeyListener { _, _, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            handler.removeCallbacksAndMessages(null)
                            category.selectedIndex = (category.selectedIndex + 1) % category.list.size
                            when (val fragment = context.toActivity()?.getCurrentFragment()) {
                                is HomeTvFragment -> when (val it = category.list[category.selectedIndex]) {
                                    is Movie -> fragment.updateBackground(it.banner)
                                    is TvShow -> fragment.updateBackground(it.banner)
                                }
                            }
                            bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }
            setOnClickListener {
                handler.removeCallbacksAndMessages(null)
                findNavController().navigate(
                    when (selected) {
                        is Movie -> HomeTvFragmentDirections.actionHomeToMovie(selected.id)
                        is TvShow -> HomeTvFragmentDirections.actionHomeToTvShow(selected.id)
                    }
                )
            }
        }

        binding.pbSwiperProgress.apply {
            val watchHistory = when (selected) {
                is Movie -> selected.watchHistory
                is TvShow -> null
            }

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.llDotsIndicator.apply {
            removeAllViews()
            repeat(category.list.size) { index ->
                val view = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(15, 15).apply {
                        setMargins(10, 0, 10, 0)
                    }
                    setBackgroundResource(R.drawable.bg_dot_indicator)
                    isSelected = (category.selectedIndex == index)
                }
                addView(view)
            }
        }
    }
}