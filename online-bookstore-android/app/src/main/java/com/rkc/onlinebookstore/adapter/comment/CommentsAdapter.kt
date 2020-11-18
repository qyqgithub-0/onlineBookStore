package com.rkc.onlinebookstore.adapter.comment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rkc.onlinebookstore.R
import com.rkc.onlinebookstore.model.user.Comment
import kotlinx.android.synthetic.main.comment_item.view.*

/**
 * @author rkc
 * @date 2020/11/17 17:14
 * @version 1.0
 */
class CommentsAdapter : ListAdapter<Comment, CommentsViewHolder>(DiffCallBack) {

    object DiffCallBack : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id == newItem.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        return CommentsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false))
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val comment = getItem(position) ?: return
        with(holder.itemView) {
            usernameTV.text = comment.account.user.nickname
            publishTimeTV.text = comment.createTime.toString()
            commentContentTV.text = comment.content
        }
    }
}

class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)