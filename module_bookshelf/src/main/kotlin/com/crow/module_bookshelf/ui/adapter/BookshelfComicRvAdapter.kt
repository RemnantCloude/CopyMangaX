package com.crow.module_bookshelf.ui.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide
import com.crow.base.copymanga.getComicCardHeight
import com.crow.base.copymanga.getComicCardWidth
import com.crow.base.copymanga.glide.AppGlideProgressFactory
import com.crow.base.copymanga.mSize10
import com.crow.base.tools.extensions.animateFadeIn
import com.crow.base.tools.extensions.animateFadeOut
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.ui.adapter.BaseGlideViewHolder
import com.crow.module_bookshelf.databinding.BookshelfFragmentRvBinding
import com.crow.module_bookshelf.model.resp.bookshelf_comic.BookshelfComicResults

class BookshelfComicRvAdapter(
    private val mGenericTransitionOptions: GenericTransitionOptions<Drawable>,
    inline val doOnTap: (BookshelfComicResults) -> Unit
) : PagingDataAdapter<BookshelfComicResults, BookshelfComicRvAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback: DiffUtil.ItemCallback<BookshelfComicResults>() {
        override fun areItemsTheSame(oldItem: BookshelfComicResults, newItem: BookshelfComicResults): Boolean {
            return oldItem.mUuid == newItem.mUuid
        }

        override fun areContentsTheSame(oldItem: BookshelfComicResults, newItem: BookshelfComicResults): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(binding: BookshelfFragmentRvBinding) : BaseGlideViewHolder<BookshelfFragmentRvBinding>(binding)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(BookshelfFragmentRvBinding.inflate(LayoutInflater.from(parent.context), parent,false)).also { vh ->

            vh.rvBinding.bookshelfRvImage.layoutParams.apply {
                width = getComicCardWidth() - mSize10
                height = getComicCardHeight()
            }

            vh.rvBinding.bookshelfRvImage.doOnClickInterval {
                doOnTap(getItem(vh.absoluteAdapterPosition) ?: return@doOnClickInterval)
            }
        }
    }

    override fun onViewRecycled(vh: ViewHolder) {
        super.onViewRecycled(vh)
        vh.mAppGlideProgressFactory?.doRemoveListener()?.doClean()
        vh.mAppGlideProgressFactory = null
    }


    override fun onBindViewHolder(vh: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        vh.mAppGlideProgressFactory = AppGlideProgressFactory.createGlideProgressListener(item.mComic.mCover) { _, _, percentage, _, _ ->
            vh.rvBinding.bookshelfRvProgressText.text = AppGlideProgressFactory.getProgressString(percentage)
        }

        Glide.with(vh.itemView.context)
            .load(item.mComic.mCover)
            .listener(vh.mAppGlideProgressFactory?.getRequestListener())
            .transition(mGenericTransitionOptions.transition { _ ->
                vh.rvBinding.bookshelfRvImage.animateFadeIn()
                vh.rvBinding.bookshelfRvLoading.animateFadeOut().withEndAction { vh.rvBinding.bookshelfRvLoading.alpha = 1f }
                vh.rvBinding.bookshelfRvProgressText.animateFadeOut().withEndAction { vh.rvBinding.bookshelfRvProgressText.alpha = 1f }
            })
            .into(vh.rvBinding.bookshelfRvImage)
        vh.rvBinding.bookshelfRvName.text = item.mComic.mName
        vh.rvBinding.bookshelfRvTime.text = item.mComic.mDatetimeUpdated
    }
}