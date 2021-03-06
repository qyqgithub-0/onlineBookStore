package com.onlinebookstore.mapper;

import com.onlinebookstore.entity.bookserver.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author rkc
 * @date 2020/9/17 21:33
 * @version 1.0
 */
@Mapper
public interface BookMapper {

    /**
     * 得到图书+库存+类型
     * @return List<Book>
     */
    List<Book> selectBookAndType();

    /**
     * 根据类型查询所有图书信息（顺序为图书创建时间的倒序）
     * @return 图书信息+资源信息
     */
    List<Book> selectAllBookWithResourceByType(int typeId);

    /**
     * 根据图书类型查询列表
     * @param typeId 类型id
     * @return 图书+库存
     */
    List<Book> selectBookAndStorageByType(int typeId);

    /**
     * 模糊查询
     * @param str 查询字符串
     * @return 图书信息+资源信息
     */
    List<Book> selectAllBookWithResourceLike(String str);

    /**
     * 模糊查询
     * @param str 查询字符串
     * @return 图书信息+库存信息+资源信息
     */
    List<Book> selectAllBookInfoLike(String str);

    /**
     * 根据id列表查询Book集合
     * @param ids id列表
     * @return Book集合
     */
    List<Book> selectBookByIds(@Param("ids") List<Integer> ids);

    /**
     * 添加图书
     * @param book 图书
     * @return 影响行数
     */
    int addBook(Book book);

    /**
     * 查询所有图书信息
     * @return 图书信息
     */
    List<Book> selectAllBookAlone();

    /**
     * 查询所有图书信息（顺序为图书创建时间的倒序）
     * @return 图书信息+库存信息
     */
    List<Book> selectBookAndStorage();

    /**
     * 查询所有图书信息（顺序为图书创建时间的倒序）
     * @return 图书信息+资源信息
     */
    List<Book> selectAllBookWithResource();

    /**
     * 查询所有图书信息（顺序为图书创建时间的倒序）
     * @return 图书信息+库存信息
     */
    List<Book> selectAllBookWithStorage();

    /**
     * 查询所有图书信息
     * @param bookId id
     * @return 图书信息
     */
    Book selectAllBookAloneById(Integer bookId);

    /**
     * 根据图书id查询图书的所有信息
     * @param bookId id
     * @return 图书信息+库存信息+资源信息
     */
    Book selectAllBookInfoByBookId(Integer bookId);

    /**
     * 查询图书信息+资源信息
     * @param bookId 图书id
     * @return 图书信息+资源信息
     */
    Book selectAllBookWithResourceByBookId(Integer bookId);

    /**
     * 查询图书信息+库存信息
     * @param bookId 图书id
     * @return 图书信息+库存信息
     */
    Book selectAllBookWithStorageByBookId(Integer bookId);

    /**
     * 更新图书信息
     * @param book book
     * @return 影响行数
     */
    int updateBook(Book book);

    /**
     * 根据id删除图书（会级联删除到库存和资源，谨慎调用）
     * @param bookId 图书id
     * @return 影响行数
     */
    int deleteBookById(Integer bookId);
}
