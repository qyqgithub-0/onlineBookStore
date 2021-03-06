package com.onlinebookstore.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.onlinebookstore.common.CommonplaceResult;
import com.onlinebookstore.entity.bookserver.Book;
import com.onlinebookstore.entity.bookserver.BookResource;
import com.onlinebookstore.mapper.BookMapper;
import com.onlinebookstore.mapper.BookResourceMapper;
import com.onlinebookstore.mapper.BookStorageMapper;
import com.onlinebookstore.service.BookService;
import com.onlinebookstore.util.RandomUtils;
import com.onlinebookstore.util.RedisUtils;
import com.onlinebookstore.util.bookutil.BookConstantPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.onlinebookstore.util.bookutil.BookConstantPool.*;

/**
 * @author rkc
 * @date 2020/9/18 16:29
 * @version 1.0
 */
@Slf4j
@Service
public class BookServiceImpl implements BookService {

    @Resource
    private BookMapper bookMapper;

    @Resource
    private BookResourceMapper bookResourceMapper;

    @Resource
    private BookStorageMapper bookStorageMapper;

    @Resource
    private RandomUtils randomUtils;

    @Resource
    private RedisUtils redisUtils;
    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(15, 30,
            5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20));

    /**
     * 查询图书+库存+类型
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult selectBookAndType() {
        return CommonplaceResult.buildSuccessNoMessage(bookMapper.selectBookAndType());
    }

    /**
     * 根据类型查询图书
     * @see BookServiceImpl#selectAllBookWithResource
     * @param typeId 类型id
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult selectAllBookWithResourceByType(int typeId) {
        Object o = redisUtils.get(SELECT_ALL_BOOK_WITH_RESOURCE_BY_TYPE + typeId);
        if (!ObjectUtils.isEmpty(o)) {
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        List<Book> books = bookMapper.selectAllBookWithResourceByType(typeId);
        if (!ObjectUtils.isEmpty(books) && books.size() > 0) {
            poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_WITH_RESOURCE_BY_TYPE + typeId, books, BookConstantPool.CACHE_TIME[1]));
            return CommonplaceResult.buildSuccessNoMessage(books);
        }
        return CommonplaceResult.buildErrorNoData("数据异常！");
    }


    /**
     * 根据类型查询图书（分页）
     * @param typeId 类型id
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult selectBookAndStorageByTypePage(int typeId, int currentPage, int pageSize) {
        Object o = redisUtils.get(SELECT_BOOK_AND_STORAGE_BY_TYPE_PAGE + ":typeId:" + typeId + ":currentPage:" + currentPage + ":pageSize:" + pageSize);
        if (!ObjectUtils.isEmpty(o)) {
            log.info("selectBookAndStorageByTypePage缓存分页查询，typeId: + " + typeId + " currentPage：" + currentPage + " pageSize：" + pageSize);
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        PageHelper.startPage(currentPage, pageSize);
        List<Book> books = bookMapper.selectBookAndStorageByType(typeId);
        PageInfo<Book> pageInfo = new PageInfo<>(books);
        if (pageInfo.getPages() < currentPage) return CommonplaceResult.buildErrorNoData("数据查询完毕！");
        poolExecutor.execute(() -> redisUtils.set(SELECT_BOOK_AND_STORAGE_BY_TYPE_PAGE + ":typeId:" + typeId + ":currentPage:" + currentPage + ":pageSize:" + pageSize, books, CACHE_TIME[4]));
        log.info("selectBookAndStorageByTypePage数据库分页查询，typeId:" + typeId + " currentPage：" + currentPage + " pageSize：" + pageSize);
        return CommonplaceResult.buildSuccessNoMessage(pageInfo.getList());
    }

    /**
     * 根据类型查询图书
     * @see BookServiceImpl#selectAllBookInfo()
     * @param typeId 类型id
     * @return BookConstantPool.CACHE_TIME[1];
     */
    @Override
    @Deprecated
    public CommonplaceResult selectAllBookInfoByType(int typeId) {
        Object o = redisUtils.get(SELECT_ALL_BOOK_INFO_TYPE + typeId);
        if (!ObjectUtils.isEmpty(o)) {
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        List<Book> books = bookMapper.selectBookAndStorageByType(typeId);
        if (!ObjectUtils.isEmpty(books) && books.size() > 0) {
            poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_INFO_TYPE + typeId, books, BookConstantPool.CACHE_TIME[1]));
            return CommonplaceResult.buildSuccessNoMessage(books);
        }
        return CommonplaceResult.buildError(books, "没有该类型的商品！");
    }

    /**
     * @see BookServiceImpl#selectAllBookInfo 方法的模糊查询
     * @param str 查询字符串
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult selectAllBookInfoLike(String str) {
        Object o = redisUtils.get(SELECT_ALL_BOOK_INFO_LIKE + str);
        if (!ObjectUtils.isEmpty(o)) {
            //不为空说明缓存有数据，直接把redis中取出的数据返回
            log.info("通过缓存读取的数据");
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        List<Book> books = bookMapper.selectAllBookInfoLike(str);
        log.info("通过数据库读取的数据");
        //加入redis缓存，时间1分钟+随机时间（秒）
        long cacheTime = BookConstantPool.CACHE_TIME[1];
        poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_INFO_LIKE + str, books, cacheTime + randomUtils.getInt(100)));
        return CommonplaceResult.buildSuccessNoMessage(books);
    }

    /**
     * @see BookServiceImpl#selectAllBookWithResource 方法的模糊查询
     * @param str 查询字符串
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult selectAllBookWithResourceLike(String str) {
        Object o = redisUtils.get(SELECT_ALL_BOOK_WITH_RESOURCE_LIKE + str);
        if (!ObjectUtils.isEmpty(o)) {
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        List<Book> books = bookMapper.selectAllBookWithResourceLike(str);
        if (!ObjectUtils.isEmpty(books) && books.size() > 0) {
            poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_WITH_RESOURCE_LIKE + str, books, BookConstantPool.CACHE_TIME[1]));
            return CommonplaceResult.buildSuccessNoMessage(books);
        }
        return CommonplaceResult.buildError(books, "没有匹配的数据！");
    }

    /**
     * 根据id列表查询Book集合
     * @param ids id列表
     * @return Book集合
     */
    @Override
    public CommonplaceResult selectBookByIds(List<Integer> ids) {
        if (ids.size() == 0) {
            return CommonplaceResult.buildSuccessNoMessage(new ArrayList<Book>());
        }
        List<Book> books = bookMapper.selectBookByIds(ids);
        return CommonplaceResult.buildSuccessNoMessage(books);
    }

    /**
     * 添加图书、资源和库存
     * @param book book
     * @return CommonplaceResult
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonplaceResult addCompleteBook(Book book) {
        book.setCreateTime(new Date());
        try {
            log.info(String.valueOf(book));
            //添加图书
            bookMapper.addBook(book);
            if (!ObjectUtils.isEmpty(book.getBookResources())) {
                for (BookResource bookResource : book.getBookResources()) {
                    bookResource.setBookId(book.getId());
                    bookResource.setCreateTime(new Date());
                }
                //添加资源
                bookResourceMapper.addBookResources(book.getBookResources());
            }
            //添加库存
            book.getBookStorage().setBookId(book.getId());
            book.getBookStorage().setLastAddTime(new Date());
            bookStorageMapper.addBookStorage(book.getBookStorage());
            return CommonplaceResult.buildSuccessNoData("添加成功！");
        } catch (Exception e) {
            log.error(e.getMessage());
            //抛出异常，让切面能够感知，进行事务回滚
            throw e;
        }
    }

    /**
     * 查询所有图书信息（缓存时间：60s）
     * @return 图书信息
     */
    @Override
    public CommonplaceResult selectAllBookAlone() {
        Object o = redisUtils.get(SELECT_ALL_BOOK_ALONE);
        if (!ObjectUtils.isEmpty(o)) {
            log.info("通过缓存读取的数据");
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        //缓存中不存在数据，从数据库中读取并且加入缓存
        List<Book> books = bookMapper.selectAllBookAlone();
        if (books.size() == 0) {
            return CommonplaceResult.buildErrorNoData("数据异常！");
        }
        log.info("通过数据库读取的数据");
        //加入redis缓存
        poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_ALONE, books, BookConstantPool.CACHE_TIME[1]));
        return CommonplaceResult.buildSuccessNoMessage(books);
    }

    /**
     * 查询所有图书信息 分页查询
     * @return 图书信息+库存信息
     */
    @Override
    public CommonplaceResult selectBookAndStoragePage(int currentPage, int pageSize) {
        Object o = redisUtils.get(SELECT_BOOK_AND_STORAGE_PAGE + ":currentPage:" + currentPage + ":pageSize:" + pageSize);
        if (!ObjectUtils.isEmpty(o)) {
            log.info("selectBookAndStoragePage缓存分页查询，currentPage：" + currentPage + " pageSize：" + pageSize);
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        PageHelper.startPage(currentPage, pageSize);
        List<Book> books = bookMapper.selectBookAndStorage();
        PageInfo<Book> pageInfo = new PageInfo<>(books);
        if (pageInfo.getPages() < currentPage) return CommonplaceResult.buildErrorNoData("数据查询完毕！");
        log.info("selectBookAndStoragePage数据库分页查询，currentPage：" + currentPage + " pageSize：" + pageSize);
        //加入缓存
        poolExecutor.execute(() -> redisUtils.set(SELECT_BOOK_AND_STORAGE_PAGE + ":currentPage:" + currentPage + ":pageSize:" + pageSize, books, CACHE_TIME[4]));
        return CommonplaceResult.buildSuccessNoMessage(pageInfo.getList());
    }


    /**
     * 查询所有图书信息（缓存时间：[60 + random.nextInt(100)] s）
     * @return 图书信息+库存信息
     * @deprecated 没有使用分页查询 BookServiceImpl#selectAllBookInfoPage代替
     */
    @Override
    @Deprecated
    public CommonplaceResult selectAllBookInfo() {
        Object o = redisUtils.get(SELECT_ALL_BOOK_INFO);
        if (!ObjectUtils.isEmpty(o)) {
            //不为空说明缓存有数据，直接把redis中取出的数据返回
            log.info("通过缓存读取的数据");
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        List<Book> books = bookMapper.selectBookAndStorage();
        if (ObjectUtils.isEmpty(books) || books.size() == 0) {
            return CommonplaceResult.buildErrorNoData("数据异常！");
        }
        log.info("通过数据库读取的数据");
        //加入redis缓存，时间1分钟+随机时间（秒）
        long cacheTime = BookConstantPool.CACHE_TIME[1];
        poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_INFO, books, cacheTime + randomUtils.getInt(100)));
        return CommonplaceResult.buildSuccessNoMessage(books);
    }

    /**
     * 查询所有图书信息（缓存时间：60s）
     * @return 图书信息+资源信息
     */
    @Override
    @Deprecated
    public CommonplaceResult selectAllBookWithResource() {
        Object o = redisUtils.get(SELECT_ALL_BOOK_WITH_RESOURCE);
        if (!ObjectUtils.isEmpty(o)) {
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        //查询数据库加入缓存
        List<Book> books = bookMapper.selectAllBookWithResource();
        if (!ObjectUtils.isEmpty(books) && books.size() > 0) {
            poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_WITH_RESOURCE, books, BookConstantPool.CACHE_TIME[1]));
            return CommonplaceResult.buildSuccessNoMessage(books);
        }
        return CommonplaceResult.buildErrorNoData("数据异常！");
    }

    /**
     * 查询所有图书信息
     * @return 图书信息+库存信息
     */
    @Override
    public CommonplaceResult selectAllBookWithStorage() {
        List<Book> books = bookMapper.selectAllBookWithStorage();
        return books.size() == 0 ? CommonplaceResult.buildErrorNoData("数据异常！") : CommonplaceResult.buildSuccessNoMessage(books);
    }

    /**
     * 查询图书所有信息（缓存时间：60s）
     * @param bookId id
     * @return 图书信息
     */
    @Override
    public CommonplaceResult selectAllBookAloneById(Integer bookId) {
        //查缓存
        Object o = redisUtils.get(SELECT_ALL_BOOK_ALONE_BY_ID + bookId);
        //缓存不存在数据就进行查询数据库并且加入到缓存中
        if (ObjectUtils.isEmpty(o)) {
            Book book = bookMapper.selectAllBookAloneById(bookId);
            //数据不存在
            if (ObjectUtils.isEmpty(book)) {
                return CommonplaceResult.buildErrorNoData("没有该数据！");
            }
            //加入缓存
            poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_ALONE_BY_ID + bookId, book, BookConstantPool.CACHE_TIME[1]));
            return CommonplaceResult.buildSuccessNoMessage(book);
        }
        //缓存有数据，直接返回
        return CommonplaceResult.buildSuccessNoMessage(o);
    }

    /**
     * 根据图书id查询图书的所有信息（缓存时间：60s）
     * @param bookId id
     * @return 图书信息+库存信息+资源信息
     */
    @Override
    public CommonplaceResult selectAllBookInfoByBookId(Integer bookId) {
        Object o = redisUtils.get(SELECT_ALL_BOOK_INFO_BY_BOOK_ID + bookId);
        if (!ObjectUtils.isEmpty(o)) return CommonplaceResult.buildSuccessNoMessage(o);
        Book book = bookMapper.selectAllBookInfoByBookId(bookId);
        log.info(book.getBookResources().toString());
        if (!ObjectUtils.isEmpty(book)) {
            poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_INFO_BY_BOOK_ID + bookId, book, BookConstantPool.CACHE_TIME[1]));
            return CommonplaceResult.buildSuccessNoMessage(book);
        }
        return CommonplaceResult.buildErrorNoData("没有该数据！");
    }

    /**
     * 查询图书信息+资源信息（缓存时间：60s）
     * @param bookId 图书id
     * @return 图书信息+资源信息
     */
    @Override
    public CommonplaceResult selectAllBookWithResourceByBookId(Integer bookId) {
        Object o = redisUtils.get(SELECT_ALL_BOOK_WITH_RESOURCE_BY_BOOK_ID + bookId);
        if (!ObjectUtils.isEmpty(o)) {
            return CommonplaceResult.buildSuccessNoMessage(o);
        }
        Book book = bookMapper.selectAllBookWithResourceByBookId(bookId);
        if (ObjectUtils.isEmpty(book)) {
            return CommonplaceResult.buildErrorNoData("没有该数据");
        }
        poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_WITH_RESOURCE_BY_BOOK_ID + bookId, book, BookConstantPool.CACHE_TIME[1]));
        return CommonplaceResult.buildSuccessNoMessage(book);
    }

    /**
     * 查询图书信息+库存信息，缓存时间：1分钟
     * @param bookId 图书id
     * @return 图书信息+库存信息
     */
    @Override
    public CommonplaceResult selectAllBookWithStorageByBookId(Integer bookId) {
        Object o = redisUtils.get(SELECT_ALL_BOOK_WITH_STORAGE_BY_BOOK_ID + bookId);
        if (ObjectUtils.isEmpty(o)) {
            Book book = bookMapper.selectAllBookWithStorageByBookId(bookId);
            if (ObjectUtils.isEmpty(book)) {
                return CommonplaceResult.buildErrorNoData("查询失败，没有该数据！");
            }
            poolExecutor.execute(() -> redisUtils.set(SELECT_ALL_BOOK_WITH_STORAGE_BY_BOOK_ID + bookId, book, BookConstantPool.CACHE_TIME[1]));
            return CommonplaceResult.buildSuccessNoMessage(book);
        }
        return CommonplaceResult.buildSuccessNoMessage(o);
    }

    /**
     * 更新图书信息
     * @param book book
     * @return 影响行数
     */
    @Override
    public CommonplaceResult updateBook(Book book) {
        //延迟双删策略
        delRelativeCache(book);
        int row = bookMapper.updateBook(book);
        if (row > 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            delRelativeCache(book);
            return CommonplaceResult.buildSuccessNoData("更新成功！");
        }
        return CommonplaceResult.buildErrorNoData("更新失败！");
    }

    private void delRelativeCache(Book book) {
        redisUtils.del(SELECT_ALL_BOOK_ALONE, SELECT_ALL_BOOK_INFO, SELECT_ALL_BOOK_INFO_TYPE + book.getTypeId(),
                SELECT_ALL_BOOK_ALONE_BY_ID + book.getId(), SELECT_ALL_BOOK_WITH_RESOURCE, SELECT_ALL_BOOK_WITH_RESOURCE_BY_TYPE + book.getTypeId(),
                SELECT_ALL_BOOK_INFO_BY_BOOK_ID + book.getId(), SELECT_ALL_BOOK_WITH_RESOURCE_BY_BOOK_ID + book.getId(),
                SELECT_ALL_BOOK_WITH_STORAGE_BY_BOOK_ID + book.getId());
        log.info("BookServiceImpl删除缓存");
    }

    /**
     * 根据id删除图书（会级联删除到库存和资源，谨慎调用）
     * @param bookId 图书id
     * @return 影响行数
     */
    @Override
    public CommonplaceResult deleteBookById(Integer bookId) {
        int row = bookMapper.deleteBookById(bookId);
        return row == 0 ? CommonplaceResult.buildErrorNoData("删除失败！") : CommonplaceResult.buildSuccessNoData("删除成功！");
    }
}
