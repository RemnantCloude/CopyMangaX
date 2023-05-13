package com.crow.module_book.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.crow.base.app.appContext
import com.crow.base.tools.extensions.DataStoreAgent
import com.crow.base.tools.extensions.appBookDataStore
import com.crow.base.tools.extensions.asyncDecode
import com.crow.base.tools.extensions.asyncEncode
import com.crow.base.tools.extensions.toJson
import com.crow.base.tools.extensions.toTypeEntity
import com.crow.base.ui.viewmodel.mvi.BaseMviViewModel
import com.crow.module_book.R
import com.crow.module_book.model.entity.BookChapterEntity
import com.crow.module_book.model.entity.BookChapterPairOf
import com.crow.module_book.model.intent.BookIntent
import com.crow.module_book.model.resp.ComicChapterResp
import com.crow.module_book.model.resp.ComicInfoResp
import com.crow.module_book.model.resp.ComicPageResp
import com.crow.module_book.model.resp.NovelChapterResp
import com.crow.module_book.model.resp.NovelInfoResp
import com.crow.module_book.network.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Path: module_comic/src/main/kotlin/com/crow/module_comic/ui/viewmodel
 * @Time: 2023/3/15 0:20
 * @Author: CrowForKotlin
 * @Description: ComicViewModel
 * @formatter:on
 **************************/
class BookInfoViewModel(val repository: BookRepository) : BaseMviViewModel<BookIntent>() {

    private var mChapterStartIndex = 0

    var mComicInfoPage: ComicInfoResp? = null
        private set

    var mComicChapterPage: ComicChapterResp? = null
        private set

    var mNovelChapterPage: NovelChapterResp? = null
        private set

    var mNovelInfoPage: NovelInfoResp? = null
        private set

    var mComicPage: ComicPageResp? = null
        private set

    var mUuid: String? = null
        private set

    private var _bookChapterEntity = MutableStateFlow<BookChapterEntity?>(null)
    val bookChapterEntity: StateFlow<BookChapterEntity?> get() = _bookChapterEntity

    init {
        viewModelScope.launch {
            _bookChapterEntity.value = appContext.appBookDataStore.asyncDecode(DataStoreAgent.DATA_BOOK).toTypeEntity<BookChapterEntity>() ?: return@launch
        }
    }


