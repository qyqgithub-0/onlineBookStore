package com.onlinebookstore.controller;

import com.onlinebookstore.common.CommonplaceResult;
import com.onlinebookstore.entity.userserver.Comment;
import com.onlinebookstore.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author rkc
 * @version 1.0
 * @date 2020/11/17 20:07
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/comment")
public class CommentController {
    @Resource
    private CommentService commentService;

    /**
     * 根据bookId查询评论
     * @param bookId bookId
     * @return CommonplaceResult
     */
    @GetMapping("selectCommentsByBookId/{bookId}")
    public CommonplaceResult selectCommentsByBookId(@PathVariable("bookId") Integer bookId) {
        return commentService.selectCommentsByBookId(bookId);
    }

    /**
     * 添加评论
     * @param comment 评论
     * @return CommonplaceResult
     */
    @PostMapping("insertComment")
    public CommonplaceResult insertComment(@RequestBody Comment comment) {
        comment.setCreateTime(new Date());
        return commentService.insertComment(comment);
    }
}
