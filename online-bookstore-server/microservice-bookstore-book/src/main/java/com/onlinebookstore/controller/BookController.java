package com.onlinebookstore.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.onlinebookstore.annotation.JsonObject;
import com.onlinebookstore.common.CommonplaceResult;
import com.onlinebookstore.entity.bookserver.*;
import com.onlinebookstore.handler.BookBlockHandler;
import com.onlinebookstore.service.*;
import com.onlinebookstore.service.impl.BookServiceImpl;
import com.onlinebookstore.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 图书微服务的接口，主要给用户微服务和前端直接申请资源
 * 当其他微服务调用该接口的时候，调用方和被调用方的参数都需要保持一致，
 * 两方的请求必须保持一致
 * 例：1、被调用方是 @RequestMapping(value = "xxx", method = RequestMethod.POST)
 *     则调用方的方法也得 @RequestMapping(value = "xxx", method = RequestMethod.POST)保持一致
 *     2、被调用方是：@RequestMapping(value = "pub/selectBookAndStorageByBookId/{bookId}", method = RequestMethod.POST)
 *                   public CommonplaceResult selectBookAndStorageByBookId(@PathVariable("bookId") Integer bookId) { }
 *        调用方也需要保持一致：@RequestMapping(value = "/api/v1/book/pub/selectBookAndStorageByBookId/{bookId}")
 *                            CommonplaceResult selectBookAndStorageByBookId(@PathVariable("bookId") Integer bookId);
 *        微服务调用直接传参：bookService.selectBookAndStorageByBookId(bookId)
 * @author rkc
 * @date 2020/9/18 16:30
 * @version 1.0
 */
@Slf4j
@RefreshScope
@RestController
@RequestMapping("/api/v1/book")
public class BookController {

    @Resource
    private BookService bookService;

    @Resource
    private BookStorageService bookStorageService;

    @Resource
    private BookBannerService bookBannerService;

    @Resource
    private BookResourceService bookResourceService;

    @Resource
    private BookTypeService bookTypeService;

    /**
     * 更新图书信息
     * @param book 图书
     * @return CommonplaceResult
     */
    @PostMapping("pri/updateBook")
    public CommonplaceResult updateBook(@RequestBody Book book) {
        return bookService.updateBook(book);
    }