    fun updateBookChapter(bookName: String,  chapterName: String, type: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookChapterEntity = appContext.appBookDataStore.asyncDecode(DataStoreAgent.DATA_BOOK).toTypeEntity<BookChapterEntity>()
            if (bookChapterEntity == null) {
                appContext.appBookDataStore.asyncEncode(DataStoreAgent.DATA_BOOK, toJson(BookChapterEntity(mutableMapOf(bookName to BookChapterPairOf(chapterName, type))).also {
                    _bookChapterEntity.value = it
                }))
            }
            else {
                bookChapterEntity.datas[bookName] = BookChapterPairOf(chapterName, type)
                appContext.appBookDataStore.asyncEncode(DataStoreAgent.DATA_BOOK, toJson(bookChapterEntity.also {
                    _bookChapterEntity.value = it
                }))
            }
        }
    }

    override fun dispatcher(intent: BookIntent) {
        when (intent) {
            is BookIntent.GetComicInfoPage -> getComicInfoPage(intent)
            is BookIntent.GetComicChapter -> getComicChapter(intent)
            is BookIntent.GetComicPage -> getComicPage(intent)
            is BookIntent.GetComicBrowserHistory -> getComicBrowserHistory(intent)
            is BookIntent.GetNovelInfoPage -> getNovelInfoPage(intent)
            is BookIntent.GetNovelChapter -> getNovelChapter(intent)
            is BookIntent.GetNovelPage -> getNovelPage(intent)
            is BookIntent.GetNovelBrowserHistory -> getNovelBrowserHistory(intent)
            is BookIntent.GetNovelCatelogue -> getNovelCatelogue(intent)
            is BookIntent.AddComicToBookshelf -> addComicToBookshelf(intent)
            is BookIntent.AddNovelToBookshelf -> addNovelToBookshelf(intent)
        }
    }

    fun reCountPos(pos: Int) { mChapterStartIndex = pos * 100 }

    fun isNovelDatasIsNotNull(): Boolean = mNovelChapterPage != null && mNovelInfoPage != null

    fun isComicDatasIsNotNull(): Boolean = mComicChapterPage != null && mComicInfoPage != null

    private fun addNovelToBookshelf(intent: BookIntent.AddNovelToBookshelf) {
        flowResult(intent, repository.addNovelToBookshelf(intent.novelId, intent.isCollect)) { value -> intent.copy(baseResultResp = value) }
    }

    private fun addComicToBookshelf(intent: BookIntent.AddComicToBookshelf) {
        flowResult(intent, repository.addComicToBookshelf(intent.comicId, intent.isCollect)) { value -> intent.copy(baseResultResp = value) }
    }

    private fun getComicBrowserHistory(intent: BookIntent.GetComicBrowserHistory) {
        flowResult(intent, repository.getComicBrowserHistory(intent.pathword)) { value -> intent.copy(comicBrowser = value.mResults) }
    }

    private fun getComicInfoPage(intent: BookIntent.GetComicInfoPage) {
        flowResult(intent, repository.getComicInfo(intent.pathword)) { value ->
            mComicInfoPage = value.mResults
            mUuid = mComicInfoPage?.mComic?.mUuid
            intent.copy(comicInfo = value.mResults)
        }
    }

    private fun getComicChapter(intent: BookIntent.GetComicChapter) {
        flowResult(intent, repository.getComicChapter(intent.pathword, mChapterStartIndex, 100)) { value ->
            if (value.mCode == HttpURLConnection.HTTP_OK) {
                val comicChapterPage = toTypeEntity<ComicChapterResp>(value.mResults)
                mComicChapterPage = comicChapterPage
                intent.copy(comicChapter = comicChapterPage)
            }
            else {
                intent.copy(invalidResp = appContext.getString(R.string.BookComicRequestThrottled, Regex("\\d+").find(value.mMessage)?.value ?: "0"))
            }
        }
    }

    private fun getComicPage(intent: BookIntent.GetComicPage) {
        flowResult(intent, repository.getComicPage(intent.pathword, intent.uuid)) { value ->
            mComicPage = value.mResults
            intent.copy(comicPage = value.mResults)
        }
    }

    private fun getNovelInfoPage(intent: BookIntent.GetNovelInfoPage) {
        flowResult(intent, repository.getNovelInfo(intent.pathword)) { value ->
            mNovelInfoPage = value.mResults
            mUuid = mNovelInfoPage!!.mNovel.mUuid
            intent.copy(novelInfo = value.mResults)
        }
    }

    private fun getNovelChapter(intent: BookIntent.GetNovelChapter) {
        flowResult(intent, repository.getNovelChapter(intent.pathword)) { value ->
            if (value.mCode == HttpURLConnection.HTTP_OK) {
                val novelChapterResp = toTypeEntity<NovelChapterResp>(value.mResults)
                mNovelChapterPage = novelChapterResp
                intent.copy(novelChapter = novelChapterResp)
            } else {
                intent.copy(invalidResp = value.mMessage)
            }
        }
    }
    private fun getNovelBrowserHistory(intent: BookIntent.GetNovelBrowserHistory) {
        flowResult(intent, repository.getNovelBrowserHistory(intent.pathword)) { value ->
            intent.copy(novelBrowser = value.mResults)
        }
    }
    private fun getNovelPage(intent: BookIntent.GetNovelPage) {
        flowResult(intent, repository.getNovelPage(intent.pathword)) { value ->
            intent.copy(novelPage = value)
        }
    }

    private fun getNovelCatelogue(intent: BookIntent.GetNovelCatelogue) {
        flowResult(intent, repository.getNovelCatelogue(intent.pathword)) { value ->
            intent.copy(novelCatelogue = value.mResults)
        }
    }

    fun clearAllData() {
        mComicInfoPage = null
        mComicChapterPage = null
    }
}