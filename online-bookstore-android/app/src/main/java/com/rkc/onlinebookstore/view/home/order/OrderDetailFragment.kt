package com.rkc.onlinebookstore.view.home.order

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.gson.internal.bind.util.ISO8601Utils.format
import com.rkc.onlinebookstore.R
import com.rkc.onlinebookstore.adapter.home.BOOK_BUNDLE_KEY
import com.rkc.onlinebookstore.adapter.order.ORDER_KEY
import com.rkc.onlinebookstore.model.book.Book
import com.rkc.onlinebookstore.model.order.*
import com.rkc.onlinebookstore.view.home.home.DATE_FORMAT
import com.rkc.onlinebookstore.viewmodel.home.detail.ORDER_CREATED_KEY
import com.rkc.onlinebookstore.viewmodel.home.order.OrderDetailViewModel
import kotlinx.android.synthetic.main.common_title.*
import kotlinx.android.synthetic.main.fragment_order_detail.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailFragment : Fragment() {
    private lateinit var orderDetailViewModel: OrderDetailViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        titleTV.text = "我的订单"
        backIV.setOnClickListener { findNavController().navigateUp() }
        orderDetailViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application)).get(OrderDetailViewModel::class.java)
        arguments?.getParcelable<Order>(ORDER_KEY)?.let { orderDetailViewModel.setOrderLiveData(it) }
        orderDetailViewModel.fetchBook()
        orderDetailViewModel.bookLiveData.observe(viewLifecycleOwner, {
            orderDetailViewModel.orderLiveData.value?.let { it1 -> load(it1, orderDetailViewModel.bookLiveData.value!!) }
        })
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun load(order: Order, book: Book) {
        //加载book信息
        Glide.with(this).load(book.mainCover)
            .placeholder(R.drawable.book_placeholder)
            .into(orderImageViewMainCover)
        orderTextViewBookName.text = book.bookName
        orderTextViewAuthor.text = book.author
        orderTextViewPublishTime.text = SimpleDateFormat(DATE_FORMAT).format(book.createTime)
        orderTextViewISBN.text = book.isbn
        orderTextViewPublisher.text = book.publisher
        orderTextViewPrice.text = book.price.toString()
        //加载order信息
        orderTextViewSerial.text = order.serialNumber
        orderTextViewCreateTime.text = SimpleDateFormat(DATE_FORMAT).format(order.createTime)
        orderTextViewProductCount.text = "${order.productCount}本"
        signAddressTV.text = order.address
        contactPhoneTV.text = order.phone
        receiverNameTV.text = order.receiverName

        when (order.orderPaymentStatus) {
            ORDER_FINISHED -> {
                orderTextViewStatus.text = "已支付"
                orderTextViewStatus.setTextColor(Color.GREEN)
                if (order.sendStatus == UN_ACKNOWLEDGE || order.sendStatus == ACKNOWLEDGED) {
                    orderTextViewSendTime.text = SimpleDateFormat(DATE_FORMAT).format(order.deliveryTime)
                } else {
                    orderTextViewSendTime.text = "--"
                }
                orderTextViewScore.text = order.obtainScore.toString()
                orderTextViewPaymentTime.text = SimpleDateFormat(DATE_FORMAT).format(order.paymentTime)
                orderTextViewWhole.text = order.wholePrice.toString()
            }
            ORDER_EXPIRE -> {
                orderTextViewStatus.text = "已过期"
                orderTextViewStatus.setTextColor(Color.GRAY)
                orderTextViewSendTime.text = "--"
                orderTextViewScore.text = "-"
                orderTextViewPaymentTime.text = "--"
                orderTextViewWhole.text = "-"
            }
            ORDER_UN_PAYMENT -> {
                orderTextViewStatus.text = "未支付"
                orderTextViewStatus.setTextColor(Color.RED)
                orderTextViewSendTime.text = "--"
                orderTextViewScore.text = "-"
                orderTextViewPaymentTime.text = "--"
                orderTextViewWhole.text = "-"
                toUnpaidOrder.visibility = View.VISIBLE
                toUnpaidOrder.setOnClickListener {
                    //跳转到支付页面
                    Bundle().apply {
                        putParcelable(ORDER_CREATED_KEY, order)
                        putParcelable(BOOK_BUNDLE_KEY, book)
                        findNavController().navigate(R.id.action_orderDetailFragment_to_unpaidOrderFragment, this)
                    }
                }
                val calendar = Calendar.getInstance().apply {
                    time = order.createTime
                    add(Calendar.MINUTE, 5)
                }
                remindTV.visibility = View.VISIBLE
                remindTV.text = "订单过期时间：" + SimpleDateFormat(DATE_FORMAT).format(calendar.time)
            }
        }
        if (order.useScore == 1) {
            orderTextViewUseScore.text = "是"
        } else {
            orderTextViewUseScore.text = "否"
        }
        orderTextViewAccount.text = order.usernameId
    }
}