    /**
     * 根据id列表查询Book集合
     * @param ids id列表
     * @return Book集合
     */
    @PostMapping("pri/selectBookByIds")
    @SentinelResource(value = "selectBookByIds", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectBookByIds")
    public CommonplaceResult selectBookByIds(@RequestBody List<Integer> ids) {
        return bookService.selectBookByIds(ids);
    }

    /**
     * 添加图书
     * @param book book
     * @return CommonplaceResult
     */
    @PostMapping("pri/addBook")
    public CommonplaceResult addCompleteBook(@RequestBody Book book) {
        if (ObjectUtils.isEmpty(book) || ObjectUtils.isEmpty(book.getBookStorage())) {
            return CommonplaceResult.buildErrorNoData("数据不全！添加失败");
        }
        log.info(book.toString());
        return bookService.addCompleteBook(book);
    }

    /**
     * 查询所有类型和对应的图书
     * @return CommonplaceResult
     */
    @GetMapping("pub/selectAllTypeWithBook")
    @SentinelResource(value = "selectAllTypeWithBook", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllTypeWithBook")
    public CommonplaceResult selectAllTypeWithBook() {
        return bookTypeService.selectAllWithBook();
    }

    /**
     * 新加图书类型
     * @param bookType 图书类型
     * @return 影响行数
     */
    @PostMapping("pri/insertType")
    public CommonplaceResult insertType(@RequestBody BookType bookType) {
        return bookTypeService.insertType(bookType);
    }

    /**
     * 更新类型
     * 数据格式：
     * {
     *     'type': 'xxx',
     *     'supplement': 'xxx',
     *     'img': 'xxx',
     *     'id': 'xx'
     * }
     * @param bookType 类型
     * @return 影响行数
     */
    @PostMapping("pri/updateType")
    public CommonplaceResult updateType(@RequestBody BookType bookType) {
        return bookTypeService.updateType(bookType);
    }

    /**
     * 根据类型id查询类型
     * @param id id
     * @return 类型信息
     */
    @GetMapping("pub/selectTypeById/{id}")
    public CommonplaceResult selectTypeById(@PathVariable("id") Integer id) {
        return bookTypeService.selectTypeById(id);
    }

    /**
     * 查询所有图书类型
     * @return 图书类型
     */
    @GetMapping("pub/selectAllType")
    @SentinelResource(value = "selectAllType", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllType")
    public CommonplaceResult selectAllType() {
        return bookTypeService.selectAllType();
    }

    /**
     * 查询所有资源信息
     * @return 资源信息集合
     */
    @GetMapping("pub/selectAllResourceAlone")
    public CommonplaceResult selectAllResourceAlone() {
        return bookResourceService.selectAllResourceAlone();
    }

    /**
     * 根据图书的id查询其所有的资源对象
     * @param bookId 图书id
     * @return 指定图书下的所有资源
     */
    @GetMapping("pub/selectAllResourceAloneByBookId/{bookId}")
    @SentinelResource(value = "selectAllResourceAloneByBookId", blockHandlerClass = BookBlockHandler.class,
            blockHandler = "handleSelectAllResourceAloneByBookId")
    public CommonplaceResult selectAllResourceAloneByBookId(@PathVariable("bookId") Integer bookId) {
        return bookResourceService.selectAllResourceAloneByBookId(bookId);
    }

    /**
     * 根据资源id查询资源对象
     * @param resourceId 资源id
     * @return 资源对象
     */
    @GetMapping("pub/selectResourceById/{id}")
    public CommonplaceResult selectResourceById(@PathVariable("id") Integer resourceId) {
        return bookResourceService.selectResourceById(resourceId);
    }

    /**
     * 更新资源信息，通常由管理员操作
     * 数据格式：
     * {
     *     'id': 'xx',
     *     'resourceUrl': 'xxx',
     *     'symbol': 'xxx',
     *     'supplement': 'xxx'
     * }
     * @param bookResource 资源对象
     * @return 影响行数
     */
    @PostMapping("pri/updateResource")
    public CommonplaceResult updateResource(@RequestBody BookResource bookResource) {
        return bookResourceService.updateResource(bookResource);
    }

    /**
     * 获取所有Banner
     */
    @GetMapping("pub/selectAllBanner")
    @SentinelResource(value = "selectAllBanner", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllBanner")
    public CommonplaceResult selectAllBanner() {
        return bookBannerService.selectAll();
    }

    /**
     * 查询最新的count条记录
     * @param count 数量
     */
    @GetMapping("pub/selectCountBanner/{count}")
    public CommonplaceResult selectCountBanner(@PathVariable("count") Integer count) {
        return bookBannerService.selectCount(count);
    }

    /**
     * 刷新缓存
     */
    @PostMapping("pri/refreshCache")
    public CommonplaceResult refreshCache() {
        return bookBannerService.refreshCache();
    }

    /**
     * 添加Banner
     * @param bookBanner banner数据对象
     */
    @PostMapping("pri/insertBookBanner")
    public CommonplaceResult insertBookBanner(@RequestBody BookBanner bookBanner) {
        bookBanner.setModifyTime(new Date());
        return bookBannerService.insertBookBanner(bookBanner);
    }

    /**
     * 根据url删除banner
     * @param url url
     */
    @PostMapping("pri/deleteBookBannerByUrl")
    public CommonplaceResult deleteBookBannerByUrl(@JsonObject("url") String url) {
        if (ObjectUtils.isEmpty(url)) {
            return CommonplaceResult.buildErrorNoData("数据错误！");
        }
        return bookBannerService.deleteBookBannerByUrl(url);
    }

    /**
     * 根据类型查询图书的接口（分页）
     * @param id 类型id
     * @return CommonplaceResult
     */
    @GetMapping("pub/selectBookAndStorageByTypePage/{typeId}/{currentPage}/{pageSize}")
    @SentinelResource(value = "selectBookAndStorageByTypePage", blockHandlerClass = BookBlockHandler.class,
            blockHandler = "handleSelectBookAndStorageByTypePage")
    public CommonplaceResult selectBookAndStorageByTypePage(@PathVariable("typeId") int id, @PathVariable("currentPage") int currentPage
            , @PathVariable("pageSize") int pageSize) {
        return bookService.selectBookAndStorageByTypePage(id, currentPage, pageSize);
    }

    /**
     * 根据类型查询图书的接口
     * @param id 类型id
     * @return CommonplaceResult
     */
    @GetMapping("pub/selectAllInfoByType/{typeId}")
    @SentinelResource(value = "selectAllInfoByType", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllInfoByType")
    public CommonplaceResult selectAllInfoByType(@PathVariable("typeId") int id) {
        return bookService.selectAllBookInfoByType(id);
    }

    /**
     * 得到所有图书的所有信息，包括库存和资源信息
     * @param currentPage 当前页
     * @param pageSize 每页大小
     */
    @GetMapping("pub/selectBookAndStoragePage/{currentPage}/{pageSize}")
    @SentinelResource(value = "selectBookAndStoragePage", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectBookAndStoragePage")
    public CommonplaceResult selectBookAndStoragePage(@PathVariable("currentPage") int currentPage, @PathVariable("pageSize") int pageSize) {
        return bookService.selectBookAndStoragePage(currentPage, pageSize);
    }

    /**
     * 得到所有图书的所有信息，包括库存和资源信息
     */
    @GetMapping("pub/selectAllInfo")
    @SentinelResource(value = "selectAllInfo", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllInfo")
    public CommonplaceResult selectAllInfo() {
        return bookService.selectAllBookInfo();
    }

    /**
     * 模糊查询：得到所有图书的所有信息，包括库存和资源信息
     */
    @PostMapping("pub/selectAllInfoLike")
    @SentinelResource(value = "selectAllInfoLike", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllInfoLike")
    public CommonplaceResult selectAllInfoLike(@RequestBody Book book) {
        if (book.getBookName().length() == 0) {
            return bookService.selectAllBookInfo();
        }
        return bookService.selectAllBookInfoLike(book.getBookName());
    }

    /**
     * 得到所有图书+类型+库存
     * @return CommonplaceResult
     */
    @GetMapping("pri/selectBookAndType")
    @SentinelResource(value = "selectBookAndType", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectBookAndType")
    public CommonplaceResult selectBookAndType() {
        return bookService.selectBookAndType();
    }

    /**
     * 得到所有图书的信息，包括图书信息和库存信息
     */
    @GetMapping("pub/selectAllBookAndStorage")
    @SentinelResource(value = "selectAllBookAndStorage", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllBookAndStorage")
    public CommonplaceResult selectAllBookAndStorage() {
        return bookService.selectAllBookWithStorage();
    }

    /**
     * 根据类型查询图书
     * @param id 类型id
     * @return CommonplaceResult
     */
    @GetMapping("pub/selectAllBookWithResourceByType/{typeId}")
    @SentinelResource(value = "selectAllBookWithResourceByType", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllBookWithResourceByType")
    public CommonplaceResult selectAllBookWithResourceByType(@PathVariable("typeId") int id) {
        return bookService.selectAllBookWithResourceByType(id);
    }

    /**
     * 得到所有的图书信息，包括图书信息和资源信息
     */
    @Deprecated
    @GetMapping("pub/selectBookAndResource")
    public CommonplaceResult selectBookAndResource() {
        return bookService.selectAllBookWithResource();
    }

    /**
     * 模糊查询：得到所有的图书信息，包括图书信息和资源信息
     */
    @PostMapping("pub/selectBookAndResourceLike")
    @SentinelResource(value = "selectBookAndResourceLike", blockHandlerClass = BookBlockHandler.class,
            blockHandler = "handleSelectBookAndResourceLike")
    public CommonplaceResult selectBookAndResourceLike(@RequestBody Book book) {
        if (book.getBookName().length() == 0) {
            return bookService.selectAllBookWithResource();
        }
        return bookService.selectAllBookWithResourceLike(book.getBookName());
    }

    /**
     * 根据图书id得到图书信息和库存信息
     * @param bookId 图书id
     */
    @GetMapping(value = "pub/selectBookAndStorageByBookId/{bookId}")
    @SentinelResource(value = "selectBookAndStorageByBookId", blockHandlerClass = BookBlockHandler.class,
            blockHandler = "handleSelectBookAndStorageByBookId")
    public CommonplaceResult selectBookAndStorageByBookId(@PathVariable("bookId") Integer bookId) {
        return bookService.selectAllBookWithStorageByBookId(bookId);
    }

    /**
     * 根据图书id得到图书信息和资源信息
     * @param bookId 图书id
     */
    @GetMapping("pub/selectBookAndResourceByBookId/{bookId}")
    @SentinelResource(value = "selectBookAndResourceByBookId", blockHandlerClass = BookBlockHandler.class,
            blockHandler = "handleSelectBookAndResourceByBookId")
    public CommonplaceResult selectBookAndResourceByBookId(@PathVariable("bookId") Integer bookId) {
        return bookService.selectAllBookWithResourceByBookId(bookId);
    }

    /**
     * 根据图书id查询信息
     * @param bookId 图书id
     * @return 只包含图书的基本信息
     */
    @GetMapping(value = "pub/selectAllBookAloneById/{bookId}")
    @SentinelResource(value = "selectAllBookAloneById", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectAllBookAloneById")
    public CommonplaceResult selectAllBookAloneById(@PathVariable("bookId") Integer bookId) {
        return bookService.selectAllBookAloneById(bookId);
    }

    /**
     * 根据图书id查询图书信息+资源信息+库存信息
     * @param bookId 图书id
     */
    @GetMapping("pub/selectBookContainAllInfoById/{bookId}")
    @SentinelResource(value = "selectBookContainAllInfoById", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectBookContainAllInfoById")
    public CommonplaceResult selectBookContainAllInfoById(@PathVariable("bookId") Integer bookId) {
        return bookService.selectAllBookInfoByBookId(bookId);
    }

    /**
     * 根据图书id得到库存信息
     * @param bookId 图书id
     */
    @GetMapping("pub/selectStorageByBookId/{bookId}")
    @SentinelResource(value = "selectStorageByBookId", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSelectStorageByBookId")
    public CommonplaceResult selectStorageByBookId(@PathVariable("bookId") Integer bookId) {
        return bookStorageService.selectStorageByBookId(bookId);
    }

    /**
     * 根据id查询库存
     * @param id id
     */
    @GetMapping("pub/selectStorageById/{id}")
    public CommonplaceResult selectStorageById(@PathVariable("id") Integer id) {
        return bookStorageService.selectStorageById(id);
    }

    /**
     * 修改库存信息
     * @param bookStorage bookStorage
     * @return CommonplaceResult
     */
    @PostMapping("pri/updateBookStorage")
    @SentinelResource(value = "updateBookStorage", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleUpdateBookStorage")
    public CommonplaceResult updateBookStorage(@RequestBody BookStorage bookStorage) {
        bookStorage.setLastAddTime(new Date());
        return bookStorageService.updateBookStorage(bookStorage);
    }

    /**
     * 根据id增加库存
     * @deprecated
     * @param pojo 包含id和count键值
     */
    @PostMapping("pri/addStorageById")
    @SentinelResource(value = "addStorageById", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleAddStorageById")
    public CommonplaceResult addStorageById(@RequestBody Map<String, Integer> pojo) {
        Integer id = pojo.get("id");
        Integer count = pojo.get("count");
        return bookStorageService.addStorageById(id, count);
    }

    /**
     * 根据id减少库存
     * 数据格式：
     * {
     *     'id': 'xx',
     *     'count': 'xxx'
     * }
     * @param pojo 包含id和count键值
     */
    @PostMapping("pri/subtractStorageById")
    @SentinelResource(value = "subtractStorageById", blockHandlerClass = BookBlockHandler.class, blockHandler = "handleSubtractStorageById")
    public CommonplaceResult subtractStorageById(@RequestBody Map<String, Integer> pojo) {
        Integer id = pojo.get("id");
        Integer count = pojo.get("count");
        return bookStorageService.subtractStorageById(id, count);
    }

    /**
     * 更新库存信息
     * @param bookStorage 新的库存信息
     */
    @PostMapping(value = "pri/updateStorage")
    public CommonplaceResult updateStorage(@RequestBody BookStorage bookStorage) {
        return bookStorageService.updateStorage(bookStorage);
    }

}
