package com.android.code.ui.sample

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.code.databinding.HolderSampleSearchDataBinding
import com.android.code.databinding.HolderSampleSearchRecentBinding

class SampleSearchAdapter(private val property: SampleSearchAdapterProperty) :
    ListAdapter<SearchData, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<SearchData>() {
            override fun areContentsTheSame(oldItem: SearchData, newItem: SearchData): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: SearchData, newItem: SearchData): Boolean {
                return true
            }
        }
    ) {
    companion object {
        private val restoredHolderMap = HashMap<String, Parcelable?>()
        private const val TYPE_BASE = 0
        private const val TYPE_RECENT = 1
    }

    init {
        restoredHolderMap.clear()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (itemCount <= holder.layoutPosition || RecyclerView.NO_POSITION == holder.layoutPosition) {
            return
        }
        if (holder is SampleRecentHolder) {
            val data = getItem(holder.layoutPosition) as? SearchRecentData ?: return
            restoredHolderMap[data.hashCode().toString()] =
                holder.binding.recyclerView.layoutManager?.onSaveInstanceState()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchBaseData -> TYPE_BASE
            is SearchRecentData -> TYPE_RECENT
            else -> throw IllegalArgumentException("Do not define Type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_BASE -> SampleSearchHolder(
                HolderSampleSearchDataBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            TYPE_RECENT -> SampleRecentHolder(
                HolderSampleSearchRecentBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
            else -> throw IllegalArgumentException("Do not define Type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SampleSearchHolder -> {
                holder.binding.apply {
                    property = this@SampleSearchAdapter.property
                    data = getItem(position) as? SearchBaseData
                    executePendingBindings()
                }
            }
            is SampleRecentHolder -> {
                holder.binding.apply {
                    holder.binding.recyclerView.layoutManager =
                        LinearLayoutManager(this.root.context, RecyclerView.HORIZONTAL, false)
                    val adapter =
                        SampleSearchRecentAdapter(object : SampleSearchRecentAdapterProperty {
                            override val searchedText: LiveData<String>
                                get() = property.searchedText

                            override fun search(text: String) {
                                property.search(text)
                            }

                            override fun removeRecentSearch(text: String) {
                                property.removeRecentSearch(text)
                                submitListAfterRemovedRecentSearch(text)
                            }

                            fun submitListAfterRemovedRecentSearch(text: String) {
                                val adapter =
                                    (holder.binding.recyclerView.adapter as? SampleSearchRecentAdapter)
                                        ?: return
                                val list = adapter.currentList.filter { it != text }
                                adapter.submitList(list)
                            }
                        })
                    holder.binding.recyclerView.adapter = adapter
                    val data = getItem(position) as? SearchRecentData
                    data.run {
                        adapter.submitList(this?.recentList)
                        restoredHolderMap[this.hashCode().toString()]?.let {
                            recyclerView.layoutManager?.onRestoreInstanceState(it)
                        }
                    }
                    executePendingBindings()
                }
            }

        }
    }

    private inner class SampleSearchHolder(val binding: HolderSampleSearchDataBinding) :
        RecyclerView.ViewHolder(binding.root)

    private inner class SampleRecentHolder(val binding: HolderSampleSearchRecentBinding) :
        RecyclerView.ViewHolder(binding.root)
}