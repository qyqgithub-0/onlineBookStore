package com.onlinebookstore.entity.bookserver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @author rkc
 * @date 2020/9/30 16:08
 * @version 1.0
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BookType implements Serializable {

    private static final long serialVersionUID = 1296533636978579980L;
    /**
     * 自增长id
     */
    private Integer id;

    /**
     * 类型名称
     */
    private String type;

    /**
     * 说明
     */
    private String supplement;

    /**
     * 图标
     */
    private String img;

    /**
     * 该分类下的图书
     */
    private List<Book> books;
}
