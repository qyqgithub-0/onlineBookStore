package com.onlinebookstore.service;

import com.onlinebookstore.common.CommonplaceResult;
import com.onlinebookstore.entity.bookserver.BookBanner;

/**
 * @author rkc
 * @date 2020/9/24 11:14
 * @version 1.0
 */
public interface BookBannerService {

    /**
     * 查询所有Banner
     */
    CommonplaceResult selectAll();

    /**
     * 查询最新的count条记录
     * @param count 数量
     */
    CommonplaceResult selectCount(Integer count);

    /**
     * 新增数据
     * @param bookBanner 实体类
     */
    CommonplaceResult insertBookBanner(BookBanner bookBanner);

    /**
     * 根据URL删除BookBanner
     * @param url url
     */
    CommonplaceResult deleteBookBannerByUrl(String url);

    /**
     * 刷新缓存
     */
    CommonplaceResult refreshCache();
}
